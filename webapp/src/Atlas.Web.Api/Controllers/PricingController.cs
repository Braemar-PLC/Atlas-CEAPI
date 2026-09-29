using Atlas.Web.Api.Factories;
using Atlas.Web.Api.Models;
using Atlas.Web.Api.Services;
using Atlas.Web.Api.Authorization;
using Atlas.Web.Core.Application.Ports;
using Atlas.Web.Core.Domain.Models;
using Microsoft.AspNetCore.Mvc;
using System.Reactive.Linq;

namespace Atlas.Web.Api.Controllers;

[ApiController]
[Route("api/pricing")]
public sealed class PricingController : ControllerBase
{
    private readonly IPricingStore _store;
    private readonly IScreenProvider _screens;
    private readonly IDeskService _desks;
    private readonly ICurrentUser _currentUser;
    private readonly ISseWriterFactory _sseFactory;
    private readonly IStreamEventFactory _eventFactory;

    public PricingController(
        IPricingStore store,
        IScreenProvider screens,
        IDeskService desks,
        ICurrentUser currentUser,
        ISseWriterFactory sseFactory,
        IStreamEventFactory eventFactory)
    {
        _store = store;
        _screens = screens;
        _desks = desks;
        _currentUser = currentUser;
        _sseFactory = sseFactory;
        _eventFactory = eventFactory;
    }

    [HttpGet("stream")]
    public async Task<IActionResult> Stream([FromQuery] string symbol, CancellationToken ct)
    {
        var user = _currentUser.Get();
        if (user is null)
        {
            return Unauthorized();
        }
        if (!user.IsAdmin && !await CanAccessSymbolAsync(symbol, user, ct))
        {
            return Forbid();
        }

        var writer = _sseFactory.Create();
        var events = _store
            .GetStream(symbol)
            .Select(_eventFactory.Create);
        await writer.StreamAsync(Response, events, ct);
        return new EmptyResult();
    }

    private async Task<bool> CanAccessSymbolAsync(string symbol, CurrentUser user, CancellationToken ct)
    {
        var accessibleDesks = await _desks.ForUserAsync(user, ct);
        var deskKeys = accessibleDesks.Select(desk => desk.Key).ToHashSet(StringComparer.OrdinalIgnoreCase);

        foreach (var screen in _screens.GetScreens())
        {
            if (screen.Symbols.Contains(symbol, StringComparer.Ordinal)
                && ScreenDeskMap.TryGetDeskKey(screen.Key, out var deskKey)
                && deskKeys.Contains(deskKey))
            {
                return true;
            }
        }

        return false;
    }
}
