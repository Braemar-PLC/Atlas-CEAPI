using Atlas.Web.Core.Domain.Enumeration;
using Atlas.Web.Core.Domain.Logic;
using Atlas.Web.Core.Domain.Models;
using Atlas.Web.Core.Tests.Fakes;
using FluentAssertions;

namespace Atlas.Web.Core.Tests.Domain.Services;

public class ScreenBuilderTests
{
    // In the fake catalogue Oct26 (and Q4 26, Winter26) expire two days before 1 October: on 29 Sep 2026.
    private static readonly DateOnly MidSeptember = new(2026, 9, 21);
    private static readonly DateOnly Oct26ExpiryDay = new(2026, 9, 29);
    private static readonly DateOnly DayAfterOct26Expires = new(2026, 9, 30);

    private static FakeInstrumentCatalogue Catalogue() =>
        new FakeInstrumentCatalogue()
            .WithStrips("TTF", new DateOnly(2026, 10, 1), 60)
            .WithStrips("NBP", new DateOnly(2026, 10, 1), 60);

    private static ScreenRules Rules(
        int months = 3, int quarters = 2, int seasons = 2, int cals = 1,
        Action<ScreenRules>? configure = null)
    {
        var rules = new ScreenRules
        {
            Flat = new FlatRule { Months = months, Quarters = quarters, Seasons = seasons, Cals = cals },
        };
        configure?.Invoke(rules);
        return rules;
    }

    private static Screen Build(FakeInstrumentCatalogue catalogue, ScreenRules rules, DateOnly today, string key) =>
        new ScreenBuilder(catalogue, rules).Build(today).Single(s => s.Key == key);

    private static IEnumerable<string> Labels(Screen screen, string group) =>
        screen.Rows.Where(r => r.Group == group).Select(r => r.Label);

    [Fact]
    public void Build_FlatScreen_TakesTheConfiguredCountsFromTheFrontMonth()
    {
        var screen = Build(Catalogue(), Rules(), MidSeptember, ScreenBuilder.TtfFlatKey);

        Labels(screen, "Months").Should().Equal("Oct26", "Nov26", "Dec26");
        Labels(screen, "Quarters").Should().Equal("Q4 26", "Q1 27");
        Labels(screen, "Seasons").Should().Equal("Winter26", "Summer27");
        Labels(screen, "Cals").Should().Equal("Cal 27");
    }

    [Fact]
    public void Build_FlatScreen_OrdersTheBlocksMonthsQuartersSeasonsCals()
    {
        var screen = Build(Catalogue(), Rules(), MidSeptember, ScreenBuilder.TtfFlatKey);

        screen.Rows.Select(r => r.Group).Distinct().Should().Equal("Months", "Quarters", "Seasons", "Cals");
    }

    [Fact]
    public void Build_FlatRows_AreQuotedWithTheExchangeSymbol()
    {
        var screen = Build(Catalogue(), Rules(), MidSeptember, ScreenBuilder.TtfFlatKey);

        var row = screen.Rows.First();
        row.Source.Should().Be(RowSource.Quoted);
        row.Symbol.Should().Be("TTF:Oct26");
        row.Hub.Should().Be("TTF");
    }

    [Fact]
    public void Build_OnTheExpiryDay_StillShowsTheExpiringStrips()
    {
        var screen = Build(Catalogue(), Rules(), Oct26ExpiryDay, ScreenBuilder.TtfFlatKey);

        Labels(screen, "Months").First().Should().Be("Oct26");
        Labels(screen, "Quarters").First().Should().Be("Q4 26");
        Labels(screen, "Seasons").First().Should().Be("Winter26");
    }

    [Fact]
    public void Build_TheDayAfterTheFrontMonthExpires_RollsEveryBlockAlthoughItIsStillSeptember()
    {
        var screen = Build(Catalogue(), Rules(), DayAfterOct26Expires, ScreenBuilder.TtfFlatKey);

        Labels(screen, "Months").Should().Equal("Nov26", "Dec26", "Jan27");
        Labels(screen, "Quarters").Should().Equal("Q1 27", "Q2 27");
        Labels(screen, "Seasons").Should().Equal("Summer27", "Winter27");
        // Cal 27 trades until the end of December, so it does not move.
        Labels(screen, "Cals").Should().Equal("Cal 27");
    }

    [Fact]
    public void Build_WhenTheExchangeListsFewerStripsThanAskedFor_ShowsWhatExists()
    {
        var catalogue = new FakeInstrumentCatalogue().WithStrips("TTF", new DateOnly(2026, 10, 1), 4);

        var screen = Build(catalogue, Rules(months: 20), MidSeptember, ScreenBuilder.TtfFlatKey);

        Labels(screen, "Months").Should().Equal("Oct26", "Nov26", "Dec26", "Jan27");
    }

    [Fact]
    public void Build_WhenEveryListedMonthHasExpired_GivesEmptyScreensRatherThanThrowing()
    {
        var screens = new ScreenBuilder(Catalogue(), Rules()).Build(new DateOnly(2040, 1, 1));

        screens.Should().OnlyContain(s => s.Rows.Count == 0);
    }

    [Fact]
    public void Build_SpreadTheExchangeQuotes_UsesTheExchangeSymbol()
    {
        var catalogue = Catalogue().WithBackToBackSpreads("TTF", StripKind.Month);
        var rules = Rules(configure: r => r.Spreads.Months.Add(new PairRule { Gap = 1, Count = 2 }));

        var screen = Build(catalogue, rules, MidSeptember, ScreenBuilder.TtfSpreadsKey);

        screen.Rows.Select(r => r.Label).Should().Equal("Oct26/Nov26", "Nov26/Dec26");
        screen.Rows.Should().OnlyContain(r => r.Source == RowSource.Quoted && r.Near == null && r.Far == null);
        screen.Rows[0].Symbol.Should().Be("TTF:Oct26/Nov26");
    }

    [Fact]
    public void Build_SpreadTheExchangeDoesNotQuote_IsComputedFromItsTwoLegs()
    {
        // No quoted spreads at all in this catalogue.
        var rules = Rules(configure: r => r.Spreads.Quarters.Add(new PairRule { Gap = 2, Count = 1 }));

        var screen = Build(Catalogue(), rules, MidSeptember, ScreenBuilder.TtfSpreadsKey);

        var row = screen.Rows.Single();
        row.Label.Should().Be("Q4 26/Q2 27");
        row.Source.Should().Be(RowSource.Computed);
        row.Symbol.Should().BeNull();
        row.Near.Should().Be(new ScreenLeg("Q4 26", "TTF:Q4 26"));
        row.Far.Should().Be(new ScreenLeg("Q2 27", "TTF:Q2 27"));
    }

    [Fact]
    public void Build_SeasonGapOfTwo_PairsTheSameSeasonAYearOn()
    {
        var rules = Rules(configure: r => r.Spreads.Seasons.Add(new PairRule { Gap = 2, Count = 2 }));

        var screen = Build(Catalogue(), rules, MidSeptember, ScreenBuilder.TtfSpreadsKey);

        screen.Rows.Select(r => r.Label).Should().Equal("Winter26/Winter27", "Summer27/Summer28");
    }

    [Fact]
    public void Build_SpreadRules_RollWithTheFrontMonth()
    {
        var catalogue = Catalogue().WithBackToBackSpreads("TTF", StripKind.Month);
        var rules = Rules(configure: r => r.Spreads.Months.Add(new PairRule { Gap = 1, Count = 2 }));

        var screen = Build(catalogue, rules, DayAfterOct26Expires, ScreenBuilder.TtfSpreadsKey);

        screen.Rows.Select(r => r.Label).Should().Equal("Nov26/Dec26", "Dec26/Jan27");
    }

    [Fact]
    public void Build_ExtraSpreadAcrossTwoKinds_IsComputedBecauseNoSuchContractExists()
    {
        var rules = Rules(configure: r => r.ExtraSpreads["TTF"] = new List<string> { "Feb27/Q3 27" });

        var screen = Build(Catalogue(), rules, MidSeptember, ScreenBuilder.TtfSpreadsKey);

        var row = screen.Rows.Single();
        row.Label.Should().Be("Feb27/Q3 27");
        row.Source.Should().Be(RowSource.Computed);
        row.Near!.Symbol.Should().Be("TTF:Feb27");
        row.Far!.Symbol.Should().Be("TTF:Q3 27");
    }

    [Fact]
    public void Build_ExtraSpreadTheExchangeQuotes_UsesTheExchangeSymbol()
    {
        var catalogue = Catalogue().WithQuotedSpread("TTF", "Apr27", "Aug27");
        var rules = Rules(configure: r => r.ExtraSpreads["TTF"] = new List<string> { "Apr27/Aug27" });

        var screen = Build(catalogue, rules, MidSeptember, ScreenBuilder.TtfSpreadsKey);

        screen.Rows.Single().Symbol.Should().Be("TTF:Apr27/Aug27");
    }

    [Fact]
    public void Build_ExtraSpreadWhoseNearLegHasExpired_DropsOffByItself()
    {
        var rules = Rules(configure: r => r.ExtraSpreads["TTF"] = new List<string> { "Oct26/Dec26", "Nov26/Jan27" });

        var screen = Build(Catalogue(), rules, DayAfterOct26Expires, ScreenBuilder.TtfSpreadsKey);

        screen.Rows.Select(r => r.Label).Should().Equal("Nov26/Jan27");
    }

    [Fact]
    public void Build_ExtraSpreadThatIsMisspelt_IsIgnoredRatherThanBreakingTheScreen()
    {
        var rules = Rules(configure: r => r.ExtraSpreads["TTF"] = new List<string> { "Octember26/Nov26", "no-slash", "Nov26/Dec26" });

        var screen = Build(Catalogue(), rules, MidSeptember, ScreenBuilder.TtfSpreadsKey);

        screen.Rows.Select(r => r.Label).Should().Equal("Nov26/Dec26");
    }

    [Fact]
    public void Build_ExtraSpreadAlreadyInTheRegularList_AppearsOnce()
    {
        var rules = Rules(configure: r =>
        {
            r.Spreads.Months.Add(new PairRule { Gap = 1, Count = 1 });
            r.ExtraSpreads["TTF"] = new List<string> { "Oct26/Nov26" };
        });

        var screen = Build(Catalogue(), rules, MidSeptember, ScreenBuilder.TtfSpreadsKey);

        screen.Rows.Select(r => r.Label).Should().Equal("Oct26/Nov26");
    }

    [Fact]
    public void Build_SpreadRows_AreOrderedByKindThenNearLegThenFarLeg()
    {
        var rules = Rules(configure: r =>
        {
            r.Spreads.Quarters.Add(new PairRule { Gap = 1, Count = 1 });
            r.Spreads.Months.Add(new PairRule { Gap = 1, Count = 2 });
            r.ExtraSpreads["TTF"] = new List<string> { "Oct26/Jan27", "Oct26/Q1 27" };
        });

        var screen = Build(Catalogue(), rules, MidSeptember, ScreenBuilder.TtfSpreadsKey);

        screen.Rows.Select(r => r.Label).Should().Equal(
            "Oct26/Nov26", "Oct26/Jan27", "Oct26/Q1 27", "Nov26/Dec26", "Q4 26/Q1 27");
    }

    [Fact]
    public void Build_ExtraSpreads_ApplyOnlyToTheirOwnHub()
    {
        var rules = Rules(configure: r => r.ExtraSpreads["TTF"] = new List<string> { "Nov26/Jan27" });

        var nbp = Build(Catalogue(), rules, MidSeptember, ScreenBuilder.NbpKey);

        nbp.Rows.Should().NotContain(r => r.Group == ScreenBuilder.SpreadsGroup);
    }

    [Fact]
    public void Build_NbpScreen_ShowsFlatRowsThenSpreadsBelow()
    {
        var catalogue = Catalogue().WithBackToBackSpreads("NBP", StripKind.Month);
        var rules = Rules(configure: r => r.Spreads.Months.Add(new PairRule { Gap = 1, Count = 1 }));

        var screen = Build(catalogue, rules, MidSeptember, ScreenBuilder.NbpKey);

        screen.Rows.Should().OnlyContain(r => r.Hub == "NBP");
        screen.Rows.Select(r => r.Group).Distinct().Should().Equal("Months", "Quarters", "Seasons", "Cals", "Spreads");
        screen.Rows.Last().Symbol.Should().Be("NBP:Oct26/Nov26");
    }

    [Fact]
    public void Screen_Symbols_IncludeBothLegsOfAComputedRowAndNoRepeats()
    {
        var rules = Rules(months: 1, quarters: 0, seasons: 0, cals: 0,
            configure: r => r.ExtraSpreads["NBP"] = new List<string> { "Oct26/Q1 27" });

        var screen = Build(Catalogue(), rules, MidSeptember, ScreenBuilder.NbpKey);

        // Oct26 is both a flat row and a leg; it is listed once.
        screen.Symbols.Should().Equal("NBP:Oct26", "NBP:Q1 27");
    }
}
