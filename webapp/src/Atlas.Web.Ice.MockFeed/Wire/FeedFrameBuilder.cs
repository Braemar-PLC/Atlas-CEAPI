using System.Text;
using Atlas.Web.Ice.MockFeed.Capture;

namespace Atlas.Web.Ice.MockFeed.Wire;

/// <summary>
/// Builds relay wire frames.
///
///   refresh: ["refresh","SYMBOL",[[id,"value"],...]]
///   update:  ["update","SYMBOL",[[id,"value"],...]]
///
/// This mirrors MessageBuilder.java deliberately rather than delegating to a
/// JSON serializer, so the mock reproduces the relay's exact output — including
/// its escaping, which covers backslash and double-quote only.
/// </summary>
public static class FeedFrameBuilder
{
    public static string Refresh(string symbol, IReadOnlyList<CapturedField> fields)
    {
        return Build("refresh", symbol, fields);
    }

    public static string Update(string symbol, IReadOnlyList<CapturedField> fields)
    {
        return Build("update", symbol, fields);
    }

    private static string Build(string type, string symbol, IReadOnlyList<CapturedField> fields)
    {
        var builder = new StringBuilder();
        builder.Append("[\"").Append(type).Append("\",\"").Append(Escape(symbol)).Append("\",[");

        for (var i = 0; i < fields.Count; i++)
        {
            if (i > 0)
            {
                builder.Append(',');
            }

            builder.Append('[')
                   .Append(fields[i].Id)
                   .Append(",\"")
                   .Append(Escape(fields[i].Value))
                   .Append("\"]");
        }

        builder.Append("]]");
        return builder.ToString();
    }

    /// <summary>
    /// Backslash is escaped before double-quote; reversing the order would
    /// double-escape the backslash this method introduces.
    /// </summary>
    private static string Escape(string? value)
    {
        if (value is null)
        {
            return string.Empty;
        }

        return value.Replace("\\", "\\\\").Replace("\"", "\\\"");
    }
}
