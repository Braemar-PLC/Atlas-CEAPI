using Atlas.Web.Ice.MockFeed.Capture;
using FluentAssertions;

namespace Atlas.Web.Ice.MockFeed.Tests;

public class SnapshotBuilderTests
{
    private static CapturedFrame Frame(string symbol, params (short Id, string Value)[] fields) =>
        new(symbol, fields.Select(f => new CapturedField(f.Id, f.Value)).ToArray());

    [Fact]
    public void Build_ProducesOneSnapshotPerSymbol()
    {
        var snapshots = SnapshotBuilder.Build([
            Frame("A", (1, "1")),
            Frame("B", (1, "2")),
            Frame("A", (1, "3"))
        ]);

        snapshots.Select(s => s.Symbol).Should().BeEquivalentTo(["A", "B"]);
    }

    [Fact]
    public void Build_UnionsFieldsSeenAcrossFrames()
    {
        var snapshots = SnapshotBuilder.Build([
            Frame("A", (1, "x")),
            Frame("A", (19, "y"))
        ]);

        snapshots[0].Fields.Select(f => f.Id).Should().Equal((short)1, (short)19);
    }

    [Fact]
    public void Build_LaterValueWinsForRepeatedField()
    {
        var snapshots = SnapshotBuilder.Build([
            Frame("A", (19, "64.6")),
            Frame("A", (19, "64.9"))
        ]);

        snapshots[0].Fields.Should().ContainSingle()
            .Which.Value.Should().Be("64.9");
    }

    [Fact]
    public void Build_PreservesFirstSeenFieldOrder()
    {
        var snapshots = SnapshotBuilder.Build([
            Frame("A", (19, "a"), (1, "b")),
            Frame("A", (13, "c"), (19, "d"))
        ]);

        snapshots[0].Fields.Select(f => f.Id).Should().Equal((short)19, (short)1, (short)13);
    }

    [Fact]
    public void Build_PreservesSymbolFirstSeenOrder()
    {
        var snapshots = SnapshotBuilder.Build([
            Frame("Z", (1, "1")),
            Frame("A", (1, "1"))
        ]);

        snapshots.Select(s => s.Symbol).Should().Equal("Z", "A");
    }

    [Fact]
    public void Build_NoFrames_ProducesNoSnapshots()
    {
        SnapshotBuilder.Build([]).Should().BeEmpty();
    }
}
