using Atlas.Web.Ice.Configuration;
using FluentAssertions;
using Microsoft.Extensions.Configuration;

namespace Atlas.Web.Api.Tests;

/// <summary>
/// The section name on the options type and the key in appsettings.json are
/// two halves of one contract, and nothing previously asserted they agree. A
/// mismatch binds nothing, fails the startup validators and stops the host
/// booting, so it is checked here rather than discovered at run time.
/// </summary>
public class IceOptionsConfigurationTests
{
    private static IConfigurationRoot ApiConfiguration()
    {
        return new ConfigurationBuilder()
            .SetBasePath(AppContext.BaseDirectory)
            .AddJsonFile("appsettings.json", optional: false)
            .Build();
    }

    [Fact]
    public void Section_ExistsInAppSettings()
    {
        ApiConfiguration().GetSection(IceOptions.Section).Exists()
            .Should().BeTrue(
                "IceOptions.Section is '{0}', which must match a key in appsettings.json",
                IceOptions.Section);
    }

    [Fact]
    public void Section_BindsEndpoint()
    {
        var options = new IceOptions();

        ApiConfiguration().GetSection(IceOptions.Section).Bind(options);

        options.Endpoint.Should().NotBeNullOrWhiteSpace();
    }

    [Fact]
    public void Section_BindsPort()
    {
        var options = new IceOptions();

        ApiConfiguration().GetSection(IceOptions.Section).Bind(options);

        options.Port.Should().BeGreaterThan(0);
    }

    [Fact]
    public void BoundOptions_SatisfyTheStartupValidators()
    {
        // Mirrors the two Validate clauses in Program.cs ConfigureOptions.
        var options = new IceOptions();
        ApiConfiguration().GetSection(IceOptions.Section).Bind(options);

        string.IsNullOrEmpty(options.Endpoint).Should().BeFalse();
        (options.Port > 0).Should().BeTrue();
    }

    [Fact]
    public void WebSocketUri_IsBuiltFromBoundConfiguration()
    {
        var options = new IceOptions();
        ApiConfiguration().GetSection(IceOptions.Section).Bind(options);

        options.WebSocketUri.Scheme.Should().Be("ws");
        options.WebSocketUri.Port.Should().Be(options.Port);
    }

    [Fact]
    public void PortOverride_UsesTheSameKeyPrefixAsTheLaunchProfile()
    {
        // The mock-feed launch profile switches feeds with an environment
        // variable, whose prefix is the section name.
        var configuration = new ConfigurationBuilder()
            .SetBasePath(AppContext.BaseDirectory)
            .AddJsonFile("appsettings.json", optional: false)
            .AddInMemoryCollection(new Dictionary<string, string?>
            {
                [$"{IceOptions.Section}:Port"] = "9102"
            })
            .Build();

        var options = new IceOptions();
        configuration.GetSection(IceOptions.Section).Bind(options);

        options.Port.Should().Be(9102);
    }
}
