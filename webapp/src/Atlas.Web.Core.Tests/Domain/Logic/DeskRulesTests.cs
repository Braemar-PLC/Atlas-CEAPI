using Atlas.Web.Core.Domain.Logic;
using FluentAssertions;

namespace Atlas.Web.Core.Tests.Domain.Logic;

public class DeskRulesTests
{
    [Theory]
    [InlineData("natural-gas")]
    [InlineData("coal")]
    [InlineData("dry-ffa2")]
    public void IsValidKey_LowerCaseWordsJoinedByHyphens_IsValid(string key)
    {
        DeskRules.IsValidKey(key).Should().BeTrue();
    }

    [Theory]
    [InlineData("Natural Gas")]
    [InlineData("natural_gas")]
    [InlineData("-coal")]
    [InlineData("coal-")]
    [InlineData("dry--ffa")]
    [InlineData("")]
    public void IsValidKey_AnythingElse_IsNotValid(string key)
    {
        DeskRules.IsValidKey(key).Should().BeFalse();
    }

    [Fact]
    public void NormaliseEmail_TrimsAndLowerCases()
    {
        DeskRules.NormaliseEmail("  Sean.Hays@Braemar.com ").Should().Be("sean.hays@braemar.com");
    }

    [Theory]
    [InlineData("sean.hays@braemar.com")]
    [InlineData("a@b")]
    public void IsValidEmail_OneAtWithSomethingEitherSide_IsValid(string email)
    {
        DeskRules.IsValidEmail(email).Should().BeTrue();
    }

    [Theory]
    [InlineData("sean.hays")]
    [InlineData("@braemar.com")]
    [InlineData("sean@")]
    [InlineData("sean@@braemar.com")]
    [InlineData("")]
    public void IsValidEmail_AnythingElse_IsNotValid(string email)
    {
        DeskRules.IsValidEmail(email).Should().BeFalse();
    }
}
