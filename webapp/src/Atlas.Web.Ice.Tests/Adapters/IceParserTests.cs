using Atlas.Web.Ice.Adapters;
using FluentAssertions;
using System.Text.Json;

namespace Atlas.Web.Ice.Tests.Adapters;

public class IceParserTests
{
    private readonly IceParser _sut = new();

    private static JsonElement Parse(string json) =>
        JsonDocument.Parse(json).RootElement;

    [Fact]
    public void TryParse_ValidArrayMessage_ReturnsTrue()
    {
        var json = Parse(@"[""update"",""TFM 26J-ICN"",[[1,""72""],[13,""TFM 26J-ICN""]]]");
        _sut.TryParse(json, out _).Should().BeTrue();
    }

    [Fact]
    public void TryParse_ValidMessage_PopulatesSymbol()
    {
        var json = Parse(@"[""update"",""TFM 26J-ICN"",[[1,""72""]]]");
        _sut.TryParse(json, out var envelope);
        envelope.Symbol.Should().Be("TFM 26J-ICN");
    }

    [Fact]
    public void TryParse_ValidMessage_PopulatesFields()
    {
        var json = Parse(@"[""update"",""TFM 26J-ICN"",[[1,""72""],[19,""64.665""]]]");
        _sut.TryParse(json, out var envelope);
        envelope.Fields.Should().Contain(f => f.FieldId == 1 && f.Value == "72");
        envelope.Fields.Should().Contain(f => f.FieldId == 19 && f.Value == "64.665");
    }

    [Fact]
    public void TryParse_NonUpdateType_ReturnsFalse()
    {
        var json = Parse(@"[""subscribe"",""TFM 26J-ICN"",[[1,""72""]]]");
        _sut.TryParse(json, out _).Should().BeFalse();
    }

    [Fact]
    public void TryParse_ArrayTooShort_ReturnsFalse()
    {
        var json = Parse(@"[""update"",""TFM 26J-ICN""]");
        _sut.TryParse(json, out _).Should().BeFalse();
    }

    [Fact]
    public void TryParse_NullInput_ReturnsFalse()
    {
        var json = Parse("null");
        _sut.TryParse(json, out _).Should().BeFalse();
    }

    [Fact]
    public void TryParse_NumberInput_ReturnsFalse()
    {
        var json = Parse("42");
        _sut.TryParse(json, out _).Should().BeFalse();
    }

    [Fact]
    public void TryParse_EmptyObject_ReturnsFalse()
    {
        var json = Parse("{}");
        _sut.TryParse(json, out _).Should().BeFalse();
    }

    [Fact]
    public void TryParse_MalformedFieldPair_ReturnsFalse()
    {
        // field array with only one element
        var json = Parse(@"[""update"",""TFM 26J-ICN"",[[1]]]");
        _sut.TryParse(json, out _).Should().BeFalse();
    }

    [Fact]
    public void TryParse_EmptyFieldArray_ReturnsTrue()
    {
        // valid structure with no fields — incremental updates can be sparse
        var json = Parse(@"[""update"",""TFM 26J-ICN"",[]]");
        _sut.TryParse(json, out var envelope);
        envelope.Fields.Should().BeEmpty();
    }

    [Fact]
    public void TryParse_EmptySymbol_ReturnsFalse()
    {
        var json = Parse(@"[""update"","""",[[1,""72""]]]");
        _sut.TryParse(json, out _).Should().BeFalse();
    }

    [Fact]
    public void TryParse_FieldsElementNotArray_ReturnsFalse()
    {
        var json = Parse(@"[""update"",""TFM 26J-ICN"",""notanarray""]");
        _sut.TryParse(json, out _).Should().BeFalse();
    }

    [Fact]
    public void TryParse_CaseInsensitiveUpdateType_ReturnsTrue()
    {
        // defensive — ICE sends lowercase but good to be explicit
        var json = Parse(@"[""UPDATE"",""TFM 26J-ICN"",[[1,""72""]]]");
        _sut.TryParse(json, out _).Should().BeTrue();
    }

    [Fact]
    public void TryParse_RealDataLine_ReturnsTrue()
    {
        // smoke test against actual ICE wire format
        var json = Parse(@"[""update"",""TFM 26J-ICN"",[[1,""72""],[13,""TFM 26J-ICN""],[330,""sequenceNumber=0, flags=1""],[19,""64.665""]]]");
        _sut.TryParse(json, out var envelope);
        envelope.Symbol.Should().Be("TFM 26J-ICN");
        envelope.Fields.Should().Contain(f => f.FieldId == 19 && f.Value == "64.665");
        envelope.Fields.Should().Contain(f => f.FieldId == 330);
    }
}