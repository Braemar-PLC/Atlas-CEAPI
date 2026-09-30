using Atlas.Web.Core.Domain.Enumeration;

namespace Atlas.Web.Core.Domain.Models;

public sealed record FeedHealthSnapshot(
    FeedState State,
    long Generation,
    DateTimeOffset? RelayTimestamp,
    DateTimeOffset? LastQuoteAt,
    string Detail,
    int SubscribedSymbols);
