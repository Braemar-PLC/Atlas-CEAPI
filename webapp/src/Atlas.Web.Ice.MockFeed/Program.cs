using Atlas.Web.Ice.MockFeed.Capture;
using Atlas.Web.Ice.MockFeed.Configuration;
using Atlas.Web.Ice.MockFeed.Server;
using Atlas.Web.Ice.MockFeed.Ticks;

MockFeedOptions options;
try
{
    options = MockFeedOptions.Parse(args);
}
catch (ArgumentException exception)
{
    Console.Error.WriteLine(exception.Message);
    return 1;
}

var capturePath = options.CapturePath
    ?? Path.Combine(AppContext.BaseDirectory, "Capture", "ceapidata.json");

if (!File.Exists(capturePath))
{
    Console.Error.WriteLine($"Capture file not found: {capturePath}");
    return 1;
}

var frames = CaptureLoader.LoadFile(capturePath);
if (frames.Count == 0)
{
    Console.Error.WriteLine($"No replayable frames found in {capturePath}");
    return 1;
}

Console.WriteLine($"Loaded {frames.Count} frames from {capturePath}");

var sequence = new FeedSequence(frames, options, Random.Shared);
using var server = new MockFeedServer(sequence, options, Console.Out);
using var shutdown = new CancellationTokenSource();

Console.CancelKeyPress += (_, eventArgs) =>
{
    eventArgs.Cancel = true;
    shutdown.Cancel();
};

server.Start();
Console.WriteLine("Commands: 'resync' resends refresh, 'reset [SYMBOL]' signals a record reset.");
await server.AcceptLoopAsync(shutdown.Token);
return 0;
