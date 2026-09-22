using System.Globalization;
using Atlas.Web.Ice.Domain.Enumeration;
using Atlas.Web.Ice.MockFeed.Capture;
using Atlas.Web.Ice.MockFeed.Ticks;
using FluentAssertions;

namespace Atlas.Web.Ice.MockFeed.Tests;

public class PriceJitterTests
{
    private static readonly Random Rng = new(20260917);

    [Theory]
    [InlineData((short)19, "64.665")]   // bid
    [InlineData((short)20, "64.61")]    // ask
    [InlineData((short)266, "125384.0")] // volume-like, moves by design
    public void IsMovable_DecimalNonMetadataField_IsTrue(short id, string value)
    {
        PriceJitter.IsMovable(new CapturedField(id, value)).Should().BeTrue();
    }

    [Theory]
    [InlineData((short)1, "72")]
    [InlineData((short)36, "110")]
    public void IsMovable_IntegerValuedField_IsFalse(short id, string value)
    {
        // No decimal point: a counter or code, not a price.
        PriceJitter.IsMovable(new CapturedField(id, value)).Should().BeFalse();
    }

    [Theory]
    [InlineData((short)13, "TFM 26J-ICN")]
    [InlineData((short)17, "U@@@@@")]
    [InlineData((short)568, "[4, 0, 0, 0]")]
    public void IsMovable_NonNumericField_IsFalse(short id, string value)
    {
        PriceJitter.IsMovable(new CapturedField(id, value)).Should().BeFalse();
    }

    [Theory]
    [InlineData((short)330)] // EventHeader
    [InlineData((short)455)] // TradingStatus
    [InlineData((short)416)] // RecordReset
    [InlineData((short)15)]  // ExchangeTimeIso
    public void IsMovable_MetadataField_IsFalse_EvenWhenDecimalLooking(short id)
    {
        // Metadata is defined by IceResponseMetaFieldIds, the same set
        // IceInterpreter strips, rather than a parallel list here.
        PriceJitter.IsMovable(new CapturedField(id, "12.5")).Should().BeFalse();
    }

    [Fact]
    public void MetadataIds_MatchTheInterpretersSet()
    {
        var expected = Enum.GetValues<IceResponseMetaFieldIds>()
            .Select(v => (short)v)
            .OrderBy(v => v);

        PriceJitter.MetadataFieldIds.OrderBy(v => v).Should().Equal(expected);
    }

    [Fact]
    public void Apply_MovesValueWithinRequestedPercentage()
    {
        var field = new CapturedField(19, "100.00");

        for (var i = 0; i < 200; i++)
        {
            var moved = PriceJitter.Apply(field, percent: 0.15, Rng);
            var value = decimal.Parse(moved.Value, CultureInfo.InvariantCulture);

            value.Should().BeInRange(99.85m, 100.15m);
        }
    }

    [Fact]
    public void Apply_PreservesDecimalPlacesOfOriginal()
    {
        var moved = PriceJitter.Apply(new CapturedField(19, "64.665"), 0.15, Rng);

        moved.Value.Split('.')[1].Should().HaveLength(3);
    }

    [Fact]
    public void Apply_PreservesFieldId()
    {
        PriceJitter.Apply(new CapturedField(19, "64.665"), 0.15, Rng).Id.Should().Be(19);
    }

    [Fact]
    public void Apply_ZeroPercent_LeavesValueUnchanged()
    {
        PriceJitter.Apply(new CapturedField(19, "64.665"), 0, Rng).Value.Should().Be("64.665");
    }

    [Fact]
    public void Apply_ImmovableField_LeavesValueUnchanged()
    {
        PriceJitter.Apply(new CapturedField(13, "TFM 26J-ICN"), 5, Rng)
            .Value.Should().Be("TFM 26J-ICN");
    }

    [Fact]
    public void ApplyToFrame_PreservesSymbolFieldIdsAndCount()
    {
        var frame = new CapturedFrame("SYM", [
            new CapturedField(1, "72"),
            new CapturedField(19, "64.665"),
            new CapturedField(13, "SYM")
        ]);

        var moved = PriceJitter.Apply(frame, 0.15, Rng);

        moved.Symbol.Should().Be("SYM");
        moved.Fields.Select(f => f.Id).Should().Equal((short)1, (short)19, (short)13);
    }

    [Fact]
    public void Apply_UsesInvariantCulture()
    {
        // The machine default is en-GB; a comma decimal separator would produce
        // a frame the consumer's decimal parse would reject.
        var moved = PriceJitter.Apply(new CapturedField(19, "64.665"), 0.15, Rng);

        moved.Value.Should().NotContain(",");
        moved.Value.Should().Contain(".");
    }
}
