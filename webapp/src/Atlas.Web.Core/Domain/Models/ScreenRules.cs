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
