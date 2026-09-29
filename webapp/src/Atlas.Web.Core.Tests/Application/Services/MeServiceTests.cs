using Atlas.Web.Core.Application.Services;
using Atlas.Web.Core.Domain.Models;
using Atlas.Web.Core.Tests.Fakes;
using FluentAssertions;

namespace Atlas.Web.Core.Tests.Application.Services;

public class MeServiceTests
{
    private static readonly Desk NaturalGas = new("natural-gas", "Natural Gas", "TTF, NBP and JKM");
    private static readonly Desk Coal = new("coal", "Coal", "API2 and API4");
    private static readonly CurrentUser Sean = new("sean.hays@braemar.com", "Sean Hays", IsAdmin: false);

    [Fact]
    public async Task GetAsync_NobodySignedIn_ReturnsNull()
    {
        var sut = new MeService(new FakeCurrentUser(null), new FakeDeskRepository().WithDesk(NaturalGas));

        (await sut.GetAsync(CancellationToken.None)).Should().BeNull();
    }

    [Fact]
    public async Task GetAsync_MemberOfExactlyOneDesk_ThatDeskIsHome()
    {
        var repository = new FakeDeskRepository().WithDesk(NaturalGas, Sean.Email).WithDesk(Coal);
        var sut = new MeService(new FakeCurrentUser(Sean), repository);

        var me = await sut.GetAsync(CancellationToken.None);

        me!.HomeDeskKey.Should().Be("natural-gas");
        me.Desks.Should().Equal(NaturalGas);
    }

    [Fact]
    public async Task GetAsync_MemberOfTwoDesks_HasNoHome()
    {
        var repository = new FakeDeskRepository().WithDesk(NaturalGas, Sean.Email).WithDesk(Coal, Sean.Email);
        var sut = new MeService(new FakeCurrentUser(Sean), repository);

        var me = await sut.GetAsync(CancellationToken.None);

        me!.HomeDeskKey.Should().BeNull();
        me.Desks.Should().HaveCount(2);
    }

    [Fact]
    public async Task GetAsync_MemberOfNoDesk_HasNoHome()
    {
        var sut = new MeService(new FakeCurrentUser(Sean), new FakeDeskRepository().WithDesk(NaturalGas));

        var me = await sut.GetAsync(CancellationToken.None);

        me!.HomeDeskKey.Should().BeNull();
        me.Desks.Should().BeEmpty();
    }

    [Fact]
    public async Task GetAsync_CarriesNameEmailAndAdminThrough()
    {
        var admin = Sean with { IsAdmin = true };
        var sut = new MeService(new FakeCurrentUser(admin), new FakeDeskRepository());

        var me = await sut.GetAsync(CancellationToken.None);

        me!.Name.Should().Be("Sean Hays");
        me.Email.Should().Be("sean.hays@braemar.com");
        me.IsAdmin.Should().BeTrue();
    }
}
