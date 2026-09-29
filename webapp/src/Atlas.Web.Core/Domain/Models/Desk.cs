namespace Atlas.Web.Core.Domain.Models;

/// <summary>
/// A trading desk: what Atlas organises screens and people by.
/// <paramref name="Key"/> is the address-safe form ("natural-gas") and never changes; <paramref name="Name"/> is what people see.
/// </summary>
public sealed record Desk(string Key, string Name, string Description);

/// <summary>One person's membership of a desk, by the email address their sign-in reports (lower-cased).</summary>
public sealed record DeskMember(string DeskKey, string Email);
