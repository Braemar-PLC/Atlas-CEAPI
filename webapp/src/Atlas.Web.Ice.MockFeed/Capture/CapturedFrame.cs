namespace Atlas.Web.Ice.MockFeed.Capture;

/// <summary>
/// One field of a quote, as it appears on the relay's wire: a numeric field id
/// and its value already rendered as a string.
/// </summary>
public sealed record CapturedField(short Id, string Value);

/// <summary>
/// One symbol's field set, as carried by a single relay frame.
/// </summary>
public sealed record CapturedFrame(string Symbol, IReadOnlyList<CapturedField> Fields);
