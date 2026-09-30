namespace Atlas.Web.Core.Application.Ports;

/// <summary>
/// The price a contract is trading around right now, for rules that depend on the market rather than the date:
/// which strikes to show around a future. Null while nothing usable has arrived for the symbol.
/// </summary>
public interface IReferencePrices
{
    decimal? For(string symbol);
}
