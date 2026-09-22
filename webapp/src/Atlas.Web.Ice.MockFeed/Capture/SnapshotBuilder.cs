namespace Atlas.Web.Ice.MockFeed.Capture;

/// <summary>
/// Derives a per-symbol snapshot from captured frames.
///
/// The relay sends a full field set as a "refresh" when ICE first responds to a
/// subscription, then deltas. Captures recorded mid-session contain updates
/// only, so the snapshot is reconstructed as the union of every field seen for
/// a symbol, carrying the most recent value of each.
/// </summary>
public static class SnapshotBuilder
{
    public static IReadOnlyList<CapturedFrame> Build(IReadOnlyList<CapturedFrame> frames)
    {
        var symbolOrder = new List<string>();
        var fieldOrder = new Dictionary<string, List<short>>();
        var values = new Dictionary<string, Dictionary<short, string>>();

        foreach (var frame in frames)
        {
            if (!values.ContainsKey(frame.Symbol))
            {
                symbolOrder.Add(frame.Symbol);
                fieldOrder[frame.Symbol] = [];
                values[frame.Symbol] = [];
            }

            foreach (var field in frame.Fields)
            {
                if (!values[frame.Symbol].ContainsKey(field.Id))
                {
                    fieldOrder[frame.Symbol].Add(field.Id);
                }

                values[frame.Symbol][field.Id] = field.Value;
            }
        }

        return symbolOrder
            .Select(symbol => new CapturedFrame(
                symbol,
                fieldOrder[symbol].Select(id => new CapturedField(id, values[symbol][id])).ToArray()))
            .ToArray();
    }
}
