using Atlas.Web.Core.Domain.Enumeration;
using Atlas.Web.Core.Domain.Models;

namespace Atlas.Web.Core.Application.Ports;

public interface IFeedHealthStore
{
    void RelayConnecting();
    void RelayDisconnected(string detail);
    void RecordRelayStatus(
        FeedState state,
        long generation,
        DateTimeOffset relayTimestamp,
        string detail,
        int subscribedSymbols);
    void RecordQuote();
    FeedHealthSnapshot Current();
    IObservable<FeedHealthSnapshot> GetStream();
}
