using Atlas.Web.Core.Application.Models;

namespace Atlas.Web.Core.Application.Ports;

/// <summary>
/// Receives a normalised PriceMessage and routes to the appropriate
/// store operation based on DataStatus.
/// </summary>
public interface IPriceMessageHandler
{
    void Handle(PriceMessage message);
}
