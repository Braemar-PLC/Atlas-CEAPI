using Atlas.Web.Core.Domain.Enumeration;
using Atlas.Web.Core.Domain.Logic;
using Atlas.Web.Core.Domain.Models;
using Atlas.Web.Ice.Catalogue;
using FluentAssertions;
using Microsoft.Extensions.Configuration;

namespace Atlas.Web.Api.Tests;

/// <summary>
/// The real thing, end to end: ICE's contract list (ice-instruments.json, built from ICE's files of 17 Sep 2026)
/// and the desk's counts exactly as written in appsettings.json. These pin what the gas desk (Harrison Lee)
/// agreed on 2026-09-21. If a count in appsettings.json is changed on purpose, update the matching test.
/// </summary>
public class DeskScreensTests
{
    private static readonly DateOnly AgreedOn = new(2026, 9, 21);
    // TTF and NBP Oct26, Q4 26 and Winter26 all stop trading on 29 Sep 2026.
    private static readonly DateOnly DayAfterOct26Expires = new(2026, 9, 30);

    private static ScreenRules RulesFromAppSettings()
    {
        var configuration = new ConfigurationBuilder()
            .SetBasePath(AppContext.BaseDirectory)
            .AddJsonFile("appsettings.json", optional: false)
            .Build();

        var rules = new ScreenRules();
        configuration.GetSection(ScreenRules.Section).Bind(rules);
        return rules;
    }

    private static Screen Build(string key, DateOnly today) =>
        new ScreenBuilder(JsonInstrumentCatalogue.LoadEmbedded(), RulesFromAppSettings())
            .Build(today)
            .Single(s => s.Key == key);

    private static List<string> Labels(Screen screen, string group) =>
        screen.Rows.Where(r => r.Group == group).Select(r => r.Label).ToList();

    [Fact]
    public void AppSettings_CarryTheCountsTheDeskAgreed()
    {
        var rules = RulesFromAppSettings();

        rules.Flat.Months.Should().Be(20);
        rules.Flat.Quarters.Should().Be(10);
        rules.Flat.Seasons.Should().Be(8);
        rules.Flat.Cals.Should().Be(5);
    }

    [Fact]
    public void AppSettings_SpreadRulesAreNotDoubledByTheBinder()
    {
        // The configuration binder appends to a list that already has items. ScreenRules starts its lists
        // empty for that reason; this fails if someone gives them defaults.
        var rules = RulesFromAppSettings();

        rules.Spreads.Months.Should().HaveCount(1);
        rules.Spreads.Seasons.Should().HaveCount(2);
    }

    [Theory]
    [InlineData(ScreenBuilder.TtfFlatKey, "TTF")]
    [InlineData(ScreenBuilder.NbpKey, "NBP")]
    public void FlatRows_OnTheDayAgreed_AreTwentyMonthsTenQuartersEightSeasonsFiveCals(string key, string hub)
    {
        var screen = Build(key, AgreedOn);

        var months = Labels(screen, "Months");
        months.Should().HaveCount(20);
        months.First().Should().Be("Oct26");
        months.Last().Should().Be("May28");

        Labels(screen, "Quarters").Should().Equal(
            "Q4 26", "Q1 27", "Q2 27", "Q3 27", "Q4 27", "Q1 28", "Q2 28", "Q3 28", "Q4 28", "Q1 29");
        Labels(screen, "Seasons").Should().Equal(
            "Winter26", "Summer27", "Winter27", "Summer28", "Winter28", "Summer29", "Winter29", "Summer30");
        Labels(screen, "Cals").Should().Equal("Cal 27", "Cal 28", "Cal 29", "Cal 30", "Cal 31");

        screen.Rows.Where(r => r.Group != ScreenBuilder.SpreadsGroup).Should().OnlyContain(r => r.Hub == hub);
    }

    [Fact]
    public void FlatRows_MatchHarrisonsEdgeviewMonthForMonth()
    {
        Labels(Build(ScreenBuilder.TtfFlatKey, AgreedOn), "Months").Should().Equal(
            "Oct26", "Nov26", "Dec26", "Jan27", "Feb27", "Mar27", "Apr27", "May27", "Jun27", "Jul27",
            "Aug27", "Sep27", "Oct27", "Nov27", "Dec27", "Jan28", "Feb28", "Mar28", "Apr28", "May28");
    }

    [Fact]
    public void FlatRows_TheDayAfterOct26Expires_RollAlthoughItIsStillSeptember()
    {
        var screen = Build(ScreenBuilder.TtfFlatKey, DayAfterOct26Expires);

        Labels(screen, "Months").First().Should().Be("Nov26");
        Labels(screen, "Months").Last().Should().Be("Jun28");
        Labels(screen, "Quarters").First().Should().Be("Q1 27");
        Labels(screen, "Quarters").Last().Should().Be("Q2 29");
        Labels(screen, "Seasons").First().Should().Be("Summer27");
        Labels(screen, "Seasons").Last().Should().Be("Winter30");
        // Cal 27 trades until 30 December, so the calendar years do not move.
        Labels(screen, "Cals").Should().Equal("Cal 27", "Cal 28", "Cal 29", "Cal 30", "Cal 31");
        screen.Rows.Should().HaveCount(43);
    }

    [Fact]
    public void FlatRows_UseIcesRealSymbols()
    {
        var ttf = Build(ScreenBuilder.TtfFlatKey, AgreedOn);
        var nbp = Build(ScreenBuilder.NbpKey, AgreedOn);

        ttf.Rows.Single(r => r.Label == "Oct26").Symbol.Should().Be("TFM 26V-ICN");
        ttf.Rows.Single(r => r.Label == "Q4 26").Symbol.Should().Be("TFMQ 26V-ICN");
        ttf.Rows.Single(r => r.Label == "Winter26").Symbol.Should().Be("TFMS 26V-ICN");
        ttf.Rows.Single(r => r.Label == "Cal 27").Symbol.Should().Be("TFMY 27F-ICN");
        nbp.Rows.First(r => r.Label == "Oct26").Symbol.Should().Be("GWM 26V-ICE");
    }

    [Fact]
    public void TtfSpreads_EveryRowIsQuotedByIce_BecauseTheDeskListIsAllVanilla()
    {
        var screen = Build(ScreenBuilder.TtfSpreadsKey, AgreedOn);

        screen.Rows.Should().OnlyContain(r => r.Source == RowSource.Quoted);
        screen.Rows.Single(r => r.Label == "Oct26/Nov26").Symbol.Should().Be("TFM 26V:TFM26X-ICN");
        screen.Rows.Single(r => r.Label == "Winter26/Summer27").Symbol.Should().Be("TFMS 26V:TFMS27J-ICN");
    }

    [Fact]
    public void TtfSpreads_ContainEveryRowReadOffTheDesksWallScreen()
    {
        // The 57 rows photographed on the WebICE "Nat Gas TTF Spreads" screen on 21 Sep 2026
        // (its first rows, with near legs Oct26 to Jan27, were out of shot).
        var wallScreen = new[]
        {
            "Feb27/Mar27", "Mar27/Apr27", "Apr27/May27", "Apr27/Jun27", "Apr27/Aug27", "May27/Jun27", "May27/Jul27",
            "May27/Aug27", "Jun27/Jul27", "Jun27/Sep27", "Jul27/Aug27", "Jul27/Sep27", "Jul27/Sep28", "Aug27/Sep27",
            "Sep27/Oct27", "Sep27/Feb28", "Oct27/Nov27", "Oct27/Dec27", "Oct27/Jan28", "Dec27/Jan28", "Apr28/May28",
            "Sep29/Sep30",
            "Winter26/Summer27", "Winter26/Winter27", "Summer27/Winter27", "Summer27/Summer28", "Winter27/Summer28",
            "Winter27/Winter28", "Summer28/Winter28", "Winter28/Summer29", "Winter28/Winter29", "Summer29/Winter29",
            "Winter29/Summer30", "Summer30/Winter30", "Summer31/Winter31",
            "Q4 26/Q1 27", "Q4 26/Q2 27", "Q1 27/Q2 27", "Q1 27/Q3 27", "Q1 27/Q4 27", "Q2 27/Q3 27", "Q2 27/Q4 27",
            "Q3 27/Q4 27", "Q3 27/Q1 28", "Q4 27/Q1 28", "Q1 28/Q2 28", "Q1 28/Q4 28", "Q2 28/Q3 28", "Q3 28/Q4 28",
            "Q3 28/Q1 29", "Q4 28/Q1 29", "Q4 29/Q1 30", "Q1 30/Q2 30",
            "Cal 27/Cal 28", "Cal 28/Cal 29", "Cal 29/Cal 30", "Cal 30/Cal 31",
        };

        var labels = Build(ScreenBuilder.TtfSpreadsKey, AgreedOn).Rows.Select(r => r.Label);

        labels.Should().Contain(wallScreen);
    }

    [Fact]
    public void NbpSpreads_FallBackToComputed_OnlyWhereIceHasNoMarket()
    {
        var screen = Build(ScreenBuilder.NbpKey, AgreedOn);

        // ICE lists no NBP spread between a quarter and the quarter after next.
        var computed = screen.Rows.Where(r => r.Source == RowSource.Computed).ToList();
        computed.Select(r => r.Label).Should().Equal("Q4 26/Q2 27", "Q1 27/Q3 27", "Q2 27/Q4 27", "Q3 27/Q1 28");
        computed[0].Near.Should().Be(new ScreenLeg("Q4 26", "GWMQ 26V-ICE"));
        computed[0].Far.Should().Be(new ScreenLeg("Q2 27", "GWMQ 27J-ICE"));

        screen.Rows.Single(r => r.Label == "Oct26/Nov26").Symbol.Should().Be("GWM 26V:GWM26X-ICE");
    }

    [Fact]
    public void NbpScreen_ShowsFlatPricesThenSpreadsBelow()
    {
        var groups = Build(ScreenBuilder.NbpKey, AgreedOn).Rows.Select(r => r.Group).Distinct();

        groups.Should().Equal("Months", "Quarters", "Seasons", "Cals", "Spreads");
    }

    [Fact]
    public void ExtraSpreads_WhoseNearLegHasExpired_AreGone()
    {
        // Apr27 expires on 30 Mar 2027, so by May 2027 the desk's Apr27/Jun27 and Apr27/Aug27 picks have dropped off.
        var labels = Build(ScreenBuilder.TtfSpreadsKey, new DateOnly(2027, 5, 3)).Rows.Select(r => r.Label).ToList();

        labels.Should().NotContain(new[] { "Apr27/Jun27", "Apr27/Aug27" });
        labels.Should().Contain("Jul27/Sep28");
    }
}
