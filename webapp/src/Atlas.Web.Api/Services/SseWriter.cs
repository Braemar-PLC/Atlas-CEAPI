using Atlas.Web.Api.Services;
using System.Reactive;
using System.Reactive.Linq;
using System.Text;
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

    public async Task StreamAsync<T>(HttpResponse response, IObservable<T> items, CancellationToken ct)
    {
        response.Headers.ContentType = "text/event-stream";
        response.Headers["X-Accel-Buffering"] = "no";

        // A disconnect completes the sequence rather than cancelling the wait for the next item, so the loop below
        // simply runs out: the client going away is the normal end of a stream, not an error.
        var untilClientGone = items.TakeUntil(Signalled(ct)).ToAsyncEnumerable();
        await using var enumerator = untilClientGone.GetAsyncEnumerator();
        Task<bool>? pendingItem = null;

        while (true)
        {
            pendingItem ??= enumerator.MoveNextAsync().AsTask();
            var heartbeatDelay = Task.Delay(_heartbeatInterval, ct);
            var completed = await Task.WhenAny(pendingItem, heartbeatDelay);

            if (completed == pendingItem)
            {
                if (!await pendingItem)
                {
                    break;
                }

                if (ct.IsCancellationRequested)
                {
                    break;
                }

                await WriteMessage(
                    ["event: snapshot", $"data: {JsonSerializer.Serialize(enumerator.Current, JsonOpts)}"],
                    response);
                pendingItem = null;
                continue;
            }

            // A canceled delay is the normal signal for a disconnected client.
            await heartbeatDelay.ConfigureAwait(ConfigureAwaitOptions.SuppressThrowing);
            if (ct.IsCancellationRequested)
            {
                break;
            }

            if (pendingItem.IsCompleted)
            {
                continue;
            }

            await WriteMessage(["event: heartbeat"], response);
        }
    }

    /// <summary>A sequence that produces one value when <paramref name="ct"/> is cancelled.</summary>
    private static IObservable<Unit> Signalled(CancellationToken ct) =>
        Observable.Create<Unit>(observer => ct.Register(() => observer.OnNext(Unit.Default)));

    private static async Task WriteMessage(string[] lines, HttpResponse response)
    {
        var bytes = Encoding.UTF8.GetBytes(string.Join('\n', lines) + "\n\n");
        // Request-abort completion is handled by the observable; don't race it into a write cancellation exception.
        await response.Body.WriteAsync(bytes.AsMemory(), CancellationToken.None);
        await response.Body.FlushAsync(CancellationToken.None);
    }
}