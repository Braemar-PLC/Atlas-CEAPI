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

    [Fact]
    public void RelayConnecting_ClearsPreviousRelaySessionButPreservesLastQuoteTime()
    {
        var time = new TestTimeProvider();
        using var sut = new FeedHealthStore(time, TimeSpan.FromSeconds(15));
        sut.RecordRelayStatus(FeedState.Live, 5, time.GetUtcNow(), "Connected", 4056);
        sut.RecordQuote();
        var lastQuoteAt = time.GetUtcNow();
        sut.RelayDisconnected("Container restarted");

        sut.RelayConnecting();

        var current = sut.Current();
        current.State.Should().Be(FeedState.Connecting);
        current.Generation.Should().Be(0);
        current.RelayTimestamp.Should().BeNull();
        current.SubscribedSymbols.Should().Be(0);
        current.LastQuoteAt.Should().Be(lastQuoteAt);
        current.Detail.Should().Be("Connecting to the CEAPI relay");
    }

    [Theory]
    [InlineData(FeedState.Live)]
    [InlineData(FeedState.Disconnected)]
    [InlineData(FeedState.AuthenticationFailed)]
    public void RecordRelayStatus_AfterRelayReconnect_AcceptsRestartedGeneration(FeedState state)
    {
        var time = new TestTimeProvider();
        using var sut = new FeedHealthStore(time, TimeSpan.FromSeconds(15));
        sut.RecordRelayStatus(FeedState.Live, 5, time.GetUtcNow(), "Previous process", 4056);
        sut.RelayDisconnected("Container restarted");
        sut.RelayConnecting();
        time.Advance(TimeSpan.FromSeconds(20));

        sut.RecordRelayStatus(state, 1, time.GetUtcNow(), "New process", 3866);

        var current = sut.Current();
        current.State.Should().Be(state);
        current.Generation.Should().Be(1);
        current.RelayTimestamp.Should().Be(time.GetUtcNow());
        current.Detail.Should().Be("New process");
        current.SubscribedSymbols.Should().Be(3866);
    }

    [Fact]
    public void RecordRelayStatus_AfterRelayReconnect_StillRejectsOlderGenerationWithinConnection()
    {
        var time = new TestTimeProvider();
        using var sut = new FeedHealthStore(time, TimeSpan.FromSeconds(15));
        sut.RecordRelayStatus(FeedState.Live, 5, time.GetUtcNow(), "Previous process", 4056);
        sut.RelayDisconnected("Container restarted");
        sut.RelayConnecting();
        sut.RecordRelayStatus(FeedState.Live, 2, time.GetUtcNow(), "Current", 3866);
        time.Advance(TimeSpan.FromSeconds(16));

        sut.RecordRelayStatus(FeedState.Reconnecting, 1, time.GetUtcNow(), "Old", 0);

        var current = sut.Current();
        current.Generation.Should().Be(2);
        current.State.Should().Be(FeedState.Stale);
        current.SubscribedSymbols.Should().Be(3866);
    }

    private sealed class TestTimeProvider : TimeProvider
    {
        private DateTimeOffset _now = new(2026, 9, 30, 13, 0, 0, TimeSpan.Zero);

        public override DateTimeOffset GetUtcNow() => _now;

        public void Advance(TimeSpan duration) => _now += duration;
    }
}
