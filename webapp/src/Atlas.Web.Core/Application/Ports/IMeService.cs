using Atlas.Web.Core.Application.Models;

namespace Atlas.Web.Core.Application.Ports;

/// <summary>The signed-in user as the app sees them: who they are, which desks they belong to, where they land.</summary>
public interface IMeService
{
    /// <summary>Null when nobody is signed in.</summary>
    Task<Me?> GetAsync(CancellationToken ct);
}
