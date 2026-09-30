namespace Atlas.Web.Core.Domain.Enumeration;

public enum FeedState
{
    Connecting,
    Live,
    Reconnecting,
    AuthenticationFailed,
    Disconnected,
    Stale
}
