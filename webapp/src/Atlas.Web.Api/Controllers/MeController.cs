using Atlas.Web.Api.Mappers;
using Atlas.Web.Api.Models.Outbound;
using Atlas.Web.Core.Application.Ports;
using Microsoft.AspNetCore.Mvc;

namespace Atlas.Web.Api.Controllers;

/// <summary>The signed-in user, as the app needs them: name, desks, where to land, whether they are an admin.</summary>
[ApiController]
[Route("api/me")]
public sealed class MeController : ControllerBase
{
    private readonly IMeService _me;

    public MeController(IMeService me)
    {
        _me = me;
    }

    /// <summary>Who is signed in, or 401 when nobody is.</summary>
    [HttpGet]
    public async Task<ActionResult<MeDto>> Get(CancellationToken ct)
    {
        var me = await _me.GetAsync(ct);
        if (me is null)
        {
            return Unauthorized();
        }
        return MeMapper.ToDto(me);
    }
}
