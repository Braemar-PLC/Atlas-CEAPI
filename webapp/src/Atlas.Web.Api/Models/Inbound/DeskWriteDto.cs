namespace Atlas.Web.Api.Models.Inbound;

/// <summary>What an admin may change about an existing desk; the key is in the address.</summary>
public sealed record DeskWriteDto(string Name, string Description, IReadOnlyList<string> Members);
