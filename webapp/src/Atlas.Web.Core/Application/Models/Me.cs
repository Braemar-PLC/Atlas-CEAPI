using Atlas.Web.Core.Domain.Models;

namespace Atlas.Web.Core.Application.Models;

/// <summary>
/// The signed-in user. <paramref name="HomeDeskKey"/> is the desk they land on: set only when they are a member of
/// exactly one desk, so someone in several desks (or none) is shown the desk chooser instead.
/// </summary>
public sealed record Me(string Name, string Email, bool IsAdmin, IReadOnlyList<Desk> Desks, string? HomeDeskKey);
