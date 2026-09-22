using Atlas.Web.Api.Factories;
using Atlas.Web.Api.Mappers;
using Atlas.Web.Api.Models.Outbound;
using Atlas.Web.Api.Services;
using Atlas.Web.Core.Application.Ports;
using Microsoft.AspNetCore.Mvc;
using System.Reactive.Linq;

namespace Atlas.Web.Api.Controllers;

/// <summary>
/// The desk screens: which rows each one shows today, and a price stream for exactly those rows.
/// Known screens: ttf-flat, ttf-spreads, nbp.
/// </summary>
[ApiController]
[Route("api/screens")]
public sealed class ScreensController : ControllerBase
{
    private readonly IScreenProvider _screens;
    private readonly IPricingStore _store;
    private readonly ISseWriterFactory _sseFactory;
    private readonly IStreamEventFactory _eventFactory;

    public ScreensController(
        IScreenProvider screens,
        IPricingStore store,
        ISseWriterFactory sseFactory,
        IStreamEventFactory eventFactory)
    {
        _screens = screens;
        _store = store;
        _sseFactory = sseFactory;
        _eventFactory = eventFactory;
    }

    /// <summary>Every screen with its rows.</summary>
    [HttpGet]
    public IReadOnlyList<ScreenDto> GetAll()
    {
        return _screens.GetScreens().Select(ScreenMapper.ToDto).ToList();
    }

    /// <summary>One screen's rows, or 404 if there is no screen with that key.</summary>
    [HttpGet("{key}")]
    public ActionResult<ScreenDto> Get(string key)
    {
        var screen = _screens.Find(key);
        if (screen is null)
        {
            return NotFound($"There is no screen called '{key}'.");
        }
        return ScreenMapper.ToDto(screen);
    }

    /// <summary>
    /// Every symbol the feed relay (CEAPI) should subscribe to, comma-separated, as plain text - the exact format
    /// of CEAPI's SYMBOLS setting. CEAPI/run-live.ps1 reads this so the list is never typed by hand.
    /// </summary>
    [HttpGet("symbols")]
    public ContentResult Symbols()
    {
        return Content(string.Join(",", _screens.SymbolsToSubscribe()), "text/plain");
    }

    /// <summary>
    /// Server-sent events for every symbol a screen needs, including the legs of worked-out spreads.
    /// Same event format as GET /api/pricing/stream; asking by screen keeps the address short however many rows
    /// the screen has.
    /// </summary>
    [HttpGet("{key}/stream")]
    public async Task Stream(string key, CancellationToken ct)
    {
        var screen = _screens.Find(key);
        if (screen is null)
        {
            Response.StatusCode = StatusCodes.Status404NotFound;
            return;
        }

        var writer = _sseFactory.Create();
        var events = _store
            .GetStream(string.Join(",", screen.Symbols))
            .Select(_eventFactory.Create)
            .ToAsyncEnumerable();
        await writer.StreamAsync(Response, events, ct);
    }
}
