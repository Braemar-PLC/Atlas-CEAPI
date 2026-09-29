using Atlas.Web.Core.Domain.Models;
using FluentAssertions;

namespace Atlas.Web.Core.Tests.Domain.Models;

public class ScreenRulesTests
{
    private static ScreenRules Rules()
    {
        var rules = new ScreenRules
        {
            Flat = new FlatRule { Months = 20, Quarters = 10, Seasons = 8, Cals = 5 },
        };
        rules.Spreads.Months.Add(new PairRule { Gap = 1, Count = 13 });
        rules.Spreads.Quarters.Add(new PairRule { Gap = 1, Count = 9 });
        return rules;
    }

    [Fact]
    public void FlatFor_HubWithoutRulesOfItsOwn_ReturnsTheDefault()
    {
        var rules = Rules();

        rules.FlatFor("TTF").Should().BeSameAs(rules.Flat);
    }

    [Fact]
    public void FlatFor_HubWithItsOwnCounts_ReturnsThose_AndTheOtherHubsKeepTheDefault()
    {
        var rules = Rules();
        var coal = new FlatRule { Months = 6, Quarters = 4, Seasons = 0, Cals = 2 };
        rules.Hubs["ARA"] = new HubRules { Flat = coal };

        rules.FlatFor("ARA").Should().BeSameAs(coal);
        rules.FlatFor("TTF").Should().BeSameAs(rules.Flat);
    }

    [Fact]
    public void FlatFor_HubThatOverridesOnlyItsSpreads_KeepsTheDefaultCounts()
    {
        var rules = Rules();
        rules.Hubs["ARA"] = new HubRules { Spreads = new SpreadRule() };

        rules.FlatFor("ARA").Should().BeSameAs(rules.Flat);
    }

    [Fact]
    public void SpreadsFor_HubWithItsOwnSpreads_ReplacesTheWholeListRatherThanAddingToIt()
    {
        var rules = Rules();
        var coal = new SpreadRule();
        coal.Months.Add(new PairRule { Gap = 2, Count = 3 });
        rules.Hubs["ARA"] = new HubRules { Spreads = coal };

        rules.SpreadsFor("ARA").Should().BeSameAs(coal);
        rules.SpreadsFor("ARA").Quarters.Should().BeEmpty("a kind the hub leaves out gets no spreads, whatever the default says");
        rules.SpreadsFor("NBP").Should().BeSameAs(rules.Spreads);
    }

    [Fact]
    public void SpreadsFor_HubThatOverridesOnlyItsCounts_KeepsTheDefaultSpreads()
    {
        var rules = Rules();
        rules.Hubs["ARA"] = new HubRules { Flat = new FlatRule { Months = 6 } };

        rules.SpreadsFor("ARA").Should().BeSameAs(rules.Spreads);
    }
}
