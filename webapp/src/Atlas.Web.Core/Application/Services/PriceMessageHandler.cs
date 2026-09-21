using Atlas.Web.Core.Application.Models;
using Atlas.Web.Core.Application.Ports;
using Atlas.Web.Core.Domain.Enumeration;
using Atlas.Web.Core.Domain.Models;

namespace Atlas.Web.Core.Application.Services;

public sealed class PriceMessageHandler : IPriceMessageHandler
{
    private readonly IPricingStore _store;

    public PriceMessageHandler(IPricingStore store)
    {
        _store = store;
    }

    public void Handle(PriceMessage message)
    {
        switch (message.Status)
        {
            case DataStatus.Active:
                _store.Update(new PricingUpdate(message.Symbol, message.Fields));
                break;
            case DataStatus.Reset:
                _store.Reset(message.Symbol);
                break;
            case DataStatus.Remove:
                _store.Remove(message.Symbol);
                break;

            default:
                throw new ArgumentOutOfRangeException(nameof(message.Status), message.Status, "Unexpected status value.");

        }
    }
}
