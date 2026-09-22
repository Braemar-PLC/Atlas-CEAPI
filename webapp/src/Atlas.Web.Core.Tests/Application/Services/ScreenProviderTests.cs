using Atlas.Web.Core.Application.Services;
using Atlas.Web.Core.Domain.Logic;
using Atlas.Web.Core.Domain.Models;
using Atlas.Web.Core.Tests.Fakes;
using FluentAssertions;

namespace Atlas.Web.Core.Tests.Application.Services;

public class ScreenProviderTests
{
    /// <summary>A clock stopped at one moment, so a test decides what "today" is.</summary>
    private sealed class FixedClock : TimeProvider
    {
        private readonly DateTimeOffset _now;
        public FixedClock(int year, int month, int day) => _now = new DateTimeOffset(year, month, day, 12, 0, 0, TimeSpan.Zero);
        public override DateTimeOffset GetUtcNow() => _now;
    }

    private static ScreenProvider Provider(TimeProvider clock)
    {
        var catalogue = new FakeInstrumentCatalogue().WithStrips("TTF", new DateOnly(2026, 10, 1), 36);
        var rules = new ScreenRules { Flat = new FlatRule { Months = 2 } };
        return new ScreenProvider(new ScreenBuilder(catalogue, rules), clock);
    }

    [Fact]
    public void GetScreens_BuildsForTodaysDate()
    {
        var sut = Provider(new FixedClock(2026, 9, 30)); // the day after Oct26 expires in the fake catalogue

        var flat = sut.GetScreens().Single(s => s.Key == ScreenBuilder.TtfFlatKey);

        flat.Rows.Select(r => r.Label).Should().Equal("Nov26", "Dec26");
    }

    [Fact]
    public void Find_KnownKey_ReturnsThatScreenWhateverTheCase()
    {
        var sut = Provider(new FixedClock(2026, 9, 21));

        sut.Find("TTF-Flat")!.Key.Should().Be(ScreenBuilder.TtfFlatKey);
    }

    [Fact]
    public void Find_UnknownKey_ReturnsNull()
    {
        var sut = Provider(new FixedClock(2026, 9, 21));

        sut.Find("coal").Should().BeNull();
    }

    [Fact]
    public void SymbolsToSubscribe_IncludesTheStripThatRollsInAtTheNextExpiry()
    {
        var sut = Provider(new FixedClock(2026, 9, 21));

        // Today the screen shows Oct26 and Nov26. Within the 45-day look-ahead both expire and Dec26 and Jan27
        // roll in, so a relay started today must already be subscribed to those too.
        sut.SymbolsToSubscribe().Should().Equal("TTF:Oct26", "TTF:Nov26", "TTF:Dec26", "TTF:Jan27");
    }
}
