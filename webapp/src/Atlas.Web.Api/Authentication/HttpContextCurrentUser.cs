using Atlas.Web.Api.Configuration;
using Atlas.Web.Core.Application.Ports;
using Atlas.Web.Core.Domain.Logic;
using Atlas.Web.Core.Domain.Models;
using Microsoft.Extensions.Options;

namespace Atlas.Web.Api.Authentication;

/// <summary>The signed-in user of the current request, as Core sees them; null outside a request or when nobody is signed in.</summary>
public sealed class HttpContextCurrentUser : ICurrentUser
{
    private readonly IHttpContextAccessor _accessor;
    private readonly AuthOptions _auth;

    public HttpContextCurrentUser(IHttpContextAccessor accessor, IOptions<AuthOptions> auth)
    {
        _accessor = accessor;
        _auth = auth.Value;
    }

    public CurrentUser? Get()
    {
        var principal = _accessor.HttpContext?.User;
        if (principal?.Identity is not { IsAuthenticated: true, Name: not null } identity)
        {
            return null;
        }

        var email = DeskRules.NormaliseEmail(identity.Name);
        var name = principal.FindFirst(EasyAuthDefaults.DisplayNameClaim)?.Value ?? email;
        return new CurrentUser(email, name, principal.IsInRole(_auth.AdminRole));
    }
}
