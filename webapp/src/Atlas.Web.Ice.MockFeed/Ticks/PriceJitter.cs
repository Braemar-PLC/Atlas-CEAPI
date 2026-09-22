using System.Globalization;
using Atlas.Web.Ice.Domain.Enumeration;
using Atlas.Web.Ice.MockFeed.Capture;

namespace Atlas.Web.Ice.MockFeed.Ticks;

/// <summary>
/// Moves price-like field values so a replayed capture keeps ticking once its
/// recorded frames are exhausted.
///
/// A field moves only when it is not metadata and its captured value looks like
/// a decimal. Metadata is defined by <see cref="IceResponseMetaFieldIds"/> —
/// the same set the consumer's interpreter strips — rather than a parallel list
/// maintained here.
/// </summary>
public static class PriceJitter
{
    public static IReadOnlySet<short> MetadataFieldIds { get; } =
        Enum.GetValues<IceResponseMetaFieldIds>().Select(id => (short)id).ToHashSet();

    public static bool IsMovable(CapturedField field)
    {
        if (MetadataFieldIds.Contains(field.Id))
        {
            return false;
        }

        // A decimal point distinguishes prices and volumes from counters, codes
        // and sequence numbers, which are integer-valued on this feed.
        if (!field.Value.Contains('.'))
        {
            return false;
        }

        return decimal.TryParse(
            field.Value, NumberStyles.Number, CultureInfo.InvariantCulture, out _);
    }

    public static CapturedFrame Apply(CapturedFrame frame, double percent, Random random)
    {
        return frame with
        {
            Fields = frame.Fields.Select(field => Apply(field, percent, random)).ToArray()
        };
    }

    public static CapturedField Apply(CapturedField field, double percent, Random random)
    {
        if (percent <= 0 || !IsMovable(field))
        {
            return field;
        }

        var value = decimal.Parse(field.Value, NumberStyles.Number, CultureInfo.InvariantCulture);
        var decimals = field.Value.Length - field.Value.IndexOf('.') - 1;

        var drift = (decimal)((random.NextDouble() * 2 - 1) * percent / 100.0);
        var moved = Math.Round(value * (1m + drift), decimals, MidpointRounding.AwayFromZero);

        return field with
        {
            Value = moved.ToString("F" + decimals, CultureInfo.InvariantCulture)
        };
    }
}
