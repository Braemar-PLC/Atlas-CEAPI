using Atlas.Web.Core.Application.Models;
using Atlas.Web.Core.Application.Ports;
using Atlas.Web.Core.Domain.Logic;
using Atlas.Web.Core.Domain.Models;

namespace Atlas.Web.Core.Application.Services;

/// <summary>Checks what an admin typed before it reaches the repository, and hands back what was stored.</summary>
public sealed class DeskService : IDeskService
{
    private readonly IDeskRepository _desks;

    public DeskService(IDeskRepository desks)
    {
        _desks = desks;
    }

    public Task<IReadOnlyList<Desk>> ListAsync(CancellationToken ct) => _desks.GetAllAsync(ct);

    public Task<IReadOnlyList<Desk>> ForUserAsync(CurrentUser user, CancellationToken ct) =>
        user.IsAdmin ? _desks.GetAllAsync(ct) : _desks.DesksForAsync(user.Email, ct);

    public async Task<bool> CanAccessAsync(string key, CurrentUser user, CancellationToken ct)
    {
        if (user.IsAdmin)
        {
            return await _desks.FindAsync(key, ct) is not null;
        }

        var desks = await _desks.DesksForAsync(user.Email, ct);
        return desks.Any(desk => string.Equals(desk.Key, key, StringComparison.OrdinalIgnoreCase));
    }

    public async Task<DeskWithMembers?> FindAsync(string key, CancellationToken ct)
    {
        var desk = await _desks.FindAsync(key, ct);
        if (desk is null)
        {
            return null;
        }
        return new DeskWithMembers(desk, await _desks.MembersAsync(key, ct));
    }

    public async Task<DeskWithMembers> SaveAsync(string key, string name, string description, IReadOnlyList<string> memberEmails, CancellationToken ct)
    {
        if (!DeskRules.IsValidKey(key))
        {
            throw new DeskValidationException($"\"{key}\" is not a valid desk key: lower-case letters and digits, words joined by hyphens.");
        }
        if (string.IsNullOrWhiteSpace(name))
        {
            throw new DeskValidationException("A desk needs a name.");
        }

        var members = new List<string>();
        foreach (var email in memberEmails)
        {
            var normalised = DeskRules.NormaliseEmail(email);
            if (!DeskRules.IsValidEmail(normalised))
            {
                throw new DeskValidationException($"\"{email.Trim()}\" is not an email address.");
            }
            if (!members.Contains(normalised))
            {
                members.Add(normalised);
            }
        }

        var desk = new Desk(key, name.Trim(), description.Trim());
        await _desks.SaveAsync(desk, members, ct);
        return new DeskWithMembers(desk, members);
    }
}
