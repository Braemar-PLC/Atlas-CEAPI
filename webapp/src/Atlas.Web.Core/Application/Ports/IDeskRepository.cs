using Atlas.Web.Core.Domain.Models;

namespace Atlas.Web.Core.Application.Ports;

/// <summary>Where desks and their members are kept. Emails are stored as <see cref="Domain.Logic.DeskRules.NormaliseEmail"/> leaves them.</summary>
public interface IDeskRepository
{
    /// <summary>Every desk, in name order.</summary>
    Task<IReadOnlyList<Desk>> GetAllAsync(CancellationToken ct);

    /// <summary>One desk by key, or null if there is no such desk.</summary>
    Task<Desk?> FindAsync(string key, CancellationToken ct);

    /// <summary>The member emails of one desk, in the order they were saved; empty for an unknown desk.</summary>
    Task<IReadOnlyList<string>> MembersAsync(string key, CancellationToken ct);

    /// <summary>The desks one email address is a member of, in name order.</summary>
    Task<IReadOnlyList<Desk>> DesksForAsync(string email, CancellationToken ct);

    /// <summary>Adds or replaces the desk and sets its members to exactly <paramref name="memberEmails"/>.</summary>
    Task SaveAsync(Desk desk, IReadOnlyList<string> memberEmails, CancellationToken ct);
}
