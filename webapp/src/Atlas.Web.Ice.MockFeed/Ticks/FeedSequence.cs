using Atlas.Web.Ice.Domain.Enumeration;
using Atlas.Web.Ice.MockFeed.Capture;
using Atlas.Web.Ice.MockFeed.Configuration;
using Atlas.Web.Ice.MockFeed.Wire;

namespace Atlas.Web.Ice.MockFeed.Ticks;

/// <summary>
/// Decides what the mock emits and in what order, mirroring the relay: a
/// refresh per symbol when a client attaches, then a stream of updates.
///
/// Updates replay the capture verbatim first. Once exhausted, the sequence
/// either loops the capture or — by default — keeps going by walking each
/// symbol's latest state forward. Generated frames reuse the captured field
/// ids and field sets; only values move.
/// </summary>
public sealed class FeedSequence
{
    private readonly IReadOnlyList<CapturedFrame> capture;
    private readonly MockFeedOptions options;
    private readonly Random random;
    private readonly List<CapturedFrame> latest;

    private int captureIndex;
    private int symbolIndex;

    public FeedSequence(
        IReadOnlyList<CapturedFrame> capture, MockFeedOptions options, Random random)
    {
        this.capture = capture;
        this.options = options;
        this.random = random;

        latest = SnapshotBuilder.Build(capture).ToList();
        OpeningFrames = latest
            .Select(snapshot => FeedFrameBuilder.Refresh(snapshot.Symbol, snapshot.Fields))
            .ToArray();
    }

    /// <summary>Refresh frames sent on client connect, and again on resync.</summary>
    public IReadOnlyList<string> OpeningFrames { get; }

    public IReadOnlyList<string> Symbols => latest.Select(snapshot => snapshot.Symbol).ToArray();

    /// <summary>
    /// The next update frame, or null when the capture is empty and there is
    /// nothing to generate from.
    /// </summary>
    public string? NextUpdate()
    {
        if (captureIndex >= capture.Count && options.Loop)
        {
            captureIndex = 0;
        }

        if (captureIndex < capture.Count)
        {
            var frame = capture[captureIndex];
            captureIndex++;
            return FeedFrameBuilder.Update(frame.Symbol, frame.Fields);
        }

        if (latest.Count == 0)
        {
            return null;
        }

        var index = symbolIndex;
        symbolIndex = (symbolIndex + 1) % latest.Count;

        var moved = PriceJitter.Apply(latest[index], options.Jitter, random);
        latest[index] = moved;

        return FeedFrameBuilder.Update(moved.Symbol, moved.Fields);
    }

    /// <summary>Restarts capture replay. Used when a client reattaches.</summary>
    public void Rewind()
    {
        captureIndex = 0;
    }

    /// <summary>
    /// A record-reset frame, which the consumer's interpreter maps to
    /// DataStatus.Reset.
    /// </summary>
    public string ResetFrame(string symbol)
    {
        return FeedFrameBuilder.Update(
            symbol, [new CapturedField((short)IceResponseMetaFieldIds.RecordReset, "0")]);
    }
}
