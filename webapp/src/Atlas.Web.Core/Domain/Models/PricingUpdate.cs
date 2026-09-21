namespace Atlas.Web.Core.Domain.Models;

public sealed record PricingUpdate(
    string Symbol,
    IReadOnlyList<(int FieldId, string Value)> Fields);
