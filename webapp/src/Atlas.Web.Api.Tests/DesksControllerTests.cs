using Atlas.Web.Api.Authentication;
using Atlas.Web.Api.Controllers;
using Atlas.Web.Api.Models.Inbound;
using Atlas.Web.Api.Models.Outbound;
using Atlas.Web.Core.Application;
using Atlas.Web.Core.Application.Models;
using Atlas.Web.Core.Application.Ports;
using Atlas.Web.Core.Domain.Models;
using FluentAssertions;
using Microsoft.AspNetCore.Authorization;
using Microsoft.AspNetCore.Http;
using Microsoft.AspNetCore.Mvc;
using Moq;

namespace Atlas.Web.Api.Tests;

public class DesksControllerTests
{
    private static readonly Desk NaturalGas = new("natural-gas", "Natural Gas", "TTF, NBP and JKM");
    private static readonly Desk Coal = new("coal", "Coal", "API2 and API4");
    private static readonly string[] Sean = { "sean.hays@braemar.com" };

    private static (DesksController Controller, Mock<IDeskService> Service) Controller(CurrentUser? user = null)
    {
        user ??= new CurrentUser("admin@braemar.com", "Atlas Admin", true);
        var service = new Mock<IDeskService>();
        service.Setup(s => s.ListAsync(It.IsAny<CancellationToken>())).ReturnsAsync(new[] { Coal, NaturalGas });
        service.Setup(s => s.ForUserAsync(It.IsAny<CurrentUser>(), It.IsAny<CancellationToken>()))
            .ReturnsAsync((CurrentUser current, CancellationToken _) => current.IsAdmin ? new[] { Coal, NaturalGas } : new[] { NaturalGas });
        service.Setup(s => s.FindAsync("natural-gas", It.IsAny<CancellationToken>())).ReturnsAsync(new DeskWithMembers(NaturalGas, Sean));
        service.Setup(s => s.FindAsync(It.IsNotIn("natural-gas"), It.IsAny<CancellationToken>())).ReturnsAsync((DeskWithMembers?)null);
        service.Setup(s => s.SaveAsync(It.IsAny<string>(), It.IsAny<string>(), It.IsAny<string>(), It.IsAny<IReadOnlyList<string>>(), It.IsAny<CancellationToken>()))
            .ReturnsAsync((string key, string name, string description, IReadOnlyList<string> members, CancellationToken _) =>
                new DeskWithMembers(new Desk(key, name, description), members));
        var currentUser = new Mock<ICurrentUser>();
        currentUser.Setup(s => s.Get()).Returns(user);
        var controller = new DesksController(service.Object, currentUser.Object)
        {
            ControllerContext = new ControllerContext { HttpContext = new DefaultHttpContext() },
        };
        return (controller, service);
    }

    [Fact]
    public async Task GetAll_ReturnsEveryDeskWithoutMembers()
    {
        var (controller, _) = Controller();

        var result = await controller.GetAll(CancellationToken.None);

        result.Value.Should().Equal(new DeskDto("coal", "Coal", "API2 and API4"), new DeskDto("natural-gas", "Natural Gas", "TTF, NBP and JKM"));
    }

    [Fact]
    public async Task GetAll_ReturnsOnlyTheSignedInUsersMemberships()
    {
        var (controller, _) = Controller(new CurrentUser("sean.hays@braemar.com", "Sean", false));

        var result = await controller.GetAll(CancellationToken.None);

        result.Value.Should().Equal(new DeskDto("natural-gas", "Natural Gas", "TTF, NBP and JKM"));
    }

    [Fact]
    public async Task Get_KnownKey_ReturnsTheDeskWithItsMembers()
    {
        var (controller, _) = Controller();

        var result = await controller.Get("natural-gas", CancellationToken.None);

        result.Value.Should().BeEquivalentTo(new DeskDetailDto("natural-gas", "Natural Gas", "TTF, NBP and JKM", Sean));
    }

    [Fact]
    public async Task Get_RegularUser_CannotReadDeskMemberEmails()
    {
        var (controller, service) = Controller(new CurrentUser("sean.hays@braemar.com", "Sean", false));

        var result = await controller.Get("natural-gas", CancellationToken.None);

        result.Result.Should().BeOfType<ForbidResult>();
        service.Verify(s => s.FindAsync(It.IsAny<string>(), It.IsAny<CancellationToken>()), Times.Never);
    }
    [Fact]
    public async Task Get_UnknownKey_Is404NamingTheKey()
    {
        var (controller, _) = Controller();

        var result = await controller.Get("power", CancellationToken.None);

        result.Result.Should().BeOfType<NotFoundObjectResult>().Which.Value.Should().Be("There is no desk called 'power'.");
    }

    [Fact]
    public async Task Create_NewKey_SavesAndAnswers201PointingAtTheDesk()
    {
        var (controller, service) = Controller();

        var result = await controller.Create(new DeskDetailDto("power", "Power", "Baseload and peak", Sean), CancellationToken.None);

        var created = result.Result.Should().BeOfType<CreatedAtActionResult>().Which;
        created.ActionName.Should().Be(nameof(DesksController.Get));
        created.RouteValues!["key"].Should().Be("power");
        created.Value.Should().BeEquivalentTo(new DeskDetailDto("power", "Power", "Baseload and peak", Sean));
        service.Verify(s => s.SaveAsync("power", "Power", "Baseload and peak", Sean, It.IsAny<CancellationToken>()));
    }

    [Fact]
    public async Task Create_ExistingKey_Is409()
    {
        var (controller, service) = Controller();

        var result = await controller.Create(new DeskDetailDto("natural-gas", "Gas", "", Sean), CancellationToken.None);

        result.Result.Should().BeOfType<ConflictObjectResult>();
        service.Verify(s => s.SaveAsync(It.IsAny<string>(), It.IsAny<string>(), It.IsAny<string>(), It.IsAny<IReadOnlyList<string>>(), It.IsAny<CancellationToken>()), Times.Never);
    }

    [Fact]
    public async Task Create_InvalidInput_Is400WithTheReason()
    {
        var (controller, service) = Controller();
        service.Setup(s => s.SaveAsync("Bad Key", It.IsAny<string>(), It.IsAny<string>(), It.IsAny<IReadOnlyList<string>>(), It.IsAny<CancellationToken>()))
            .ThrowsAsync(new DeskValidationException("\"Bad Key\" is not a valid desk key."));

        var result = await controller.Create(new DeskDetailDto("Bad Key", "Bad", "", Sean), CancellationToken.None);

        result.Result.Should().BeOfType<BadRequestObjectResult>().Which.Value.Should().Be("\"Bad Key\" is not a valid desk key.");
    }

    [Fact]
    public async Task Update_KnownKey_SavesAndReturnsTheDesk()
    {
        var (controller, service) = Controller();

        var result = await controller.Update("natural-gas", new DeskWriteDto("Nat Gas", "Gas", Sean), CancellationToken.None);

        result.Value.Should().BeEquivalentTo(new DeskDetailDto("natural-gas", "Nat Gas", "Gas", Sean));
        service.Verify(s => s.SaveAsync("natural-gas", "Nat Gas", "Gas", Sean, It.IsAny<CancellationToken>()));
    }

    [Fact]
    public async Task Update_UnknownKey_Is404()
    {
        var (controller, _) = Controller();

        var result = await controller.Update("power", new DeskWriteDto("Power", "", Sean), CancellationToken.None);

        result.Result.Should().BeOfType<NotFoundObjectResult>();
    }

    [Fact]
    public void Writes_NeedTheAdminPolicy_AndNothingHereIsOpenToAnonymous()
    {
        var create = typeof(DesksController).GetMethod(nameof(DesksController.Create))!;
        var update = typeof(DesksController).GetMethod(nameof(DesksController.Update))!;

        create.GetCustomAttributes(typeof(AuthorizeAttribute), false).Cast<AuthorizeAttribute>().Single().Policy.Should().Be(AuthPolicies.Admin);
        update.GetCustomAttributes(typeof(AuthorizeAttribute), false).Cast<AuthorizeAttribute>().Single().Policy.Should().Be(AuthPolicies.Admin);
        typeof(DesksController).GetMethods().Concat(typeof(MeController).GetMethods())
            .Should().OnlyContain(m => m.GetCustomAttributes(typeof(AllowAnonymousAttribute), false).Length == 0);
    }
}
