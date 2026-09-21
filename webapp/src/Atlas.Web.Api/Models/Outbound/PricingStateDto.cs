namespace Atlas.Web.Api.Models.Outbound;

/// <summary>
/// Pricing data payload. Carried inside StreamEvent{PricingStateDto}.
/// Action (update/reset) is in the envelope metadata.
/// </summary>
public sealed record PricingStateDto(
    string Symbol,
    IReadOnlyDictionary<int, string> Fields);
