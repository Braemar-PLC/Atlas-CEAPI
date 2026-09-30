using Atlas.Web.Api.Controllers;
using Atlas.Web.Api.Models.Outbound;
using Atlas.Web.Core.Application.Ports;
using Atlas.Web.Core.Domain.Enumeration;
using Atlas.Web.Core.Domain.Models;
using FluentAssertions;
using Microsoft.AspNetCore.Mvc;
using Moq;

namespace Atlas.Web.Api.Tests;

public class OptionsControllerTests
{
    private static readonly OptionMatrix Ttf = new("TTF", new[]
    {
        new OptionMatrixExpiry(StripKind.Month, "Nov26", new DateOnly(2026, 10, 27), "TFM 26X-ICN", new decimal[] { 79, 80, 81 }),
        new OptionMatrixExpiry(StripKind.Quarter, "Q1 27", new DateOnly(2026, 12, 24), "TFMQ 27F-ICN", new decimal[] { 80 }),
    });

    private static readonly CurrentUser Admin = new("admin@braemar.com", "Atlas Admin", true);
    private static readonly CurrentUser Trader = new("trader@braemar.com", "Trader", false);

    private static OptionsController Controller(CurrentUser? user, bool memberOfCrossCommodities = false)
    {
        var matrices = new Mock<IOptionMatrixProvider>();
        matrices.Setup(m => m.Find("ttf")).Returns(Ttf);
        var desks = new Mock<IDeskService>();
        desks.Setup(d => d.CanAccessAsync("cross-commodities", It.IsAny<CurrentUser>(), It.IsAny<CancellationToken>()))
            .ReturnsAsync(memberOfCrossCommodities);
        var currentUser = new Mock<ICurrentUser>();
        currentUser.Setup(c => c.Get()).Returns(user);
        return new OptionsController(matrices.Object, desks.Object, currentUser.Object);
    }

    [Fact]
    public async Task Get_KnownProduct_ReturnsItsMatrix()
    {
        var result = await Controller(Admin).Get("ttf", CancellationToken.None);

        var dto = result.Value.Should().BeOfType<OptionMatrixDto>().Subject;
        dto.Product.Should().Be("TTF");
        // Structural: a record holding a list compares the list by reference, which is not what is being checked.
        dto.Expiries.Should().BeEquivalentTo(new[]
        {
            new OptionMatrixExpiryDto("Month", "Nov26", "2026-10-27", "TFM 26X-ICN", new decimal[] { 79, 80, 81 }),
            new OptionMatrixExpiryDto("Quarter", "Q1 27", "2026-12-24", "TFMQ 27F-ICN", new decimal[] { 80 }),
        }, options => options.WithStrictOrdering());
    }

    [Fact]
    public async Task Get_UnknownProduct_Is404()
    {
        var result = await Controller(Admin).Get("jkm", CancellationToken.None);

        result.Result.Should().BeOfType<NotFoundObjectResult>();
    }

    [Fact]
    public async Task Get_MemberOfCrossCommodities_ReturnsTheMatrix()
    {
        var result = await Controller(Trader, memberOfCrossCommodities: true).Get("ttf", CancellationToken.None);

        result.Value.Should().NotBeNull();
    }

    [Fact]
    public async Task Get_NotAMemberOfCrossCommodities_IsForbidden()
    {
        var result = await Controller(Trader).Get("ttf", CancellationToken.None);

        result.Result.Should().BeOfType<ForbidResult>();
    }

    [Fact]
    public async Task Get_NoSignedInUser_IsUnauthorized()
    {
        var result = await Controller(null).Get("ttf", CancellationToken.None);

        result.Result.Should().BeOfType<UnauthorizedResult>();
    }
}
