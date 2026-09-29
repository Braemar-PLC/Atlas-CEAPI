using System.Reactive.Linq;
using System.Runtime.ExceptionServices;
using System.Text;
using FluentAssertions;
using Microsoft.AspNetCore.Http;
using Atlas.Web.Api.Services;
using Atlas.Web.Api.Models.Outbound;


namespace Atlas.Web.Api.Tests;

public class SseWriterTests
{
    private static (SseWriter writer, HttpResponse response, MemoryStream body) Build(
        TimeSpan? heartbeatInterval = null)
    {
        var writer = new SseWriter(heartbeatInterval ?? TimeSpan.FromSeconds(30));
        var ctx = new DefaultHttpContext();
        var ms = new MemoryStream();
        ctx.Response.Body = ms;
        return (writer, ctx.Response, ms);
    }
    private static string BodyText(MemoryStream ms) =>
        Encoding.UTF8.GetString(ms.ToArray());

    private static IObservable<PricingStateDto> Items(params PricingStateDto[] items) =>
        items.ToObservable();

    [Fact]
    public async Task StreamAsync_SetsContentTypeHeader()
    {
        var (sut, response, _) = Build();
        await sut.StreamAsync(response, Items(), CancellationToken.None);
        response.Headers.ContentType.ToString().Should().Be("text/event-stream");
    }

    [Fact]
    public async Task StreamAsync_SetsAccelBufferingHeader()
    {
        var (sut, response, _) = Build();
        await sut.StreamAsync(response, Items(), CancellationToken.None);
        response.Headers["X-Accel-Buffering"].ToString().Should().Be("no");
    }

    [Fact]
    public async Task StreamAsync_WritesCorrectEventFormat()
    {
        var (sut, response, ms) = Build();
        var dto = new PricingStateDto("SYM", new Dictionary<int, string> { { 1, "72" } });
        await sut.StreamAsync(response, Items(dto), CancellationToken.None);
        var body = BodyText(ms);
        body.Should().Contain("event: snapshot");
        body.Should().Contain("data: ");
    }

    [Fact]
    public async Task StreamAsync_SerializesDto_InDataLine()
    {
        var (sut, response, ms) = Build();
        var dto = new PricingStateDto("EURUSD", new Dictionary<int, string> { { 1, "1.08" } });
        await sut.StreamAsync(response, Items(dto), CancellationToken.None);
        BodyText(ms).Should().Contain("EURUSD");
    }

    [Fact]
    public async Task StreamAsync_EmitsOneEventPerItem()
    {
        var (sut, response, ms) = Build();
        var dtos = new[]
        {
            new PricingStateDto("SYM", new Dictionary<int, string> { { 1, "72" } }),
            new PricingStateDto("SYM", new Dictionary<int, string> { { 1, "99" } }),
        };
        await sut.StreamAsync(response, dtos.ToObservable(), CancellationToken.None);
        BodyText(ms).Split("event: snapshot").Length.Should().Be(3);
    }

    [Fact]
    public async Task StreamAsync_EmptySequence_WritesHeadersOnly()
    {
        var (sut, response, ms) = Build();
        await sut.StreamAsync(response, Items(), CancellationToken.None);
        BodyText(ms).Should().NotContain("event: snapshot");
    }

    [Fact]
    public async Task StreamAsync_SendsHeartbeat_WhenNoUpdatesArriveWithinInterval()
    {
        var ctx = new DefaultHttpContext();
        var ms = new MemoryStream();
        ctx.Response.Body = ms;
        var sut = new SseWriter(heartbeatInterval: TimeSpan.FromMilliseconds(50));
        var items = Observable.Timer(TimeSpan.FromMilliseconds(150))
            .SelectMany(_ => Observable.Empty<PricingStateDto>());
        await sut.StreamAsync(ctx.Response, items, CancellationToken.None);
        BodyText(ms).Should().Contain("event: heartbeat");
    }

    [Fact]
    public async Task StreamAsync_DoesNotSendHeartbeat_WhenUpdatesFlowFasterThanInterval()
    {
        var ctx = new DefaultHttpContext();
        var ms = new MemoryStream();
        ctx.Response.Body = ms;
        var sut = new SseWriter(heartbeatInterval: TimeSpan.FromMilliseconds(50));
        var fastItems = Observable.Interval(TimeSpan.FromMilliseconds(10))
            .Take(20)
            .Select(_ => new PricingStateDto("SYM", new Dictionary<int, string> { { 1, "72" } }));
        await sut.StreamAsync(ctx.Response, fastItems, CancellationToken.None);
        BodyText(ms).Should().NotContain("event: heartbeat");
    }

    [Fact]
    public async Task StreamAsync_ClientDisconnects_CompletesWithoutThrowing()
    {
        var (sut, response, _) = Build(TimeSpan.FromMilliseconds(20));
        using var disconnect = new CancellationTokenSource(TimeSpan.FromMilliseconds(100));

        var streaming = sut.StreamAsync(response, Observable.Never<PricingStateDto>(), disconnect.Token);

        var first = await Task.WhenAny(streaming, Task.Delay(TimeSpan.FromSeconds(2)));
        first.Should().BeSameAs(streaming, "the stream must end once the client has gone");
        await FluentActions.Awaiting(() => streaming).Should().NotThrowAsync();
    }

    [Fact]
    public async Task StreamAsync_ClientDisconnects_RaisesNoCancellationExceptionInternally()
    {
        // A client closing the page is the normal end of a stream, so it must not be signalled by an exception -
        // not even one the writer catches itself.
        var thrownBySseWriter = new List<Exception>();
        void Record(object? sender, FirstChanceExceptionEventArgs e)
        {
            if (e.Exception is OperationCanceledException && (e.Exception.StackTrace ?? "").Contains(nameof(SseWriter)))
            {
                lock (thrownBySseWriter) { thrownBySseWriter.Add(e.Exception); }
            }
        }
        var (sut, response, _) = Build(TimeSpan.FromMilliseconds(20));
        using var disconnect = new CancellationTokenSource(TimeSpan.FromMilliseconds(100));

        AppDomain.CurrentDomain.FirstChanceException += Record;
        try
        {
            await sut.StreamAsync(response, Observable.Never<PricingStateDto>(), disconnect.Token);
        }
        finally
        {
            AppDomain.CurrentDomain.FirstChanceException -= Record;
        }

        thrownBySseWriter.Should().BeEmpty();
    }
}
