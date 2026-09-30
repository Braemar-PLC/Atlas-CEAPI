using Atlas.Web.Core.Application.Ports;

namespace Atlas.Web.Core.Tests.Fakes;

/// <summary>Reference prices a test sets by hand. A symbol not set has no price yet.</summary>
public sealed class FakeReferencePrices : IReferencePrices
{
    private readonly Dictionary<string, decimal> _prices = new();

    public FakeReferencePrices With(string symbol, decimal price)
    {
        _prices[symbol] = price;
        return this;
    }

    public decimal? For(string symbol) => _prices.TryGetValue(symbol, out var price) ? price : null;
}
