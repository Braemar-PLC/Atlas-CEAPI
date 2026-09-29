namespace Atlas.Web.Api.Authentication;

/// <summary>Authorization policy names, for <c>[Authorize(Policy = …)]</c>.</summary>
public static class AuthPolicies
{
    /// <summary>Holders of the admin role: they manage desks and members.</summary>
    public const string Admin = "Admin";
}
