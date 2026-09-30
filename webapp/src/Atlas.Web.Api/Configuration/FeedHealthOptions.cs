namespace Atlas.Web.Api.Configuration;

public sealed class FeedHealthOptions
{
    public const string Section = "FeedHealth";
    public TimeSpan StaleAfter { get; set; } = TimeSpan.FromSeconds(15);
}
