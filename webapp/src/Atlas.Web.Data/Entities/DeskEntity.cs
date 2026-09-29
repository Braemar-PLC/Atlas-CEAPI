namespace Atlas.Web.Data.Entities;

/// <summary>The Desks table. Core's <see cref="Core.Domain.Models.Desk"/> record is what the rest of the app sees.</summary>
public sealed class DeskEntity
{
    public string Key { get; set; } = string.Empty;
    public string Name { get; set; } = string.Empty;
    public string Description { get; set; } = string.Empty;
    public List<DeskMemberEntity> Members { get; set; } = new();
}
