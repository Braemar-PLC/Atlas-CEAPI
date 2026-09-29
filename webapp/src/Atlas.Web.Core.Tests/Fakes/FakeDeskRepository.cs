using Atlas.Web.Core.Application.Ports;
using Atlas.Web.Core.Domain.Models;

namespace Atlas.Web.Core.Tests.Fakes;

/// <summary>Desks and members held in memory, so application services can be tested without a database.</summary>
public sealed class FakeDeskRepository : IDeskRepository
{
    private readonly Dictionary<string, Desk> _desks = new();
    private readonly Dictionary<string, List<string>> _members = new();

    public FakeDeskRepository WithDesk(Desk desk, params string[] memberEmails)
    {
        _desks[desk.Key] = desk;
        _members[desk.Key] = memberEmails.ToList();
        return this;
    }

    public Task<IReadOnlyList<Desk>> GetAllAsync(CancellationToken ct) =>
        Task.FromResult<IReadOnlyList<Desk>>(_desks.Values.OrderBy(d => d.Name).ToList());

    public Task<Desk?> FindAsync(string key, CancellationToken ct) =>
        Task.FromResult(_desks.GetValueOrDefault(key));

    public Task<IReadOnlyList<string>> MembersAsync(string key, CancellationToken ct) =>
        Task.FromResult<IReadOnlyList<string>>(_members.GetValueOrDefault(key) ?? new List<string>());

    public Task<IReadOnlyList<Desk>> DesksForAsync(string email, CancellationToken ct) =>
        Task.FromResult<IReadOnlyList<Desk>>(_members
            .Where(m => m.Value.Contains(email))
            .Select(m => _desks[m.Key])
            .OrderBy(d => d.Name)
            .ToList());

    public Task SaveAsync(Desk desk, IReadOnlyList<string> memberEmails, CancellationToken ct)
    {
        _desks[desk.Key] = desk;
        _members[desk.Key] = memberEmails.ToList();
        return Task.CompletedTask;
    }
}
