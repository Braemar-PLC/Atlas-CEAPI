using Atlas.Web.Ice.MockFeed.Capture;
using FluentAssertions;

namespace Atlas.Web.Ice.MockFeed.Tests;

public class CaptureLoaderTests
{
    private const string Line =
        @"[""update"",""TFM 26J-ICN"",[[1,""72""],[13,""TFM 26J-ICN""],[19,""64.665""]]]";

    [Fact]
    public void Load_ReadsSymbol()
    {
        var frames = CaptureLoader.Load([Line]);

        frames.Should().ContainSingle()
            .Which.Symbol.Should().Be("TFM 26J-ICN");
    }

    [Fact]
    public void Load_ReadsFieldIdsAndValuesInOrder()
    {
        var frames = CaptureLoader.Load([Line]);

        frames[0].Fields.Should().Equal(
            new CapturedField(1, "72"),
            new CapturedField(13, "TFM 26J-ICN"),
            new CapturedField(19, "64.665"));
    }

    [Fact]
    public void Load_SkipsBlankLines()
    {
        var frames = CaptureLoader.Load([Line, "", "   ", Line]);

        frames.Should().HaveCount(2);
    }

    [Fact]
    public void Load_SkipsUnparseableLines()
    {
        var frames = CaptureLoader.Load([Line, "not json", @"[""status"",""hello""]"]);

        frames.Should().ContainSingle();
    }

    [Fact]
    public void Load_PreservesValuesContainingCommasAndEquals()
    {
        // Field 330 in the real capture is a compound string full of separators.
        var line = @"[""update"",""SYM"",[[330,""sequenceNumber=0, lineId=0, flags=1""]]]";

        var frames = CaptureLoader.Load([line]);

        frames[0].Fields[0].Value.Should().Be("sequenceNumber=0, lineId=0, flags=1");
    }

    [Fact]
    public void Load_RoundTripsThroughFrameBuilder()
    {
        var frames = CaptureLoader.Load([Line]);

        var rebuilt = Wire.FeedFrameBuilder.Update(frames[0].Symbol, frames[0].Fields);

        rebuilt.Should().Be(Line);
    }
}
