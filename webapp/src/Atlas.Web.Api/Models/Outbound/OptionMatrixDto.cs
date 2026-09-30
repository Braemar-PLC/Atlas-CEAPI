namespace Atlas.Web.Api.Models.Outbound;

/// <summary>
/// The calculator's matrix for one product, from GET /api/options/{product}: every expiry the desk's rules pick,
/// and every strike the exchange lists at it. No prices - the browser prices the matrix with the model and takes
/// the futures' prices and the near-the-money quotes from the product's chain stream.
/// </summary>
public sealed record OptionMatrixDto(string Product, IReadOnlyList<OptionMatrixExpiryDto> Expiries);

/// <summary><see cref="Kind"/> is "Month", "Quarter", "Season" or "Cal"; <see cref="ExpiryDate"/> is "2026-10-27".</summary>
public sealed record OptionMatrixExpiryDto(
    string Kind,
    string Label,
    string ExpiryDate,
    string UnderlyingSymbol,
    IReadOnlyList<decimal> Strikes);
