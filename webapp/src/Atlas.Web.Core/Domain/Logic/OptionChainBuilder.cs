using System.Globalization;
using Atlas.Web.Core.Application.Ports;
using Atlas.Web.Core.Domain.Enumeration;
using Atlas.Web.Core.Domain.Models;

namespace Atlas.Web.Core.Domain.Logic;

/// <summary>
/// Turns the options desk's rules into one screen per product - the option chain - and the matrix the calculator
/// prices.
///
/// A chain is read expiry by expiry: the monthly expiries first, then the quarter, season and calendar-year strip
/// options, as many of each as the rules ask for (12, 8, 8 and 5 on 2026-09-25), each still trading today. Each
/// expiry's block starts with the future the options are priced against - the month's future, or the strip
/// future - then one row per strike and right: the 80.00 call, the 80.00 put, the 81.00 call, and so on, strikes
/// rising, the call before the put. The exchange lists hundreds of strikes per expiry, most of them far from
/// where the future trades, so a block is cut to the rule's strikes either side of the listed strike nearest the
/// future's price: the near-the-money band. That price comes through <see cref="IReferencePrices"/> (the
/// settlement, or the last trade), and <see cref="OptionRule.SeedPrice"/> stands in until one has arrived. So,
/// unlike the flat screens, a chain moves with the market and not only with the calendar.
///
/// The feed relay reads its symbol list once at start-up, so <see cref="SymbolsToSubscribe"/> hands it a wider
/// band and the future can drift some way before a strike on screen has no prices. Expiries roll as the flat
/// screens do: an option stays through its last trading day.
///
/// The matrix (<see cref="Matrix"/>) is the same expiries with every listed strike and no prices: the calculator
/// prices them with the model, using the band's quotes for the vol.
///
/// Screen keys are "xcom-" plus the product in lower case: xcom-ttf, xcom-eua, xcom-brent, xcom-wti.
/// </summary>
public sealed class OptionChainBuilder
{
    public const string KeyPrefix = "xcom-";

    private static readonly StripKind[] KindOrder =
        { StripKind.Month, StripKind.Quarter, StripKind.Season, StripKind.Cal };

    private readonly IInstrumentCatalogue _catalogue;
    private readonly ScreenRules _rules;
    private readonly IReferencePrices _prices;

    public OptionChainBuilder(IInstrumentCatalogue catalogue, ScreenRules rules, IReferencePrices prices)
    {
        _catalogue = catalogue;
        _rules = rules;
        _prices = prices;
    }

    public static string KeyFor(string product) => KeyPrefix + product.ToLowerInvariant();

    /// <summary>
    /// One screen per product that has rules, by product name. The order the desk sees its tabs in is the
    /// frontend's; here the order is fixed so the API answers the same way whatever the configuration's order.
    /// </summary>
    public IReadOnlyList<Screen> Build(DateOnly today) =>
        _rules.Options
            .OrderBy(p => p.Key, StringComparer.Ordinal)
            .Select(p => Chain(p.Key, p.Value, today, subscribing: false))
            .ToList();

    /// <summary>Every symbol the chains may need while the relay runs: the wider band at each expiry, and the futures.</summary>
    public IReadOnlyList<string> SymbolsToSubscribe(DateOnly today) =>
        _rules.Options
            .SelectMany(p => Chain(p.Key, p.Value, today, subscribing: true).Symbols)
            .Distinct()
            .ToList();

    /// <summary>The matrix for a product: its expiries with every listed strike; null for a product without rules.</summary>
    public OptionMatrix? Matrix(string product, DateOnly today)
    {
        var rule = _rules.Options.FirstOrDefault(p => string.Equals(p.Key, product, StringComparison.OrdinalIgnoreCase));
        if (rule.Key is null)
        {
            return null;
        }

        var expiries = Expiries(rule.Key, rule.Value, today)
            .Select(e => new OptionMatrixExpiry(
                e.Key.Kind,
                e.Key.Expiry,
                e.Key.ExpiryDate,
                e.Key.UnderlyingSymbol,
                e.Select(o => o.Strike).Distinct().OrderBy(s => s).ToList()))
            .ToList();

        return new OptionMatrix(rule.Key, expiries);
    }

    private Screen Chain(string product, OptionRule rule, DateOnly today, bool subscribing)
    {
        var rows = new List<ScreenRow>();

        foreach (var expiry in Expiries(product, rule, today))
        {
            var (kind, label, expiryDate, underlying) = expiry.Key;

            // The future first: every strike in the block is measured against it.
            rows.Add(new ScreenRow(product, FutureLabel(underlying), label, RowSource.Quoted, underlying, null, null));

            var listed = expiry.Select(o => o.Strike).Distinct().OrderBy(s => s).ToList();
            var centre = _prices.For(underlying) ?? rule.SeedPrice;

            foreach (var strike in Band(listed, centre, rule.StrikesFor(kind, subscribing)))
            {
                foreach (var option in expiry.Where(o => o.Strike == strike).OrderBy(o => o.Right))
                {
                    rows.Add(new ScreenRow(
                        product,
                        $"{label} {Strike(strike)} {Letter(option.Right)}",
                        label,
                        RowSource.Quoted,
                        option.Symbol,
                        null,
                        null,
                        new ScreenOption(expiryDate, underlying, strike, option.Right)));
                }
            }
        }

        return new Screen(KeyFor(product), $"{product} Options", rows);
    }

    /// <summary>
    /// The expiries the rules pick, still trading today: the first N of each kind by date, months first, then
    /// quarters, seasons and cals. Each group holds that expiry's listed options.
    /// </summary>
    private IEnumerable<IGrouping<(StripKind Kind, string Expiry, DateOnly ExpiryDate, string UnderlyingSymbol), OptionInstrument>> Expiries(
        string product, OptionRule rule, DateOnly today)
    {
        var live = _catalogue.Options.Where(o => o.Product == product && o.ExpiryDate >= today).ToList();

        return KindOrder.SelectMany(kind => live
            .Where(o => o.Kind == kind)
            .GroupBy(o => (o.Kind, o.Expiry, o.ExpiryDate, o.UnderlyingSymbol))
            .OrderBy(g => g.Key.ExpiryDate)
            .Take(rule.ExpiriesFor(kind)));
    }

    /// <summary>The strikes within <paramref name="eachSide"/> places of the one nearest <paramref name="centre"/>.</summary>
    private static IEnumerable<decimal> Band(List<decimal> listed, decimal centre, int eachSide)
    {
        if (listed.Count == 0)
        {
            return listed;
        }

        // On a tie the lower strike wins, because "<" keeps the first one found.
        var nearest = 0;
        for (var i = 1; i < listed.Count; i++)
        {
            if (Math.Abs(listed[i] - centre) < Math.Abs(listed[nearest] - centre))
            {
                nearest = i;
            }
        }

        var first = Math.Max(0, nearest - eachSide);
        var last = Math.Min(listed.Count - 1, nearest + eachSide);
        return listed.GetRange(first, last - first + 1);
    }

    /// <summary>The future's strip name ("Dec26", "Q1 27") when the catalogue lists it, otherwise its symbol.</summary>
    private string FutureLabel(string symbol) =>
        _catalogue.Outrights.FirstOrDefault(o => o.Symbol == symbol)?.Name ?? symbol;

    private static string Strike(decimal strike) => strike.ToString("0.00", CultureInfo.InvariantCulture);

    private static string Letter(OptionRight right) => right == OptionRight.Call ? "C" : "P";
}
