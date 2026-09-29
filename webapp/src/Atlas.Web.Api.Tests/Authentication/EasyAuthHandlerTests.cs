using System.Text.Encodings.Web;
using Atlas.Web.Api.Authentication;
using Atlas.Web.Api.Configuration;
using FluentAssertions;
using Microsoft.AspNetCore.Authentication;
using Microsoft.AspNetCore.Http;
using Microsoft.Extensions.Hosting;
using Microsoft.Extensions.Logging.Abstractions;
using Microsoft.Extensions.Options;
using Moq;

namespace Atlas.Web.Api.Tests.Authentication;

public class EasyAuthHandlerTests
{
    private static readonly AuthOptions WithStandIn = new()
    {
        AdminRole = "Atlas.Admin",
        DevelopmentUser = new DevelopmentUserOptions { Email = "dev@braemar.com", Name = "Dev User", Roles = { "Atlas.Admin" } },
    };

    private static async Task<AuthenticateResult> Authenticate(string environment, AuthOptions options, string? header)
    {
        var schemeOptions = new Mock<IOptionsMonitor<AuthenticationSchemeOptions>>();
        schemeOptions.Setup(o => o.Get(It.IsAny<string>())).Returns(new AuthenticationSchemeOptions());
        var host = new Mock<IHostEnvironment>();
        host.SetupGet(h => h.EnvironmentName).Returns(environment);

        var handler = new EasyAuthHandler(schemeOptions.Object, NullLoggerFactory.Instance, UrlEncoder.Default, Options.Create(options), host.Object);
        var context = new DefaultHttpContext();
        if (header is not null)
        {
            context.Request.Headers[EasyAuthDefaults.PrincipalHeader] = header;
        }
        await handler.InitializeAsync(new AuthenticationScheme(EasyAuthDefaults.Scheme, null, typeof(EasyAuthHandler)), context);
        return await handler.AuthenticateAsync();
    }

    [Fact]
    public async Task Authenticate_HeaderPresent_SucceedsWithThatUser()
    {
        var result = await Authenticate(Environments.Production, WithStandIn, EasyAuthPrincipalParserTests.Encode(EasyAuthPrincipalParserTests.SeanPayload));

        result.Succeeded.Should().BeTrue();
        result.Principal!.Identity!.Name.Should().Be("Sean.Hays@braemar.com");
        result.Principal.IsInRole("Atlas.Admin").Should().BeTrue();
    }

    [Fact]
    public async Task Authenticate_NoHeaderInDevelopment_SucceedsWithTheStandInUser()
    {
        var result = await Authenticate(Environments.Development, WithStandIn, header: null);

        result.Succeeded.Should().BeTrue();
        result.Principal!.Identity!.Name.Should().Be("dev@braemar.com");
        result.Principal.Identity.AuthenticationType.Should().Be(EasyAuthDefaults.DevelopmentAuthenticationType);
        result.Principal.FindFirst(EasyAuthDefaults.DisplayNameClaim)!.Value.Should().Be("Dev User");
        result.Principal.IsInRole("Atlas.Admin").Should().BeTrue();
    }

    [Fact]
    public async Task Authenticate_NoHeaderInProduction_IsNobodyEvenWithAStandInConfigured()
    {
        var result = await Authenticate(Environments.Production, WithStandIn, header: null);

        result.None.Should().BeTrue();
    }

    [Fact]
    public async Task Authenticate_NoHeaderInDevelopmentWithoutAStandIn_IsNobody()
    {
        var result = await Authenticate(Environments.Development, new AuthOptions { AdminRole = "Atlas.Admin" }, header: null);

        result.None.Should().BeTrue();
    }

    [Fact]
    public async Task Authenticate_MalformedHeader_Fails()
    {
        var result = await Authenticate(Environments.Production, WithStandIn, "not base64 json");

        result.Failure.Should().NotBeNull();
        result.Failure!.Message.Should().Contain(EasyAuthDefaults.PrincipalHeader);
    }
}
