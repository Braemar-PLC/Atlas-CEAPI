namespace Atlas.Web.Api.Models.Outbound;

/// <summary>A desk as the top bar and the chooser need it.</summary>
public sealed record DeskDto(string Key, string Name, string Description);

/// <summary>A desk with its members, for the admin area. Members are email addresses.</summary>
public sealed record DeskDetailDto(string Key, string Name, string Description, IReadOnlyList<string> Members);
