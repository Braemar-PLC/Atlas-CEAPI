namespace Atlas.Web.Ice;

/// <summary>
/// Full ICE wire format including all metadata fields.
/// Never leaves the Ice project.
/// </summary>
public sealed record IceEnvelope(
    string Symbol,
    IReadOnlyList<(int FieldId, string Value)> Fields);
