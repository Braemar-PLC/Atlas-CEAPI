namespace Atlas.Web.Api.Configuration;

/// <summary>How the API reads the sign-in in front of it.</summary>
public sealed class AuthOptions
{
    public const string Section = "Auth";

    /// <summary>The Entra app role that makes someone an Atlas admin, as it arrives in the sign-in's role claims.</summary>
    public string AdminRole { get; set; } = "Atlas.Admin";

    /// <summary>Who the API treats as signed in when no sign-in header is present. Honoured in Development only.</summary>
    public DevelopmentUserOptions? DevelopmentUser { get; set; }
}

public sealed class DevelopmentUserOptions
{
    public string Email { get; set; } = string.Empty;
    public string Name { get; set; } = string.Empty;

    /// <summary>Starts empty on purpose: the configuration binder appends to a list that already has items.</summary>
    public List<string> Roles { get; set; } = new();
}
