namespace Atlas.Web.Ice.Configuration;
public sealed class IceOptions
{
    public const string Section = "Ice";
    public string Endpoint { get; set; } = string.Empty;
    public int Port { get; set; }
    public List<string> Symbols { get; set; } = new();
    public Uri WebSocketUri => new($"ws://{Endpoint}:{Port}");

    /// <summary>
    /// How long one attempt to reach the relay may take. Without a limit, an attempt made while the relay's container
    /// was being replaced hung for as long as the Web App ran, and the grids stayed empty.
    /// </summary>
    public TimeSpan ConnectTimeout { get; set; } = TimeSpan.FromSeconds(15);
}
