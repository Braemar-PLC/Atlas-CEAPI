using Atlas.Web.Api.Mappers;
using Atlas.Web.Api.Models.Outbound;
using Atlas.Web.Core.Application.Ports;
using Atlas.Web.Core.Domain.Enumeration;
using Atlas.Web.Core.Domain.Logic;
using Atlas.Web.Core.Domain.Models;
using Atlas.Web.Ice.Catalogue;
using FluentAssertions;
using Microsoft.Extensions.Configuration;

namespace Atlas.Web.Api.Tests;

/// <summary>
/// The real thing, end to end: ICE's option list (ice-options.json, built from ICE's files of 24 Sep 2026) and the
/// options desk's counts exactly as written in appsettings.json - 12 months, 8 quarters, 8 seasons, 5 cals (Sean
/// Hays, 2026-09-25). If a count there is changed on purpose, update the matching test.
/// </summary>
public class OptionChainScreensTests
{
    // A Monday: TTF's Oct26 options expired on Fri 25 Sep, so the front expiry is Nov26.
    private static readonly DateOnly Today = new(2026, 9, 28);

    private sealed class FixedPrices : IReferencePrices
    {
        private readonly Dictionary<string, decimal> _prices = new();
        public FixedPrices With(string symbol, decimal price) { _prices[symbol] = price; return this; }
        public decimal? For(string symbol) => _prices.TryGetValue(symbol, out var price) ? price : null;
    }

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

    private static readonly JsonInstrumentCatalogue Catalogue = JsonInstrumentCatalogue.LoadEmbedded();

    private static OptionChainBuilder Builder(IReferencePrices? prices = null) =>
        new(Catalogue, RulesFromAppSettings(), prices ?? new FixedPrices());

    private static Screen Build(string key, IReferencePrices? prices = null) =>
        Builder(prices).Build(Today).Single(s => s.Key == key);

    private static List<string> Labels(Screen screen, string group) =>
        screen.Rows.Where(r => r.Group == group).Select(r => r.Label).ToList();

    private static List<string> Groups(Screen screen) => screen.Rows.Select(r => r.Group).Distinct().ToList();

    private static string KindOf(string group) =>
        group.StartsWith("Q") ? "Quarter" : group.StartsWith("Cal") ? "Cal" : group.Contains('-') || group.StartsWith("Winter") || group.StartsWith("Summer") ? "Season" : "Month";

    [Fact]
    public void AppSettings_CarryAnOptionRuleForEachOfTheFourProducts_TwelveMonthsEightQuartersEightSeasonsFiveCals()
    {
        var rules = RulesFromAppSettings();

        rules.Options.Keys.Should().BeEquivalentTo("TTF", "EUA", "WTI", "Brent");
        rules.Options.Values.Should().OnlyContain(r =>
            r.Months == 12 && r.Quarters == 8 && r.Seasons == 8 && r.Cals == 5
            && r.StrikesEachSide == 7 && r.StripStrikesEachSide == 3 && r.SeedPrice > 0);
        // Oil strikes are 0.25 or 0.50 apart and oil moves several dollars a day, so the relay is given a wider band
        // of them than of gas and carbon, whose strikes are a whole unit apart.
        rules.Options["TTF"].SubscribeStrikesEachSide.Should().Be(10);
        rules.Options["EUA"].SubscribeStrikesEachSide.Should().Be(10);
        rules.Options["WTI"].SubscribeStrikesEachSide.Should().Be(14);
        rules.Options["Brent"].SubscribeStrikesEachSide.Should().Be(14);
        rules.Options["TTF"].SubscribeStripStrikesEachSide.Should().Be(5);
        rules.Options["Brent"].SubscribeStripStrikesEachSide.Should().Be(7);
    }

    [Fact]
    public void AppSettings_SeedPrices_AreWithinReachOfWhereTheFuturesTradedOn25Sep2026()
    {
        // The seed centres the relay's band until the feed answers; a seed far from the market leaves the whole
        // chain blank (as Brent at 70 against a market of 105 did on 2026-09-25). Update these with the market.
        var seeds = RulesFromAppSettings().Options;

        seeds["TTF"].SeedPrice.Should().BeInRange(60, 85);
        seeds["EUA"].SeedPrice.Should().BeInRange(75, 100);
        seeds["WTI"].SeedPrice.Should().BeInRange(80, 105);
        seeds["Brent"].SeedPrice.Should().BeInRange(90, 120);
    }

    [Fact]
    public void Build_GivesOneChainPerProduct_ByName()
    {
        Builder().Build(Today).Select(s => s.Key).Should().Equal("xcom-brent", "xcom-eua", "xcom-ttf", "xcom-wti");
    }

    [Fact]
    public void TtfChain_ShowsTwelveMonthsThenEightQuartersEightSeasonsAndTheCalsIceLists()
    {
        var groups = Groups(Build("xcom-ttf"));

        groups.Take(12).Should().Equal(
            "Nov26", "Dec26", "Jan27", "Feb27", "Mar27", "Apr27", "May27", "Jun27", "Jul27", "Aug27", "Sep27", "Oct27");
        // The Q4 26 and Winter26 strips expired with October on 25 Sep, so the quarters begin at Q1 27 and the
        // seasons at Summer27.
        groups.Skip(12).Take(8).Should().Equal("Q1 27", "Q2 27", "Q3 27", "Q4 27", "Q1 28", "Q2 28", "Q3 28", "Q4 28");
        // ICE lists eight TTF season strips and four calendar years; with Winter26 gone, seven seasons remain and
        // the fifth cal the desk asked for does not exist yet.
        groups.Skip(20).Take(7).Should().Equal(
            "Summer27", "Winter27", "Summer28", "Winter28", "Summer29", "Winter29", "Summer30");
        groups.Skip(27).Should().Equal("Cal 27", "Cal 28", "Cal 29", "Cal 30");
    }

    [Fact]
    public void TtfChain_EachBlockIsOnItsOwnFuture_TheStripBlocksOnTheStripFutures()
    {
        var screen = Build("xcom-ttf");

        var futures = screen.Rows.Where(r => r.Option is null).ToDictionary(r => r.Group, r => (r.Label, r.Symbol));
        futures["Nov26"].Should().Be(("Nov26", "TFM 26X-ICN"));
        futures["Q1 27"].Should().Be(("Q1 27", "TFMQ 27F-ICN"));
        futures["Summer27"].Should().Be(("Summer27", "TFMS 27J-ICN"));
        futures["Cal 27"].Should().Be(("Cal 27", "TFMY 27F-ICN"));
    }

    [Fact]
    public void TtfChain_WithNov26At80_ShowsSevenWholeEuroStrikesEitherSide_UsingIcesRealSymbols()
    {
        var screen = Build("xcom-ttf", new FixedPrices().With("TFM 26X-ICN", 80));

        var labels = Labels(screen, "Nov26");
        labels.Should().HaveCount(1 + 15 * 2);
        labels[1].Should().Be("Nov26 73.00 C");
        labels[^1].Should().Be("Nov26 87.00 P");

        var call = screen.Rows.Single(r => r.Label == "Nov26 80.00 C");
        call.Symbol.Should().Be("TFO 26XC8000-ICN");
        call.Option.Should().Be(new ScreenOption(new DateOnly(2026, 10, 27), "TFM 26X-ICN", 80, OptionRight.Call));
        screen.Rows.Single(r => r.Label == "Nov26 80.00 P").Symbol.Should().Be("TFO 26XP8000-ICN");
    }

    [Fact]
    public void TtfChain_AStripBlock_ShowsThreeStrikesEitherSide_UsingIcesRealStripSymbols()
    {
        var screen = Build("xcom-ttf", new FixedPrices().With("TFMQ 27F-ICN", 80));

        var labels = Labels(screen, "Q1 27");
        labels.Should().HaveCount(1 + 7 * 2);
        screen.Rows.Single(r => r.Label == "Q1 27 80.00 C").Symbol.Should().Be("TFOQ 27FC8000-ICN");
    }

    [Fact]
    public void TtfChain_WithNoPriceYet_CentresOnTheSeedPriceInAppSettings()
    {
        var seed = RulesFromAppSettings().Options["TTF"].SeedPrice;

        var labels = Labels(Build("xcom-ttf"), "Nov26");

        labels.Should().Contain($"Nov26 {seed:0.00} C");
    }

    [Fact]
    public void EuaChain_HasTwelveMonthsAndNoStrips_EveryMonthOnADecemberFuture()
    {
        var screen = Build("xcom-eua");

        var groups = Groups(screen);
        groups.Should().HaveCount(12);
        groups.First().Should().Be("Oct26");
        groups.Should().OnlyContain(g => KindOf(g) == "Month");
        screen.Rows.Where(r => r.Option is null).Should().OnlyContain(r => r.Label.StartsWith("Dec") && r.Symbol!.EndsWith("Z-ICN"));
    }

    [Fact]
    public void OilChains_CountTheirStripsAsCalendarQuartersHalfYearsAndYears_NamedAsIceNamesThem()
    {
        var groups = Groups(Build("xcom-brent"));

        groups.Select(KindOf).Should().Equal(
            Enumerable.Repeat("Month", 12).Concat(Enumerable.Repeat("Quarter", 8))
                .Concat(Enumerable.Repeat("Season", 8)).Concat(Enumerable.Repeat("Cal", 5)));
        groups.Skip(12).Take(2).Should().Equal("Q1 27", "Q2 27");
        groups.Skip(20).Take(2).Should().Equal("Jan27-Jun27", "Jul27-Dec27");
    }

    [Fact]
    public void BrentAndWtiChains_UseIcesRealSymbols()
    {
        var wti = Build("xcom-wti", new FixedPrices().With("WBS 26X-ICE", 65));
        var brent = Build("xcom-brent", new FixedPrices().With("BRN 27F-ICE", 78.5m));

        wti.Rows.Single(r => r.Label == "Nov26 65.00 C").Symbol.Should().Be("WBS 26XC6500-ICE");
        brent.Rows.Single(r => r.Label == "Jan27 78.50 C").Symbol.Should().Be("BRN 27FC7850-ICE");
    }

    [Fact]
    public void SymbolsToSubscribe_ReachWiderThanTheScreen_SoTheFutureMayMoveBeforeTheRelayIsRestarted()
    {
        var prices = new FixedPrices().With("TFM 26X-ICN", 80);

        var symbols = Builder(prices).SymbolsToSubscribe(Today);

        symbols.Should().Contain("TFO 26XC7000-ICN").And.Contain("TFO 26XP9000-ICN");
        symbols.Should().NotContain("TFO 26XC6900-ICN");
        symbols.Should().Contain("TFM 26X-ICN").And.Contain("TFMQ 27F-ICN");
        symbols.Count.Should().BeGreaterThan(Build("xcom-ttf", prices).Symbols.Count);
    }

    [Fact]
    public void Matrix_HasTheChainsExpiriesWithEveryListedStrike()
    {
        var matrix = Builder().Matrix("TTF", Today)!;

        matrix.Expiries.Select(e => e.Label).Should().Equal(Groups(Build("xcom-ttf")));
        var nov26 = matrix.Expiries[0];
        nov26.Kind.Should().Be(StripKind.Month);
        nov26.UnderlyingSymbol.Should().Be("TFM 26X-ICN");
        nov26.Strikes.Count.Should().BeGreaterThan(50);
        nov26.Strikes.Should().Contain(80).And.BeInAscendingOrder();
        matrix.Expiries.Single(e => e.Label == "Q1 27").Kind.Should().Be(StripKind.Quarter);
    }

    [Fact]
    public void ScreenMapper_SendsTheOptionDetail_AndNullForTheFuture()
    {
        var dto = ScreenMapper.ToDto(Build("xcom-ttf", new FixedPrices().With("TFM 26X-ICN", 80)));

        dto.Rows.First().Option.Should().BeNull();
        dto.Rows.Single(r => r.Label == "Nov26 80.00 C").Option
            .Should().Be(new ScreenOptionDto("2026-10-27", "TFM 26X-ICN", 80, "C"));
    }

    [Fact]
    public void OptionMatrixMapper_SendsKindsAsWordsAndDatesAsText()
    {
        var dto = OptionMatrixMapper.ToDto(Builder().Matrix("Brent", Today)!);

        dto.Product.Should().Be("Brent");
        dto.Expiries.First().Kind.Should().Be("Month");
        dto.Expiries.First().ExpiryDate.Should().MatchRegex(@"^\d{4}-\d{2}-\d{2}$");
        dto.Expiries.Single(e => e.Label == "Jan27-Jun27").Kind.Should().Be("Season");
    }
}
