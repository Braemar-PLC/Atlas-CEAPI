using System.Net.WebSockets;

namespace Atlas.Web.Ice.WebSocket;

public sealed class WebSocketClient : IWebSocketClient
{
    private readonly ClientWebSocket _ws = new();

    public WebSocketState State => _ws.State;

    public Task ConnectAsync(Uri uri, CancellationToken ct) =>
        _ws.ConnectAsync(uri, ct);

    public Task<WebSocketReceiveResult> ReceiveAsync(ArraySegment<byte> buffer, CancellationToken ct) =>
        _ws.ReceiveAsync(buffer, ct);

    public Task CloseAsync(WebSocketCloseStatus closeStatus, string statusDescription, CancellationToken ct) =>
        _ws.CloseAsync(closeStatus, statusDescription, ct);

    public void Dispose() => _ws.Dispose();
}
