using Atlas.Web.Core.Domain.Models;

namespace Atlas.Web.Core.Application.Models;

/// <summary>A desk and the email addresses of its members.</summary>
public sealed record DeskWithMembers(Desk Desk, IReadOnlyList<string> Members);
