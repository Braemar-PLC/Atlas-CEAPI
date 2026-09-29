using System.Security.Claims;
using Atlas.Web.Api.Authentication;
using Atlas.Web.Api.Configuration;
using FluentAssertions;
using Microsoft.AspNetCore.Http;
using Microsoft.Extensions.Options;

namespace Atlas.Web.Api.Tests.Authentication;

public class HttpContextCurrentUserTests
{
    private static HttpContextCurrentUser CurrentUser(HttpContext? context) =>
        new(new HttpContextAccessor { HttpContext = context }, Options.Create(new AuthOptions { AdminRole = "Atlas.Admin" }));

    private static DefaultHttpContext ContextFor(ClaimsIdentity identity) => new() { User = new ClaimsPrincipal(identity) };

    [Fact]
    public void Get_AuthenticatedUser_LowerCasesTheEmailAndReadsNameAndRole()
    {
        var identity = new ClaimsIdentity(
            new[] { new Claim(ClaimTypes.Name, "Sean.Hays@Braemar.com"), new Claim("name", "Sean Hays"), new Claim(ClaimTypes.Role, "Atlas.Admin") },
            "aad", ClaimTypes.Name, ClaimTypes.Role);

        var user = CurrentUser(ContextFor(identity)).Get();

        user.Should().Be(new Core.Domain.Models.CurrentUser("sean.hays@braemar.com", "Sean Hays", IsAdmin: true));
    }

    [Fact]
    public void Get_NoDisplayNameClaim_UsesTheEmailAsTheName()
    {
        var identity = new ClaimsIdentity(new[] { new Claim(ClaimTypes.Name, "marc.jarvis@braemar.com") }, "aad", ClaimTypes.Name, ClaimTypes.Role);

        var user = CurrentUser(ContextFor(identity)).Get();

        user!.Name.Should().Be("marc.jarvis@braemar.com");
        user.IsAdmin.Should().BeFalse();
    }

    [Fact]
    public void Get_NotAuthenticated_ReturnsNull()
    {
        CurrentUser(ContextFor(new ClaimsIdentity())).Get().Should().BeNull();
    }

    [Fact]
    public void Get_OutsideARequest_ReturnsNull()
    {
        CurrentUser(context: null).Get().Should().BeNull();
    }
}
