using System.Net.WebSockets;

namespace Atlas.Web.Ice.WebSocket;

public sealed class WebSocketClient : IWebSocketClient
{
    private readonly ClientWebSocket _ws = new();

    public WebSocketClient()
    {
        // A connection Azure drops silently (idle NAT timeout, relay restarted on a new address) would otherwise leave
        // ReceiveAsync waiting forever with empty grids. Ping every 15s; no pong within 15s aborts the socket, and
        // IceReceiver reconnects.
        _ws.Options.KeepAliveInterval = TimeSpan.FromSeconds(15);
        _ws.Options.KeepAliveTimeout = TimeSpan.FromSeconds(15);
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
