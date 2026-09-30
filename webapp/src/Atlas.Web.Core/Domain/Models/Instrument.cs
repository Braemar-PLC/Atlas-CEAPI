using Atlas.Web.Core.Domain.Enumeration;

namespace Atlas.Web.Core.Domain.Models;

/// <summary>
/// One flat-price contract the exchange lists, e.g. TTF Oct26.
/// </summary>
/// <param name="Hub">The gas hub: "TTF" or "NBP".</param>
/// <param name="Kind">Month, quarter, season or calendar year.</param>
/// <param name="Name">The exchange's strip name, used as the row label: "Oct26", "Q4 26", "Winter26", "Cal 27".</param>
/// <param name="Symbol">The code the feed knows it by, e.g. "TFM 26V-ICN".</param>
/// <param name="Start">First day of the first delivery month. Used to put strips in date order.</param>
/// <param name="Expiry">The last day the contract trades. It stays on screen through this day.</param>
public sealed record Instrument(
    string Hub,
    StripKind Kind,
    string Name,
    string Symbol,
    DateOnly Start,
    DateOnly Expiry);

/// <summary>
/// One spread the exchange quotes as a contract of its own, e.g. TTF Oct26/Nov26.
/// The exchange only lists spreads between two strips of the same kind (the desk calls these "vanilla").
/// </summary>
/// <param name="Near">Strip name of the first leg, e.g. "Oct26".</param>
/// <param name="Far">Strip name of the second leg, e.g. "Nov26".</param>
/// <param name="Expiry">The last day it trades - the same day its near leg expires.</param>
public sealed record SpreadInstrument(
    string Hub,
    string Name,
    string Near,
    string Far,
    string Symbol,
    DateOnly Expiry);

/// <summary>
/// One option the exchange lists, e.g. the TTF Nov26 80.00 call, or the Q1 27 80.00 call - a strip option, one
/// price for the monthly options of every month in the quarter, season or year.
/// </summary>
/// <param name="Product">What the option is on, as the desk names it: "TTF", "EUA", "Brent", "WTI".</param>
/// <param name="Kind">A month, or a quarter, season or calendar-year strip.</param>
/// <param name="Expiry">The period the option covers, which labels its block on screen: "Nov26", "Q1 27", "Winter26", "Cal 27".</param>
/// <param name="ExpiryDate">The last day the option trades. It stays on screen through this day.</param>
/// <param name="UnderlyingSymbol">
/// The future the option is priced against: "TFM 26X-ICN" for a month (listed per option by ICE - EUA's monthly
/// options all sit on the December future), the strip future "TFMQ 27F-ICN" for a strip.
/// </param>
/// <param name="Strike">The price the option gives the right to buy or sell the future at.</param>
/// <param name="Symbol">The code the feed knows it by, e.g. "TFO 26XC8000-ICN".</param>
public sealed record OptionInstrument(
    string Product,
    StripKind Kind,
    string Expiry,
    DateOnly ExpiryDate,
    string UnderlyingSymbol,
    decimal Strike,
    OptionRight Right,
    string Symbol);

/// <summary>
/// The skeleton of an option chain for the calculator's matrix: every expiry the desk's rules pick, and every
/// strike the exchange lists at each - far more than the screens subscribe to, because the matrix prices them
/// with the model rather than from the feed. No prices here.
/// </summary>
public sealed record OptionMatrix(string Product, IReadOnlyList<OptionMatrixExpiry> Expiries);

public sealed record OptionMatrixExpiry(
    StripKind Kind,
    string Label,
    DateOnly ExpiryDate,
    string UnderlyingSymbol,
    IReadOnlyList<decimal> Strikes);
