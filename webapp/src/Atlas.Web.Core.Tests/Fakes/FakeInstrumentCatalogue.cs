using System.Globalization;
using Atlas.Web.Core.Application.Ports;
using Atlas.Web.Core.Domain.Enumeration;
using Atlas.Web.Core.Domain.Models;

namespace Atlas.Web.Core.Tests.Fakes;

/// <summary>
/// A small made-up exchange listing for tests. Every strip expires two days before its first delivery month,
/// close enough to the real rule, and named the way ICE names them: Oct26, Q4 26, Winter26, Cal 27.
/// Symbols are readable stand-ins ("TTF:Oct26"), not real ICE tickers.
/// </summary>
public sealed class FakeInstrumentCatalogue : IInstrumentCatalogue
{
    private readonly List<Instrument> _outrights = new();
    private readonly List<SpreadInstrument> _spreads = new();
    private readonly List<OptionInstrument> _options = new();

    public IReadOnlyList<Instrument> Outrights => _outrights;
    public IReadOnlyList<SpreadInstrument> Spreads => _spreads;
    public IReadOnlyList<OptionInstrument> Options => _options;

    /// <summary>
    /// Lists a call and a put at each strike of a monthly option, on the given future, all expiring on the given
    /// day. Symbols read "TTF:Nov26:80C".
    /// </summary>
    public FakeInstrumentCatalogue WithOptionChain(
        string product, string expiry, DateOnly expiryDate, string underlyingSymbol, params decimal[] strikes) =>
        WithOptionChain(product, StripKind.Month, expiry, expiryDate, underlyingSymbol, strikes);

    /// <summary>The same for a strip option: a quarter, season or calendar year.</summary>
    public FakeInstrumentCatalogue WithOptionChain(
        string product, StripKind kind, string expiry, DateOnly expiryDate, string underlyingSymbol, params decimal[] strikes)
    {
        foreach (var strike in strikes)
        {
            foreach (var right in new[] { OptionRight.Call, OptionRight.Put })
            {
                var letter = right == OptionRight.Call ? "C" : "P";
                _options.Add(new OptionInstrument(
                    product, kind, expiry, expiryDate, underlyingSymbol, strike, right, $"{product}:{expiry}:{strike}{letter}"));
            }
        }
        return this;
    }

    /// <summary>Lists every month, quarter, season and calendar year that starts within the given range.</summary>
    public FakeInstrumentCatalogue WithStrips(string hub, DateOnly firstMonth, int months)
    {
        for (var i = 0; i < months; i++)
        {
            var start = firstMonth.AddMonths(i);
            var yy = (start.Year % 100).ToString("00");

            Add(hub, StripKind.Month, start.ToString("MMM", CultureInfo.InvariantCulture) + yy, start);

            if (start.Month is 1 or 4 or 7 or 10)
            {
                Add(hub, StripKind.Quarter, $"Q{(start.Month + 2) / 3} {yy}", start);
            }
            if (start.Month == 4) Add(hub, StripKind.Season, $"Summer{yy}", start);
            if (start.Month == 10) Add(hub, StripKind.Season, $"Winter{yy}", start);
            if (start.Month == 1) Add(hub, StripKind.Cal, $"Cal {yy}", start);
        }
        return this;
    }

    /// <summary>Says the exchange quotes this pair as a contract of its own.</summary>
    public FakeInstrumentCatalogue WithQuotedSpread(string hub, string near, string far)
    {
        var nearLeg = _outrights.Single(o => o.Hub == hub && o.Name == near);
        _spreads.Add(new SpreadInstrument(hub, $"{near}/{far}", near, far, $"{hub}:{near}/{far}", nearLeg.Expiry));
        return this;
    }

    /// <summary>Quotes every back-to-back pair of the given kind, as ICE does.</summary>
    public FakeInstrumentCatalogue WithBackToBackSpreads(string hub, StripKind kind)
    {
        var strips = _outrights.Where(o => o.Hub == hub && o.Kind == kind).OrderBy(o => o.Start).ToList();
        for (var i = 0; i + 1 < strips.Count; i++)
        {
            WithQuotedSpread(hub, strips[i].Name, strips[i + 1].Name);
        }
        return this;
    }

    private void Add(string hub, StripKind kind, string name, DateOnly start) =>
        _outrights.Add(new Instrument(hub, kind, name, $"{hub}:{name}", start, start.AddDays(-2)));
}
