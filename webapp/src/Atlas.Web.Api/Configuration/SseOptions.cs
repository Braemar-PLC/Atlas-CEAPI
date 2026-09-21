namespace Atlas.Web.Api.Configuration;

public sealed class SseOptions
{
    public const string Section = "Sse";
    public TimeSpan HeartbeatInterval { get; set; } = TimeSpan.FromSeconds(30);
}
