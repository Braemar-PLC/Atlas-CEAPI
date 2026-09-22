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
