using Atlas.Web.Core.Domain.Enumeration;

namespace Atlas.Web.Core.Domain.Models;

/// <summary>
/// How many strips each desk screen shows. These are the desks' preferences, so they live in configuration
/// (the "Screens" section of appsettings.json) - changing a count is an edit there, not a code change.
/// <see cref="Flat"/> and <see cref="Spreads"/> are the defaults, agreed with the gas desk (Harrison Lee) on
/// 2026-09-21. A hub that wants something else has its own entry in <see cref="Hubs"/> (the coal hubs, agreed
/// with Sean Hays on 2026-09-24). Ask through <see cref="FlatFor"/> and <see cref="SpreadsFor"/>.
///
/// The lists below start empty on purpose: the configuration binder ADDS to a list that already has items,
/// so defaults written here would be doubled up by the values in appsettings.json.
/// </summary>
public sealed class ScreenRules
{
    public const string Section = "Screens";

    /// <summary>Flat-price rows, for every hub without rules of its own.</summary>
    public FlatRule Flat { get; set; } = new();

    /// <summary>The regular spread rows, for every hub without rules of its own.</summary>
    public SpreadRule Spreads { get; set; } = new();

    /// <summary>
    /// One-off spreads the desk has asked for on top of the regular ones, by hub, written "Near/Far" with the
    /// exchange's strip names: "Apr27/Aug27", "Q1 27/Q4 27". The two legs may be different kinds ("Feb27/Q3 27").
    /// An entry drops off by itself once its near leg has expired.
    /// </summary>
    public Dictionary<string, List<string>> ExtraSpreads { get; set; } = new();

    /// <summary>
    /// Hubs whose counts or spreads differ from the defaults, by hub name ("ARA", "Newcastle"). A part a hub
    /// leaves out falls back to the default; a part it gives replaces the default whole, so a kind missing from
    /// a hub's own spread rules gets no spread rows at all.
    /// </summary>
    public Dictionary<string, HubRules> Hubs { get; set; } = new();

    /// <summary>
    /// The options screens, by product ("TTF", "EUA", "Brent", "WTI"). A product without an entry has no options
    /// screen. Bound by key, so nothing here doubles up; the binder sorts the keys by name, so the order the
    /// desk sees the products in is decided in the frontend, not here.
    /// </summary>
    public Dictionary<string, OptionRule> Options { get; set; } = new();

    public FlatRule FlatFor(string hub) =>
        Hubs.TryGetValue(hub, out var own) && own.Flat is not null ? own.Flat : Flat;

    public SpreadRule SpreadsFor(string hub) =>
        Hubs.TryGetValue(hub, out var own) && own.Spreads is not null ? own.Spreads : Spreads;
}

/// <summary>A hub's own rules. Either part may be left out to keep the default.</summary>
public sealed class HubRules
{
    public FlatRule? Flat { get; set; }
    public SpreadRule? Spreads { get; set; }
}

/// <summary>How many consecutive strips of each kind to show, counting from the front month.</summary>
public sealed class FlatRule
{
    public int Months { get; set; }
    public int Quarters { get; set; }
    public int Seasons { get; set; }
    public int Cals { get; set; }
}

/// <summary>Which regular spreads to show for each kind of strip.</summary>
public sealed class SpreadRule
{
    public List<PairRule> Months { get; set; } = new();
    public List<PairRule> Quarters { get; set; } = new();
    public List<PairRule> Seasons { get; set; } = new();
    public List<PairRule> Cals { get; set; } = new();
}

/// <summary>
/// "Pair each of the first <see cref="Count"/> strips with the strip <see cref="Gap"/> places after it."
/// Gap 1 is back-to-back (Oct26/Nov26, Winter26/Summer27). For seasons, gap 2 is the same season a year on
/// (Winter26/Winter27).
/// </summary>
public sealed class PairRule
{
    public int Gap { get; set; }
    public int Count { get; set; }
}

/// <summary>
/// How much of an option chain to show. The expiries: the first <see cref="Months"/> monthly expiries still
/// trading, then <see cref="Quarters"/>, <see cref="Seasons"/> and <see cref="Cals"/> strip expiries (the desk's
/// 12, 8, 8 and 5 of 2026-09-25). The strikes at each: <see cref="StrikesEachSide"/> either side of the listed
/// strike nearest the future's price for a month, <see cref="StripStrikesEachSide"/> for a strip - fewer, because
/// the strips are there for their at-the-money vol rather than for trading a ladder. The feed relay is given the
/// wider <see cref="SubscribeStrikesEachSide"/> and <see cref="SubscribeStripStrikesEachSide"/> instead, because it
/// reads its list once at start-up and the future moves in the meantime. <see cref="SeedPrice"/> stands in for
/// the future's price until one has arrived from the feed.
/// </summary>
public sealed class OptionRule
{
    public int Months { get; set; }
    public int Quarters { get; set; }
    public int Seasons { get; set; }
    public int Cals { get; set; }
    public int StrikesEachSide { get; set; }
    public int SubscribeStrikesEachSide { get; set; }
    public int StripStrikesEachSide { get; set; }
    public int SubscribeStripStrikesEachSide { get; set; }
    public decimal SeedPrice { get; set; }

    public int ExpiriesFor(StripKind kind) => kind switch
    {
        StripKind.Month => Months,
        StripKind.Quarter => Quarters,
        StripKind.Season => Seasons,
        _ => Cals,
    };

    public int StrikesFor(StripKind kind, bool subscribing) => (kind, subscribing) switch
    {
        (StripKind.Month, false) => StrikesEachSide,
        (StripKind.Month, true) => SubscribeStrikesEachSide,
        (_, false) => StripStrikesEachSide,
        (_, true) => SubscribeStripStrikesEachSide,
    };
}
