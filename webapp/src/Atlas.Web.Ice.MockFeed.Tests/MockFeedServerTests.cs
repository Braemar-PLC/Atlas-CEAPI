using System.Net;
using System.Net.Sockets;
using System.Net.WebSockets;
using System.Text;
using Atlas.Web.Ice.MockFeed.Capture;
using Atlas.Web.Ice.MockFeed.Configuration;
using Atlas.Web.Ice.MockFeed.Server;
using Atlas.Web.Ice.MockFeed.Ticks;
using FluentAssertions;

namespace Atlas.Web.Ice.MockFeed.Tests;

/// <summary>
/// Smoke tests over a real socket. The frame-shaping logic is covered by unit
/// tests; these assert the observable protocol behaviour a consumer depends on.
/// </summary>
public class MockFeedServerTests
{
    private static readonly IReadOnlyList<CapturedFrame> Capture =
    [
        new CapturedFrame("A", [new CapturedField(1, "72"), new CapturedField(19, "64.665")]),
        new CapturedFrame("B", [new CapturedField(1, "73"), new CapturedField(19, "31.200")])
    ];

    private static int FreePort()
    {
        var probe = new TcpListener(IPAddress.Loopback, 0);
        probe.Start();
        var port = ((IPEndPoint)probe.LocalEndpoint).Port;
        probe.Stop();
        return port;
    }

    private sealed class Harness : IDisposable
    {
        public MockFeedServer Server { get; }
        public CancellationTokenSource Cancellation { get; }
        public int Port { get; }

        public Harness(MockFeedOptions? overrides = null)
        {
            Port = FreePort();
            var options = (overrides ?? new MockFeedOptions()) with
            {
                Port = Port,
                Interval = TimeSpan.FromMilliseconds(10)
            };

            Server = new MockFeedServer(
                new FeedSequence(Capture, options, new Random(20260917)),
                options,
                TextWriter.Null);

            Cancellation = new CancellationTokenSource(TimeSpan.FromSeconds(20));
            Server.Start();
            _ = Server.AcceptLoopAsync(Cancellation.Token);
        }

        public async Task<ClientWebSocket> ConnectAsync()
        {
            var client = new ClientWebSocket();
            await client.ConnectAsync(new Uri($"ws://localhost:{Port}/"), Cancellation.Token);
            return client;
        }

        public void Dispose()
        {
            Cancellation.Cancel();
            Server.Dispose();
            Cancellation.Dispose();
        }
    }

    private static async Task<string> ReceiveAsync(ClientWebSocket socket, CancellationToken token)
    {
        var buffer = new byte[16 * 1024];
        var result = await socket.ReceiveAsync(new ArraySegment<byte>(buffer), token);
        return Encoding.UTF8.GetString(buffer, 0, result.Count);
    }

    [Fact]
    public async Task OnConnect_SendsRefreshForEverySymbolBeforeAnyUpdate()
    {
        using var harness = new Harness();
        using var client = await harness.ConnectAsync();

        var first = await ReceiveAsync(client, harness.Cancellation.Token);
        var second = await ReceiveAsync(client, harness.Cancellation.Token);

        first.Should().StartWith(@"[""refresh"",""A""");
        second.Should().StartWith(@"[""refresh"",""B""");
    }

    [Fact]
    public async Task AfterRefresh_StreamsUpdates()
    {
        using var harness = new Harness();
        using var client = await harness.ConnectAsync();

        await ReceiveAsync(client, harness.Cancellation.Token);
        await ReceiveAsync(client, harness.Cancellation.Token);

        var update = await ReceiveAsync(client, harness.Cancellation.Token);

        update.Should().StartWith(@"[""update""");
    }

    [Fact]
    public async Task Frames_AreSentOnePerWebSocketMessage()
    {
        // The relay publishes one message per quote; the consumer additionally
        // splits on newline. Neither should see a frame split across messages.
        using var harness = new Harness();
        using var client = await harness.ConnectAsync();

        var frame = await ReceiveAsync(client, harness.Cancellation.Token);

        frame.Should().StartWith("[").And.EndWith("]");
        frame.Should().NotContain("\n");
    }

    [Fact]
    public async Task Resync_ResendsRefresh()
    {
        using var harness = new Harness();
        using var client = await harness.ConnectAsync();

        await ReceiveAsync(client, harness.Cancellation.Token);
        await ReceiveAsync(client, harness.Cancellation.Token);

        await client.SendAsync(
            Encoding.UTF8.GetBytes("resync"),
            WebSocketMessageType.Text,
            endOfMessage: true,
            harness.Cancellation.Token);

        var frames = new List<string>();
        for (var i = 0; i < 12 && !frames.Any(f => f.StartsWith(@"[""refresh""")); i++)
        {
            frames.Add(await ReceiveAsync(client, harness.Cancellation.Token));
        }

        frames.Should().Contain(f => f.StartsWith(@"[""refresh"",""A"""));
    }

    [Fact]
    public async Task Reset_EmitsRecordResetFrame()
    {
        using var harness = new Harness();
        using var client = await harness.ConnectAsync();

        await client.SendAsync(
            Encoding.UTF8.GetBytes("reset A"),
            WebSocketMessageType.Text,
            endOfMessage: true,
            harness.Cancellation.Token);

        var frames = new List<string>();
        for (var i = 0; i < 12 && !frames.Any(f => f.Contains(@"[416,""0""]")); i++)
        {
            frames.Add(await ReceiveAsync(client, harness.Cancellation.Token));
        }

        frames.Should().Contain(@"[""update"",""A"",[[416,""0""]]]");
    }

    [Fact]
    public async Task SecondClient_DisplacesTheFirst()
    {
        using var harness = new Harness();
        using var first = await harness.ConnectAsync();
        await ReceiveAsync(first, harness.Cancellation.Token);

        using var second = await harness.ConnectAsync();
        var secondFirstFrame = await ReceiveAsync(second, harness.Cancellation.Token);

        secondFirstFrame.Should().StartWith(@"[""refresh""");
    }

    [Fact]
    public async Task Reconnect_StartsFromRefreshAgain()
    {
        using var harness = new Harness();

        using (var first = await harness.ConnectAsync())
        {
            await ReceiveAsync(first, harness.Cancellation.Token);
            await first.CloseAsync(
                WebSocketCloseStatus.NormalClosure, "bye", harness.Cancellation.Token);
        }

        using var reconnected = await harness.ConnectAsync();
        var frame = await ReceiveAsync(reconnected, harness.Cancellation.Token);

        frame.Should().StartWith(@"[""refresh"",""A""");
    }

    [Fact]
    public async Task NonWebSocketRequest_IsRejected()
    {
        using var harness = new Harness();
        using var http = new HttpClient();

        var response = await http.GetAsync(
            $"http://localhost:{harness.Port}/", harness.Cancellation.Token);

        response.StatusCode.Should().Be(HttpStatusCode.BadRequest);
    }
}
