using Atlas.Web.Api.Models.Outbound;
using Atlas.Web.Core.Domain.Models;

namespace Atlas.Web.Api.Mappers;

public static class PricingMapper
{
    public static PricingStateDto ToDto(PricingSnapshot snapshot)
    {
        return new(snapshot.Symbol, snapshot.Fields);
    }
}
