using Microsoft.EntityFrameworkCore;
using Microsoft.EntityFrameworkCore.Design;

namespace Atlas.Web.Data.Migrations;

/// <summary>
/// Used only by the <c>dotnet ef</c> tool to build the model when adding a migration. The in-memory data source
/// means no file is touched; at run time the API supplies the real options.
/// </summary>
public sealed class DesignTimeContextFactory : IDesignTimeDbContextFactory<AtlasDbContext>
{
    public AtlasDbContext CreateDbContext(string[] args) =>
        new(new DbContextOptionsBuilder<AtlasDbContext>().UseSqlite("DataSource=:memory:").Options);
}
