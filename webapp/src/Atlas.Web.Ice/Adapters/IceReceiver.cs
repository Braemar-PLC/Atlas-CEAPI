using System.Net.WebSockets;
using System.Text;
using Atlas.Web.Ice.Configuration;
using Atlas.Web.Ice.Domain.Services;
using Atlas.Web.Ice.WebSocket;
using Microsoft.Extensions.Hosting;
using Microsoft.Extensions.Logging;
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
    private IWebSocketClient? _ws;
    private CancellationTokenSource? _cts;

    public IceReceiver(
        IWebSocketClientFactory wsFactory,
        IIceMessageProcessor processor,
        IOptions<IceOptions> options,
        ILogger<IceReceiver> logger)
    {
        _wsFactory = wsFactory;
        _processor = processor;
        _options = options.Value;
        _logger = logger;
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
                _logger.LogInformation("Connecting to CEAPI WebSocket at {Uri}", _options.WebSocketUri);
                _ws = _wsFactory.Create();
                await _ws.ConnectAsync(_options.WebSocketUri, ct);
                _logger.LogInformation("Connected to CEAPI WebSocket at {Uri}", _options.WebSocketUri);
                attempt = 0;
                await ReceiveLoopAsync(_ws, ct);
                _logger.LogWarning("CEAPI WebSocket connection at {Uri} closed; reconnecting", _options.WebSocketUri);
            }
            catch (OperationCanceledException) when (ct.IsCancellationRequested)
            {
                return;
            }
            catch (Exception ex)
            {
                var delay = BackoffSequence[Math.Min(attempt, BackoffSequence.Length - 1)];
                _logger.LogWarning(ex, "Failed to connect to CEAPI WebSocket at {Uri}; retrying in {Delay}", _options.WebSocketUri, delay);
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
                _processor.Process(line.TrimEnd('\r'));
            }
            sb.Clear();
        }
    }
}
