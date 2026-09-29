using System.Security.Claims;
using System.Text.Encodings.Web;
using Atlas.Web.Api.Configuration;
using Microsoft.AspNetCore.Authentication;
using Microsoft.Extensions.Options;

namespace Atlas.Web.Api.Authentication;

/// <summary>
/// Authenticates a request from the identity header App Service Authentication adds in front of the API.
/// On the laptop there is no such header; in Development, and only there, the configured stand-in user is used instead.
/// A request with neither is nobody, and the default challenge answers it with a plain 401 — the browser redirect to
/// the sign-in page is the platform's job, not the API's.
/// </summary>
public sealed class EasyAuthHandler : AuthenticationHandler<AuthenticationSchemeOptions>
{
    private readonly AuthOptions _auth;
    private readonly IHostEnvironment _environment;

    public EasyAuthHandler(
        IOptionsMonitor<AuthenticationSchemeOptions> options,
        ILoggerFactory logger,
        UrlEncoder encoder,
        IOptions<AuthOptions> auth,
        IHostEnvironment environment)
        : base(options, logger, encoder)
    {
        _auth = auth.Value;
        _environment = environment;
    }

    protected override Task<AuthenticateResult> HandleAuthenticateAsync()
    {
        if (Request.Headers.TryGetValue(EasyAuthDefaults.PrincipalHeader, out var header))
        {
            try
            {
                var principal = EasyAuthPrincipalParser.Parse(header.ToString());
                return Task.FromResult(AuthenticateResult.Success(new AuthenticationTicket(principal, Scheme.Name)));
            }
            catch (FormatException e)
            {
                return Task.FromResult(AuthenticateResult.Fail(e.Message));
            }
        }

        var standIn = _auth.DevelopmentUser;
        if (_environment.IsDevelopment() && standIn is not null)
        {
            return Task.FromResult(AuthenticateResult.Success(new AuthenticationTicket(StandInPrincipal(standIn), Scheme.Name)));
        }

        return Task.FromResult(AuthenticateResult.NoResult());
    }

    private static ClaimsPrincipal StandInPrincipal(DevelopmentUserOptions user)
    {
        var claims = new List<Claim>
        {
            new(ClaimTypes.Name, user.Email),
            new(EasyAuthDefaults.DisplayNameClaim, user.Name),
        };
        foreach (var role in user.Roles)
        {
            claims.Add(new Claim(ClaimTypes.Role, role));
        }
        return new ClaimsPrincipal(new ClaimsIdentity(claims, EasyAuthDefaults.DevelopmentAuthenticationType, ClaimTypes.Name, ClaimTypes.Role));
    }
}
