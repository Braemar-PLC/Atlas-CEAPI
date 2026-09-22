namespace Atlas.Web.Core.Domain.Enumeration;

/// <summary>Where the prices on a screen row come from.</summary>
public enum RowSource
{
    /// <summary>The exchange quotes this as a contract of its own, so its prices arrive on the feed.</summary>
    Quoted,

    /// <summary>
    /// No such contract exists (for example a month against a quarter), so the screen works the price out
    /// from the two legs: bid = near bid - far offer, offer = near offer - far bid.
    /// </summary>
    Computed
}
