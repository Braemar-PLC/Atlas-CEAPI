using Atlas.Web.Api.Authentication;
using Atlas.Web.Api.Controllers;
using Atlas.Web.Core.Application.Ports;
using FluentAssertions;
using Microsoft.AspNetCore.Authentication;
using Microsoft.AspNetCore.Authorization;
using Microsoft.AspNetCore.Authorization.Infrastructure;
using Microsoft.Extensions.Configuration;
using Microsoft.Extensions.DependencyInjection;
using Microsoft.Extensions.Options;

namespace Atlas.Web.Api.Tests.Authentication;

public class AuthenticationSetupTests
{
    private static ServiceProvider Provider(string adminRole = "Atlas.Admin")
    {
        var configuration = new ConfigurationBuilder()
            .AddInMemoryCollection(new Dictionary<string, string?> { ["Auth:AdminRole"] = adminRole })
            .Build();
        var services = new ServiceCollection();
        services.AddLogging();
        services.AddEasyAuth(configuration);
        return services.BuildServiceProvider();
    }

    [Fact]
    public async Task AddEasyAuth_MakesEasyAuthTheDefaultScheme()
    {
        var schemes = Provider().GetRequiredService<IAuthenticationSchemeProvider>();

        (await schemes.GetDefaultAuthenticateSchemeAsync())!.Name.Should().Be(EasyAuthDefaults.Scheme);
    }

    [Fact]
    public void AddEasyAuth_RequiresASignedInUserByDefault()
    {
        var authorization = Provider().GetRequiredService<IOptions<AuthorizationOptions>>().Value;

        authorization.FallbackPolicy!.Requirements.Should().ContainItemsAssignableTo<DenyAnonymousAuthorizationRequirement>();
    }

    [Fact]
    public void AddEasyAuth_AdminPolicyRequiresTheConfiguredRole()
    {
        var authorization = Provider(adminRole: "Atlas.Boss").GetRequiredService<IOptions<AuthorizationOptions>>().Value;

        authorization.GetPolicy(AuthPolicies.Admin)!.Requirements.OfType<RolesAuthorizationRequirement>()
            .Single().AllowedRoles.Should().Equal("Atlas.Boss");
    }

    [Fact]
    public void AddEasyAuth_ProvidesTheCurrentUserPort()
    {
        using var scope = Provider().CreateScope();

        scope.ServiceProvider.GetRequiredService<ICurrentUser>().Should().BeOfType<HttpContextCurrentUser>();
    }

    [Fact]
    public void SymbolsEndpoint_StaysOpenToTheFeedRelayScript()
    {
        // CEAPI's run-live.ps1 asks for the symbol list without a browser or a sign-in.
        var symbols = typeof(ScreensController).GetMethod(nameof(ScreensController.Symbols))!;

        symbols.GetCustomAttributes(typeof(AllowAnonymousAttribute), inherit: false).Should().NotBeEmpty();
    }
}
