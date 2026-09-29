using Atlas.Web.Api.Controllers;
using Atlas.Web.Api.Models.Outbound;
using Atlas.Web.Core.Application.Models;
using Atlas.Web.Core.Application.Ports;
using Atlas.Web.Core.Domain.Models;
using FluentAssertions;
using Microsoft.AspNetCore.Http;
using Microsoft.AspNetCore.Mvc;
using Moq;

namespace Atlas.Web.Api.Tests;

public class MeControllerTests
{
    private static readonly Desk NaturalGas = new("natural-gas", "Natural Gas", "TTF, NBP and JKM");

    private static MeController Controller(Me? me)
    {
        var service = new Mock<IMeService>();
        service.Setup(s => s.GetAsync(It.IsAny<CancellationToken>())).ReturnsAsync(me);
        return new MeController(service.Object)
        {
            ControllerContext = new ControllerContext { HttpContext = new DefaultHttpContext() },
        };
    }

    [Fact]
    public async Task Get_SignedIn_ReturnsWhoTheyAreTheirDesksAndWhereTheyLand()
    {
        var me = new Me("Sean Hays", "sean.hays@braemar.com", IsAdmin: true, new[] { NaturalGas }, "natural-gas");

        var result = await Controller(me).Get(CancellationToken.None);

        result.Value.Should().BeEquivalentTo(new MeDto(
            "Sean Hays", "sean.hays@braemar.com", true,
            new[] { new DeskDto("natural-gas", "Natural Gas", "TTF, NBP and JKM") }, "natural-gas"));
    }

    [Fact]
    public async Task Get_NobodySignedIn_Is401()
    {
        var result = await Controller(me: null).Get(CancellationToken.None);

        result.Result.Should().BeOfType<UnauthorizedResult>();
    }
}
