using Atlas.Web.Core.Application.Models;
using Atlas.Web.Core.Domain.Models;

namespace Atlas.Web.Core.Application.Ports;

/// <summary>Desks and their members, as an admin manages them.</summary>
public interface IDeskService
{
    /// <summary>Every desk, in name order.</summary>
    Task<IReadOnlyList<Desk>> ListAsync(CancellationToken ct);

    /// <summary>Every desk for an admin, or only the user's memberships for a regular user.</summary>
    Task<IReadOnlyList<Desk>> ForUserAsync(CurrentUser user, CancellationToken ct);

    /// <summary>Whether a user may access a desk; admins may access every existing desk.</summary>
    Task<bool> CanAccessAsync(string key, CurrentUser user, CancellationToken ct);

    /// <summary>One desk with its members, or null if there is no such desk.</summary>
    Task<DeskWithMembers?> FindAsync(string key, CancellationToken ct);

    /// <summary>
    /// Adds or replaces a desk and sets its members. Emails are trimmed, lower-cased and de-duplicated.
    /// Throws <see cref="DeskValidationException"/> for a bad key, a blank name or something that is not an email address.
    /// </summary>
    Task<DeskWithMembers> SaveAsync(string key, string name, string description, IReadOnlyList<string> memberEmails, CancellationToken ct);
}
