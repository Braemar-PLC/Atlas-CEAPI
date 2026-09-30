using System.Text;
using Atlas.Web.Core.Domain.Enumeration;
using Atlas.Web.Core.Domain.Models;
using Atlas.Web.Ice.Catalogue;
using FluentAssertions;

namespace Atlas.Web.Ice.Tests.Catalogue;

public class JsonInstrumentCatalogueTests
{
    private static readonly JsonInstrumentCatalogue Embedded = JsonInstrumentCatalogue.LoadEmbedded();

    [Fact]
    public void Load_ReadsAnOutright()
    {
        const string json = """
            { "asOf": "2026-09-17",
              "outrights": [ {"hub":"TTF","kind":"Quarter","name":"Q4 26","symbol":"TFMQ 26V-ICN","start":"2026-10","expiry":"2026-09-29"} ],
              "spreads": [] }
            """;

        var sut = JsonInstrumentCatalogue.Load(new MemoryStream(Encoding.UTF8.GetBytes(json)));

        sut.AsOf.Should().Be(new DateOnly(2026, 9, 17));
        sut.Outrights.Single().Should().Be(new Instrument(
            "TTF", StripKind.Quarter, "Q4 26", "TFMQ 26V-ICN", new DateOnly(2026, 10, 1), new DateOnly(2026, 9, 29)));
    }

    [Fact]
    public void Load_ReadsASpread()
    {
        const string json = """
            { "asOf": "2026-09-17", "outrights": [],
              "spreads": [ {"hub":"NBP","name":"Oct26/Nov26","near":"Oct26","far":"Nov26","symbol":"GWM 26V:GWM26X-ICE","expiry":"2026-09-29"} ] }
            """;

        var sut = JsonInstrumentCatalogue.Load(new MemoryStream(Encoding.UTF8.GetBytes(json)));

        sut.Spreads.Single().Should().Be(new SpreadInstrument(
            "NBP", "Oct26/Nov26", "Oct26", "Nov26", "GWM 26V:GWM26X-ICE", new DateOnly(2026, 9, 29)));
    }

    [Fact]
    public void Load_ReadsAnOption_FromTheSecondFile()
    {
        const string futures = """{ "asOf": "2026-09-24", "outrights": [], "spreads": [] }""";
        const string options = """
            { "asOf": "2026-09-24",
              "options": [ {"product":"TTF","kind":"Month","expiry":"Nov26","expiryDate":"2026-10-27","underlying":"TFM 26X-ICN","strike":80,"right":"C","symbol":"TFO 26XC8000-ICN"},
                           {"product":"Brent","kind":"Quarter","expiry":"Q1 27","expiryDate":"2026-11-25","underlying":"BRNQ 27F-ICE","strike":78.5,"right":"P","symbol":"BRNQ 27FP7850-ICE"} ] }
            """;

        var sut = JsonInstrumentCatalogue.Load(Stream(futures), () => Stream(options));

        sut.Options.Should().Equal(
            new OptionInstrument("TTF", StripKind.Month, "Nov26", new DateOnly(2026, 10, 27), "TFM 26X-ICN", 80m, OptionRight.Call, "TFO 26XC8000-ICN"),
            new OptionInstrument("Brent", StripKind.Quarter, "Q1 27", new DateOnly(2026, 11, 25), "BRNQ 27F-ICE", 78.5m, OptionRight.Put, "BRNQ 27FP7850-ICE"));
    }

    [Fact]
    public void Load_WithoutAnOptionsFile_HasNoOptions()
    {
        var sut = JsonInstrumentCatalogue.Load(Stream("""{ "asOf": "2026-09-24", "outrights": [], "spreads": [] }"""));

        sut.Options.Should().BeEmpty();
    }

    [Fact]
    public void Load_ARightOtherThanCOrP_IsRefused()
    {
        const string options = """
            { "asOf": "2026-09-24",
              "options": [ {"product":"TTF","kind":"Month","expiry":"Nov26","expiryDate":"2026-10-27","underlying":"TFM 26X-ICN","strike":80,"right":"X","symbol":"TFO 26XC8000-ICN"} ] }
            """;
        var sut = JsonInstrumentCatalogue.Load(Stream("""{ "asOf": "2026-09-24", "outrights": [], "spreads": [] }"""), () => Stream(options));

        var reading = () => sut.Options;

        reading.Should().Throw<InvalidOperationException>().WithMessage("*'X'*");
    }

    private static MemoryStream Stream(string json) => new(Encoding.UTF8.GetBytes(json));

    [Fact]
    public void LoadEmbedded_FindsTheFileBuiltIntoTheAssembly()
    {
        Embedded.Outrights.Should().NotBeEmpty();
        Embedded.Spreads.Should().NotBeEmpty();
    }

    [Theory]
    [InlineData("TTF")]
    [InlineData("EUA")]
    [InlineData("Brent")]
    [InlineData("WTI")]
    public void LoadEmbedded_HasCallsAndPutsForEachOptionsProduct(string product)
    {
        Embedded.Options.Should().Contain(o => o.Product == product && o.Right == OptionRight.Call);
        Embedded.Options.Should().Contain(o => o.Product == product && o.Right == OptionRight.Put);
    }

    [Theory]
    [InlineData("EUA")]
    [InlineData("Brent")]
    [InlineData("WTI")]
    public void LoadEmbedded_ListsTheFuturesTheOptionsArePricedOff_AsMonthsOfTheirOwnHub(string hub)
    {
        Embedded.Outrights.Should().Contain(o => o.Hub == hub && o.Kind == StripKind.Month);
    }

    [Fact]
    public void LoadEmbedded_EveryOptionSitsOnAFutureTheCatalogueLists()
    {
        var futures = Embedded.Outrights.Select(o => o.Symbol).ToHashSet();

        Embedded.Options.Should().OnlyContain(o => futures.Contains(o.UnderlyingSymbol));
    }

    [Theory]
    [InlineData("TTF")]
    [InlineData("Brent")]
    [InlineData("WTI")]
    public void LoadEmbedded_HasQuarterSeasonAndCalStripOptions_EachOnTheStripFutureOfTheSameKind(string product)
    {
        var byKind = Embedded.Outrights.ToDictionary(o => o.Symbol, o => o.Kind);

        foreach (var kind in new[] { StripKind.Quarter, StripKind.Season, StripKind.Cal })
        {
            var strips = Embedded.Options.Where(o => o.Product == product && o.Kind == kind).ToList();
            strips.Should().NotBeEmpty("{0} should list {1} options", product, kind);
            strips.Should().OnlyContain(o => byKind[o.UnderlyingSymbol] == kind);
        }
    }

    [Fact]
    public void LoadEmbedded_EuaHasMonthlyOptionsOnly()
    {
        Embedded.Options.Where(o => o.Product == "EUA").Should().OnlyContain(o => o.Kind == StripKind.Month);
    }

    [Fact]
    public void LoadEmbedded_AStripOptionsPeriod_IsNamedAsItsStripFutureIs()
    {
        var names = Embedded.Outrights.ToDictionary(o => o.Symbol, o => o.Name);

        Embedded.Options.Where(o => o.Kind != StripKind.Month).Should().OnlyContain(o => names[o.UnderlyingSymbol] == o.Expiry);
    }

    [Fact]
    public void LoadEmbedded_OptionSymbolsAreUnique()
    {
        Embedded.Options.GroupBy(o => o.Symbol).Should().OnlyContain(g => g.Count() == 1);
    }

    [Fact]
    public void LoadEmbedded_EuasMonthlyOptionsSitOnTheDecemberFuture()
    {
        Embedded.Options.Where(o => o.Product == "EUA").Should().OnlyContain(o => o.UnderlyingSymbol.EndsWith("Z-ICN"));
    }

    [Fact]
    public void LoadEmbedded_OilStripOptionsStartOnlyOnCalendarQuartersHalfYearsAndYears()
    {
        var starts = Embedded.Outrights.ToDictionary(o => o.Symbol, o => o.Start.Month);

        Embedded.Options.Where(o => o.Product == "Brent" && o.Kind == StripKind.Quarter)
            .Should().OnlyContain(o => new[] { 1, 4, 7, 10 }.Contains(starts[o.UnderlyingSymbol]));
        Embedded.Options.Where(o => o.Product == "Brent" && o.Kind == StripKind.Season)
            .Should().OnlyContain(o => new[] { 1, 7 }.Contains(starts[o.UnderlyingSymbol]));
    }

    [Theory]
    [InlineData("TTF")]
    [InlineData("NBP")]
    public void LoadEmbedded_HasEveryKindOfStripForEachGasHub(string hub)
    {
        foreach (var kind in Enum.GetValues<StripKind>())
        {
            Embedded.Outrights.Should().Contain(o => o.Hub == hub && o.Kind == kind);
        }
    }

    [Theory]
    [InlineData("ARA")]
    [InlineData("Newcastle")]
    public void LoadEmbedded_HasMonthsQuartersAndCalsForEachCoalHub(string hub)
    {
        foreach (var kind in new[] { StripKind.Month, StripKind.Quarter, StripKind.Cal })
        {
            Embedded.Outrights.Should().Contain(o => o.Hub == hub && o.Kind == kind);
        }
    }

    [Theory]
    [InlineData("TTF")]
    [InlineData("NBP")]
    public void LoadEmbedded_EveryGasStripExpiresBeforeItsDeliveryStarts(string hub)
    {
        Embedded.Outrights.Where(o => o.Hub == hub).Should().OnlyContain(o => o.Expiry < o.Start);
    }

    [Theory]
    [InlineData("ARA")]
    [InlineData("Newcastle")]
    public void LoadEmbedded_EveryCoalStripExpiresInsideItsFirstDeliveryMonth_SoTheFrontMonthIsTheCurrentOne(string hub)
    {
        Embedded.Outrights.Where(o => o.Hub == hub)
            .Should().OnlyContain(o => o.Start <= o.Expiry && o.Expiry < o.Start.AddMonths(1));
    }

    [Theory]
    [InlineData("TTF")]
    [InlineData("NBP")]
    [InlineData("ARA")]
    [InlineData("Newcastle")]
    public void LoadEmbedded_MonthsRunWithoutGaps_SoCountingStripsFromTheFrontMonthIsSafe(string hub)
    {
        var months = Embedded.Outrights
            .Where(o => o.Hub == hub && o.Kind == StripKind.Month)
            .OrderBy(o => o.Start)
            .ToList();

        months.Should().NotBeEmpty();
        for (var i = 1; i < months.Count; i++)
        {
            months[i].Start.Should().Be(months[i - 1].Start.AddMonths(1), "{0} should follow {1}", months[i].Name, months[i - 1].Name);
        }
    }

    [Fact]
    public void LoadEmbedded_StripNamesAreUniqueWithinAHub_SoALabelIdentifiesOneContract()
    {
        Embedded.Outrights.GroupBy(o => (o.Hub, o.Name)).Should().OnlyContain(g => g.Count() == 1);
    }

    [Fact]
    public void LoadEmbedded_EverySpreadJoinsTwoListedStripsOfTheSameKind_WhatTheDeskCallsVanilla()
    {
        var byName = Embedded.Outrights.ToDictionary(o => (o.Hub, o.Name));

        foreach (var spread in Embedded.Spreads)
        {
            byName.Should().ContainKey((spread.Hub, spread.Near), "near leg of {0}", spread.Name);
            byName.Should().ContainKey((spread.Hub, spread.Far), "far leg of {0}", spread.Name);
            byName[(spread.Hub, spread.Near)].Kind.Should().Be(byName[(spread.Hub, spread.Far)].Kind, spread.Name);
        }
    }

    [Fact]
    public void LoadEmbedded_ASpreadStopsTradingWithItsNearLeg()
    {
        var byName = Embedded.Outrights.ToDictionary(o => (o.Hub, o.Name));

        foreach (var spread in Embedded.Spreads)
        {
            spread.Expiry.Should().Be(byName[(spread.Hub, spread.Near)].Expiry, spread.Name);
        }
    }
}
