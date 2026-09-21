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

    private static IAsyncEnumerable<PricingStateDto> Items(params PricingStateDto[] items) =>
        items.ToAsyncEnumerable();

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
        await sut.StreamAsync(response, dtos.ToAsyncEnumerable(), CancellationToken.None);
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
        var items = AsyncEnumerable.Empty<PricingStateDto>()
            .Concat(DelayedEmpty(TimeSpan.FromMilliseconds(150)));
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
        async IAsyncEnumerable<PricingStateDto> FastItems()
        {
            for (int i = 0; i < 20; i++)
            {
                yield return new PricingStateDto("SYM", new Dictionary<int, string> { { 1, "72" } });
                await Task.Delay(10);
            }
        }
        await sut.StreamAsync(ctx.Response, FastItems(), CancellationToken.None);
        BodyText(ms).Should().NotContain("event: heartbeat");
    }

    private static async IAsyncEnumerable<PricingStateDto> DelayedEmpty(TimeSpan delay)
    {
        await Task.Delay(delay);
        yield break;
    }
}
