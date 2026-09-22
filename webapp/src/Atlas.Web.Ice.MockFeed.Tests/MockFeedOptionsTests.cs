using Atlas.Web.Ice.MockFeed.Configuration;
using FluentAssertions;

namespace Atlas.Web.Ice.MockFeed.Tests;

public class MockFeedOptionsTests
{
    [Fact]
    public void Parse_NoArguments_UsesMockPortNotRelayPort()
    {
        // 9002 is the real relay. Defaulting there would let a forgotten mock
        // silently shadow it.
        MockFeedOptions.Parse([]).Port.Should().Be(9102);
    }

    [Fact]
    public void Parse_NoArguments_UsesDocumentedDefaults()
    {
        var options = MockFeedOptions.Parse([]);

        options.Interval.Should().Be(TimeSpan.FromMilliseconds(250));
        options.Loop.Should().BeFalse();
        options.Jitter.Should().Be(0.15);
        options.CapturePath.Should().BeNull();
    }

    [Fact]
    public void Parse_Port_OverridesDefault()
    {
        MockFeedOptions.Parse(["--port", "9002"]).Port.Should().Be(9002);
    }

    [Fact]
    public void Parse_Interval_IsReadAsMilliseconds()
    {
        MockFeedOptions.Parse(["--interval", "40"]).Interval
            .Should().Be(TimeSpan.FromMilliseconds(40));
    }

    [Fact]
    public void Parse_Loop_IsAFlagNeedingNoValue()
    {
        MockFeedOptions.Parse(["--loop"]).Loop.Should().BeTrue();
    }

    [Fact]
    public void Parse_Jitter_OverridesDefault()
    {
        MockFeedOptions.Parse(["--jitter", "0"]).Jitter.Should().Be(0);
    }

    [Fact]
    public void Parse_Capture_OverridesDefault()
    {
        MockFeedOptions.Parse(["--capture", @"c:\feed.json"]).CapturePath
            .Should().Be(@"c:\feed.json");
    }

    [Fact]
    public void Parse_CombinesArguments()
    {
        var options = MockFeedOptions.Parse(["--port", "9002", "--loop", "--jitter", "1.5"]);

        options.Port.Should().Be(9002);
        options.Loop.Should().BeTrue();
        options.Jitter.Should().Be(1.5);
    }

    [Fact]
    public void Parse_UnknownArgument_Throws()
    {
        var parse = () => MockFeedOptions.Parse(["--speed", "fast"]);

        parse.Should().Throw<ArgumentException>().WithMessage("*--speed*");
    }

    [Fact]
    public void Parse_MissingValue_Throws()
    {
        var parse = () => MockFeedOptions.Parse(["--port"]);

        parse.Should().Throw<ArgumentException>().WithMessage("*--port*");
    }

    [Theory]
    [InlineData("0")]
    [InlineData("70000")]
    [InlineData("not-a-port")]
    public void Parse_InvalidPort_Throws(string port)
    {
        var parse = () => MockFeedOptions.Parse(["--port", port]);

        parse.Should().Throw<ArgumentException>();
    }

    [Fact]
    public void Parse_NegativeJitter_Throws()
    {
        var parse = () => MockFeedOptions.Parse(["--jitter", "-1"]);

        parse.Should().Throw<ArgumentException>();
    }

    [Fact]
    public void Parse_JitterUsesInvariantCulture()
    {
        // en-GB is the machine default; "0.5" must not be read as 5.
        MockFeedOptions.Parse(["--jitter", "0.5"]).Jitter.Should().Be(0.5);
    }
}
