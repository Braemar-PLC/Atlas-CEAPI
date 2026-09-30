using Atlas.Web.Api.Models.Outbound;
using Atlas.Web.Api.Services;
using Atlas.Web.Core.Application.Ports;
using Atlas.Web.Core.Domain.Models;
using Microsoft.AspNetCore.Mvc;
using System.Reactive.Linq;

namespace Atlas.Web.Api.Controllers;

[ApiController]
[Route("api/feed")]
public sealed class FeedController : ControllerBase
{
    private readonly IFeedHealthStore _health;
    private readonly ISseWriterFactory _sseFactory;

    public FeedController(IFeedHealthStore health, ISseWriterFactory sseFactory)
    {
        _health = health;
        _sseFactory = sseFactory;
    }

    /// <summary>Returns the current ICE feed connection and freshness state.</summary>
    [HttpGet("status")]
    [ProducesResponseType<FeedHealthResponse>(StatusCodes.Status200OK)]
    public ActionResult<FeedHealthResponse> Status(CancellationToken ct)
    {
        ct.ThrowIfCancellationRequested();
        return Ok(ToResponse(_health.Current()));
    }

    /// <summary>Streams ICE feed health changes and periodic freshness checks.</summary>
    [HttpGet("status/stream")]
    public async Task Stream(CancellationToken ct)
    {
        var changes = _health.GetStream()
            .Merge(Observable.Interval(TimeSpan.FromSeconds(1)).Select(_ => _health.Current()))
            .Select(ToResponse)
            .DistinctUntilChanged();

        await _sseFactory.Create().StreamAsync(Response, changes, ct, "feed-status");
    }

    private static FeedHealthResponse ToResponse(FeedHealthSnapshot snapshot) => new(
        snapshot.State.ToString(),
        snapshot.Generation,
        snapshot.RelayTimestamp,
        snapshot.LastQuoteAt,
        snapshot.Detail,
        snapshot.SubscribedSymbols);
}
