namespace Atlas.Web.Api.Authorization;

public static class ScreenDeskMap
{
    private static readonly IReadOnlyDictionary<string, string> DeskByScreen =
        new Dictionary<string, string>(StringComparer.OrdinalIgnoreCase)
        {
            ["ttf-flat"] = "natural-gas",
            ["ttf-spreads"] = "natural-gas",
            ["nbp"] = "natural-gas",
            ["coal-api2"] = "coal",
            ["coal-newcastle"] = "coal",
            ["coal-spreads"] = "coal",
            ["xcom-ttf"] = CrossCommodities,
            ["xcom-eua"] = CrossCommodities,
            ["xcom-wti"] = CrossCommodities,
            ["xcom-brent"] = CrossCommodities,
        };

    /// <summary>The options desk: its chains above and the calculator's matrices (OptionsController).</summary>
    public const string CrossCommodities = "cross-commodities";

    public static bool TryGetDeskKey(string screenKey, out string deskKey) =>
        DeskByScreen.TryGetValue(screenKey, out deskKey!);
}
