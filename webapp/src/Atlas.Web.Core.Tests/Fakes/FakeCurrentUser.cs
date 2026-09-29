using Atlas.Web.Core.Application.Ports;
using Atlas.Web.Core.Domain.Models;

namespace Atlas.Web.Core.Tests.Fakes;

/// <summary>Stands in for the signed-in user; null means nobody is signed in.</summary>
public sealed class FakeCurrentUser : ICurrentUser
{
    private readonly CurrentUser? _user;

    public FakeCurrentUser(CurrentUser? user)
    {
        _user = user;
    }

    public CurrentUser? Get() => _user;
}
