using Atlas.Web.Api.Factories;
using Atlas.Web.Api.Mappers;
using Atlas.Web.Api.Models.Outbound;
using Atlas.Web.Api.Services;
using Atlas.Web.Api.Authorization;
using Atlas.Web.Core.Application.Ports;
using Atlas.Web.Core.Domain.Models;
using Microsoft.AspNetCore.Authorization;
using Microsoft.AspNetCore.Http;
using Microsoft.AspNetCore.Mvc;
using System.Reactive.Linq;

namespace Atlas.Web.Api.Controllers;

/// <summary>
/// The desk screens: which rows each one shows today, and a price stream for exactly those rows.
/// Known screens: ttf-flat, ttf-spreads, nbp, coal-api2, coal-newcastle, coal-spreads.
/// </summary>
[ApiController]
[Route("api/screens")]
public sealed class ScreensController : ControllerBase
{
    private readonly IScreenProvider _screens;
    private readonly IDeskService _desks;
    private readonly ICurrentUser _currentUser;
    private readonly IPricingStore _store;
    private readonly ISseWriterFactory _sseFactory;
    private readonly IStreamEventFactory _eventFactory;

    public ScreensController(
        IScreenProvider screens,
        IDeskService desks,
        ICurrentUser currentUser,
        IPricingStore store,
        ISseWriterFactory sseFactory,
        IStreamEventFactory eventFactory)
    {
        _screens = screens;
        _desks = desks;
        _currentUser = currentUser;
        _store = store;
        _sseFactory = sseFactory;
        _eventFactory = eventFactory;
    }

    /// <summary>Every screen the signed-in user may access.</summary>
    [HttpGet]
    public async Task<ActionResult<IReadOnlyList<ScreenDto>>> GetAll(CancellationToken ct)
    {
        var user = _currentUser.Get();
        if (user is null)
        {
            return Unauthorized();
        }

        var screens = _screens.GetScreens();
        if (!user.IsAdmin)
        {
            var deskKeys = (await _desks.ForUserAsync(user, ct))
                .Select(desk => desk.Key)
                .ToHashSet(StringComparer.OrdinalIgnoreCase);
            screens = screens
                .Where(screen => ScreenDeskMap.TryGetDeskKey(screen.Key, out var deskKey) && deskKeys.Contains(deskKey))
                .ToList();
        }

        return screens.Select(ScreenMapper.ToDto).ToList();
    }

    /// <summary>One screen's rows, or 404 if there is no screen with that key.</summary>
    [HttpGet("{key}")]
    public async Task<ActionResult<ScreenDto>> Get(string key, CancellationToken ct)
    {
        var user = _currentUser.Get();
        if (user is null)
        {
            return Unauthorized();
        }

        var screen = _screens.Find(key);
        if (screen is null)
        {
            return NotFound($"There is no screen called '{key}'.");
        }
        if (!await CanAccessScreenAsync(screen.Key, user, ct))
        {
            return Forbid();
        }
        return ScreenMapper.ToDto(screen);
    }

    /// <summary>
    /// Every symbol the feed relay (CEAPI) should subscribe to, comma-separated, as plain text - the exact format
    /// of CEAPI's SYMBOLS setting. CEAPI/run-live.ps1 reads this so the list is never typed by hand - without a
    /// browser or a sign-in, which is why this one endpoint stays open at the API level.
    /// </summary>
    [HttpGet("symbols")]
    [AllowAnonymous]
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
        var user = _currentUser.Get();
        if (user is null)
        {
            Response.StatusCode = StatusCodes.Status401Unauthorized;
            return;
        }

        var screen = _screens.Find(key);
        if (screen is null)
        {
            Response.StatusCode = StatusCodes.Status404NotFound;
            return;
        }

        if (!await CanAccessScreenAsync(screen.Key, user, ct))
        {
            Response.StatusCode = StatusCodes.Status403Forbidden;
            return;
        }

        var writer = _sseFactory.Create();
        var events = _store
            .GetStream(string.Join(",", screen.Symbols))
            .Select(_eventFactory.Create);
        await writer.StreamAsync(Response, events, ct);
    }

    private async Task<bool> CanAccessScreenAsync(string screenKey, CurrentUser user, CancellationToken ct)
    {
        if (user.IsAdmin)
        {
            return true;
        }

        return ScreenDeskMap.TryGetDeskKey(screenKey, out var deskKey)
            && await _desks.CanAccessAsync(deskKey, user, ct);
    }
}
