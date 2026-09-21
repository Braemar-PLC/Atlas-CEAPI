using Atlas.Web.Ice.WebSocket;

namespace Atlas.Web.Ice.Tests.Fakes;

public sealed class FakeWebSocketClientFactory : IWebSocketClientFactory
{
    private readonly IWebSocketClient _repeating;
    private readonly Queue<IWebSocketClient> _queue;

    /// <summary>
    /// Single client — returned on every Create() call (supports retry loops).
    /// </summary>
    public FakeWebSocketClientFactory(IWebSocketClient single)
    {
        _repeating = single;
        _queue = new Queue<IWebSocketClient>();
    }

    /// <summary>
    /// Multiple clients — dequeued in order, last one repeated when exhausted.
    /// </summary>
    public FakeWebSocketClientFactory(params IWebSocketClient[] clients)
    {
        _repeating = clients.Last();
        _queue = new Queue<IWebSocketClient>(clients);
    }

    public IWebSocketClient Create() =>
        _queue.Count > 0 ? _queue.Dequeue() : _repeating;
}
