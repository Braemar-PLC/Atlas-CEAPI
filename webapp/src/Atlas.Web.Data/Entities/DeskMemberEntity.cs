namespace Atlas.Web.Data.Entities;

/// <summary>The DeskMembers table: one row per (desk, email), which is also its key.</summary>
public sealed class DeskMemberEntity
{
    public string DeskKey { get; set; } = string.Empty;
    public string Email { get; set; } = string.Empty;
    public DeskEntity Desk { get; set; } = null!;
}
