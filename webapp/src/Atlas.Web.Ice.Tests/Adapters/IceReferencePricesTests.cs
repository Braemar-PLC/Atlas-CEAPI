using Atlas.Web.Core.Application.Ports;
using Atlas.Web.Core.Domain.Models;
using Atlas.Web.Ice.Adapters;
using FluentAssertions;
using Moq;

namespace Atlas.Web.Ice.Tests.Adapters;

public class IceReferencePricesTests
{
    private const string Symbol = "TFM 26X-ICN";

    private static IceReferencePrices Sut(params (int FieldId, string Value)[] fields)
    {
        var store = new Mock<IPricingStore>();
        store.Setup(s => s.CurrentSnapshot(Symbol))
            .Returns(new PricingSnapshot(Symbol, fields.ToDictionary(f => f.FieldId, f => f.Value)));
        return new IceReferencePrices(new Lazy<IPricingStore>(() => store.Object));
    }

    [Fact]
    public void For_PrefersTheSettlement_BecauseItMovesOnlyOnceADay()
    {
        Sut((273, "73.253"), (19, "70.995"), (20, "70.98"), (21, "71.035")).For(Symbol).Should().Be(73.253m);
    }

    [Fact]
    public void For_WithoutASettlement_TakesTheLastTrade()
    {
        Sut((19, "70.995"), (20, "70.98"), (21, "71.035")).For(Symbol).Should().Be(70.995m);
    }

    [Fact]
    public void For_WithOnlyBidAndAsk_TakesTheMiddle()
    {
        Sut((20, "70.98"), (21, "71.04")).For(Symbol).Should().Be(71.01m);
    }

    [Fact]
    public void For_WithOnlyOneSide_HasNoPrice()
    {
        Sut((20, "70.98")).For(Symbol).Should().BeNull();
    }

    [Fact]
    public void For_ASymbolTheFeedHasNotSent_HasNoPrice()
    {
        Sut().For(Symbol).Should().BeNull();
    }

    [Theory]
    [InlineData("0")]
    [InlineData("0.0")]
    [InlineData("-99.0")]
    [InlineData("n/a")]
    public void For_IcesMarkersForNoPrice_FallThroughToTheNextField(string marker)
    {
        Sut((273, marker), (19, "70.995")).For(Symbol).Should().Be(70.995m);
    }

    [Fact]
    public void For_ReadsTheStoreOnlyWhenAsked()
    {
        var store = new Mock<IPricingStore>(MockBehavior.Strict);

        var sut = new IceReferencePrices(new Lazy<IPricingStore>(() => store.Object));

        store.Verify(s => s.CurrentSnapshot(It.IsAny<string>()), Times.Never);
        sut.Should().NotBeNull();
    }
}
