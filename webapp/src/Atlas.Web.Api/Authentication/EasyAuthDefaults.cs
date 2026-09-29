namespace Atlas.Web.Api.Authentication;

/// <summary>Names shared by the pieces that read Azure App Service Authentication's identity headers.</summary>
public static class EasyAuthDefaults
{
    public const string Scheme = "EasyAuth";

    /// <summary>The header App Service Authentication adds to every signed-in request: base64 of a JSON document of claims.</summary>
    public const string PrincipalHeader = "X-MS-CLIENT-PRINCIPAL";

    /// <summary>The claim carrying the person's display name ("Sean Hays"), as Entra issues it.</summary>
    public const string DisplayNameClaim = "name";

    /// <summary>The authentication type of the stand-in identity used on the laptop, so it can never be mistaken for a real sign-in.</summary>
    public const string DevelopmentAuthenticationType = "Development";
}
