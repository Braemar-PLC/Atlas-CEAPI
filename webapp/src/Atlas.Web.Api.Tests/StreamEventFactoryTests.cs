using Atlas.Web.Api.Enumeration;
using Atlas.Web.Api.Factories;
using Atlas.Web.Core.Domain.Enumeration;
using Atlas.Web.Core.Domain.Models;
using FluentAssertions;

namespace Atlas.Web.Api.Tests;

public class StreamEventFactoryTests
{
    private readonly StreamEventFactory _sut = new();

    [Fact]
    public void Create_ActiveSnapshot_ReturnsUpdateAction()
    {
        var snapshot = new PricingSnapshot("TFM 26J-ICN", new Dictionary<int, string> { { 19, "64.665" } }, DataStatus.Active);
        var result = _sut.Create(snapshot);
        result.Metadata.Action.Should().Be(PricingEventActions.Update);
    }

    [Fact]
    public void Create_ResetSnapshot_ReturnsResetAction()
    {
        var snapshot = new PricingSnapshot("TFM 26J-ICN", new Dictionary<int, string>(), DataStatus.Reset);
        var result = _sut.Create(snapshot);
        result.Metadata.Action.Should().Be(PricingEventActions.Reset);
    }

    [Fact]
    public void Create_RemoveSnapshot_ReturnsRemoveAction()
    {
        var snapshot = new PricingSnapshot("TFM 26J-ICN", new Dictionary<int, string>(), DataStatus.Remove);
        var result = _sut.Create(snapshot);
        result.Metadata.Action.Should().Be(PricingEventActions.Remove);
    }

    [Fact]
    public void Create_SetsTypeAsPricing()
    {
        var snapshot = new PricingSnapshot("TFM 26J-ICN", new Dictionary<int, string>(), DataStatus.Active);
        var result = _sut.Create(snapshot);
        result.Metadata.Type.Should().Be(StreamEventTypes.Pricing);
    }

    [Fact]
    public void Create_SetsKeyFromSymbol()
    {
        var snapshot = new PricingSnapshot("TFM 26J-ICN", new Dictionary<int, string>(), DataStatus.Active);
        var result = _sut.Create(snapshot);
        result.Metadata.Key.Should().Be("TFM 26J-ICN");
    }

    [Fact]
    public void Create_SetsServerTimestamp()
    {
        var before = DateTime.UtcNow;
        var snapshot = new PricingSnapshot("TFM 26J-ICN", new Dictionary<int, string>(), DataStatus.Active);
        var result = _sut.Create(snapshot);
        var after = DateTime.UtcNow;
        result.Metadata.ServerTimestamp.Should().BeOnOrAfter(before).And.BeOnOrBefore(after);
    }

    [Fact]
    public void Create_MapsDataFields()
    {
        var snapshot = new PricingSnapshot("TFM 26J-ICN", new Dictionary<int, string> { { 19, "64.665" }, { 20, "64.61" } }, DataStatus.Active);
        var result = _sut.Create(snapshot);
        result.Data.Fields[19].Should().Be("64.665");
        result.Data.Fields[20].Should().Be("64.61");
    }

    [Fact]
    public void Create_MapsDataSymbol()
    {
        var snapshot = new PricingSnapshot("TFM 26J-ICN", new Dictionary<int, string>(), DataStatus.Active);
        var result = _sut.Create(snapshot);
        result.Data.Symbol.Should().Be("TFM 26J-ICN");
    }
}
