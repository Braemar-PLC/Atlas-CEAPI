using Atlas.Web.Api.Factories;
using Atlas.Web.Api.Models;
using Atlas.Web.Api.Services;
using Atlas.Web.Core.Application.Ports;
using Microsoft.AspNetCore.Mvc;
using System.Reactive.Linq;

namespace Atlas.Web.Api.Controllers;

[ApiController]
[Route("api/pricing")]
public sealed class PricingController : ControllerBase
{
    private readonly IPricingStore _store;
    private readonly ISseWriterFactory _sseFactory;
    private readonly IStreamEventFactory _eventFactory;

    public PricingController(IPricingStore store, ISseWriterFactory sseFactory, IStreamEventFactory eventFactory)
    {
        _store = store;
        _sseFactory = sseFactory;
        _eventFactory = eventFactory;
    }

    [HttpGet("stream")]
    public async Task Stream([FromQuery] string symbol, CancellationToken ct)
    {
        var writer = _sseFactory.Create();
        var events = _store
            .GetStream(symbol)
            .Select(_eventFactory.Create)
            .ToAsyncEnumerable();
        await writer.StreamAsync(Response, events, ct);
    }
}
