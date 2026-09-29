using System.Text;
using System.Text.Json;
using Atlas.Web.Api.Authentication;
using FluentAssertions;

namespace Atlas.Web.Api.Tests.Authentication;

public class EasyAuthPrincipalParserTests
{
    private const string NameType = "http://schemas.xmlsoap.org/ws/2005/05/identity/claims/name";
    private const string RoleType = "http://schemas.microsoft.com/ws/2008/06/identity/claims/role";

    /// <summary>What App Service Authentication puts in X-MS-CLIENT-PRINCIPAL: base64 of a small JSON document.</summary>
    public static string Encode(object payload) =>
        Convert.ToBase64String(Encoding.UTF8.GetBytes(JsonSerializer.Serialize(payload)));

    public static readonly object SeanPayload = new
    {
        auth_typ = "aad",
        name_typ = NameType,
        role_typ = RoleType,
        claims = new object[]
        {
            new { typ = NameType, val = "Sean.Hays@braemar.com" },
            new { typ = "name", val = "Sean Hays" },
            new { typ = "preferred_username", val = "Sean.Hays@braemar.com" },
            new { typ = "roles", val = "Atlas.Admin" },
        },
    };

    [Fact]
    public void Parse_TypicalPayload_NameIsTheAccountAndRolesAreRoles()
    {
        var principal = EasyAuthPrincipalParser.Parse(Encode(SeanPayload));

        principal.Identity!.IsAuthenticated.Should().BeTrue();
        principal.Identity.AuthenticationType.Should().Be("aad");
        principal.Identity.Name.Should().Be("Sean.Hays@braemar.com");
        principal.IsInRole("Atlas.Admin").Should().BeTrue();
        principal.FindFirst("name")!.Value.Should().Be("Sean Hays");
    }

    [Fact]
    public void Parse_NoClaimOfTheNameType_FallsBackToPreferredUsername()
    {
        var payload = new
        {
            auth_typ = "aad",
            name_typ = NameType,
            role_typ = RoleType,
            claims = new object[] { new { typ = "preferred_username", val = "marc.jarvis@braemar.com" } },
        };

        var principal = EasyAuthPrincipalParser.Parse(Encode(payload));

        principal.Identity!.Name.Should().Be("marc.jarvis@braemar.com");
        principal.IsInRole("Atlas.Admin").Should().BeFalse();
    }

    [Theory]
    [InlineData("not base64 at all!")]
    [InlineData("bm90IGpzb24=")]
    public void Parse_NotBase64Json_Throws(string header)
    {
        var act = () => EasyAuthPrincipalParser.Parse(header);

        act.Should().Throw<FormatException>();
    }
}
