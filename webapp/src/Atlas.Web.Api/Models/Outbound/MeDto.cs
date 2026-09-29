namespace Atlas.Web.Api.Models.Outbound;

/// <summary>The signed-in user: who they are, the desks they belong to, and the desk they land on (null when there is no single one).</summary>
public sealed record MeDto(string Name, string Email, bool IsAdmin, IReadOnlyList<DeskDto> Desks, string? HomeDesk);
