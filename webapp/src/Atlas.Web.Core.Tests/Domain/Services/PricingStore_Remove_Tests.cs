using FluentAssertions;
using Atlas.Web.Core.Domain.Enumeration;
using Atlas.Web.Core.Domain.Logic;
using Atlas.Web.Core.Domain.Models;

namespace Atlas.Web.Core.Tests.Domain.Services;

public partial class PricingStore_Remove_Tests : IDisposable
{

    
    private readonly PricingStore _sut = new PricingStore(new FakeSymbolListParser());
    public void Dispose() => _sut.Dispose();

    [Fact]
    public void Remove_EmitsSnapshotWithRemoveStatus_BeforeCompleting()
    {
        var received = new List<PricingSnapshot>();
        _sut.GetStream("SYM").Subscribe(s => received.Add(s));
        _sut.Update(new PricingUpdate("SYM", [(1, "72")]));

        _sut.Remove("SYM");

        received.Last().Status.Should().Be(DataStatus.Remove);
    }

    [Fact]
    public void Remove_CompletesStream()
    {
        var completed = false;
        _sut.GetStream("SYM").Subscribe(_ => { }, () => completed = true);
        _sut.Update(new PricingUpdate("SYM", [(1, "72")]));

        _sut.Remove("SYM");

        completed.Should().BeTrue();
    }

    [Fact]
    public void Remove_RemovesSymbolFromStore()
    {
        _sut.Update(new PricingUpdate("SYM", [(1, "72")]));
        _sut.Remove("SYM");
        _sut.CurrentSnapshot("SYM").Fields.Should().BeEmpty();
    }

    [Fact]
    public void Remove_UnknownSymbol_DoesNotThrow()
    {
        var act = () => _sut.Remove("UNKNOWN");
        act.Should().NotThrow();
    }

    [Fact]
    public void Remove_DoesNotAffectOtherSymbols()
    {
        _sut.Update(new PricingUpdate("AAA", [(1, "72")]));
        _sut.Update(new PricingUpdate("BBB", [(1, "99")]));

        _sut.Remove("AAA");

        _sut.CurrentSnapshot("BBB").Fields[1].Should().Be("99");
    }

    [Fact]
    public void GetStream_WithMultipleSymbols_MergesStreams()
    {
        var received = new List<PricingSnapshot>();

        using var _ = _sut.GetStream("AAA,BBB").Subscribe(s => received.Add(s));

        _sut.Update(new PricingUpdate("AAA", [(1, "10")]));
        _sut.Update(new PricingUpdate("BBB", [(1, "20")]));


        // we expect 2 initial and 2 updates 
        received.Should().HaveCount(4);

        received.Count(s => s.Symbol == "AAA").Should().Be(2);
        received.Count(s => s.Symbol == "BBB").Should().Be(2);
    }

    [Fact]
    public void GetStream_WithWhitespace_TrimsCorrectly()
    {
        var received = new List<PricingSnapshot>();

        using var _ = _sut.GetStream(" AAA ,  BBB ").Subscribe(s => received.Add(s));

        _sut.Update(new PricingUpdate("AAA", [(1, "10")]));
        _sut.Update(new PricingUpdate("BBB", [(1, "20")]));


        // we expect 2 initial and 2 updates 

        received.Should().HaveCount(4);

        received.Count(s => s.Symbol == "AAA").Should().Be(2);
        received.Count(s => s.Symbol == "BBB").Should().Be(2);

    }
}