using Atlas.Web.Core.Application.Ports;
using Atlas.Web.Core.Domain.Logic;
using Atlas.Web.Ice.Configuration;
using FluentAssertions;
using Microsoft.Extensions.Options;


namespace Atlas.Web.Ice.Tests.Adapters;

public class SymbolListParserTests
{
    private static IceSymbolListParser CreateParser(params string[] validSymbols)
    {
        var options = Options.Create(new IceOptions
        {
            Symbols = validSymbols.ToList()
        });

        return new IceSymbolListParser(options);
    }

    [Fact]
    public void SplitSymbols_SplitsOnComma()
    {
        var sut = CreateParser("AAA", "BBB", "CCC");

        var result = sut.SplitSymbols("AAA,BBB,CCC");

        result.Should().BeEquivalentTo(new[] { "AAA", "BBB", "CCC" });
    }

    [Fact]
    public void SplitSymbols_TrimsWhitespace()
    {
        var sut = CreateParser("AAA", "BBB");

        var result = sut.SplitSymbols("  AAA ,   BBB ");

        result.Should().BeEquivalentTo(new[] { "AAA", "BBB" });
    }

    [Fact]
    public void SplitSymbols_FiltersOutEmptyEntries()
    {
        var sut = CreateParser("AAA", "BBB");

        var result = sut.SplitSymbols("AAA,, BBB , ,");

        result.Should().BeEquivalentTo(new[] { "AAA", "BBB" });
    }

    [Fact]
    public void SplitSymbols_ReturnsList_WhenAllSymbolsAreValid()
    {
        var sut = CreateParser("AAA", "BBB");

        var result = sut.SplitSymbols("AAA,BBB");

        result.Should().BeEquivalentTo(new[] { "AAA", "BBB" });
    }

    [Fact]
    public void SplitSymbols_Throws_WhenSymbolNotInOptions()
    {
        var sut = CreateParser("AAA", "BBB");

        Action act = () => sut.SplitSymbols("AAA,CCC");

        act.Should().Throw<Exception>();
    }

    [Fact]
    public void IsValid_ReturnsTrue_WhenAllSymbolsAreKnown()
    {
        var sut = CreateParser("AAA", "BBB", "CCC");

        sut.IsValid(new[] { "AAA", "CCC" }).Should().BeTrue();
    }

    [Fact]
    public void IsValid_ReturnsFalse_WhenAnySymbolIsUnknown()
    {
        var sut = CreateParser("AAA", "BBB");

        sut.IsValid(new[] { "AAA", "ZZZ" }).Should().BeFalse();
    }
}
