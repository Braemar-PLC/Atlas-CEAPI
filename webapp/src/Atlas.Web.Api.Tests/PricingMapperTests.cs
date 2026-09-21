using FluentAssertions;
using Atlas.Web.Api.Mappers;
using Atlas.Web.Core.Domain.Models;

namespace Atlas.Web.Api.Tests;

public class PricingMapperTests
{
    [Fact]
    public void ToDto_MapsSymbol()
    {
        var snapshot = new PricingSnapshot("EURUSD", new Dictionary<int, string>());
        PricingMapper.ToDto(snapshot).Symbol.Should().Be("EURUSD");
    }

    [Fact]
    public void ToDto_MapsFields()
    {
        var snapshot = new PricingSnapshot("SYM", new Dictionary<int, string> { { 1, "72" }, { 36, "110" } });
        var dto = PricingMapper.ToDto(snapshot);
        dto.Fields[1].Should().Be("72");
        dto.Fields[36].Should().Be("110");
    }

    [Fact]
    public void ToDto_EmptyFields_MapsToEmptyDictionary()
    {
        var snapshot = new PricingSnapshot("SYM", new Dictionary<int, string>());
        PricingMapper.ToDto(snapshot).Fields.Should().BeEmpty();
    }
}
