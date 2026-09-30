using Atlas.Web.Core.Domain.Enumeration;

namespace Atlas.Web.Core.Domain.Models;

/// <summary>One leg of a worked-out spread: the strip's label and the symbol its prices arrive under.</summary>
public sealed record ScreenLeg(string Label, string Symbol);

/// <summary>
/// What makes a row an option: when it expires, the future it is on, the strike, and call or put. Rows of the
/// flat and spread screens carry none. On an options screen the one row per expiry without it is the future.
/// </summary>
public sealed record ScreenOption(DateOnly ExpiryDate, string UnderlyingSymbol, decimal Strike, OptionRight Right);

/// <summary>
/// One row of a desk screen.
/// A <see cref="RowSource.Quoted"/> row has a <paramref name="Symbol"/> and no legs.
/// A <see cref="RowSource.Computed"/> row has both legs and no symbol of its own.
/// </summary>
/// <param name="Group">
/// Which block of the screen the row belongs to: "Months", "Quarters", "Seasons", "Cals" or "Spreads", or on an
/// options screen the expiry ("Nov26").
/// </param>
/// <param name="Option">The option's details, on an options screen; null on every other row.</param>
public sealed record ScreenRow(
    string Hub,
    string Label,
    string Group,
    RowSource Source,
    string? Symbol,
    ScreenLeg? Near,
    ScreenLeg? Far,
    ScreenOption? Option = null)
{
    /// <summary>Every feed symbol this row needs: its own, or its two legs'.</summary>
    public IEnumerable<string> Symbols =>
        Source == RowSource.Quoted
            ? new[] { Symbol! }
            : new[] { Near!.Symbol, Far!.Symbol };
}

/// <summary>One desk screen: a title and its rows, top to bottom.</summary>
public sealed record Screen(string Key, string Title, IReadOnlyList<ScreenRow> Rows)
{
    /// <summary>Every feed symbol the screen needs, without repeats, in row order.</summary>
    public IReadOnlyList<string> Symbols => Rows.SelectMany(r => r.Symbols).Distinct().ToList();
}
