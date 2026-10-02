using Atlas.Web.Core.Domain.Enumeration;
using Atlas.Web.Core.Domain.Logic;
using FluentAssertions;

namespace Atlas.Web.Core.Tests.Domain.Services;

public sealed class FeedHealthStoreTests
{
    [Fact]
    public void Current_WhenHeartbeatExpires_MarksFeedStale()
    {
        var time = new TestTimeProvider();
        using var sut = new FeedHealthStore(time, TimeSpan.FromSeconds(15));
        sut.RecordRelayStatus(FeedState.Live, 1, time.GetUtcNow(), "Connected", 100);

        time.Advance(TimeSpan.FromSeconds(16));

        sut.Current().State.Should().Be(FeedState.Stale);
    }

    [Fact]
    public void RecordRelayStatus_IgnoresOlderGeneration()
    {
        var time = new TestTimeProvider();
        using var sut = new FeedHealthStore(time, TimeSpan.FromSeconds(15));
        sut.RecordRelayStatus(FeedState.Live, 2, time.GetUtcNow(), "Current", 100);
        sut.RecordRelayStatus(FeedState.Reconnecting, 1, time.GetUtcNow(), "Old", 0);

        sut.Current().State.Should().Be(FeedState.Live);
        sut.Current().Generation.Should().Be(2);
    }

    [Fact]
    public void RecordQuote_TracksLastQuoteTime()
    {
        var time = new TestTimeProvider();
        using var sut = new FeedHealthStore(time, TimeSpan.FromSeconds(15));

        sut.RecordQuote();

        sut.Current().LastQuoteAt.Should().Be(time.GetUtcNow());
    }

    private sealed class TestTimeProvider : TimeProvider
    {
        private DateTimeOffset _now = new(2026, 9, 30, 13, 0, 0, TimeSpan.Zero);

        public override DateTimeOffset GetUtcNow() => _now;

        public void Advance(TimeSpan duration) => _now += duration;
    }
}
