using Atlas.Web.Api.Configuration;
using FluentAssertions;
using Microsoft.Extensions.Configuration;

namespace Atlas.Web.Api.Tests;

/// <summary>The deployed configuration is under test: the real appsettings files are linked into this project.</summary>
public class AuthOptionsConfigurationTests
{
    private static AuthOptions Bind(bool development)
    {
        var builder = new ConfigurationBuilder()
            .SetBasePath(AppContext.BaseDirectory)
            .AddJsonFile("appsettings.json", optional: false);
        if (development)
        {
            builder.AddJsonFile("appsettings.Development.json", optional: false);
        }
        var options = new AuthOptions();
        builder.Build().GetSection(AuthOptions.Section).Bind(options);
        return options;
    }

    [Fact]
    public void Section_IsAuth()
    {
        AuthOptions.Section.Should().Be("Auth");
    }

    [Fact]
    public void AppSettings_NameTheAdminRole()
    {
        Bind(development: false).AdminRole.Should().Be("Atlas.Admin");
    }

    [Fact]
    public void AppSettings_HaveNoStandInUserOutsideDevelopment()
    {
        Bind(development: false).DevelopmentUser.Should().BeNull();
    }

    [Fact]
    public void DevelopmentSettings_DescribeTheStandInUserOnce()
    {
        var user = Bind(development: true).DevelopmentUser;

        user.Should().NotBeNull();
        user!.Email.Should().Be("sean.hays@braemar.com");
        user.Name.Should().Be("Sean Hays");
        // The binder appends to a list that already has items, so the default must start empty.
        user.Roles.Should().Equal("Atlas.Admin");
    }
}
