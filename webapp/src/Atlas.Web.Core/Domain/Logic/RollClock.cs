namespace Atlas.Web.Core.Domain.Logic;

/// <summary>
/// When a contract stops counting as "still trading" on the screens.
///
/// ICE keeps two independent roll clocks (the rule from ICE, via Sean Hays, 2026-09-25): an option rolls at
/// 14:00 Europe/Amsterdam on its last trading day, a future at 18:00 on its own - and neither is derived from the
/// other. Every contract carries its own last trading day in the catalogue, so the screens only need to know
/// which calendar day to compare it with: on an expiry day the expiring contracts stay until the cut-off, then
/// the next ones take their place - the same afternoon, as the desk sees on Edgeview, not at midnight.
///
/// <see cref="TradingDay"/> answers that day: the exchange's local date, or the day after once the local time has
/// reached the cut-off. A contract whose last trading day is on or after it is still shown.
/// </summary>
public static class RollClock
{
    public static readonly TimeOnly OptionsCutOff = new(14, 0);
    public static readonly TimeOnly FuturesCutOff = new(18, 0);

    /// <summary>ICE Endex and ICE Futures Europe roll on Amsterdam time for these contracts.</summary>
    public static readonly TimeZoneInfo Exchange = FindExchangeZone();

    public static DateOnly OptionsDay(DateTimeOffset utcNow) => TradingDay(utcNow, OptionsCutOff, Exchange);

    public static DateOnly FuturesDay(DateTimeOffset utcNow) => TradingDay(utcNow, FuturesCutOff, Exchange);

    public static DateOnly TradingDay(DateTimeOffset utcNow, TimeOnly cutOff, TimeZoneInfo zone)
    {
        var local = TimeZoneInfo.ConvertTime(utcNow, zone).DateTime;
        var day = DateOnly.FromDateTime(local);
        return TimeOnly.FromDateTime(local) >= cutOff ? day.AddDays(1) : day;
    }

    private static TimeZoneInfo FindExchangeZone()
    {
        try
        {
            return TimeZoneInfo.FindSystemTimeZoneById("Europe/Amsterdam");
        }
        catch (TimeZoneNotFoundException)
        {
            // A Windows machine without ICU knows the zone only by its Windows name.
            return TimeZoneInfo.FindSystemTimeZoneById("W. Europe Standard Time");
        }
    }
}
