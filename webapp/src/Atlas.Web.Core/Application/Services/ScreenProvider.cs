using Atlas.Web.Core.Application.Ports;
using Atlas.Web.Core.Domain.Logic;
using Atlas.Web.Core.Domain.Models;

namespace Atlas.Web.Core.Application.Services;

/// <summary>
/// Gives <see cref="ScreenBuilder"/> today's date. The screens are rebuilt on every call, which is cheap,
/// so a strip that expired last night is gone from the next request without anything being restarted.
/// "Today" is the UTC date; the hour's difference from London time in summer does not matter for a roll
/// that happens overnight.
/// </summary>
public sealed class ScreenProvider : IScreenProvider
{
    /// <summary>
    /// How far ahead <see cref="SymbolsToSubscribe"/> looks. Gas months expire once a month, so 45 days always
    /// reaches past the next expiry. The feed relay reads its symbol list once, when it starts; with this
    /// look-ahead a relay restarted at least every month or so never misses a newly rolled-in strip.
    /// </summary>
    public static readonly TimeSpan LookAhead = TimeSpan.FromDays(45);

    private readonly ScreenBuilder _builder;
    private readonly TimeProvider _clock;

    public ScreenProvider(ScreenBuilder builder, TimeProvider clock)
    {
        _builder = builder;
        _clock = clock;
    }

    public IReadOnlyList<Screen> GetScreens() => _builder.Build(Today());

    public Screen? Find(string key) =>
        GetScreens().FirstOrDefault(s => string.Equals(s.Key, key, StringComparison.OrdinalIgnoreCase));

    public IReadOnlyList<string> SymbolsToSubscribe()
    {
        var today = Today();
        var later = today.AddDays((int)LookAhead.TotalDays);

        return _builder.Build(today)
            .Concat(_builder.Build(later))
            .SelectMany(s => s.Symbols)
            .Distinct()
            .ToList();
    }

    private DateOnly Today() => DateOnly.FromDateTime(_clock.GetUtcNow().UtcDateTime);
}
