using System.Text.Json;

namespace Atlas.Web.Ice;

public interface IInboundParser
{
    bool TryParse(JsonElement payload, out IceEnvelope envelope);
}
