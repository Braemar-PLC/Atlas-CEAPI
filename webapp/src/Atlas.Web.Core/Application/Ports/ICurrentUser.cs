using Atlas.Web.Core.Domain.Models;

namespace Atlas.Web.Core.Application.Ports;

/// <summary>Who is making the current request; null when nobody is signed in.</summary>
public interface ICurrentUser
{
    CurrentUser? Get();
}
