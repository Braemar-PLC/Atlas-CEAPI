using Atlas.Web.Core.Domain.Logic;
using Atlas.Web.Core.Domain.Models;
using FluentAssertions;
using System.Reactive.Linq;

namespace Atlas.Web.Core.Tests.Domain.Services;

public class PricingStore_UnitTests : IDisposable
{
    private readonly PricingStore _sut = new PricingStore(new FakeSymbolListParser());
    public void Dispose() => _sut.Dispose();

    [Fact]
    public void CurrentSnapshot_UnknownSymbol_ReturnsEmptyFields()
    {
        var snap = _sut.CurrentSnapshot("UNKNOWN");
        snap.Symbol.Should().Be("UNKNOWN");
        snap.Fields.Should().BeEmpty();
    }

    [Fact]
    public void CurrentSnapshot_AfterUpdate_ReturnsLatestFields()
    {
        _sut.Update(new PricingUpdate("SYM", [(1, "72"), (36, "110")]));
        var snap = _sut.CurrentSnapshot("SYM");
        snap.Fields[1].Should().Be("72");
        snap.Fields[36].Should().Be("110");
    }

    [Fact]
    public void CurrentSnapshot_IsImmutableCopy_NotALiveReference()
    {
        _sut.Update(new PricingUpdate("SYM", [(1, "A")]));
        var snap1 = _sut.CurrentSnapshot("SYM");
        _sut.Update(new PricingUpdate("SYM", [(1, "B")]));
        var snap2 = _sut.CurrentSnapshot("SYM");
        snap1.Fields[1].Should().Be("A");
        snap2.Fields[1].Should().Be("B");
    }

    [Fact]
    public void Update_MergesNewFieldsIntoExistingState()
    {
        _sut.Update(new PricingUpdate("SYM", [(1, "72")]));
        _sut.Update(new PricingUpdate("SYM", [(2, "110")]));
        var snap = _sut.CurrentSnapshot("SYM");
        snap.Fields[1].Should().Be("72");
        snap.Fields[2].Should().Be("110");
    }

    [Fact]
    public void Update_OverwritesExistingFieldValue()
    {
        _sut.Update(new PricingUpdate("SYM", [(1, "72")]));
        _sut.Update(new PricingUpdate("SYM", [(1, "99")]));
        _sut.CurrentSnapshot("SYM").Fields[1].Should().Be("99");
    }

    [Fact]
    public void Update_MultipleFieldsInOneUpdate_AllApplied()
    {
        _sut.Update(new PricingUpdate("SYM", [(1, "A"), (2, "B"), (3, "C")]));
        var snap = _sut.CurrentSnapshot("SYM");
        snap.Fields[1].Should().Be("A");
        snap.Fields[2].Should().Be("B");
        snap.Fields[3].Should().Be("C");
    }

    [Fact]
    public void Update_DifferentSymbols_AreIsolated()
    {
        _sut.Update(new PricingUpdate("AAA", [(1, "aaa")]));
        _sut.Update(new PricingUpdate("BBB", [(1, "bbb")]));
        _sut.CurrentSnapshot("AAA").Fields[1].Should().Be("aaa");
        _sut.CurrentSnapshot("BBB").Fields[1].Should().Be("bbb");
    }

    [Fact]
    public void Update_EmptyFieldsList_PreservesPreviousState()
    {
        _sut.Update(new PricingUpdate("SYM", [(1, "72")]));
        _sut.Update(new PricingUpdate("SYM", []));
        _sut.CurrentSnapshot("SYM").Fields[1].Should().Be("72");
    }

    [Fact]
    public void Update_EmptyStringFieldValue_StoredAsEmptyString()
    {
        _sut.Update(new PricingUpdate("SYM", [(1, "72")]));
        _sut.Update(new PricingUpdate("SYM", [(1, "")]));
        _sut.CurrentSnapshot("SYM").Fields[1].Should().Be("");
    }

    [Fact]
    public void GetStream_NewSubscriber_ImmediatelyReceivesCurrentSnapshot()
    {
        _sut.Update(new PricingUpdate("SYM", [(1, "72")]));
        PricingSnapshot? received = null;
        _sut.GetStream("SYM").Subscribe(s => received = s);
        received.Should().NotBeNull();
        received!.Fields[1].Should().Be("72");
    }

    [Fact]
    public void GetStream_UnknownSymbol_ImmediatelyReceivesEmptySnapshot()
    {
        PricingSnapshot? received = null;
        _sut.GetStream("UNKNOWN").Subscribe(s => received = s);
        received.Should().NotBeNull();
        received!.Symbol.Should().Be("UNKNOWN");
        received!.Fields.Should().BeEmpty();
    }

    [Fact]
    public void GetStream_SubsequentUpdate_EmitsNewSnapshot()
    {
        var received = new List<PricingSnapshot>();
        _sut.GetStream("SYM").Subscribe(s => received.Add(s));
        _sut.Update(new PricingUpdate("SYM", [(1, "72")]));
        _sut.Update(new PricingUpdate("SYM", [(1, "99")]));
        received.Should().HaveCount(3);
        received[0].Fields.Should().BeEmpty();
        received[1].Fields[1].Should().Be("72");
        received[2].Fields[1].Should().Be("99");
    }

    [Fact]
    public void GetStream_EachSnapshotInStream_IsImmutableCopy()
    {
        var received = new List<PricingSnapshot>();
        _sut.GetStream("SYM").Subscribe(s => received.Add(s));
        _sut.Update(new PricingUpdate("SYM", [(1, "A")]));
        _sut.Update(new PricingUpdate("SYM", [(1, "B")]));
        received[1].Fields[1].Should().Be("A");
        received[2].Fields[1].Should().Be("B");
    }

    [Fact]
    public void GetStream_MultipleSubscribers_AllReceiveUpdate()
    {
        PricingSnapshot? a = null, b = null;
        using var sub1 = _sut.GetStream("SYM").Skip(1).Subscribe(s => a = s);
        using var sub2 = _sut.GetStream("SYM").Skip(1).Subscribe(s => b = s);
        _sut.Update(new PricingUpdate("SYM", [(1, "72")]));
        a.Should().NotBeNull();
        b.Should().NotBeNull();
        a!.Fields[1].Should().Be("72");
        b!.Fields[1].Should().Be("72");
    }

    [Fact]
    public void GetStream_SubscriberReceivesOwnIndependentSequence()
    {
        var receivedA = new List<PricingSnapshot>();
        var receivedB = new List<PricingSnapshot>();
        using var subA = _sut.GetStream("SYM").Subscribe(s => receivedA.Add(s));
        _sut.Update(new PricingUpdate("SYM", [(1, "72")]));
        using var subB = _sut.GetStream("SYM").Subscribe(s => receivedB.Add(s));
        _sut.Update(new PricingUpdate("SYM", [(1, "99")]));
        receivedA.Should().HaveCount(3);
        receivedA[0].Fields.Should().BeEmpty();
        receivedA[1].Fields[1].Should().Be("72");
        receivedA[2].Fields[1].Should().Be("99");
        receivedB.Should().HaveCount(2);
        receivedB[0].Fields[1].Should().Be("72");
        receivedB[1].Fields[1].Should().Be("99");
    }

    [Fact]
    public void GetStream_DisposedSubscription_ReceivesNoFurtherEmissions()
    {
        var received = new List<PricingSnapshot>();
        var sub = _sut.GetStream("SYM").Subscribe(s => received.Add(s));
        _sut.Update(new PricingUpdate("SYM", [(1, "72")]));
        var countBeforeDispose = received.Count;
        sub.Dispose();
        _sut.Update(new PricingUpdate("SYM", [(1, "99")]));
        received.Should().HaveCount(countBeforeDispose);
    }

    [Fact]
    public void GetStream_UpdateToOtherSymbol_DoesNotEmitOnThisStream()
    {
        var received = new List<PricingSnapshot>();
        using var _ = _sut.GetStream("AAA").Subscribe(s => received.Add(s));
        var countAfterSubscribe = received.Count;
        _sut.Update(new PricingUpdate("BBB", [(1, "x")]));
        received.Should().HaveCount(countAfterSubscribe);
    }

    [Fact]
    public void GetStream_SnapshotReceivedOnSubscribe_ReflectsAllPriorUpdates()
    {
        for (var i = 0; i < 100; i++)
        {
            _sut.Update(new PricingUpdate("SYM", [(1, i.ToString())]));
        }
        PricingSnapshot? first = null;
        _sut.GetStream("SYM").Take(1).Subscribe(s => first = s);
        first!.Fields[1].Should().Be("99");
    }

    [Fact]
    public void Dispose_CompletesAllObservables()
    {
        var completed = false;
        _sut.GetStream("SYM").Subscribe(_ => { }, () => completed = true);
        _sut.Dispose();
        completed.Should().BeTrue();
    }

    [Fact]
    public void Publish_EmptyStringFieldValue_StoredAsEmptyString()
    {
        // cleared token arrives as "" — should be stored not ignored
        _sut.Update(new PricingUpdate("SYM", [(1, "72")]));
        _sut.Update(new PricingUpdate("SYM", [(1, "")]));
        _sut.CurrentSnapshot("SYM").Fields[1].Should().Be("");
    }
}
