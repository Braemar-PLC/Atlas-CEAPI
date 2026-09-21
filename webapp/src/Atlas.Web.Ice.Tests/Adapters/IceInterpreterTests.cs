using Atlas.Web.Core.Domain.Enumeration;
using Atlas.Web.Ice.Adapters;
using FluentAssertions;

namespace Atlas.Web.Ice.Tests.Adapters;

public class IceInterpreterTests
{
    private readonly IceInterpreter _sut = new();

    [Fact]
    public void Interpret_PricingFieldsOnly_ReturnsActiveMessage()
    {
        var envelope = new IceEnvelope("SYM", [(19, "64.665"), (20, "64.61")]);
        var result = _sut.Interpret(envelope);
        result.Should().NotBeNull();
        result!.Status.Should().Be(DataStatus.Active);
    }

    [Fact]
    public void Interpret_MapsSymbol()
    {
        var envelope = new IceEnvelope("TFM 26J-ICN", [(19, "64.665")]);
        var result = _sut.Interpret(envelope);
        result!.Symbol.Should().Be("TFM 26J-ICN");
    }

    [Fact]
    public void Interpret_FiltersPricingFields_ExcludesMetadata()
    {
        // FieldId 330 = EventHeader (metadata), 19 = price field
        var envelope = new IceEnvelope("SYM", [(330, "sequenceNumber=..."), (19, "64.665")]);
        var result = _sut.Interpret(envelope);
        result!.Fields.Should().NotContain(f => f.FieldId == 330);
        result!.Fields.Should().Contain(f => f.FieldId == 19);
    }

    [Fact]
    public void Interpret_AllMetadataFields_ReturnsNull()
    {
        var envelope = new IceEnvelope("SYM", [(330, "sequenceNumber=..."), (568, "[0,0,0,0]")]);
        var result = _sut.Interpret(envelope);
        result.Should().BeNull();
    }

    [Fact]
    public void Interpret_EmptyFields_ReturnsNull()
    {
        var envelope = new IceEnvelope("SYM", []);
        var result = _sut.Interpret(envelope);
        result.Should().BeNull();
    }

    [Fact]
    public void Interpret_RecordReset_ReturnsResetStatus()
    {
        var envelope = new IceEnvelope("SYM", [(416, "0"), (19, "64.665")]);
        var result = _sut.Interpret(envelope);
        result!.Status.Should().Be(DataStatus.Reset);
    }

    [Fact]
    public void Interpret_RecordReset_ReturnsEmptyFields()
    {
        var envelope = new IceEnvelope("SYM", [(416, "0"), (19, "64.665")]);
        var result = _sut.Interpret(envelope);
        result!.Fields.Should().BeEmpty();
    }

    [Fact]
    public void Interpret_RecordReset_NonZeroValue_TreatedAsActive()
    {
        // 416 with a non-zero value is not a reset
        var envelope = new IceEnvelope("SYM", [(416, "1"), (19, "64.665")]);
        var result = _sut.Interpret(envelope);
        result!.Status.Should().Be(DataStatus.Active);
    }
}
