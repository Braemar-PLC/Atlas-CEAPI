using Atlas.Web.Core.Application;
using Atlas.Web.Core.Application.Services;
using Atlas.Web.Core.Domain.Models;
using Atlas.Web.Core.Tests.Fakes;
using FluentAssertions;

namespace Atlas.Web.Core.Tests.Application.Services;

public class DeskServiceTests
{
    private static readonly Desk NaturalGas = new("natural-gas", "Natural Gas", "TTF, NBP and JKM");
    private static readonly Desk Coal = new("coal", "Coal", "API2 and API4");

    [Fact]
    public async Task ListAsync_ReturnsEveryDesk()
    {
        var sut = new DeskService(new FakeDeskRepository().WithDesk(NaturalGas).WithDesk(Coal));

        var desks = await sut.ListAsync(CancellationToken.None);

        desks.Select(d => d.Key).Should().Equal("coal", "natural-gas");
    }

    [Fact]
    public async Task ForUserAsync_RegularUserGetsOnlyTheirMemberships()
    {
        var repository = new FakeDeskRepository()
            .WithDesk(NaturalGas, "sean.hays@braemar.com")
            .WithDesk(Coal, "another.user@braemar.com");
        var sut = new DeskService(repository);

        var desks = await sut.ForUserAsync(
            new CurrentUser("sean.hays@braemar.com", "Sean", false),
            CancellationToken.None);

        desks.Select(desk => desk.Key).Should().Equal("natural-gas");
    }

    [Fact]
    public async Task ForUserAsync_AdminGetsEveryDesk()
    {
        var repository = new FakeDeskRepository().WithDesk(NaturalGas).WithDesk(Coal);
        var sut = new DeskService(repository);

        var desks = await sut.ForUserAsync(
            new CurrentUser("admin@braemar.com", "Admin", true),
            CancellationToken.None);

        desks.Select(desk => desk.Key).Should().Equal("coal", "natural-gas");
    }

    [Fact]
    public async Task CanAccessAsync_RequiresMembershipForRegularUsers()
    {
        var repository = new FakeDeskRepository()
            .WithDesk(NaturalGas, "sean.hays@braemar.com")
            .WithDesk(Coal, "another.user@braemar.com");
        var sut = new DeskService(repository);
        var user = new CurrentUser("sean.hays@braemar.com", "Sean", false);

        (await sut.CanAccessAsync("natural-gas", user, CancellationToken.None)).Should().BeTrue();
        (await sut.CanAccessAsync("coal", user, CancellationToken.None)).Should().BeFalse();
    }

    [Fact]
    public async Task FindAsync_KnownKey_ReturnsTheDeskWithItsMembers()
    {
        var sut = new DeskService(new FakeDeskRepository().WithDesk(NaturalGas, "sean.hays@braemar.com"));

        var found = await sut.FindAsync("natural-gas", CancellationToken.None);

        found!.Desk.Should().Be(NaturalGas);
        found.Members.Should().Equal("sean.hays@braemar.com");
    }

    [Fact]
    public async Task FindAsync_UnknownKey_ReturnsNull()
    {
        var sut = new DeskService(new FakeDeskRepository());

        (await sut.FindAsync("power", CancellationToken.None)).Should().BeNull();
    }

    [Fact]
    public async Task SaveAsync_StoresEmailsLowerCasedTrimmedAndWithoutRepeats()
    {
        var repository = new FakeDeskRepository();
        var sut = new DeskService(repository);

        var saved = await sut.SaveAsync("natural-gas", "Natural Gas", "TTF, NBP and JKM",
            new[] { " Sean.Hays@Braemar.com ", "marc.jarvis@braemar.com", "sean.hays@braemar.com" }, CancellationToken.None);

        saved.Members.Should().Equal("sean.hays@braemar.com", "marc.jarvis@braemar.com");
        (await repository.MembersAsync("natural-gas", CancellationToken.None))
            .Should().Equal("sean.hays@braemar.com", "marc.jarvis@braemar.com");
    }

    [Fact]
    public async Task SaveAsync_InvalidKey_RefusesAndNamesTheKey()
    {
        var sut = new DeskService(new FakeDeskRepository());

        var act = () => sut.SaveAsync("Natural Gas", "Natural Gas", "", Array.Empty<string>(), CancellationToken.None);

        await act.Should().ThrowAsync<DeskValidationException>().WithMessage("*\"Natural Gas\"*");
    }

    [Fact]
    public async Task SaveAsync_BlankName_Refuses()
    {
        var sut = new DeskService(new FakeDeskRepository());

        var act = () => sut.SaveAsync("coal", "   ", "", Array.Empty<string>(), CancellationToken.None);

        await act.Should().ThrowAsync<DeskValidationException>().WithMessage("*name*");
    }

    [Fact]
    public async Task SaveAsync_InvalidEmail_RefusesAndNamesIt()
    {
        var sut = new DeskService(new FakeDeskRepository());

        var act = () => sut.SaveAsync("coal", "Coal", "", new[] { "sean.hays" }, CancellationToken.None);

        await act.Should().ThrowAsync<DeskValidationException>().WithMessage("*\"sean.hays\"*");
    }
}
