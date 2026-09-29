using Atlas.Web.Core.Application.Models;
using Atlas.Web.Core.Application.Ports;

namespace Atlas.Web.Core.Application.Services;

/// <summary>Joins the signed-in user to the desks they are a member of and decides where they land.</summary>
public sealed class MeService : IMeService
{
    private readonly ICurrentUser _currentUser;
    private readonly IDeskRepository _desks;

    public MeService(ICurrentUser currentUser, IDeskRepository desks)
    {
        _currentUser = currentUser;
        _desks = desks;
    }

    public async Task<Me?> GetAsync(CancellationToken ct)
    {
        var user = _currentUser.Get();
        if (user is null)
        {
            return null;
        }

        var desks = await _desks.DesksForAsync(user.Email, ct);
        var home = desks.Count == 1 ? desks[0].Key : null;
        return new Me(user.Name, user.Email, user.IsAdmin, desks, home);
    }
}
