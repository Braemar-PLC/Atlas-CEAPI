using Atlas.Web.Core.Application.Ports;
using Atlas.Web.Core.Application.Services;
using Atlas.Web.Core.Domain.Logic;
using Atlas.Web.Core.Domain.Models;
using Atlas.Web.Core.Tests.Fakes;
using FluentAssertions;

namespace Atlas.Web.Core.Tests.Application.Services;

public class ScreenProviderTests
{
    /// <summary>A clock stopped at one moment (UTC), so a test decides what "now" is. Noon UTC unless told otherwise.</summary>
    private sealed class FixedClock : TimeProvider
    {
        private readonly DateTimeOffset _now;
        public FixedClock(int year, int month, int day, int hour = 12, int minute = 0) =>
            _now = new DateTimeOffset(year, month, day, hour, minute, 0, TimeSpan.Zero);
        public override DateTimeOffset GetUtcNow() => _now;
    }

    private static ScreenProvider Provider(TimeProvider clock, bool withOptions = false)
    {
        var catalogue = new FakeInstrumentCatalogue().WithStrips("TTF", new DateOnly(2026, 10, 1), 36);
        var rules = new ScreenRules { Flat = new FlatRule { Months = 2 } };
        if (withOptions)
        {
            catalogue
                .WithOptionChain("TTF", "Nov26", new DateOnly(2026, 10, 27), "TTF:Nov26", 79, 80, 81)
                .WithOptionChain("TTF", "Dec26", new DateOnly(2026, 11, 26), "TTF:Dec26", 79, 80, 81);
            rules.Options["TTF"] = new OptionRule { Months = 1, StrikesEachSide = 0, SubscribeStrikesEachSide = 1, SeedPrice = 80 };
        }
        return new ScreenProvider(
            new ScreenBuilder(catalogue, rules),
            new OptionChainBuilder(catalogue, rules, new FakeReferencePrices()),
            clock);
    }

    [Fact]
    public void GetScreens_ListsTheOptionChainsAfterTheFlatAndSpreadScreens()
    {
        var sut = Provider(new FixedClock(2026, 9, 21), withOptions: true);

        sut.GetScreens().Select(s => s.Key).Should().EndWith(new[] { ScreenBuilder.CoalSpreadsKey, "xcom-ttf" });
        sut.Find("xcom-ttf")!.Rows.Select(r => r.Label).Should().Equal("Nov26", "Nov26 80.00 C", "Nov26 80.00 P");
    }

    [Fact]
    public void SymbolsToSubscribe_IncludesTheChainsWiderBand()
    {
        var sut = Provider(new FixedClock(2026, 9, 21), withOptions: true);

        sut.SymbolsToSubscribe().Should().Contain(new[] { "TTF:Nov26:79C", "TTF:Nov26:81P" });
    }

    [Fact]
    public void Options_RollAt1400Amsterdam_OnTheirExpiryDay_WhileTheFuturesStay()
    {
        // 27 Oct 2026 is winter time (UTC+1): 12:59 UTC is 13:59 in Amsterdam, 13:00 UTC is 14:00.
        var before = Provider(new FixedClock(2026, 10, 27, 12, 59), withOptions: true);
        var after = Provider(new FixedClock(2026, 10, 27, 13, 0), withOptions: true);

        before.Find("xcom-ttf")!.Rows.First().Label.Should().Be("Nov26");
        after.Find("xcom-ttf")!.Rows.First().Label.Should().Be("Dec26");
        // The Nov26 future expires two days before 1 Nov in the fake catalogue, so it is still the front future.
        after.Find(ScreenBuilder.TtfFlatKey)!.Rows.First().Label.Should().Be("Nov26");
    }

    [Fact]
    public void Futures_RollAt1800Amsterdam_OnTheirExpiryDay()
    {
        // The fake Oct26 future expires on 29 Sep 2026 (summer time, UTC+2): 15:59 UTC is 17:59, 16:00 UTC is 18:00.
        var before = Provider(new FixedClock(2026, 9, 29, 15, 59));
        var after = Provider(new FixedClock(2026, 9, 29, 16, 0));

        before.Find(ScreenBuilder.TtfFlatKey)!.Rows.First().Label.Should().Be("Oct26");
        after.Find(ScreenBuilder.TtfFlatKey)!.Rows.First().Label.Should().Be("Nov26");
    }

    [Fact]
    public void Find_Matrix_AnswersTheProductsMatrixForToday_AndNullForAProductWithoutRules()
    {
        IOptionMatrixProvider sut = Provider(new FixedClock(2026, 9, 21), withOptions: true);

        sut.Find("ttf")!.Expiries.Single().Strikes.Should().Equal(79, 80, 81);
        sut.Find("EUA").Should().BeNull();
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
