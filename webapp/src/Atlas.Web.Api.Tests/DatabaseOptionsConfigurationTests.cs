using Atlas.Web.Data.Configuration;
using FluentAssertions;
using Microsoft.Extensions.Configuration;

namespace Atlas.Web.Api.Tests;

/// <summary>The deployed configuration is under test: the real appsettings.json is linked into this project.</summary>
public class DatabaseOptionsConfigurationTests
{
    private static DatabaseOptions Bind()
    {
        var configuration = new ConfigurationBuilder()
            .SetBasePath(AppContext.BaseDirectory)
            .AddJsonFile("appsettings.json", optional: false)
            .Build();
        var options = new DatabaseOptions();
        configuration.GetSection(DatabaseOptions.Section).Bind(options);
        return options;
    }

    [Fact]
    public void AppSettings_PutTheDatabaseUnderAppData()
    {
        Bind().Path.Should().Be("App_Data/atlas.db");
    }

    [Fact]
    public void PathOverride_IsReadFromTheEnvironmentShapedKey()
    {
        var configuration = new ConfigurationBuilder()
            .AddInMemoryCollection(new Dictionary<string, string?> { [$"{DatabaseOptions.Section}:Path"] = "/home/data/atlas.db" })
            .Build();
        var options = new DatabaseOptions();
        configuration.GetSection(DatabaseOptions.Section).Bind(options);

        options.FullPath(@"C:\anywhere").Should().Be("/home/data/atlas.db");
    }
}
