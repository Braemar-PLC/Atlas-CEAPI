using Atlas.Web.Api.Models.Outbound;
using Atlas.Web.Core.Domain.Enumeration;
using Atlas.Web.Core.Domain.Models;

namespace Atlas.Web.Api.Mappers;

public static class ScreenMapper
{
    public static ScreenDto ToDto(Screen screen)
    {
        return new(screen.Key, screen.Title, screen.Rows.Select(ToDto).ToList());
    }

    private static ScreenRowDto ToDto(ScreenRow row)
    {
        return new(
            row.Hub,
            row.Label,
            row.Group,
            // Sent as a word, not the enum's number, so the JSON explains itself.
            row.Source == RowSource.Quoted ? "quoted" : "computed",
            row.Symbol,
            ToDto(row.Near),
            ToDto(row.Far));
    }

    private static ScreenLegDto? ToDto(ScreenLeg? leg)
    {
        return leg is null ? null : new(leg.Label, leg.Symbol);
    }
}
