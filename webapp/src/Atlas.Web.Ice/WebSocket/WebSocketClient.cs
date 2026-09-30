using System.Net.WebSockets;

namespace Atlas.Web.Ice.WebSocket;

public sealed class WebSocketClient : IWebSocketClient
{
    private readonly ClientWebSocket _ws = new();

    public WebSocketClient()
    {
        // A connection Azure drops silently (idle NAT timeout, relay restarted on a new address) would otherwise leave
        // ReceiveAsync waiting forever with empty grids. Ping every 5s; no pong within 10s aborts the socket, and
        // IceReceiver reconnects. The timeout stays well above the interval on purpose: the relay answers pings on the
        // thread that also fans out ticks, so a burst or a garbage collection pause must not be mistaken for a death.
        _ws.Options.KeepAliveInterval = TimeSpan.FromSeconds(5);
        _ws.Options.KeepAliveTimeout = TimeSpan.FromSeconds(10);
    }

    public WebSocketState State => _ws.State;

    public Task ConnectAsync(Uri uri, CancellationToken ct) =>
        _ws.ConnectAsync(uri, ct);

    public Task<WebSocketReceiveResult> ReceiveAsync(ArraySegment<byte> buffer, CancellationToken ct) =>
        _ws.ReceiveAsync(buffer, ct);

    public Task CloseAsync(WebSocketCloseStatus closeStatus, string statusDescription, CancellationToken ct) =>
        _ws.CloseAsync(closeStatus, statusDescription, ct);

    public void Dispose() => _ws.Dispose();
}
