using Atlas.Web.Core.Application.Models;
using Atlas.Web.Core.Application.Ports;
using Atlas.Web.Core.Application.Services;
using Atlas.Web.Core.Domain.Enumeration;
using Atlas.Web.Core.Domain.Models;
using FluentAssertions;
using Moq;

namespace Atlas.Web.Core.Tests.Application.Services;

public class PriceMessageHandlerTests
{
    private static PriceMessageHandler Build(IPricingStore? store = null) =>
        new(store ?? new Mock<IPricingStore>().Object);

    [Fact]
    public void Handle_ActiveStatus_CallsUpdate()
    {
        var store = new Mock<IPricingStore>();
        var sut = Build(store.Object);
        var message = new PriceMessage("SYM", [(1, "72")], DataStatus.Active);

        sut.Handle(message);

        store.Verify(s => s.Update(It.Is<PricingUpdate>(u =>
            u.Symbol == "SYM" &&
            u.Fields.Any(f => f.FieldId == 1 && f.Value == "72"))), Times.Once);
    }

    [Fact]
    public void Handle_ResetStatus_CallsReset()
    {
        var store = new Mock<IPricingStore>();
        var sut = Build(store.Object);
        var message = new PriceMessage("SYM", [], DataStatus.Reset);

        sut.Handle(message);

        store.Verify(s => s.Reset("SYM"), Times.Once);
    }

    [Fact]
    public void Handle_RemoveStatus_CallsRemove()
    {
        var store = new Mock<IPricingStore>();
        var sut = Build(store.Object);
        var message = new PriceMessage("SYM", [], DataStatus.Remove);

        sut.Handle(message);

        store.Verify(s => s.Remove("SYM"), Times.Once);
    }

    [Fact]
    public void Handle_ActiveStatus_DoesNotCallReset()
    {
        var store = new Mock<IPricingStore>();
        var sut = Build(store.Object);
        var message = new PriceMessage("SYM", [(1, "72")], DataStatus.Active);

        sut.Handle(message);

        store.Verify(s => s.Reset(It.IsAny<string>()), Times.Never);
    }

    [Fact]
    public void Handle_ActiveStatus_DoesNotCallRemove()
    {
        var store = new Mock<IPricingStore>();
        var sut = Build(store.Object);
        var message = new PriceMessage("SYM", [(1, "72")], DataStatus.Active);

        sut.Handle(message);

        store.Verify(s => s.Remove(It.IsAny<string>()), Times.Never);
    }

    [Fact]
    public void Handle_ResetStatus_DoesNotCallUpdate()
    {
        var store = new Mock<IPricingStore>();
        var sut = Build(store.Object);
        var message = new PriceMessage("SYM", [], DataStatus.Reset);

        sut.Handle(message);

        store.Verify(s => s.Update(It.IsAny<PricingUpdate>()), Times.Never);
    }

    [Fact]
    public void Handle_RemoveStatus_DoesNotCallUpdate()
    {
        var store = new Mock<IPricingStore>();
        var sut = Build(store.Object);
        var message = new PriceMessage("SYM", [], DataStatus.Remove);

        sut.Handle(message);

        store.Verify(s => s.Update(It.IsAny<PricingUpdate>()), Times.Never);
    }
}
