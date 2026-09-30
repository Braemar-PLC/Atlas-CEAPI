using System.Globalization;
using Atlas.Web.Api.Models.Outbound;
using Atlas.Web.Core.Domain.Models;

namespace Atlas.Web.Api.Mappers;

public static class OptionMatrixMapper
{
    public static OptionMatrixDto ToDto(OptionMatrix matrix)
    {
        return new(matrix.Product, matrix.Expiries.Select(ToDto).ToList());
    }

    private static OptionMatrixExpiryDto ToDto(OptionMatrixExpiry expiry)
    {
        return new(
            // Sent as a word, not the enum's number, so the JSON explains itself.
            expiry.Kind.ToString(),
            expiry.Label,
            expiry.ExpiryDate.ToString("yyyy-MM-dd", CultureInfo.InvariantCulture),
            expiry.UnderlyingSymbol,
            expiry.Strikes);
    }
}
