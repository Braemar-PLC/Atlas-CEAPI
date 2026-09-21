using Atlas.Web.Ice.Adapters;
using Atlas.Web.Ice.Configuration;
using Atlas.Web.Ice.Domain.Services;
using Atlas.Web.Ice.Tests.Fakes;
using Atlas.Web.Ice.WebSocket;
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
