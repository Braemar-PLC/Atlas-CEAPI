using Atlas.Web.Data.Entities;
using Microsoft.EntityFrameworkCore;

namespace Atlas.Web.Data.Seed;

/// <summary>The desks a fresh database starts with: Braemar's five, with no members until an admin adds them.</summary>
public static class DeskSeed
{
    public static readonly IReadOnlyList<(string Key, string Name, string Description)> Desks = new[]
    {
        ("coal", "Coal", "API2, API4 and Newcastle physical and paper"),
        ("cross-commodities", "Cross-Commodities", "Spreads across gas, power, coal and carbon"),
        ("dry-ffa", "Dry FFA", "Capesize, Panamax and Supramax forward freight"),
        ("natural-gas", "Natural Gas", "TTF, NBP and JKM flat prices and time spreads"),
        ("power", "Power", "UK and continental baseload and peak"),
    };

    /// <summary>Adds the five desks when there are none at all; a database that already has desks is left as it is.</summary>
    public static async Task SeedIfEmptyAsync(AtlasDbContext db, CancellationToken ct)
    {
        if (await db.Desks.AnyAsync(ct))
        {
            return;
        }
        foreach (var (key, name, description) in Desks)
        {
            db.Desks.Add(new DeskEntity { Key = key, Name = name, Description = description });
        }
        await db.SaveChangesAsync(ct);
    }
}
