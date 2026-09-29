using Atlas.Web.Data.Entities;
using Microsoft.EntityFrameworkCore;

namespace Atlas.Web.Data;

/// <summary>Atlas's database: desks and their members. The schema is kept in <c>Migrations/</c>.</summary>
public sealed class AtlasDbContext : DbContext
{
    public AtlasDbContext(DbContextOptions<AtlasDbContext> options) : base(options)
    {
    }

    public DbSet<DeskEntity> Desks => Set<DeskEntity>();
    public DbSet<DeskMemberEntity> DeskMembers => Set<DeskMemberEntity>();

    protected override void OnModelCreating(ModelBuilder modelBuilder)
    {
        modelBuilder.Entity<DeskEntity>(desk =>
        {
            desk.ToTable("Desks");
            desk.HasKey(d => d.Key);
            desk.Property(d => d.Name).IsRequired();
            desk.Property(d => d.Description).IsRequired();
            desk.HasMany(d => d.Members)
                .WithOne(m => m.Desk)
                .HasForeignKey(m => m.DeskKey)
                .OnDelete(DeleteBehavior.Cascade);
        });

        modelBuilder.Entity<DeskMemberEntity>(member =>
        {
            member.ToTable("DeskMembers");
            member.HasKey(m => new { m.DeskKey, m.Email });
        });
    }
}
