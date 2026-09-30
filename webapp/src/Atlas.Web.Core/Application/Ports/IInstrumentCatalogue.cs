using Atlas.Web.Core.Domain.Models;

namespace Atlas.Web.Core.Application.Ports;

/// <summary>
/// Everything the exchange lists for the hubs Atlas shows: each contract's symbol, strip name and expiry date.
/// The core asks for it through this port so it never has to know where the list comes from.
/// </summary>
public interface IInstrumentCatalogue
{
    /// <summary>Flat-price contracts: months, quarters, seasons and calendar years.</summary>
    IReadOnlyList<Instrument> Outrights { get; }

    /// <summary>Spreads the exchange quotes as contracts of their own.</summary>
    IReadOnlyList<SpreadInstrument> Spreads { get; }

    /// <summary>Monthly options on the listed futures, for the products the options desk prices.</summary>
    IReadOnlyList<OptionInstrument> Options { get; }
}
