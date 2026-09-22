namespace Atlas.Web.Ice.Configuration;
public sealed class IceOptions
{
    public const string Section = "Ice";
    public string Endpoint { get; set; } = string.Empty;
    public int Port { get; set; }
    public List<string> Symbols { get; set; } = new();
    public Uri WebSocketUri => new($"ws://{Endpoint}:{Port}");
}
