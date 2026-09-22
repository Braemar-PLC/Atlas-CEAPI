namespace Atlas.Web.Ice.Domain.Enumeration;

public enum IceResponseMetaFieldIds
{
    EventHeader         = 330,
    SecurityQualifiers  = 568,
    TradingStatus       = 455,
    TradeConditionFlags = 135,
    MarketPhasesCompact = 40,
    SessionSeqLike      = 39,
    ExchangeTimeIso     = 15,
    BeaconTimeIso       = 16,
    SourceId            = 201,
    // 47 used to be listed here as "LotSize". It is LRT_TYPE_MINUTESDELAYED in the ICE SDK, and it is
    // deliberately NOT stripped: the screen uses it to show how old the prices are.
    RecordReset         = 416,
}
