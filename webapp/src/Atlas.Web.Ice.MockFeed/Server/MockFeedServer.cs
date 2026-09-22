using System.Net;
using System.Net.WebSockets;
using System.Text;
using Atlas.Web.Ice.MockFeed.Configuration;
using Atlas.Web.Ice.MockFeed.Ticks;

// Atlas.Web.Ice.WebSocket is a namespace in the referenced project, and an
// enclosing namespace wins over the framework type name from inside
// Atlas.Web.Ice.MockFeed. Aliasing avoids qualifying every usage.
using WebSocketConnection = System.Net.WebSockets.WebSocket;

namespace Atlas.Web.Ice.MockFeed.Server;

/// <summary>
/// Serves the mock feed over a WebSocket, reproducing the relay's observable
/// behaviour: single client, refresh-then-updates on connect, resync on
/// command, and teardown when the client goes away.
/// </summary>
public sealed class MockFeedServer : IDisposable
{
    private const string ResyncCommand = "resync";
    private const string ResetCommand = "reset";

    private readonly HttpListener listener = new();
    private readonly FeedSequence sequence;
    private readonly MockFeedOptions options;
    private readonly TextWriter log;

    private CancellationTokenSource? currentSession;

    public MockFeedServer(FeedSequence sequence, MockFeedOptions options, TextWriter log)
    {
        this.sequence = sequence;
        this.options = options;
        this.log = log;

        listener.Prefixes.Add($"http://localhost:{options.Port}/");
    }

    public void Start()
    {
        listener.Start();
        log.WriteLine($"Mock ICE feed listening on ws://localhost:{options.Port}/");
        log.WriteLine($"  symbols  : {string.Join(", ", sequence.Symbols)}");
        log.WriteLine($"  interval : {options.Interval.TotalMilliseconds:0}ms");
        log.WriteLine($"  mode     : {(options.Loop ? "loop capture" : $"generate ticks at {options.Jitter}%")}");
    }

    public async Task AcceptLoopAsync(CancellationToken cancellationToken)
    {
        while (!cancellationToken.IsCancellationRequested)
        {
            HttpListenerContext context;
            try
            {
                context = await listener.GetContextAsync().WaitAsync(cancellationToken);
            }
            catch (OperationCanceledException)
            {
                // Shutdown requested; leaving the loop is the intended path.
                return;
            }
            catch (HttpListenerException)
            {
                // The listener was stopped underneath us, which is how Dispose
                // unblocks this call. Nothing left to accept.
                return;
            }

            if (!context.Request.IsWebSocketRequest)
            {
                context.Response.StatusCode = (int)HttpStatusCode.BadRequest;
                context.Response.Close();
                continue;
            }

            var webSocketContext = await context.AcceptWebSocketAsync(subProtocol: null);

            // The relay serves one client at a time; a new connection displaces
            // the previous one.
            currentSession?.Cancel();
            var session = CancellationTokenSource.CreateLinkedTokenSource(cancellationToken);
            currentSession = session;

            _ = ServeAsync(webSocketContext.WebSocket, session.Token);
        }
    }

    private async Task ServeAsync(WebSocketConnection socket, CancellationToken cancellationToken)
    {
        log.WriteLine("Client connected — sending refresh");

        // Matches the relay: a fresh client gets the capture from the top.
        sequence.Rewind();

        var sendLock = new SemaphoreSlim(1, 1);

        // Lets the receive loop stop the tick loop when the client closes.
        using var session = CancellationTokenSource.CreateLinkedTokenSource(cancellationToken);
        var sessionToken = session.Token;

        try
        {
            foreach (var frame in sequence.OpeningFrames)
            {
                await SendAsync(socket, frame, sendLock, sessionToken);
            }

            var receiving = ReceiveLoopAsync(socket, sendLock, session);

            while (!sessionToken.IsCancellationRequested && socket.State == WebSocketState.Open)
            {
                await Task.Delay(options.Interval, sessionToken);

                var frame = sequence.NextUpdate();
                if (frame is null)
                {
                    break;
                }

                await SendAsync(socket, frame, sendLock, sessionToken);
            }

            await receiving;
        }
        catch (OperationCanceledException)
        {
            // Session cancelled by shutdown or by a newer client taking over.
        }
        catch (WebSocketException)
        {
            // The client vanished mid-write. Nothing to recover: the session is
            // over either way, and the accept loop keeps running.
        }
        finally
        {
            log.WriteLine("Client disconnected");
            sendLock.Dispose();
            socket.Dispose();
        }
    }

    private async Task ReceiveLoopAsync(
        WebSocketConnection socket, SemaphoreSlim sendLock, CancellationTokenSource session)
    {
        var buffer = new byte[4096];

        while (!session.IsCancellationRequested && socket.State == WebSocketState.Open)
        {
            WebSocketReceiveResult result;
            try
            {
                result = await socket.ReceiveAsync(
                    new ArraySegment<byte>(buffer), session.Token);
            }
            catch (OperationCanceledException)
            {
                return;
            }
            catch (WebSocketException)
            {
                // Client vanished without a close frame. Nothing to acknowledge;
                // ending the session is all that is left to do.
                session.Cancel();
                return;
            }

            if (result.MessageType == WebSocketMessageType.Close)
            {
                await CompleteCloseHandshakeAsync(socket, sendLock);
                session.Cancel();
                return;
            }

            var command = Encoding.UTF8.GetString(buffer, 0, result.Count).Trim();
            await HandleCommandAsync(socket, command, sendLock, session.Token);
        }
    }

    /// <summary>
    /// Replies to the client's close frame. Skipping this leaves a client that
    /// closed gracefully waiting on a handshake that never completes.
    /// </summary>
    private static async Task CompleteCloseHandshakeAsync(
        WebSocketConnection socket, SemaphoreSlim sendLock)
    {
        await sendLock.WaitAsync(CancellationToken.None);
        try
        {
            if (socket.State is WebSocketState.CloseReceived or WebSocketState.Open)
            {
                await socket.CloseOutputAsync(
                    WebSocketCloseStatus.NormalClosure, string.Empty, CancellationToken.None);
            }
        }
        catch (WebSocketException)
        {
            // The client is already gone, so there is nobody left to acknowledge.
            // The session tears down either way.
        }
        finally
        {
            sendLock.Release();
        }
    }

    private async Task HandleCommandAsync(
        WebSocketConnection socket, string command, SemaphoreSlim sendLock, CancellationToken cancellationToken)
    {
        if (command.Equals(ResyncCommand, StringComparison.OrdinalIgnoreCase))
        {
            log.WriteLine("resync — resending refresh");
            foreach (var frame in sequence.OpeningFrames)
            {
                await SendAsync(socket, frame, sendLock, cancellationToken);
            }

            return;
        }

        if (command.StartsWith(ResetCommand, StringComparison.OrdinalIgnoreCase))
        {
            // "reset" resets every symbol; "reset SYM" resets one. Not a relay
            // command — it exists so the consumer's DataStatus.Reset path can be
            // exercised, which no live input currently reaches.
            var requested = command[ResetCommand.Length..].Trim();
            var symbols = requested.Length == 0
                ? sequence.Symbols
                : [requested];

            log.WriteLine($"reset — {string.Join(", ", symbols)}");
            foreach (var symbol in symbols)
            {
                await SendAsync(socket, sequence.ResetFrame(symbol), sendLock, cancellationToken);
            }

            return;
        }

        log.WriteLine($"Unknown command: {command}");
    }

    /// <summary>
    /// Serialized because the tick loop and inbound command handling both write
    /// to the same socket, and WebSocket.SendAsync is not safe to call
    /// concurrently.
    /// </summary>
    private static async Task SendAsync(
        WebSocketConnection socket, string frame, SemaphoreSlim sendLock, CancellationToken cancellationToken)
    {
        await sendLock.WaitAsync(cancellationToken);
        try
        {
            if (socket.State != WebSocketState.Open)
            {
                return;
            }

            await socket.SendAsync(
                Encoding.UTF8.GetBytes(frame),
                WebSocketMessageType.Text,
                endOfMessage: true,
                cancellationToken);
        }
        finally
        {
            sendLock.Release();
        }
    }

    public void Dispose()
    {
        currentSession?.Cancel();
        currentSession?.Dispose();

        if (listener.IsListening)
        {
            listener.Stop();
        }

        listener.Close();
    }
}
