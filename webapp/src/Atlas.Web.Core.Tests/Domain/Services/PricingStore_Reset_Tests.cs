using FluentAssertions;
using Atlas.Web.Core.Domain.Enumeration;
using Atlas.Web.Core.Domain.Logic;
using Atlas.Web.Core.Domain.Models;

namespace Atlas.Web.Core.Tests.Domain.Services;

public class PricingStore_Reset_Tests : IDisposable
{
    private readonly PricingStore _sut = new PricingStore(new FakeSymbolListParser());
    public void Dispose() => _sut.Dispose();

    [Fact]
    public void Reset_ClearsAllFields()
    {
        _sut.Update(new PricingUpdate("SYM", [(1, "72"), (19, "64.665")]));
        _sut.Reset("SYM");
        _sut.CurrentSnapshot("SYM").Fields.Should().BeEmpty();
    }

    [Fact]
    public void Reset_KeepsSymbolInStore()
    {
        _sut.Update(new PricingUpdate("SYM", [(1, "72")]));
        _sut.Reset("SYM");
        _sut.CurrentSnapshot("SYM").Symbol.Should().Be("SYM");
    }

    [Fact]
    public void Reset_EmittedSnapshot_HasResetStatus()
    {
        var received = new List<PricingSnapshot>();
        _sut.GetStream("SYM").Subscribe(s => received.Add(s));
        _sut.Update(new PricingUpdate("SYM", [(1, "72")]));
        _sut.Reset("SYM");
        received.Last().Status.Should().Be(DataStatus.Reset);
    }

    [Fact]
    public void Reset_EmitsEmptySnapshot_ToSubscribers()
    {
        var received = new List<PricingSnapshot>();
        _sut.GetStream("SYM").Subscribe(s => received.Add(s));
        _sut.Update(new PricingUpdate("SYM", [(1, "72")]));
        _sut.Reset("SYM");
        received.Last().Fields.Should().BeEmpty();
    }

    [Fact]
    public void Reset_SubscribersStillReceiveSubsequentUpdates()
    {
        var received = new List<PricingSnapshot>();
        _sut.GetStream("SYM").Subscribe(s => received.Add(s));
        _sut.Update(new PricingUpdate("SYM", [(1, "72")]));
        _sut.Reset("SYM");
        _sut.Update(new PricingUpdate("SYM", [(1, "99")]));
        received.Last().Fields[1].Should().Be("99");
    }

    [Fact]
    public void Reset_UnknownSymbol_DoesNotThrow()
    {
        var act = () => _sut.Reset("UNKNOWN");
        act.Should().NotThrow();
    }

    [Fact]
    public void Reset_DoesNotAffectOtherSymbols()
    {
        _sut.Update(new PricingUpdate("AAA", [(1, "72")]));
        _sut.Update(new PricingUpdate("BBB", [(1, "99")]));
        _sut.Reset("AAA");
        _sut.CurrentSnapshot("BBB").Fields[1].Should().Be("99");
    }

    [Fact]
    public void Reset_ThenUpdate_BuildsStateFromScratch()
    {
        _sut.Update(new PricingUpdate("SYM", [(1, "72"), (2, "old")]));
        _sut.Reset("SYM");
        _sut.Update(new PricingUpdate("SYM", [(1, "99")]));
        _sut.CurrentSnapshot("SYM").Fields[1].Should().Be("99");
        _sut.CurrentSnapshot("SYM").Fields.Should().NotContainKey(2);
    }

    [Fact]
    public void Reset_NewSubscriberAfterReset_ReceivesEmptySnapshot()
    {
        _sut.Update(new PricingUpdate("SYM", [(1, "72")]));
        _sut.Reset("SYM");
        PricingSnapshot? received = null;
        _sut.GetStream("SYM").Subscribe(s => received = s);
        received!.Fields.Should().BeEmpty();
    }

    [Fact]
    public void Update_EmittedSnapshot_HasActiveStatus()
    {
        var received = new List<PricingSnapshot>();
        _sut.GetStream("SYM").Subscribe(s => received.Add(s));
        _sut.Update(new PricingUpdate("SYM", [(1, "72")]));
        received.Last().Status.Should().Be(DataStatus.Active);
    }
}
