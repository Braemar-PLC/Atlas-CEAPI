using Atlas.Web.Data.Configuration;
using FluentAssertions;

namespace Atlas.Web.Data.Tests.Configuration;

public class DatabaseOptionsTests
{
    [Fact]
    public void Section_IsDatabase()
    {
        DatabaseOptions.Section.Should().Be("Database");
    }

    [Fact]
    public void FullPath_RelativePath_IsPlacedUnderTheContentRoot()
    {
        var options = new DatabaseOptions { Path = "App_Data/atlas.db" };

        options.FullPath(@"C:\atlas\api").Should().Be(System.IO.Path.Combine(@"C:\atlas\api", "App_Data/atlas.db"));
    }

    [Fact]
    public void FullPath_RootedPath_IsKeptAsIs()
    {
        var rooted = System.IO.Path.Combine(System.IO.Path.GetTempPath(), "atlas.db");
        var options = new DatabaseOptions { Path = rooted };

        options.FullPath(@"C:\atlas\api").Should().Be(rooted);
    }
}
