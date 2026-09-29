using Microsoft.Data.Sqlite;
using Microsoft.EntityFrameworkCore;

namespace Atlas.Web.Data.Tests.Support;

/// <summary>
/// A real SQLite database that lives only while its connection is open, created by the same migrations Azure
/// runs. Every context handed out shares the one connection, so a second context sees what the first wrote.
/// </summary>
public sealed class InMemoryDatabase : IDisposable
{
    private readonly SqliteConnection _connection;
    private readonly DbContextOptions<AtlasDbContext> _options;

    public InMemoryDatabase()
    {
        _connection = new SqliteConnection("DataSource=:memory:");
        _connection.Open();
        _options = new DbContextOptionsBuilder<AtlasDbContext>().UseSqlite(_connection).Options;
        using var context = NewContext();
        context.Database.Migrate();
    }

    public AtlasDbContext NewContext() => new(_options);

    public void Dispose() => _connection.Dispose();
}
