using Atlas.Web.Core.Domain.Logic;
using FluentAssertions;

namespace Atlas.Web.Core.Tests.Domain.Services;

public class RollClockTests
{
    private static DateTimeOffset Utc(string iso) => DateTimeOffset.Parse(iso, null, System.Globalization.DateTimeStyles.AssumeUniversal);

    [Theory]
    [InlineData("2026-09-25T11:59:00Z", "2026-09-25")] // 13:59 in Amsterdam (summer, UTC+2): still today
    [InlineData("2026-09-25T12:00:00Z", "2026-09-26")] // 14:00: the options have rolled
    [InlineData("2026-12-24T12:59:00Z", "2026-12-24")] // winter, UTC+1: 13:59
    [InlineData("2026-12-24T13:00:00Z", "2026-12-25")] // 14:00
    public void OptionsDay_MovesOnAt1400Amsterdam(string utc, string expected)
    {
        RollClock.OptionsDay(Utc(utc)).Should().Be(DateOnly.Parse(expected));
    }

    [Theory]
    [InlineData("2026-09-29T15:59:00Z", "2026-09-29")] // 17:59 in summer
    [InlineData("2026-09-29T16:00:00Z", "2026-09-30")] // 18:00: the futures have rolled
    [InlineData("2026-09-29T12:00:00Z", "2026-09-29")] // 14:00 moves the options, not the futures
    public void FuturesDay_MovesOnAt1800Amsterdam(string utc, string expected)
    {
        RollClock.FuturesDay(Utc(utc)).Should().Be(DateOnly.Parse(expected));
    }

    [Fact]
    public void TradingDay_CountsInAmsterdamsCalendar_NotUtcs()
    {
        // 22:30 UTC on the 25th is already 00:30 on the 26th in Amsterdam: the 26th, before either cut-off.
        RollClock.OptionsDay(Utc("2026-09-25T22:30:00Z")).Should().Be(new DateOnly(2026, 9, 26));
        RollClock.FuturesDay(Utc("2026-09-25T22:30:00Z")).Should().Be(new DateOnly(2026, 9, 26));
    }

    [Fact]
    public void Exchange_IsAmsterdam_SummerTwoHoursAheadOfUtc_WinterOne()
    {
        RollClock.Exchange.GetUtcOffset(Utc("2026-07-01T12:00:00Z")).Should().Be(TimeSpan.FromHours(2));
        RollClock.Exchange.GetUtcOffset(Utc("2026-12-01T12:00:00Z")).Should().Be(TimeSpan.FromHours(1));
    }
}
