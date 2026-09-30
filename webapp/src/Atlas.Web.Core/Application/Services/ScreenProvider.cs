using Atlas.Web.Core.Application.Ports;
using Atlas.Web.Core.Domain.Logic;
using Atlas.Web.Core.Domain.Models;

namespace Atlas.Web.Core.Application.Services;

/// <summary>
/// Gives <see cref="ScreenBuilder"/> and <see cref="OptionChainBuilder"/> the day to build for. The screens are
/// rebuilt on every call, which is cheap, so a contract that has just expired is gone from the next request
/// without anything being restarted, and an option chain follows the future's price the same way. The day comes
/// from <see cref="RollClock"/>: the futures roll at 18:00 Amsterdam on their last trading day and the options at
/// 14:00 on theirs, as ICE and the desk's Edgeview do - two clocks, never derived from each other.
/// </summary>
public sealed class ScreenProvider : IScreenProvider, IOptionMatrixProvider
{
    /// <summary>
    /// How far ahead <see cref="SymbolsToSubscribe"/> looks. Gas months expire once a month, so 45 days always
    /// reaches past the next expiry. The feed relay reads its symbol list once, when it starts; with this
    /// look-ahead a relay restarted at least every month or so never misses a newly rolled-in strip.
    /// </summary>
    public static readonly TimeSpan LookAhead = TimeSpan.FromDays(45);

    private readonly ScreenBuilder _builder;
    private readonly OptionChainBuilder _chains;
    private readonly TimeProvider _clock;

    public ScreenProvider(ScreenBuilder builder, OptionChainBuilder chains, TimeProvider clock)
    {
        _builder = builder;
        _chains = chains;
        _clock = clock;
    }

    /// <summary>The flat and spread screens, then the option chains.</summary>
    public IReadOnlyList<Screen> GetScreens()
    {
        var now = _clock.GetUtcNow();
        return _builder.Build(RollClock.FuturesDay(now)).Concat(_chains.Build(RollClock.OptionsDay(now))).ToList();
    }

    public Screen? Find(string key) =>
        GetScreens().FirstOrDefault(s => string.Equals(s.Key, key, StringComparison.OrdinalIgnoreCase));

    OptionMatrix? IOptionMatrixProvider.Find(string product) => _chains.Matrix(product, RollClock.OptionsDay(_clock.GetUtcNow()));

    public IReadOnlyList<string> SymbolsToSubscribe()
    {
        var now = _clock.GetUtcNow();
        var futuresDay = RollClock.FuturesDay(now);
        var optionsDay = RollClock.OptionsDay(now);
        var lookAhead = (int)LookAhead.TotalDays;

        return _builder.Build(futuresDay)
            .Concat(_builder.Build(futuresDay.AddDays(lookAhead)))
            .SelectMany(s => s.Symbols)
            .Concat(_chains.SymbolsToSubscribe(optionsDay))
            .Concat(_chains.SymbolsToSubscribe(optionsDay.AddDays(lookAhead)))
            .Distinct()
            .ToList();
    }
}
