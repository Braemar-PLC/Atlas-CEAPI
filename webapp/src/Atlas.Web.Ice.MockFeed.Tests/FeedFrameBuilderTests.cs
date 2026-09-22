using Atlas.Web.Ice.MockFeed.Capture;
using Atlas.Web.Ice.MockFeed.Wire;
using FluentAssertions;

namespace Atlas.Web.Ice.MockFeed.Tests;

/// <summary>
/// The relay's wire format is reproduced here rather than reused, so that a
/// change in the consumer cannot silently change what the mock emits. Expected
/// strings are taken from MessageBuilder.java.
/// </summary>
public class FeedFrameBuilderTests
{
    private static CapturedField[] TwoFields =>
    [
        new CapturedField(1, "72"),
        new CapturedField(19, "64.665")
    ];

    [Fact]
    public void Refresh_ProducesRelayWireFormat()
    {
        var frame = FeedFrameBuilder.Refresh("TFM 26J-ICN", TwoFields);

        frame.Should().Be(@"[""refresh"",""TFM 26J-ICN"",[[1,""72""],[19,""64.665""]]]");
    }

    [Fact]
    public void Update_ProducesRelayWireFormat()
    {
        var frame = FeedFrameBuilder.Update("TFM 26J-ICN", TwoFields);

        frame.Should().Be(@"[""update"",""TFM 26J-ICN"",[[1,""72""],[19,""64.665""]]]");
    }

    [Fact]
    public void Update_NoFields_ProducesEmptyFieldArray()
    {
        var frame = FeedFrameBuilder.Update("SYM", []);

        frame.Should().Be(@"[""update"",""SYM"",[]]");
    }

    [Fact]
    public void Update_EscapesDoubleQuotesInValue()
    {
        var frame = FeedFrameBuilder.Update("SYM", [new CapturedField(17, "a\"b")]);

        frame.Should().Be(@"[""update"",""SYM"",[[17,""a\""b""]]]");
    }

    [Fact]
    public void Update_EscapesBackslashesInValue()
    {
        var frame = FeedFrameBuilder.Update("SYM", [new CapturedField(17, @"a\b")]);

        frame.Should().Be(@"[""update"",""SYM"",[[17,""a\\b""]]]");
    }

    [Fact]
    public void Update_EscapesSymbol()
    {
        var frame = FeedFrameBuilder.Update(@"A""B", []);

        frame.Should().Be(@"[""update"",""A\""B"",[]]");
    }

    [Fact]
    public void Update_EscapesBackslashBeforeQuote_MatchingRelayOrdering()
    {
        // MessageBuilder.java escapes backslash first, then quote. Escaping in
        // the other order would double-escape the introduced backslash.
        var frame = FeedFrameBuilder.Update("SYM", [new CapturedField(17, @"\""")]);

        frame.Should().Be(@"[""update"",""SYM"",[[17,""\\\""""]]]");
    }

    [Fact]
    public void Frames_ContainNoNewline_SoLineFramingHolds()
    {
        // IceReceiver splits the socket stream on '\n'; an embedded newline
        // would corrupt framing. The relay does not escape newlines either, so
        // this asserts the fields we emit never carry one.
        var frame = FeedFrameBuilder.Update("SYM", [new CapturedField(1, "72")]);

        frame.Should().NotContain("\n");
    }
}
