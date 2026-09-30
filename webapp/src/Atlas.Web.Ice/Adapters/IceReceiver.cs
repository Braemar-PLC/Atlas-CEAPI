using System.Net.WebSockets;
using System.Text;
using Atlas.Web.Ice.Configuration;
using Atlas.Web.Ice.Domain.Services;
using Atlas.Web.Ice.WebSocket;
using Atlas.Web.Core.Application.Ports;
using Atlas.Web.Core.Domain.Enumeration;
using Microsoft.Extensions.Hosting;
using Microsoft.Extensions.Logging;
using Microsoft.Extensions.Logging.Abstractions;
using Microsoft.Extensions.Options;

namespace Atlas.Web.Ice.Adapters;

public sealed class IceReceiver : IHostedService
{
    private static readonly TimeSpan[] BackoffSequence =
    [
        TimeSpan.FromMilliseconds(100),
        TimeSpan.FromMilliseconds(200),
        TimeSpan.FromMilliseconds(500),
        TimeSpan.FromSeconds(1),
        TimeSpan.FromSeconds(2),
        TimeSpan.FromSeconds(5),
        TimeSpan.FromSeconds(10),
    ];

    private readonly IWebSocketClientFactory _wsFactory;
    private readonly IIceMessageProcessor _processor;
    private readonly IceOptions _options;
    private readonly ILogger<IceReceiver> _logger;
    private readonly IFeedHealthStore _feedHealth;
    private IWebSocketClient? _ws;
    private CancellationTokenSource? _cts;

    public IceReceiver(
        IWebSocketClientFactory wsFactory,
        IIceMessageProcessor processor,
        IFeedHealthStore feedHealth,
        IOptions<IceOptions> options,
        ILogger<IceReceiver>? logger = null)
    {
        _wsFactory = wsFactory;
        _processor = processor;
        _feedHealth = feedHealth;
        _options = options.Value;
        _logger = logger ?? NullLogger<IceReceiver>.Instance;
    }

    public Task StartAsync(CancellationToken ct)
    {
        _cts = CancellationTokenSource.CreateLinkedTokenSource(ct);
        _ = RunAsync(_cts.Token);
        return Task.CompletedTask;
    }

    public async Task StopAsync(CancellationToken ct)
    {
        _cts?.Cancel();
        if (_ws is not null && _ws.State == WebSocketState.Open)
        {
            await _ws.CloseAsync(WebSocketCloseStatus.NormalClosure, "Stopping", ct);
        }
    }

    private async Task RunAsync(CancellationToken ct)
    {
        var attempt = 0;
        while (!ct.IsCancellationRequested)
        {
            try
            {
                _ws = _wsFactory.Create();
                _feedHealth.RelayConnecting();
                using (var connectCts = CancellationTokenSource.CreateLinkedTokenSource(ct))
                {
                    connectCts.CancelAfter(_options.ConnectTimeout);
                    try
                    {
                        await _ws.ConnectAsync(_options.WebSocketUri, connectCts.Token);
                    }
                    catch (OperationCanceledException) when (!ct.IsCancellationRequested)
                    {
                        throw new TimeoutException($"No answer from the CEAPI relay within {_options.ConnectTimeout}.");
                    }
                }
                _logger.LogInformation("Connected to the CEAPI relay at {Uri}", _options.WebSocketUri);
                attempt = 0;
                await ReceiveLoopAsync(_ws, ct);
                _logger.LogWarning("The CEAPI relay closed the connection; reconnecting");
                _feedHealth.RelayDisconnected("The CEAPI relay closed the connection");
            }
            catch (OperationCanceledException) when (ct.IsCancellationRequested)
            {
                return;
            }
            catch (Exception e)
            {
                var delay = BackoffSequence[Math.Min(attempt, BackoffSequence.Length - 1)];
                _logger.LogWarning(e, "CEAPI relay connection at {Uri} failed or was lost; retrying in {Delay}", _options.WebSocketUri, delay);
                _feedHealth.RelayDisconnected($"CEAPI relay connection failed: {e.Message}");
                attempt++;
                try { await Task.Delay(delay, ct); }
                catch (OperationCanceledException) { return; }
            }
            finally
            {
                _ws?.Dispose();
                _ws = null;
            }
        }
    }

    private async Task ReceiveLoopAsync(IWebSocketClient ws, CancellationToken ct)
    {
        var buffer = new byte[64 * 1024];
        var sb = new StringBuilder();
        while (!ct.IsCancellationRequested)
        {
            var result = await ws.ReceiveAsync(new ArraySegment<byte>(buffer), ct);
            if (result.MessageType == WebSocketMessageType.Close) { break; }
            sb.Append(Encoding.UTF8.GetString(buffer, 0, result.Count));
            if (!result.EndOfMessage) { continue; }
            foreach (var line in sb.ToString().Split('\n', StringSplitOptions.RemoveEmptyEntries))
            {
                ProcessLine(line.TrimEnd('\r'));
            }
            sb.Clear();
        }
    }

    private void ProcessLine(string line)
    {
        try
        {
            using var document = System.Text.Json.JsonDocument.Parse(line);
            var root = document.RootElement;
            if (root.ValueKind == System.Text.Json.JsonValueKind.Array
                && root.GetArrayLength() >= 2
                && string.Equals(root[0].GetString(), "status", StringComparison.OrdinalIgnoreCase)
                && root[1].ValueKind == System.Text.Json.JsonValueKind.Object)
            {
                var status = root[1];
                var stateText = status.GetProperty("state").GetString();
                var state = stateText?.ToUpperInvariant() switch
                {
                    "CONNECTING" => FeedState.Connecting,
                    "LIVE" => FeedState.Live,
                    "RECONNECTING" => FeedState.Reconnecting,
                    "AUTHENTICATION_FAILED" => FeedState.AuthenticationFailed,
                    "DISCONNECTED" => FeedState.Disconnected,
                    _ => (FeedState?)null
                };
                if (state is null)
                {
                    _logger.LogWarning("CEAPI sent an unknown feed state {State}", stateText);
                    return;
                }

                _feedHealth.RecordRelayStatus(
                    state.Value,
                    status.GetProperty("generation").GetInt64(),
                    status.GetProperty("timestamp").GetDateTimeOffset(),
                    status.GetProperty("detail").GetString() ?? stateText ?? "Unknown",
                    status.GetProperty("subscribedSymbols").GetInt32());
                return;
            }

            if (root.ValueKind == System.Text.Json.JsonValueKind.Array
                && root.GetArrayLength() >= 3
                && (string.Equals(root[0].GetString(), "update", StringComparison.OrdinalIgnoreCase)
                    || string.Equals(root[0].GetString(), "refresh", StringComparison.OrdinalIgnoreCase)))
            {
                _feedHealth.RecordQuote();
            }
        }
        catch (System.Text.Json.JsonException)
        {
            // The normal processor logs malformed relay messages with the full parsing context.
        }

        _processor.Process(line);
    }
}
