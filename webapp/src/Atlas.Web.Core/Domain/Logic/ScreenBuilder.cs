using Atlas.Web.Core.Application.Ports;
using Atlas.Web.Core.Domain.Enumeration;
using Atlas.Web.Core.Domain.Models;

namespace Atlas.Web.Core.Domain.Logic;

/// <summary>
/// Turns the desks' rules into the rows of each screen for a given day.
///
/// Everything counts from the FRONT MONTH: the nearest month contract that is still trading - not the calendar
/// month. Gas Oct26 stops trading on 29 Sep 2026, so on 30 Sep the front month is already Nov26 although it is
/// still September. Coal months trade on into their delivery month (Sep26 until 25 Sep 2026), so on 24 Sep coal's
/// front month is still Sep26 while gas has moved to Oct26 - nothing here knows the difference, the catalogue's
/// expiry dates carry it. Quarters, seasons and calendar years each begin with the first one that starts on or
/// after the front month. A strip stays on screen through its last trading day and is gone the next morning, and
/// the next strip along takes the freed place - that is the automatic roll the desks asked for.
///
/// Spreads follow the desk's rule of 2026-09-21: if the exchange quotes the pair as a contract ("vanilla":
/// month/month, quarter/quarter, season/season, cal/cal), use the exchange's own price. Only when it does not
/// (a month against a quarter, say) is the row worked out from its two legs.
///
/// Each hub's counts come from <see cref="ScreenRules.FlatFor"/> and <see cref="ScreenRules.SpreadsFor"/>: the
/// gas hubs share the defaults, the coal hubs have their own.
///
/// This class is pure logic: no clock, no files, no network. Same inputs, same screens.
/// </summary>
public sealed class ScreenBuilder
{
    public const string TtfFlatKey = "ttf-flat";
    public const string TtfSpreadsKey = "ttf-spreads";
    public const string NbpKey = "nbp";
    public const string CoalApi2Key = "coal-api2";
    public const string CoalNewcastleKey = "coal-newcastle";
    public const string CoalSpreadsKey = "coal-spreads";

    public const string SpreadsGroup = "Spreads";

    // Hub names as the catalogue (and ICE's field 951) spell them.
    private const string TtfHub = "TTF";
    private const string NbpHub = "NBP";
    private const string AraHub = "ARA";
    private const string NewcastleHub = "Newcastle";

    // Top-to-bottom order of the blocks on a screen, as on the desk's Edgeview: months, quarters, seasons, cals.
    private static readonly StripKind[] KindOrder =
        { StripKind.Month, StripKind.Quarter, StripKind.Season, StripKind.Cal };

    private static readonly IReadOnlyDictionary<StripKind, string> GroupNames = new Dictionary<StripKind, string>
    {
        [StripKind.Month] = "Months",
        [StripKind.Quarter] = "Quarters",
        [StripKind.Season] = "Seasons",
        [StripKind.Cal] = "Cals",
    };

    private readonly IInstrumentCatalogue _catalogue;
    private readonly ScreenRules _rules;
    private readonly Dictionary<(string Hub, string Near, string Far), SpreadInstrument> _quotedSpreads;

    public ScreenBuilder(IInstrumentCatalogue catalogue, ScreenRules rules)
    {
        _catalogue = catalogue;
        _rules = rules;
        _quotedSpreads = new Dictionary<(string, string, string), SpreadInstrument>();
        foreach (var spread in catalogue.Spreads)
        {
            _quotedSpreads[(spread.Hub, spread.Near, spread.Far)] = spread;
        }
    }

    public IReadOnlyList<Screen> Build(DateOnly today)
    {
        return new[]
        {
            new Screen(TtfFlatKey, "Nat Gas TTF Flat Price", FlatRows(TtfHub, today)),
            new Screen(TtfSpreadsKey, "Nat Gas TTF Spreads", SpreadRows(TtfHub, today)),
            // The desk asked for NBP on one screen: flat prices, then the spreads below.
            new Screen(NbpKey, "Nat Gas NBP", FlatRows(NbpHub, today).Concat(SpreadRows(NbpHub, today)).ToList()),
            new Screen(CoalApi2Key, "Coal API2 (Rotterdam)", FlatRows(AraHub, today)),
            new Screen(CoalNewcastleKey, "Coal Newcastle", FlatRows(NewcastleHub, today)),
            // Both coal hubs' spreads on one screen, Rotterdam's first, as on WebICE's Coal tab.
            new Screen(CoalSpreadsKey, "Coal Spreads", SpreadRows(AraHub, today).Concat(SpreadRows(NewcastleHub, today)).ToList()),
        };
    }

    private List<ScreenRow> FlatRows(string hub, DateOnly today)
    {
        var rows = new List<ScreenRow>();
        foreach (var kind in KindOrder)
        {
            // If the exchange lists fewer strips than asked for (NBP has few calendar years), show what exists.
            foreach (var strip in Strips(hub, kind, today).Take(CountFor(hub, kind)))
            {
                rows.Add(new ScreenRow(hub, strip.Name, GroupNames[kind], RowSource.Quoted, strip.Symbol, null, null));
            }
        }
        return rows;
    }

    private List<ScreenRow> SpreadRows(string hub, DateOnly today)
    {
        var pairs = new List<(Instrument Near, Instrument Far)>();

        // The regular spreads: each of the first N strips against the strip `Gap` places further on.
        foreach (var kind in KindOrder)
        {
            var strips = Strips(hub, kind, today);
            foreach (var rule in PairRulesFor(hub, kind))
            {
                for (var i = 0; i < rule.Count && i + rule.Gap < strips.Count; i++)
                {
                    pairs.Add((strips[i], strips[i + rule.Gap]));
                }
            }
        }

        // The desk's one-off picks. Either leg may have expired since the entry was written - then it drops off.
        if (_rules.ExtraSpreads.TryGetValue(hub, out var extras))
        {
            var live = _catalogue.Outrights
                .Where(o => o.Hub == hub && o.Expiry >= today)
                .GroupBy(o => o.Name)
                .ToDictionary(g => g.Key, g => g.First());

            foreach (var extra in extras)
            {
                var legs = extra.Split('/', 2, StringSplitOptions.TrimEntries);
                if (legs.Length == 2 && live.TryGetValue(legs[0], out var near) && live.TryGetValue(legs[1], out var far))
                {
                    pairs.Add((near, far));
                }
            }
        }

        return pairs
            .DistinctBy(p => (p.Near.Name, p.Far.Name))
            .OrderBy(p => Array.IndexOf(KindOrder, p.Near.Kind))
            .ThenBy(p => p.Near.Start)
            .ThenBy(p => Array.IndexOf(KindOrder, p.Far.Kind))
            .ThenBy(p => p.Far.Start)
            .Select(p => SpreadRow(hub, p.Near, p.Far, today))
            .ToList();
    }

    private ScreenRow SpreadRow(string hub, Instrument near, Instrument far, DateOnly today)
    {
        // The exchange's own spread market is tighter than the two legs and carries volume and a settlement
        // price, so it wins whenever it exists.
        if (_quotedSpreads.TryGetValue((hub, near.Name, far.Name), out var quoted) && quoted.Expiry >= today)
        {
            return new ScreenRow(hub, quoted.Name, SpreadsGroup, RowSource.Quoted, quoted.Symbol, null, null);
        }

        return new ScreenRow(
            hub,
            $"{near.Name}/{far.Name}",
            SpreadsGroup,
            RowSource.Computed,
            null,
            new ScreenLeg(near.Name, near.Symbol),
            new ScreenLeg(far.Name, far.Symbol));
    }

    /// <summary>Strips of one kind still trading today, in date order, beginning on or after the front month.</summary>
    private List<Instrument> Strips(string hub, StripKind kind, DateOnly today)
    {
        var frontMonth = _catalogue.Outrights
            .Where(o => o.Hub == hub && o.Kind == StripKind.Month && o.Expiry >= today)
            .OrderBy(o => o.Start)
            .FirstOrDefault();

        if (frontMonth is null)
        {
            // The catalogue is older than every month it lists. Nothing can be shown until it is refreshed.
            return new List<Instrument>();
        }

        return _catalogue.Outrights
            .Where(o => o.Hub == hub && o.Kind == kind && o.Expiry >= today && o.Start >= frontMonth.Start)
            .OrderBy(o => o.Start)
            .ToList();
    }

    private int CountFor(string hub, StripKind kind)
    {
        var flat = _rules.FlatFor(hub);
        return kind switch
        {
            StripKind.Month => flat.Months,
            StripKind.Quarter => flat.Quarters,
            StripKind.Season => flat.Seasons,
            _ => flat.Cals,
        };
    }

    private List<PairRule> PairRulesFor(string hub, StripKind kind)
    {
        var spreads = _rules.SpreadsFor(hub);
        return kind switch
        {
            StripKind.Month => spreads.Months,
            StripKind.Quarter => spreads.Quarters,
            StripKind.Season => spreads.Seasons,
            _ => spreads.Cals,
        };
    }
}
