using Atlas.Web.Api.Services;
using System.Diagnostics;
using System.Text.Json;

namespace Atlas.Web.Api.Services;

public sealed class SseWriter : ISseWriter
{
    private static readonly JsonSerializerOptions JsonOpts = new(JsonSerializerDefaults.Web);
    private readonly TimeSpan _heartbeatInterval;

    public SseWriter(TimeSpan heartbeatInterval)
    {
        _heartbeatInterval = heartbeatInterval;
    }

    public async Task StreamAsync<T>(HttpResponse response, IAsyncEnumerable<T> items, CancellationToken ct)
    {
        response.Headers.ContentType = "text/event-stream";
        response.Headers["X-Accel-Buffering"] = "no";

        var state = new WriteState();
        using var linked = CancellationTokenSource.CreateLinkedTokenSource(ct);

        var heartbeatTask = StartHeartBeat(response, linked.Token, state);

        try
        {
            await foreach (var item in items.WithCancellation(ct))
            {
                await WriteMessage(["event: snapshot", $"data: {JsonSerializer.Serialize(item, JsonOpts)}"], response, ct);
                state.UpdateLastWrite();
            }
        }
        finally
        {
            linked.Cancel();
            // cancel heartbeat
            try { await heartbeatTask; }
            catch (OperationCanceledException) { }
        }
    }

    private async Task StartHeartBeat(HttpResponse response, CancellationToken ct, WriteState state)
    {
        // Kickstart my heart
        while (!ct.IsCancellationRequested)
        {
            await Task.Delay(_heartbeatInterval, ct);
            if (!state.HeartbeatPeriodElapsed(_heartbeatInterval)) continue;
            await WriteMessage(["event: heartbeat"], response, ct);
        }
    }

    private static async Task WriteMessage(string[] lines, HttpResponse response, CancellationToken ct)
    {
        foreach (var line in lines)
        {
            await response.WriteAsync(line + "\n", ct);
        }
        await response.WriteAsync("\n", ct);
        await response.Body.FlushAsync(ct);
    }

    private sealed class WriteState
    {
        private long _lastWriteTicks = Stopwatch.GetTimestamp();

        public void UpdateLastWrite() =>
            Interlocked.Exchange(ref _lastWriteTicks, Stopwatch.GetTimestamp());

        public bool HeartbeatPeriodElapsed(TimeSpan interval)
        {
            var elapsed = (Stopwatch.GetTimestamp() - Interlocked.Read(ref _lastWriteTicks)) / (double)Stopwatch.Frequency;
            return elapsed >= interval.TotalSeconds;
        }
    }
}