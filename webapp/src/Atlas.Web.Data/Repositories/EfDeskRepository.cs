using Atlas.Web.Core.Application.Ports;
using Atlas.Web.Core.Domain.Models;
using Atlas.Web.Data.Entities;
using Microsoft.EntityFrameworkCore;

namespace Atlas.Web.Data.Repositories;

/// <summary>Desks and members in the SQLite database, through Entity Framework.</summary>
public sealed class EfDeskRepository : IDeskRepository
{
    private readonly AtlasDbContext _db;

    public EfDeskRepository(AtlasDbContext db)
    {
        _db = db;
    }

    public async Task<IReadOnlyList<Desk>> GetAllAsync(CancellationToken ct)
    {
        var rows = await _db.Desks.AsNoTracking().OrderBy(d => d.Name).ToListAsync(ct);
        return rows.Select(ToDesk).ToList();
    }

    public async Task<Desk?> FindAsync(string key, CancellationToken ct)
    {
        var row = await _db.Desks.AsNoTracking().FirstOrDefaultAsync(d => d.Key == key, ct);
        return row is null ? null : ToDesk(row);
    }

    public async Task<IReadOnlyList<string>> MembersAsync(string key, CancellationToken ct) =>
        await _db.DeskMembers.AsNoTracking()
            .Where(m => m.DeskKey == key)
            .OrderBy(m => m.Email)
            .Select(m => m.Email)
            .ToListAsync(ct);

    public async Task<IReadOnlyList<Desk>> DesksForAsync(string email, CancellationToken ct)
    {
        var rows = await _db.DeskMembers.AsNoTracking()
            .Where(m => m.Email == email)
            .Select(m => m.Desk)
            .OrderBy(d => d.Name)
            .ToListAsync(ct);
        return rows.Select(ToDesk).ToList();
    }

    public async Task SaveAsync(Desk desk, IReadOnlyList<string> memberEmails, CancellationToken ct)
    {
        var row = await _db.Desks.Include(d => d.Members).FirstOrDefaultAsync(d => d.Key == desk.Key, ct);
        if (row is null)
        {
            row = new DeskEntity { Key = desk.Key };
            _db.Desks.Add(row);
        }
        row.Name = desk.Name;
        row.Description = desk.Description;
        row.Members.Clear();
        foreach (var email in memberEmails)
        {
            row.Members.Add(new DeskMemberEntity { DeskKey = desk.Key, Email = email });
        }
        await _db.SaveChangesAsync(ct);
    }

    private static Desk ToDesk(DeskEntity row) => new(row.Key, row.Name, row.Description);
}
