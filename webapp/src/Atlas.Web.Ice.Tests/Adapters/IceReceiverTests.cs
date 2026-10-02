using Atlas.Web.Ice.Adapters;
using Atlas.Web.Ice.Configuration;
using Atlas.Web.Ice.Domain.Services;
using Atlas.Web.Ice.Tests.Fakes;
using Atlas.Web.Ice.WebSocket;
using Atlas.Web.Core.Domain.Logic;
using Atlas.Web.Core.Domain.Enumeration;
using FluentAssertions;
using Microsoft.Extensions.Options;
using Moq;

namespace Atlas.Web.Ice.Tests.Adapters;

public class IceReceiverTests
{
    private static readonly string TestDataPath =
        Path.Combine(AppContext.BaseDirectory, "TestData", "ceapidata.json");

    private static IOptions<IceOptions> DefaultOptions(string endpoint = "localhost", int port = 9002) =>
        Options.Create(new IceOptions { Endpoint = endpoint, Port = port });

    private static IceReceiver Build(
        IWebSocketClientFactory wsFactory,
        IIceMessageProcessor? processor = null,
        IOptions<IceOptions>? options = null) =>
        new(
            wsFactory,
            processor ?? new Mock<IIceMessageProcessor>().Object,
            new FeedHealthStore(TimeProvider.System, TimeSpan.FromSeconds(15)),
            options ?? DefaultOptions());

    // ─────────────────────────────────────────
    // Connection
    // ─────────────────────────────────────────

    [Fact]
    public async Task StartAsync_ConnectsToCorrectUri()
    {
        var ws = new FakeWebSocketClient(Enumerable.Empty<string>());
        var factory = new FakeWebSocketClientFactory(ws);
        using var cts = new CancellationTokenSource(TimeSpan.FromMilliseconds(200));

        var sut = Build(factory, options: DefaultOptions("ice-server", 9002));
        await sut.StartAsync(cts.Token);
        await Task.Delay(50);

        ws.ConnectedUri.Should().Be(new Uri("ws://ice-server:9002"));
    }

    [Fact]
    public async Task StartAsync_UsesPortFromOptions()
    {
        var ws = new FakeWebSocketClient(Enumerable.Empty<string>());
        var factory = new FakeWebSocketClientFactory(ws);
        using var cts = new CancellationTokenSource(TimeSpan.FromMilliseconds(200));

        var sut = Build(factory, options: DefaultOptions("localhost", 8080));
        await sut.StartAsync(cts.Token);
        await Task.Delay(50);

        ws.ConnectedUri!.Port.Should().Be(8080);
    }

    [Fact]
    public async Task StartAsync_ReturnsImmediately()
    {
        var ws = new FakeWebSocketClient(Enumerable.Empty<string>());
        var factory = new FakeWebSocketClientFactory(ws);

        var sut = Build(factory);
        var act = async () => await sut.StartAsync(CancellationToken.None);
        await act.Should().CompleteWithinAsync(TimeSpan.FromMilliseconds(100));
        await sut.StopAsync(CancellationToken.None);
    }

    [Fact]
    public async Task StopAsync_ClosesWebSocketConnection()
    {
        var ws = new FakeWebSocketClient(Enumerable.Empty<string>());
        var factory = new FakeWebSocketClientFactory(ws);

        var sut = Build(factory);
        await sut.StartAsync(CancellationToken.None);
        await Task.Delay(50);
        await sut.StopAsync(CancellationToken.None);

        ws.CloseWasCalled.Should().BeTrue();
    }

    // ─────────────────────────────────────────
    // Message routing
    // ─────────────────────────────────────────

    [Fact]
    public async Task StartAsync_CallsProcessorForEachLine()
    {
        var nonEmpty = File.ReadAllLines(TestDataPath)
            .Where(l => !string.IsNullOrWhiteSpace(l)).ToArray();

        var ws = FakeWebSocketClient.FromFile(TestDataPath);
        var factory = new FakeWebSocketClientFactory(ws);
        var processor = new Mock<IIceMessageProcessor>();

        var sut = Build(factory, processor: processor.Object);
        await sut.StartAsync(CancellationToken.None);
        await Task.Delay(500);
        await sut.StopAsync(CancellationToken.None);

        processor.Verify(p => p.Process(It.IsAny<string>()),
            Times.Exactly(nonEmpty.Length));
    }

    [Fact]
    public async Task StartAsync_PassesLineContentToProcessor()
    {
        var firstLine = File.ReadAllLines(TestDataPath)
            .First(l => !string.IsNullOrWhiteSpace(l));

        var ws = FakeWebSocketClient.FromString(firstLine);
        var factory = new FakeWebSocketClientFactory(ws);
        var processor = new Mock<IIceMessageProcessor>();

        var sut = Build(factory, processor: processor.Object);
        await sut.StartAsync(CancellationToken.None);
        await Task.Delay(200);
        await sut.StopAsync(CancellationToken.None);

        processor.Verify(p => p.Process(firstLine.TrimEnd('\r')), Times.Once);
    }

    // ─────────────────────────────────────────
    // Error handling
    // ─────────────────────────────────────────

    [Fact]
    public async Task StartAsync_WhenServerUnavailable_ReturnsImmediately()
    {
        var factory = new FakeWebSocketClientFactory(new UnavailableWebSocketClient());
        var sut = Build(factory);
        var act = async () => await sut.StartAsync(CancellationToken.None);
        await act.Should().CompleteWithinAsync(TimeSpan.FromMilliseconds(100));
        await sut.StopAsync(CancellationToken.None);
    }

    [Fact]
    public async Task StartAsync_WhenServerUnavailable_RetriesWithBackoff()
    {
        var client = new UnavailableWebSocketClient();
        var factory = new FakeWebSocketClientFactory(client);

        var sut = Build(factory);
        await sut.StartAsync(CancellationToken.None);
        await Task.Delay(450);
        await sut.StopAsync(CancellationToken.None);

        // 100ms + 200ms delay = at least 3 attempts within 450ms
        client.ConnectAttempts.Should().BeGreaterThanOrEqualTo(3);
    }

    [Fact]
    public async Task ReceiveLoop_WhenConnectionDropped_Reconnects()
    {
        var line = File.ReadAllLines(TestDataPath).First(l => !string.IsNullOrWhiteSpace(l));
        var firstClient = new DroppingWebSocketClient(line);
        var secondClient = FakeWebSocketClient.FromFile(TestDataPath);
        var factory = new FakeWebSocketClientFactory(firstClient, secondClient);

        var sut = Build(factory);
        await sut.StartAsync(CancellationToken.None);
        await Task.Delay(500);
        await sut.StopAsync(CancellationToken.None);

        secondClient.ConnectedUri.Should().NotBeNull();
    }

    [Fact]
    public async Task ReceiveLoop_WhenServerInitiatesClose_Reconnects()
    {
        var line = File.ReadAllLines(TestDataPath).First(l => !string.IsNullOrWhiteSpace(l));
        var firstClient = new ServerClosingWebSocketClient(line);
        var secondClient = FakeWebSocketClient.FromFile(TestDataPath);
        var factory = new FakeWebSocketClientFactory(firstClient, secondClient);

        var sut = Build(factory);
        await sut.StartAsync(CancellationToken.None);
        await Task.Delay(500);
        await sut.StopAsync(CancellationToken.None);

        secondClient.ConnectedUri.Should().NotBeNull();
    }

    [Fact]
    public async Task Connect_ThatNeverAnswers_TimesOutAndIsRetried()
    {
        var hanging = new HangingWebSocketClient();
        var good = FakeWebSocketClient.FromFile(TestDataPath);
        var factory = new FakeWebSocketClientFactory(hanging, good);
        var options = Options.Create(new IceOptions
        {
            Endpoint = "localhost", Port = 9002, ConnectTimeout = TimeSpan.FromMilliseconds(100),
        });

        var sut = Build(factory, options: options);
        await sut.StartAsync(CancellationToken.None);
        await Task.Delay(600);
        await sut.StopAsync(CancellationToken.None);

        good.ConnectedUri.Should().NotBeNull();
    }

    [Theory]
    [InlineData(true)]
    [InlineData(false)]
    public async Task ReceiveLoop_WhenRelayRestarts_AcceptsHealthFromNewProcess(bool serverCloses)
    {
        const string previousStatus = """
            ["status",{"state":"LIVE","generation":5,"timestamp":"2026-10-02T09:00:00Z","detail":"Previous process","subscribedSymbols":4056}]
            """;
        const string restartedStatus = """
            ["status",{"state":"DISCONNECTED","generation":1,"timestamp":"2026-10-02T09:00:05Z","detail":"ICE disconnected: DBCAPI_ERROR_ADDRESS_CHANGE","subscribedSymbols":3866}]
            """;
        IWebSocketClient firstClient = serverCloses
            ? new ServerClosingWebSocketClient(previousStatus)
            : new DroppingWebSocketClient(previousStatus);
        var secondClient = FakeWebSocketClient.FromString(restartedStatus);
        var factory = new FakeWebSocketClientFactory(firstClient, secondClient);
        using var health = new FeedHealthStore(TimeProvider.System, TimeSpan.FromSeconds(15));
        var received = new TaskCompletionSource(TaskCreationOptions.RunContinuationsAsynchronously);
        using var subscription = health.GetStream().Subscribe(snapshot =>
        {
            if (snapshot.Generation == 1 && snapshot.State == FeedState.Disconnected)
            {
                received.TrySetResult();
            }
        });
        var sut = new IceReceiver(factory, new Mock<IIceMessageProcessor>().Object, health, DefaultOptions());
        await sut.StartAsync(CancellationToken.None);
        try
        {
            await received.Task.WaitAsync(TimeSpan.FromSeconds(5));

            secondClient.ConnectedUri.Should().NotBeNull();
            health.Current().Generation.Should().Be(1);
            health.Current().Detail.Should().Be("ICE disconnected: DBCAPI_ERROR_ADDRESS_CHANGE");
            health.Current().SubscribedSymbols.Should().Be(3866);
        }
        finally
        {
            await sut.StopAsync(CancellationToken.None);
        }
    }

    /// <summary>A relay that accepts nothing and refuses nothing, as one being replaced can.</summary>
    private sealed class HangingWebSocketClient : IWebSocketClient
    {
        public System.Net.WebSockets.WebSocketState State => System.Net.WebSockets.WebSocketState.Connecting;

        public Task ConnectAsync(Uri uri, CancellationToken ct) => Task.Delay(Timeout.Infinite, ct);

        public Task<System.Net.WebSockets.WebSocketReceiveResult> ReceiveAsync(ArraySegment<byte> buffer, CancellationToken ct) =>
            throw new InvalidOperationException("Not connected");

        public Task CloseAsync(System.Net.WebSockets.WebSocketCloseStatus closeStatus, string statusDescription, CancellationToken ct) =>
            Task.CompletedTask;

        public void Dispose() { }
    }

    [Fact]
    public async Task StartAsync_WhenCancelled_StopsGracefully()
    {
        var ws = FakeWebSocketClient.FromFile(TestDataPath);
        var factory = new FakeWebSocketClientFactory(ws);
        var sut = Build(factory);

        using var cts = new CancellationTokenSource();
        await sut.StartAsync(cts.Token);
        cts.Cancel();
        await Task.Delay(100);

        var act = async () => await sut.StopAsync(CancellationToken.None);
        await act.Should().NotThrowAsync();
    }
}
