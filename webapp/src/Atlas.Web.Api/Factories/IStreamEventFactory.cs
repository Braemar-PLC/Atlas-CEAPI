using Atlas.Web.Api.Models.Outbound;
using Atlas.Web.Core.Domain.Models;

namespace Atlas.Web.Api.Factories;

public interface IStreamEventFactory
{
    StreamEvent<PricingStateDto> Create(PricingSnapshot snapshot);
}
