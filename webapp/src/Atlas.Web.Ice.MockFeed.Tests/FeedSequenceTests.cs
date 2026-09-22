using Atlas.Web.Ice.MockFeed.Capture;
using Atlas.Web.Ice.MockFeed.Configuration;
using Atlas.Web.Ice.MockFeed.Ticks;
using FluentAssertions;

namespace Atlas.Web.Ice.MockFeed.Tests;

public class FeedSequenceTests
{
    private static CapturedFrame Frame(string symbol, params (short Id, string Value)[] fields) =>
        new(symbol, fields.Select(f => new CapturedField(f.Id, f.Value)).ToArray());

    private static readonly IReadOnlyList<CapturedFrame> Capture =
    [
        Frame("A", (1, "72"), (19, "64.665")),
        Frame("B", (1, "73"), (19, "31.200")),
        Frame("A", (19, "64.700"))
    ];

    private static FeedSequence Build(bool loop = false, double jitter = 0.15) =>
        new(Capture,
            new MockFeedOptions { Loop = loop, Jitter = jitter },
            new Random(20260917));

    [Fact]
    public void OpeningFrames_AreRefreshType()
    {
        Build().OpeningFrames.Should().OnlyContain(f => f.StartsWith(@"[""refresh"""));
    }

    [Fact]
    public void OpeningFrames_AreOnePerSymbol()
    {
        Build().OpeningFrames.Should().HaveCount(2);
    }

    [Fact]
    public void OpeningFrames_CarryMergedFieldSetForSymbol()
    {
        // Symbol A appears twice in the capture with field 19 updated; the
        // opening refresh must carry the union, at the latest value.
        var opening = Build().OpeningFrames.First(f => f.Contains(@"""A"""));

        opening.Should().Be(@"[""refresh"",""A"",[[1,""72""],[19,""64.700""]]]");
    }

    [Fact]
    public void NextUpdate_ReplaysCapturedFramesVerbatimInOrder()
    {
        var sequence = Build();

        sequence.NextUpdate().Should().Be(@"[""update"",""A"",[[1,""72""],[19,""64.665""]]]");
        sequence.NextUpdate().Should().Be(@"[""update"",""B"",[[1,""73""],[19,""31.200""]]]");
        sequence.NextUpdate().Should().Be(@"[""update"",""A"",[[19,""64.700""]]]");
    }

    [Fact]
    public void NextUpdate_WhenLooping_RepeatsCaptureVerbatim()
    {
        var sequence = Build(loop: true);

        for (var i = 0; i < Capture.Count; i++)
        {
            sequence.NextUpdate();
        }

        sequence.NextUpdate().Should().Be(@"[""update"",""A"",[[1,""72""],[19,""64.665""]]]");
    }

    [Fact]
    public void NextUpdate_AfterCapture_KeepsFieldIdsButMovesPrices()
    {
        var sequence = Build();
        for (var i = 0; i < Capture.Count; i++)
        {
            sequence.NextUpdate();
        }

        var generated = sequence.NextUpdate();

        generated.Should().StartWith(@"[""update"",""A"",[[1,""72""],[19,""");
        generated.Should().NotContain(@"[19,""64.665""]");
    }

    [Fact]
    public void NextUpdate_AfterCapture_CyclesThroughEverySymbol()
    {
        var sequence = Build();
        for (var i = 0; i < Capture.Count; i++)
        {
            sequence.NextUpdate();
        }

        var generated = new[] { sequence.NextUpdate(), sequence.NextUpdate() };

        generated.Should().Contain(f => f.Contains(@"""A"""));
        generated.Should().Contain(f => f.Contains(@"""B"""));
    }

    [Fact]
    public void NextUpdate_WithJitterDisabled_StillProducesValidFrames()
    {
        var sequence = Build(jitter: 0);
        for (var i = 0; i < Capture.Count; i++)
        {
            sequence.NextUpdate();
        }

        sequence.NextUpdate().Should().Be(@"[""update"",""A"",[[1,""72""],[19,""64.700""]]]");
    }

    [Fact]
    public void Rewind_RestartsTheCaptureFromTheBeginning()
    {
        var sequence = Build();
        sequence.NextUpdate();
        sequence.NextUpdate();

        sequence.Rewind();

        sequence.NextUpdate().Should().Be(@"[""update"",""A"",[[1,""72""],[19,""64.665""]]]");
    }

    [Fact]
    public void ResetFrame_SignalsRecordResetForTheSymbol()
    {
        // Field 416 = "0" is what IceInterpreter maps to DataStatus.Reset.
        Build().ResetFrame("A").Should().Be(@"[""update"",""A"",[[416,""0""]]]");
    }

    [Fact]
    public void Symbols_AreTakenFromTheCapture()
    {
        Build().Symbols.Should().Equal("A", "B");
    }

    [Fact]
    public void EmptyCapture_YieldsNoOpeningFramesAndNoUpdates()
    {
        var sequence = new FeedSequence([], new MockFeedOptions(), new Random(1));

        sequence.OpeningFrames.Should().BeEmpty();
        sequence.NextUpdate().Should().BeNull();
    }
}
