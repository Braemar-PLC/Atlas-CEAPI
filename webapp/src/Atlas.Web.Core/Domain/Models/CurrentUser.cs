namespace Atlas.Web.Core.Domain.Models;

/// <summary>The person making the current request, as the sign-in in front of the API reports them.</summary>
public sealed record CurrentUser(string Email, string Name, bool IsAdmin);
