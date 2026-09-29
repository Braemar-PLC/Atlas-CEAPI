namespace Atlas.Web.Data.Configuration;

/// <summary>Where the SQLite file lives. A relative <see cref="Path"/> is taken from the API's content root.</summary>
public sealed class DatabaseOptions
{
    public const string Section = "Database";

    public string Path { get; set; } = string.Empty;

    public string FullPath(string contentRoot) =>
        System.IO.Path.IsPathRooted(Path) ? Path : System.IO.Path.Combine(contentRoot, Path);
}
