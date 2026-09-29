using Atlas.Web.Data.Seed;
using Microsoft.Data.Sqlite;
using Microsoft.EntityFrameworkCore;

namespace Atlas.Web.Data;

/// <summary>What the API does with the database when it starts: make sure the file can exist, bring the schema up to date, seed the desks.</summary>
public static class DatabaseStartup
{
    /// <summary>The connection string for the file at <paramref name="fullPath"/>, creating its folder if it is missing.</summary>
    public static string ConnectionString(string fullPath)
    {
        Directory.CreateDirectory(Path.GetDirectoryName(fullPath)!);
        return new SqliteConnectionStringBuilder { DataSource = fullPath }.ToString();
    }

    /// <summary>Applies any migrations not yet applied, then seeds an empty database. Safe to run on every start.</summary>
    public static async Task InitialiseAsync(AtlasDbContext db, CancellationToken ct)
    {
        await db.Database.MigrateAsync(ct);
        await DeskSeed.SeedIfEmptyAsync(db, ct);
    }
}
