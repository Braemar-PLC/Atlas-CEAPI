using System.Security.Claims;
using System.Text.Json;

namespace Atlas.Web.Api.Authentication;

/// <summary>
/// Turns the X-MS-CLIENT-PRINCIPAL header into a <see cref="ClaimsPrincipal"/>. The header is base64 of
/// <c>{ "auth_typ", "name_typ", "role_typ", "claims": [ { "typ", "val" } ] }</c>; the two <c>*_typ</c> values say which
/// claim types hold the account name and the roles, so <see cref="ClaimsIdentity.Name"/> and
/// <see cref="ClaimsPrincipal.IsInRole"/> work without anyone knowing Entra's claim-type URIs.
/// </summary>
public static class EasyAuthPrincipalParser
{
    private const string RolesClaim = "roles";
    private static readonly string[] AccountNameFallbacks = { "preferred_username", "upn", "email" };
    private static readonly JsonSerializerOptions Json = new() { PropertyNamingPolicy = JsonNamingPolicy.SnakeCaseLower };

    private sealed record Payload(string? AuthTyp, string? NameTyp, string? RoleTyp, List<PayloadClaim>? Claims);
    private sealed record PayloadClaim(string Typ, string Val);

    /// <exception cref="FormatException">The header is not base64, or the bytes are not the JSON document above.</exception>
    public static ClaimsPrincipal Parse(string header)
    {
        Payload payload;
        try
        {
            payload = JsonSerializer.Deserialize<Payload>(Convert.FromBase64String(header), Json)
                ?? throw new FormatException("the document is empty");
        }
        catch (Exception e) when (e is FormatException or JsonException)
        {
            throw new FormatException($"{EasyAuthDefaults.PrincipalHeader} is not base64 JSON: {e.Message}", e);
        }

        var nameType = payload.NameTyp ?? ClaimTypes.Name;
        var roleType = payload.RoleTyp ?? ClaimTypes.Role;
        var claims = (payload.Claims ?? new List<PayloadClaim>())
            .Select(c => new Claim(c.Typ == RolesClaim ? roleType : c.Typ, c.Val))
            .ToList();

        if (claims.All(c => c.Type != nameType))
        {
            var account = claims.FirstOrDefault(c => AccountNameFallbacks.Contains(c.Type));
            if (account is not null)
            {
                claims.Add(new Claim(nameType, account.Value));
            }
        }

        return new ClaimsPrincipal(new ClaimsIdentity(claims, payload.AuthTyp ?? EasyAuthDefaults.Scheme, nameType, roleType));
    }
}
