using Atlas.Web.Data.Seed;
using Atlas.Web.Data.Tests.Support;
using FluentAssertions;
using Microsoft.EntityFrameworkCore;

namespace Atlas.Web.Data.Tests.Seed;

public class DeskSeedTests : IDisposable
{
    private readonly InMemoryDatabase _database = new();

    public void Dispose() => _database.Dispose();

    [Fact]
    public async Task SeedIfEmptyAsync_EmptyDatabase_AddsTheFiveDesks()
    {
        await using var context = _database.NewContext();

        await DeskSeed.SeedIfEmptyAsync(context, CancellationToken.None);

        (await context.Desks.OrderBy(d => d.Key).Select(d => d.Key).ToListAsync())
            .Should().Equal("coal", "cross-commodities", "dry-ffa", "natural-gas", "power");
        (await context.DeskMembers.CountAsync()).Should().Be(0);
    }

    [Fact]
    public async Task SeedIfEmptyAsync_SecondRun_AddsNothing()
    {
        await using (var first = _database.NewContext())
        {
            await DeskSeed.SeedIfEmptyAsync(first, CancellationToken.None);
        }

        await using var second = _database.NewContext();
        await DeskSeed.SeedIfEmptyAsync(second, CancellationToken.None);

        (await second.Desks.CountAsync()).Should().Be(5);
    }

    [Fact]
    public async Task SeedIfEmptyAsync_DatabaseAlreadyHasADesk_LeavesItAlone()
    {
        await using var context = _database.NewContext();
        context.Desks.Add(new Entities.DeskEntity { Key = "lng", Name = "LNG", Description = "Cargoes" });
        await context.SaveChangesAsync();

        await DeskSeed.SeedIfEmptyAsync(context, CancellationToken.None);

        (await context.Desks.Select(d => d.Key).ToListAsync()).Should().Equal("lng");
    }
}
