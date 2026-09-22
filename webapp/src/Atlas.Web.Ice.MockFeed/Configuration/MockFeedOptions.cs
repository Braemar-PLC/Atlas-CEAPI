using System.Globalization;

namespace Atlas.Web.Ice.MockFeed.Configuration;

/// <summary>
/// Command-line configuration for the mock feed. All parameters are optional.
/// </summary>
public sealed record MockFeedOptions
{
    /// <summary>
    /// Distinct from the real relay's 9002 so both can run at once and a
    /// forgotten mock cannot silently shadow the real feed.
    /// </summary>
    public const int DefaultPort = 9102;

    public int Port { get; init; } = DefaultPort;

    public TimeSpan Interval { get; init; } = TimeSpan.FromMilliseconds(250);

    /// <summary>Null selects the capture shipped alongside the executable.</summary>
    public string? CapturePath { get; init; }

    /// <summary>Replay the capture verbatim on exhaustion instead of generating ticks.</summary>
    public bool Loop { get; init; }

    /// <summary>Percentage movement applied per generated tick. Zero holds prices still.</summary>
    public double Jitter { get; init; } = 0.15;

    public static MockFeedOptions Parse(IReadOnlyList<string> args)
    {
        var options = new MockFeedOptions();

        for (var i = 0; i < args.Count; i++)
        {
            switch (args[i])
            {
                case "--loop":
                    options = options with { Loop = true };
                    break;

                case "--port":
                    options = options with { Port = ParsePort(TakeValue(args, ref i)) };
                    break;

                case "--interval":
                    options = options with
                    {
                        Interval = TimeSpan.FromMilliseconds(
                            ParseNonNegative(TakeValue(args, ref i), "--interval"))
                    };
                    break;

                case "--jitter":
                    options = options with
                    {
                        Jitter = ParseNonNegative(TakeValue(args, ref i), "--jitter")
                    };
                    break;

                case "--capture":
                    options = options with { CapturePath = TakeValue(args, ref i) };
                    break;

                default:
                    throw new ArgumentException(
                        $"Unknown argument '{args[i]}'. {Usage}", nameof(args));
            }
        }

        return options;
    }

    public static string Usage =>
        "Usage: Atlas.Web.Ice.MockFeed [--port N] [--interval MS] [--capture PATH] [--loop] [--jitter PCT]";

    private static string TakeValue(IReadOnlyList<string> args, ref int index)
    {
        var name = args[index];
        if (index + 1 >= args.Count)
        {
            throw new ArgumentException($"Argument '{name}' requires a value. {Usage}", nameof(args));
        }

        index++;
        return args[index];
    }

    private static int ParsePort(string value)
    {
        if (!int.TryParse(value, NumberStyles.Integer, CultureInfo.InvariantCulture, out var port)
            || port < 1
            || port > 65535)
        {
            throw new ArgumentException($"'--port' must be between 1 and 65535, got '{value}'.");
        }

        return port;
    }

    private static double ParseNonNegative(string value, string name)
    {
        if (!double.TryParse(value, NumberStyles.Float, CultureInfo.InvariantCulture, out var parsed)
            || parsed < 0)
        {
            throw new ArgumentException($"'{name}' must be zero or greater, got '{value}'.");
        }

        return parsed;
    }
}
