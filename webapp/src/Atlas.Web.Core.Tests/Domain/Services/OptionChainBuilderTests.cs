using Atlas.Web.Core.Domain.Enumeration;
using Atlas.Web.Core.Domain.Logic;
using Atlas.Web.Core.Domain.Models;
using Atlas.Web.Core.Tests.Fakes;
using FluentAssertions;

namespace Atlas.Web.Core.Tests.Domain.Services;

public class OptionChainBuilderTests
{
    private static readonly DateOnly Today = new(2026, 9, 28);
    private static readonly DateOnly Nov26Expiry = new(2026, 10, 27);
    private static readonly DateOnly Dec26Expiry = new(2026, 11, 26);
    private static readonly DateOnly Jan27Expiry = new(2026, 12, 24);

    /// <summary>TTF futures Oct26 onwards (symbols "TTF:Nov26", "TTF:Q1 27" ...), and options at whole strikes 76 to 84 on three months.</summary>
    private static FakeInstrumentCatalogue Catalogue() =>
        new FakeInstrumentCatalogue()
            .WithStrips("TTF", new DateOnly(2026, 10, 1), 24)
            .WithOptionChain("TTF", "Nov26", Nov26Expiry, "TTF:Nov26", 76, 77, 78, 79, 80, 81, 82, 83, 84)
            .WithOptionChain("TTF", "Dec26", Dec26Expiry, "TTF:Dec26", 76, 77, 78, 79, 80, 81, 82, 83, 84)
            .WithOptionChain("TTF", "Jan27", Jan27Expiry, "TTF:Jan27", 76, 77, 78, 79, 80, 81, 82, 83, 84);

    /// <summary>The same with a quarter, two seasons and a cal strip option, listed out of kind order on purpose.</summary>
    private static FakeInstrumentCatalogue CatalogueWithStrips() =>
        Catalogue()
            .WithOptionChain("TTF", StripKind.Cal, "Cal 27", Jan27Expiry, "TTF:Cal 27", 76, 78, 80, 82, 84)
            .WithOptionChain("TTF", StripKind.Season, "Summer27", new DateOnly(2027, 3, 25), "TTF:Summer27", 76, 78, 80, 82, 84)
            .WithOptionChain("TTF", StripKind.Season, "Winter26", new DateOnly(2026, 9, 29), "TTF:Winter26", 76, 78, 80, 82, 84)
            .WithOptionChain("TTF", StripKind.Quarter, "Q1 27", Jan27Expiry, "TTF:Q1 27", 76, 77, 78, 79, 80, 81, 82, 83, 84);

    private static ScreenRules Rules(
        int months = 2, int quarters = 0, int seasons = 0, int cals = 0,
        int strikesEachSide = 2, int subscribeEachSide = 3, int stripStrikesEachSide = 1, int subscribeStripEachSide = 2,
        decimal seed = 80)
    {
        var rules = new ScreenRules();
        rules.Options["TTF"] = new OptionRule
        {
            Months = months,
            Quarters = quarters,
            Seasons = seasons,
            Cals = cals,
            StrikesEachSide = strikesEachSide,
            SubscribeStrikesEachSide = subscribeEachSide,
            StripStrikesEachSide = stripStrikesEachSide,
            SubscribeStripStrikesEachSide = subscribeStripEachSide,
            SeedPrice = seed,
        };
        return rules;
    }

    private static Screen Chain(ScreenRules rules, FakeReferencePrices prices, DateOnly today, FakeInstrumentCatalogue? catalogue = null) =>
        new OptionChainBuilder(catalogue ?? Catalogue(), rules, prices).Build(today).Single();

    private static List<string> Labels(Screen screen, string group) =>
        screen.Rows.Where(r => r.Group == group).Select(r => r.Label).ToList();

    [Fact]
    public void Build_OneScreenPerProduct_KeyedXcomAndTheProduct()
    {
        var screen = Chain(Rules(), new FakeReferencePrices(), Today);

        screen.Key.Should().Be("xcom-ttf");
        screen.Title.Should().Be("TTF Options");
    }

    [Fact]
    public void Build_EachExpiryBlock_IsTheFutureThenTheStrikesRisingWithTheCallBeforeThePut()
    {
        var prices = new FakeReferencePrices().With("TTF:Nov26", 80);

        var screen = Chain(Rules(), prices, Today);

        Labels(screen, "Nov26").Should().Equal(
            "Nov26",
            "Nov26 78.00 C", "Nov26 78.00 P",
            "Nov26 79.00 C", "Nov26 79.00 P",
            "Nov26 80.00 C", "Nov26 80.00 P",
            "Nov26 81.00 C", "Nov26 81.00 P",
            "Nov26 82.00 C", "Nov26 82.00 P");
    }

    [Fact]
    public void Build_TheFutureRow_IsQuotedUnderTheFuturesSymbol_WithNoOptionDetail()
    {
        var screen = Chain(Rules(), new FakeReferencePrices(), Today);

        var future = screen.Rows.First();
        future.Should().Be(new ScreenRow("TTF", "Nov26", "Nov26", RowSource.Quoted, "TTF:Nov26", null, null));
    }

    [Fact]
    public void Build_AnOptionRow_CarriesItsExpiryUnderlyingStrikeAndRight()
    {
        var screen = Chain(Rules(), new FakeReferencePrices().With("TTF:Nov26", 80), Today);

        var call = screen.Rows.Single(r => r.Label == "Nov26 80.00 C");
        call.Hub.Should().Be("TTF");
        call.Source.Should().Be(RowSource.Quoted);
        call.Symbol.Should().Be("TTF:Nov26:80C");
        call.Option.Should().Be(new ScreenOption(Nov26Expiry, "TTF:Nov26", 80, OptionRight.Call));

        screen.Rows.Single(r => r.Label == "Nov26 80.00 P").Option!.Right.Should().Be(OptionRight.Put);
    }

    [Theory]
    [InlineData(80.4, "Nov26 78.00 C", "Nov26 82.00 P")]
    [InlineData(80.6, "Nov26 79.00 C", "Nov26 83.00 P")]
    [InlineData(80.5, "Nov26 78.00 C", "Nov26 82.00 P")] // halfway: the lower strike wins
    public void Build_CentresTheBandOnTheListedStrikeNearestTheFuturesPrice(decimal price, string first, string last)
    {
        var screen = Chain(Rules(), new FakeReferencePrices().With("TTF:Nov26", price), Today);

        var options = Labels(screen, "Nov26").Skip(1).ToList();
        options.First().Should().Be(first);
        options.Last().Should().Be(last);
    }

    [Fact]
    public void Build_WithNoPriceForTheFutureYet_CentresOnTheSeedPrice()
    {
        var screen = Chain(Rules(seed: 83), new FakeReferencePrices(), Today);

        Labels(screen, "Nov26").Skip(1).Should().Equal(
            "Nov26 81.00 C", "Nov26 81.00 P", "Nov26 82.00 C", "Nov26 82.00 P", "Nov26 83.00 C", "Nov26 83.00 P",
            "Nov26 84.00 C", "Nov26 84.00 P");
    }

    [Fact]
    public void Build_AtTheEdgeOfTheListing_ShowsWhatExistsRatherThanInventingStrikes()
    {
        var screen = Chain(Rules(), new FakeReferencePrices().With("TTF:Nov26", 60), Today);

        Labels(screen, "Nov26").Skip(1).Should().Equal(
            "Nov26 76.00 C", "Nov26 76.00 P", "Nov26 77.00 C", "Nov26 77.00 P", "Nov26 78.00 C", "Nov26 78.00 P");
    }

    [Fact]
    public void Build_KeepsTheConfiguredNumberOfMonths_InDateOrder()
    {
        var screen = Chain(Rules(months: 2), new FakeReferencePrices(), Today);

        screen.Rows.Select(r => r.Group).Distinct().Should().Equal("Nov26", "Dec26");
    }

    [Fact]
    public void Build_OnTheExpiryDay_StillShowsTheExpiringOptions_AndTheNextDayTheNextExpiryRollsIn()
    {
        Chain(Rules(months: 2), new FakeReferencePrices(), Nov26Expiry)
            .Rows.Select(r => r.Group).Distinct().Should().Equal("Nov26", "Dec26");

        Chain(Rules(months: 2), new FakeReferencePrices(), Nov26Expiry.AddDays(1))
            .Rows.Select(r => r.Group).Distinct().Should().Equal("Dec26", "Jan27");
    }

    [Fact]
    public void Build_ListsTheStripsAfterTheMonths_QuartersThenSeasonsThenCals_EachInDateOrder()
    {
        var rules = Rules(months: 2, quarters: 1, seasons: 2, cals: 1);

        var screen = Chain(rules, new FakeReferencePrices(), Today, CatalogueWithStrips());

        screen.Rows.Select(r => r.Group).Distinct().Should().Equal("Nov26", "Dec26", "Q1 27", "Winter26", "Summer27", "Cal 27");
    }

    [Fact]
    public void Build_AStripBlock_IsCutToTheNarrowerStripBand_AroundTheStripFuturesPrice()
    {
        var rules = Rules(months: 0, quarters: 1, stripStrikesEachSide: 1);
        var prices = new FakeReferencePrices().With("TTF:Q1 27", 81.2m);

        var screen = Chain(rules, prices, Today, CatalogueWithStrips());

        Labels(screen, "Q1 27").Should().Equal("Q1 27", "Q1 27 80.00 C", "Q1 27 80.00 P", "Q1 27 81.00 C", "Q1 27 81.00 P", "Q1 27 82.00 C", "Q1 27 82.00 P");
        screen.Rows.First().Symbol.Should().Be("TTF:Q1 27");
    }

    [Fact]
    public void Build_ShowsFewerStripsThanAskedFor_WhenTheExchangeListsFewer()
    {
        var screen = Chain(Rules(months: 0, cals: 5), new FakeReferencePrices(), Today, CatalogueWithStrips());

        screen.Rows.Select(r => r.Group).Distinct().Should().Equal("Cal 27");
    }

    [Fact]
    public void Build_AProductWithoutRules_HasNoScreen()
    {
        var catalogue = Catalogue().WithOptionChain("EUA", "Dec26", new DateOnly(2026, 12, 9), "EUA:Dec26", 85, 86, 87);

        var screens = new OptionChainBuilder(catalogue, Rules(), new FakeReferencePrices()).Build(Today);

        screens.Select(s => s.Key).Should().Equal("xcom-ttf");
    }

    [Fact]
    public void Build_ScreensComeByProductName_WhateverOrderTheRulesWereGivenIn()
    {
        var catalogue = Catalogue().WithOptionChain("EUA", "Dec26", new DateOnly(2026, 12, 9), "EUA:Dec26", 85, 86, 87);
        var rules = new ScreenRules();
        rules.Options["TTF"] = new OptionRule { Months = 1, StrikesEachSide = 1, SubscribeStrikesEachSide = 1, SeedPrice = 80 };
        rules.Options["EUA"] = new OptionRule { Months = 1, StrikesEachSide = 1, SubscribeStrikesEachSide = 1, SeedPrice = 86 };

        var screens = new OptionChainBuilder(catalogue, rules, new FakeReferencePrices()).Build(Today);

        screens.Select(s => s.Key).Should().Equal("xcom-eua", "xcom-ttf");
    }

    [Fact]
    public void Build_AFutureTheCatalogueDoesNotList_IsLabelledByItsSymbol()
    {
        var catalogue = new FakeInstrumentCatalogue()
            .WithOptionChain("EUA", "Oct26", new DateOnly(2026, 10, 21), "EUA:Dec26", 85, 86, 87);
        var rules = new ScreenRules();
        rules.Options["EUA"] = new OptionRule { Months = 1, StrikesEachSide = 1, SubscribeStrikesEachSide = 1, SeedPrice = 86 };

        var screen = new OptionChainBuilder(catalogue, rules, new FakeReferencePrices()).Build(Today).Single();

        screen.Rows.First().Label.Should().Be("EUA:Dec26");
    }

    [Fact]
    public void SymbolsToSubscribe_IsTheWiderBandAtEveryExpiry_PlusTheFutures()
    {
        var sut = new OptionChainBuilder(Catalogue(), Rules(months: 1, strikesEachSide: 1, subscribeEachSide: 2), new FakeReferencePrices());

        sut.SymbolsToSubscribe(Today).Should().Equal(
            "TTF:Nov26",
            "TTF:Nov26:78C", "TTF:Nov26:78P", "TTF:Nov26:79C", "TTF:Nov26:79P", "TTF:Nov26:80C", "TTF:Nov26:80P",
            "TTF:Nov26:81C", "TTF:Nov26:81P", "TTF:Nov26:82C", "TTF:Nov26:82P");
    }

    [Fact]
    public void SymbolsToSubscribe_UsesTheWiderStripBandForAStrip()
    {
        var rules = Rules(months: 0, cals: 1, stripStrikesEachSide: 0, subscribeStripEachSide: 1);
        var sut = new OptionChainBuilder(CatalogueWithStrips(), rules, new FakeReferencePrices());

        sut.SymbolsToSubscribe(Today).Should().Equal(
            "TTF:Cal 27", "TTF:Cal 27:78C", "TTF:Cal 27:78P", "TTF:Cal 27:80C", "TTF:Cal 27:80P", "TTF:Cal 27:82C", "TTF:Cal 27:82P");
    }

    [Fact]
    public void Symbols_OfAChainScreen_AreTheFutureAndEveryOptionShown_WithoutRepeats()
    {
        var screen = Chain(Rules(months: 1, strikesEachSide: 1), new FakeReferencePrices(), Today);

        screen.Symbols.Should().Equal(
            "TTF:Nov26", "TTF:Nov26:79C", "TTF:Nov26:79P", "TTF:Nov26:80C", "TTF:Nov26:80P", "TTF:Nov26:81C", "TTF:Nov26:81P");
    }

    [Fact]
    public void Matrix_ListsTheSameExpiries_WithEveryListedStrike_AndNoBand()
    {
        var rules = Rules(months: 1, quarters: 1, strikesEachSide: 1, stripStrikesEachSide: 1);

        var matrix = new OptionChainBuilder(CatalogueWithStrips(), rules, new FakeReferencePrices()).Matrix("ttf", Today)!;

        matrix.Product.Should().Be("TTF");
        matrix.Expiries.Select(e => (e.Kind, e.Label, e.ExpiryDate, e.UnderlyingSymbol)).Should().Equal(
            (StripKind.Month, "Nov26", Nov26Expiry, "TTF:Nov26"),
            (StripKind.Quarter, "Q1 27", Jan27Expiry, "TTF:Q1 27"));
        matrix.Expiries[0].Strikes.Should().Equal(76, 77, 78, 79, 80, 81, 82, 83, 84);
    }

    [Fact]
    public void Matrix_ForAProductWithoutRules_IsNull()
    {
        new OptionChainBuilder(Catalogue(), Rules(), new FakeReferencePrices()).Matrix("EUA", Today).Should().BeNull();
    }
}
