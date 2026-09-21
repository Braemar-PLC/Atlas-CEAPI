using System.Net.WebSockets;

namespace Atlas.Web.Ice.WebSocket;

public interface IWebSocketClient : IDisposable
{
    WebSocketState State { get; }
    Task ConnectAsync(Uri uri, CancellationToken ct);
    Task<WebSocketReceiveResult> ReceiveAsync(ArraySegment<byte> buffer, CancellationToken ct);
    Task CloseAsync(WebSocketCloseStatus closeStatus, string statusDescription, CancellationToken ct);
}
