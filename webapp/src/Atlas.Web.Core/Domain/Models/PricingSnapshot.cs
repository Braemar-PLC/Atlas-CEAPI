using Atlas.Web.Core.Domain.Enumeration;

namespace Atlas.Web.Core.Domain.Models;

public sealed record PricingSnapshot(
    string Symbol,
    IReadOnlyDictionary<int, string> Fields,
    DataStatus Status = DataStatus.Active
);
