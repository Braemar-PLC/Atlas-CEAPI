using System.Text.Json;

namespace Atlas.Web.Ice.MockFeed.Capture;

/// <summary>
/// Reads captured relay output into frames. The capture is a file of
/// newline-delimited wire frames recorded from the real feed.
/// </summary>
public static class CaptureLoader
{
    public static IReadOnlyList<CapturedFrame> LoadFile(string path)
    {
        return Load(File.ReadLines(path));
    }

    public static IReadOnlyList<CapturedFrame> Load(IEnumerable<string> lines)
    {
        var frames = new List<CapturedFrame>();

        foreach (var line in lines)
        {
            if (string.IsNullOrWhiteSpace(line))
            {
                continue;
            }

            if (TryParse(line.Trim(), out var frame))
            {
                frames.Add(frame);
            }
        }

        return frames;
    }

    private static bool TryParse(string line, out CapturedFrame frame)
    {
        frame = default!;

        try
        {
            using var document = JsonDocument.Parse(line);
            var root = document.RootElement;

            if (root.ValueKind != JsonValueKind.Array || root.GetArrayLength() < 3)
            {
                return false;
            }

            if (!string.Equals(root[0].GetString(), "update", StringComparison.OrdinalIgnoreCase))
            {
                return false;
            }

            var symbol = root[1].GetString();
            if (string.IsNullOrEmpty(symbol))
            {
                return false;
            }

            if (root[2].ValueKind != JsonValueKind.Array)
            {
                return false;
            }

            var fields = new List<CapturedField>();
            foreach (var pair in root[2].EnumerateArray())
            {
                if (pair.ValueKind != JsonValueKind.Array || pair.GetArrayLength() < 2)
                {
                    return false;
                }

                fields.Add(new CapturedField((short)pair[0].GetInt32(), pair[1].ToString()));
            }

            frame = new CapturedFrame(symbol, fields);
            return true;
        }
        catch (Exception)
        {
            // A capture is a recording of a live feed and may legitimately contain
            // a truncated final line or a frame shape this mock does not replay.
            // Skipping the line is correct here: one unreadable frame must not
            // prevent the rest of the capture from being served.
            return false;
        }
    }
}
