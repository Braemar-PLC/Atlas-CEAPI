using System.Text.RegularExpressions;

namespace Atlas.Web.Core.Domain.Logic;

/// <summary>What a desk key and a member's email address must look like.</summary>
public static class DeskRules
{
    private static readonly Regex KeyPattern = new("^[a-z0-9]+(-[a-z0-9]+)*$", RegexOptions.Compiled);

    /// <summary>Lower-case letters and digits, words joined by single hyphens: "natural-gas", "dry-ffa".</summary>
    public static bool IsValidKey(string key) => KeyPattern.IsMatch(key);

    /// <summary>The one form an address is stored and compared in, whatever case the sign-in or an admin typed it in.</summary>
    public static string NormaliseEmail(string email) => email.Trim().ToLowerInvariant();

    /// <summary>Exactly one "@" with something either side. Enough to catch a name typed where an address belongs.</summary>
    public static bool IsValidEmail(string email)
    {
        var at = email.IndexOf('@');
        return at > 0 && at == email.LastIndexOf('@') && at < email.Length - 1;
    }
}
