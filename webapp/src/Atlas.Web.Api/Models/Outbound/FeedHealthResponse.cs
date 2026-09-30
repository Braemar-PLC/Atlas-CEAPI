namespace Atlas.Web.Api.Models.Outbound;

/// <summary>Current health and freshness of the ICE market-data feed.</summary>
public sealed record FeedHealthResponse(
    string State,
    long Generation,
    DateTimeOffset? RelayTimestamp,
    DateTimeOffset? LastQuoteAt,
    string Detail,
    int SubscribedSymbols);
