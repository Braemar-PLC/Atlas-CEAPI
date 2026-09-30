namespace Atlas.Web.Api.Models.Outbound;

/// <summary>
/// One desk screen as the browser receives it from GET /api/screens/{key}: the title and the rows, top to bottom.
/// The browser does not decide which strips to show - it draws exactly these rows, so a roll needs no frontend change.
/// </summary>
public sealed record ScreenDto(string Key, string Title, IReadOnlyList<ScreenRowDto> Rows);

/// <summary>
/// One row. <see cref="Source"/> is "quoted" (prices arrive under <see cref="Symbol"/>) or "computed"
/// (no such contract exists; the browser works the row out from <see cref="Near"/> and <see cref="Far"/>).
/// <see cref="Option"/> is set on an option row and null on every other, including the future at the top of
/// each expiry on an options screen.
/// </summary>
public sealed record ScreenRowDto(
    string Hub,
    string Label,
    string Group,
    string Source,
    string? Symbol,
    ScreenLegDto? Near,
    ScreenLegDto? Far,
    ScreenOptionDto? Option = null);

public sealed record ScreenLegDto(string Label, string Symbol);

/// <summary>An option row's details: <see cref="ExpiryDate"/> as "2026-10-27", <see cref="Right"/> as "C" or "P".</summary>
public sealed record ScreenOptionDto(string ExpiryDate, string UnderlyingSymbol, decimal Strike, string Right);
