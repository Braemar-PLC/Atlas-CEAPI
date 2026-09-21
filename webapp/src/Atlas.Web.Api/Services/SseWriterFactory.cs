using Atlas.Web.Api.Configuration;
using Microsoft.Extensions.Options;

namespace Atlas.Web.Api.Services;

public sealed class SseWriterFactory : ISseWriterFactory
{
    private readonly TimeSpan _heartbeatInterval;

    public SseWriterFactory(IOptions<SseOptions> options)
    {
        _heartbeatInterval = options.Value.HeartbeatInterval;
    }

    public ISseWriter Create() => new SseWriter(_heartbeatInterval);
}
