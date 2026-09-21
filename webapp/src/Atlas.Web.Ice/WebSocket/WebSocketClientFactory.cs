namespace Atlas.Web.Ice.WebSocket;

public sealed class WebSocketClientFactory : IWebSocketClientFactory
{
    public IWebSocketClient Create() => new WebSocketClient();
}
