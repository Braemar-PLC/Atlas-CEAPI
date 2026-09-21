using System.Net.WebSockets;
using System.Text;
using Atlas.Web.Ice.WebSocket;

namespace Atlas.Web.Ice.Tests.Fakes;

/// <summary>
/// Replays a sequence of newline-delimited messages as if received from a
/// WebSocket connection. Each line is delivered as a complete message
/// (EndOfMessage = true). After all lines are consumed, blocks until cancelled.
/// </summary>
public sealed class FakeWebSocketClient : IWebSocketClient
{
    private readonly string[] _lines;
    private int _index;
    private WebSocketState _state = WebSocketState.None;

    public Uri? ConnectedUri { get; private set; }
    public bool CloseWasCalled { get; private set; }

    public FakeWebSocketClient(IEnumerable<string> lines)
    {
        _lines = lines.Where(l => !string.IsNullOrWhiteSpace(l)).ToArray();
    }

    public static FakeWebSocketClient FromFile(string path) =>
        new(File.ReadAllLines(path));

    public static FakeWebSocketClient FromString(string content) =>
        new(content.Split('\n'));

    public WebSocketState State => _state;

    public Task ConnectAsync(Uri uri, CancellationToken ct)
    {
        ct.ThrowIfCancellationRequested();
        ConnectedUri = uri;
        _state = WebSocketState.Open;
        return Task.CompletedTask;
    }

    public async Task<WebSocketReceiveResult> ReceiveAsync(ArraySegment<byte> buffer, CancellationToken ct)
    {
        if (_index >= _lines.Length)
        {
            // All messages delivered — block until cancelled
            await Task.Delay(Timeout.Infinite, ct);
            ct.ThrowIfCancellationRequested();
        }

        var line = _lines[_index++];
        var bytes = Encoding.UTF8.GetBytes(line);
        bytes.CopyTo(buffer.Array!, buffer.Offset);

        return new WebSocketReceiveResult(
            bytes.Length,
            WebSocketMessageType.Text,
            endOfMessage: true);
    }

    public Task CloseAsync(WebSocketCloseStatus closeStatus, string statusDescription, CancellationToken ct)
    {
        CloseWasCalled = true;
        _state = WebSocketState.Closed;
        return Task.CompletedTask;
    }

    public void Dispose()
    {
        _state = WebSocketState.Closed;
    }
}

/// <summary>
/// Simulates a server that is unavailable — ConnectAsync always throws.
/// </summary>
public sealed class UnavailableWebSocketClient : IWebSocketClient
{
    public WebSocketState State => WebSocketState.None;
    public Uri? ConnectedUri { get; private set; }
    public bool CloseWasCalled { get; private set; }
    public int ConnectAttempts { get; private set; }

    public Task ConnectAsync(Uri uri, CancellationToken ct)
    {
        ConnectedUri = uri;
        ConnectAttempts++;
        throw new WebSocketException("Connection refused");
    }

    public Task<WebSocketReceiveResult> ReceiveAsync(ArraySegment<byte> buffer, CancellationToken ct) =>
        throw new InvalidOperationException("Not connected");

    public Task CloseAsync(WebSocketCloseStatus closeStatus, string statusDescription, CancellationToken ct)
    {
        CloseWasCalled = true;
        return Task.CompletedTask;
    }

    public void Dispose() { }
}

/// <summary>
/// Delivers one message then drops the connection mid-stream.
/// </summary>
public sealed class DroppingWebSocketClient : IWebSocketClient
{
    private readonly string _message;
    private bool _delivered;
    private WebSocketState _state = WebSocketState.None;

    public DroppingWebSocketClient(string message) => _message = message;

    public WebSocketState State => _state;

    public Task ConnectAsync(Uri uri, CancellationToken ct)
    {
        _state = WebSocketState.Open;
        return Task.CompletedTask;
    }

    public Task<WebSocketReceiveResult> ReceiveAsync(ArraySegment<byte> buffer, CancellationToken ct)
    {
        if (!_delivered)
        {
            _delivered = true;
            var bytes = System.Text.Encoding.UTF8.GetBytes(_message);
            bytes.CopyTo(buffer.Array!, buffer.Offset);
            return Task.FromResult(new WebSocketReceiveResult(bytes.Length, WebSocketMessageType.Text, true));
        }
        _state = WebSocketState.Aborted;
        throw new WebSocketException("Connection dropped");
    }

    public Task CloseAsync(WebSocketCloseStatus closeStatus, string statusDescription, CancellationToken ct)
    {
        _state = WebSocketState.Closed;
        return Task.CompletedTask;
    }

    public void Dispose() { }
}

/// <summary>
/// Delivers one message then sends a server-initiated close frame.
/// </summary>
public sealed class ServerClosingWebSocketClient : IWebSocketClient
{
    private readonly string _message;
    private bool _delivered;
    private WebSocketState _state = WebSocketState.None;

    public ServerClosingWebSocketClient(string message) => _message = message;

    public WebSocketState State => _state;

    public Task ConnectAsync(Uri uri, CancellationToken ct)
    {
        _state = WebSocketState.Open;
        return Task.CompletedTask;
    }

    public Task<WebSocketReceiveResult> ReceiveAsync(ArraySegment<byte> buffer, CancellationToken ct)
    {
        if (!_delivered)
        {
            _delivered = true;
            var bytes = System.Text.Encoding.UTF8.GetBytes(_message);
            bytes.CopyTo(buffer.Array!, buffer.Offset);
            return Task.FromResult(new WebSocketReceiveResult(bytes.Length, WebSocketMessageType.Text, true));
        }
        _state = WebSocketState.CloseReceived;
        return Task.FromResult(new WebSocketReceiveResult(0, WebSocketMessageType.Close, true));
    }

    public Task CloseAsync(WebSocketCloseStatus closeStatus, string statusDescription, CancellationToken ct)
    {
        _state = WebSocketState.Closed;
        return Task.CompletedTask;
    }

    public void Dispose() { }
}
