using Atlas.Web.Core.Domain.Enumeration;

namespace Atlas.Web.Core.Application.Models;

/// <summary>
/// Normalised inbound boundary model crossing from the adapter layer into Core.
/// Carries only domain-relevant fields — ICE metadata has already been
/// interpreted and stripped by IceInterpreter before this is created.
/// </summary>
public sealed record PriceMessage(
    string Symbol,
    IReadOnlyList<(int FieldId, string Value)> Fields,
    DataStatus Status = DataStatus.Active);
