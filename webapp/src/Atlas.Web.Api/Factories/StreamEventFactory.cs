using Atlas.Web.Api.Enumeration;
using Atlas.Web.Api.Mappers;
using Atlas.Web.Api.Models.Outbound;
using Atlas.Web.Core.Domain.Enumeration;
using Atlas.Web.Core.Domain.Models;

namespace Atlas.Web.Api.Factories;

public sealed class StreamEventFactory : IStreamEventFactory
{
    public StreamEvent<PricingStateDto> Create(PricingSnapshot snapshot)
    {
        var action = snapshot.Status switch
        {
            DataStatus.Active => PricingEventActions.Update,
            DataStatus.Reset => PricingEventActions.Reset,
            DataStatus.Remove => PricingEventActions.Remove,
            _ => PricingEventActions.Update
        };

        var metadata = new StreamEventMetadata(
            Key: snapshot.Symbol,
            Type: StreamEventTypes.Pricing,
            Action: action,
            ServerTimestamp: DateTime.UtcNow);

        var data = PricingMapper.ToDto(snapshot);

        return new StreamEvent<PricingStateDto>(metadata, data);
    }
}
