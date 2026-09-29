using Atlas.Web.Core.Domain.Models;
using Atlas.Web.Data.Repositories;
using Atlas.Web.Data.Tests.Support;
using FluentAssertions;
using Microsoft.EntityFrameworkCore;

namespace Atlas.Web.Data.Tests.Repositories;

public class EfDeskRepositoryTests : IDisposable
{
    private static readonly Desk NaturalGas = new("natural-gas", "Natural Gas", "TTF, NBP and JKM");
    private static readonly Desk Coal = new("coal", "Coal", "API2 and API4");
    private static readonly string[] Nobody = Array.Empty<string>();

    private readonly InMemoryDatabase _database = new();

    private EfDeskRepository Repository() => new(_database.NewContext());

    public void Dispose() => _database.Dispose();

    [Fact]
    public async Task GetAllAsync_ReturnsDesksInNameOrder()
    {
        await Repository().SaveAsync(NaturalGas, Nobody, CancellationToken.None);
        await Repository().SaveAsync(Coal, Nobody, CancellationToken.None);

        var all = await Repository().GetAllAsync(CancellationToken.None);

        all.Should().Equal(Coal, NaturalGas);
    }

    [Fact]
    public async Task FindAsync_UnknownKey_ReturnsNull()
    {
        (await Repository().FindAsync("power", CancellationToken.None)).Should().BeNull();
    }

    [Fact]
    public async Task SaveAsync_NewDesk_IsStoredWithItsMembers()
    {
        await Repository().SaveAsync(NaturalGas, new[] { "sean.hays@braemar.com", "marc.jarvis@braemar.com" }, CancellationToken.None);

        (await Repository().FindAsync("natural-gas", CancellationToken.None)).Should().Be(NaturalGas);
        (await Repository().MembersAsync("natural-gas", CancellationToken.None))
            .Should().BeEquivalentTo("sean.hays@braemar.com", "marc.jarvis@braemar.com");
    }

    [Fact]
    public async Task SaveAsync_ExistingDesk_ReplacesNameDescriptionAndMembers()
    {
        await Repository().SaveAsync(NaturalGas, new[] { "sean.hays@braemar.com" }, CancellationToken.None);

        var renamed = NaturalGas with { Name = "Nat Gas", Description = "Gas" };
        await Repository().SaveAsync(renamed, new[] { "marc.jarvis@braemar.com" }, CancellationToken.None);

        (await Repository().FindAsync("natural-gas", CancellationToken.None)).Should().Be(renamed);
        (await Repository().MembersAsync("natural-gas", CancellationToken.None)).Should().Equal("marc.jarvis@braemar.com");
        (await Repository().GetAllAsync(CancellationToken.None)).Should().HaveCount(1);
    }

    [Fact]
    public async Task DesksForAsync_ReturnsOnlyTheDesksThatEmailIsIn()
    {
        await Repository().SaveAsync(NaturalGas, new[] { "sean.hays@braemar.com" }, CancellationToken.None);
        await Repository().SaveAsync(Coal, new[] { "marc.jarvis@braemar.com" }, CancellationToken.None);

        (await Repository().DesksForAsync("sean.hays@braemar.com", CancellationToken.None)).Should().Equal(NaturalGas);
        (await Repository().DesksForAsync("nobody@braemar.com", CancellationToken.None)).Should().BeEmpty();
    }

    [Fact]
    public async Task MembersAsync_UnknownDesk_ReturnsNobody()
    {
        (await Repository().MembersAsync("power", CancellationToken.None)).Should().BeEmpty();
    }

    [Fact]
    public async Task Schema_SameEmailTwiceForOneDesk_IsRefusedByTheDatabase()
    {
        await Repository().SaveAsync(Coal, new[] { "sean.hays@braemar.com" }, CancellationToken.None);

        // A second context has its own change tracker, so this reaches the database's key constraint.
        await using var context = _database.NewContext();
        context.DeskMembers.Add(new Entities.DeskMemberEntity { DeskKey = "coal", Email = "sean.hays@braemar.com" });
        var act = () => context.SaveChangesAsync();

        await act.Should().ThrowAsync<DbUpdateException>();
    }
}
