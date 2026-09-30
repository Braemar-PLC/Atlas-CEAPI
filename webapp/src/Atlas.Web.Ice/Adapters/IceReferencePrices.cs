using System.Globalization;
using Atlas.Web.Core.Application.Ports;
using Atlas.Web.Ice.Domain.Enumeration;

namespace Atlas.Web.Ice.Adapters;

/// <summary>
/// A contract's reference price, read out of what the feed has sent so far: the settlement if there is one (it
/// moves once a day, so the strikes around it stay put through the session), else the last trade, else the middle
/// of bid and ask. Null until something usable has arrived - the store answers an empty record for a symbol it
/// has not seen. ICE writes a price it does not have as 0 or -99, so nothing at or below zero counts.
///
/// The store is taken lazily because of a loop in the wiring: the store checks symbols against the screens, the
/// screens need these prices, and these prices need the store. Nothing is read until a chain is built.
/// </summary>
public sealed class IceReferencePrices : IReferencePrices
{
    private readonly Lazy<IPricingStore> _store;

    public IceReferencePrices(Lazy<IPricingStore> store)
    {
        _store = store;
    }

    public decimal? For(string symbol)
    {
        var fields = _store.Value.CurrentSnapshot(symbol).Fields;
        return Price(fields, IceQuoteFieldIds.Settlement)
            ?? Price(fields, IceQuoteFieldIds.Last)
            ?? Mid(Price(fields, IceQuoteFieldIds.Bid), Price(fields, IceQuoteFieldIds.Ask));
    }

    private static decimal? Price(IReadOnlyDictionary<int, string> fields, IceQuoteFieldIds id)
    {
        if (fields.TryGetValue((int)id, out var value)
            && decimal.TryParse(value, NumberStyles.Float, CultureInfo.InvariantCulture, out var price)
            && price > 0)
        {
            return price;
        }
        return null;
    }

    private static decimal? Mid(decimal? bid, decimal? ask) =>
        bid is not null && ask is not null ? (bid + ask) / 2 : null;
}
