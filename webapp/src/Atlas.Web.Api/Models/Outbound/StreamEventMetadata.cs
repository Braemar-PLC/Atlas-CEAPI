using Atlas.Web.Api.Enumeration;

namespace Atlas.Web.Api.Models.Outbound;

public sealed record StreamEventMetadata(
    string Key,
    StreamEventTypes Type,
    PricingEventActions Action,
    DateTime ServerTimestamp
);
