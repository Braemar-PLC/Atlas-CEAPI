using Atlas.Web.Core.Application.Ports;
using Atlas.Web.Core.Domain.Enumeration;
using Atlas.Web.Core.Domain.Models;
using System.Reactive.Linq;
using System.Reactive.Subjects;

namespace Atlas.Web.Core.Domain.Logic;

public sealed class FeedHealthStore : IFeedHealthStore, IDisposable
{
    private readonly object _writeLock = new();
    private readonly BehaviorSubject<FeedHealthSnapshot> _subject;
    private readonly TimeProvider _timeProvider;
    private readonly TimeSpan _staleAfter;
    private DateTimeOffset? _lastHeartbeatReceivedAt;

    public FeedHealthStore(TimeProvider timeProvider, TimeSpan staleAfter)
    {
        _timeProvider = timeProvider;
        _staleAfter = staleAfter;
        _subject = new BehaviorSubject<FeedHealthSnapshot>(new(
            FeedState.Connecting,
            0,
            null,
            null,
            "Connecting to the CEAPI relay",
            0));
    }

    public void RelayConnecting() => Publish(current => current with
    {
        State = FeedState.Connecting,
        Detail = "Connecting to the CEAPI relay"
    });

    public void RelayDisconnected(string detail) => Publish(current => current with
    {
        State = FeedState.Reconnecting,
        RelayTimestamp = null,
        Detail = detail,
        SubscribedSymbols = 0
    });

    public void RecordRelayStatus(
        FeedState state,
        long generation,
        DateTimeOffset relayTimestamp,
        string detail,
        int subscribedSymbols)
    {
        lock (_writeLock)
        {
            var current = _subject.Value;
            if (generation < current.Generation)
            {
                return;
            }

            _lastHeartbeatReceivedAt = _timeProvider.GetUtcNow();
            _subject.OnNext(current with
            {
                State = state,
                Generation = generation,
                RelayTimestamp = relayTimestamp,
                Detail = detail,
                SubscribedSymbols = subscribedSymbols
            });
        }
    }

    public void RecordQuote() => Publish(current => current with
    {
        LastQuoteAt = _timeProvider.GetUtcNow()
    });

    public FeedHealthSnapshot Current()
    {
        lock (_writeLock)
        {
            var current = _subject.Value;
            if (current.State is FeedState.AuthenticationFailed or FeedState.Disconnected
                or FeedState.Reconnecting or FeedState.Connecting)
            {
                return current;
            }

            return _lastHeartbeatReceivedAt is null
                || _timeProvider.GetUtcNow() - _lastHeartbeatReceivedAt > _staleAfter
                    ? current with
                    {
                        State = FeedState.Stale,
                        Detail = $"No CEAPI health heartbeat received for more than {_staleAfter.TotalSeconds:0} seconds"
                    }
                    : current;
        }
    }

    public IObservable<FeedHealthSnapshot> GetStream() => _subject.AsObservable();

    public void Dispose()
    {
        _subject.OnCompleted();
        _subject.Dispose();
    }

    private void Publish(Func<FeedHealthSnapshot, FeedHealthSnapshot> update)
    {
        lock (_writeLock)
        {
            _subject.OnNext(update(_subject.Value));
        }
    }
}
