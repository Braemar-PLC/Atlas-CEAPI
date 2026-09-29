using Atlas.Web.Api.Configuration;
using Atlas.Web.Core.Application.Ports;
using Microsoft.AspNetCore.Authentication;
using Microsoft.AspNetCore.Authorization;
using Microsoft.Extensions.Options;

namespace Atlas.Web.Api.Authentication;

public static class AuthenticationSetup
{
    /// <summary>
    /// Sign-in as the API sees it: the EasyAuth scheme reads the platform's identity header, every endpoint needs a
    /// signed-in user unless it says otherwise, and the Admin policy needs the configured role.
    /// </summary>
    public static IServiceCollection AddEasyAuth(this IServiceCollection services, IConfiguration configuration)
    {
        services.AddOptions<AuthOptions>()
            .Bind(configuration.GetSection(AuthOptions.Section))
            .Validate(o => !string.IsNullOrWhiteSpace(o.AdminRole), $"{AuthOptions.Section}:AdminRole is required")
            .ValidateOnStart();

        services.AddAuthentication(EasyAuthDefaults.Scheme)
            .AddScheme<AuthenticationSchemeOptions, EasyAuthHandler>(EasyAuthDefaults.Scheme, _ => { });
        services.AddHttpContextAccessor();
        services.AddScoped<ICurrentUser, HttpContextCurrentUser>();

        services.AddAuthorization();
        services.AddOptions<AuthorizationOptions>().Configure<IOptions<AuthOptions>>((authorization, auth) =>
        {
            authorization.FallbackPolicy = new AuthorizationPolicyBuilder().RequireAuthenticatedUser().Build();
            authorization.AddPolicy(AuthPolicies.Admin, policy => policy.RequireRole(auth.Value.AdminRole));
        });

        return services;
    }
}
