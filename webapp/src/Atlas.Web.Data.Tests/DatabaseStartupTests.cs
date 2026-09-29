using Atlas.Web.Data.Tests.Support;
using FluentAssertions;
using Microsoft.EntityFrameworkCore;

namespace Atlas.Web.Data.Tests;

public class DatabaseStartupTests
{
    [Fact]
    public async Task InitialiseAsync_RunTwice_StillHasTheFiveSeededDesks()
    {
        using var database = new InMemoryDatabase();

        await using (var first = database.NewContext())
        {
            await DatabaseStartup.InitialiseAsync(first, CancellationToken.None);
        }
        await using var second = database.NewContext();
        await DatabaseStartup.InitialiseAsync(second, CancellationToken.None);

        (await second.Desks.CountAsync()).Should().Be(5);
    }

    [Fact]
    public void ConnectionString_CreatesTheFolderTheFileWillLiveIn()
    {
        var folder = Path.Combine(Path.GetTempPath(), "atlas-tests", Guid.NewGuid().ToString("N"));
        var file = Path.Combine(folder, "atlas.db");

        var connectionString = DatabaseStartup.ConnectionString(file);

        Directory.Exists(folder).Should().BeTrue();
        connectionString.Should().Contain(file);
        Directory.Delete(folder, recursive: true);
    }
}
