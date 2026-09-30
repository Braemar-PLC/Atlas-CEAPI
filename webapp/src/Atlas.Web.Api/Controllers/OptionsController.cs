using Atlas.Web.Api.Authorization;
using Atlas.Web.Api.Mappers;
using Atlas.Web.Api.Models.Outbound;
using Atlas.Web.Core.Application.Ports;
using Microsoft.AspNetCore.Mvc;

namespace Atlas.Web.Api.Controllers;

/// <summary>
/// The options calculator's matrix: for a product, every expiry the desk's rules pick and every strike ICE lists
/// at it. Prices come from the product's chain screen (GET /api/screens/xcom-{product}/stream), not from here.
/// Known products: TTF, EUA, WTI, Brent. Only admins and members of the Cross-Commodities desk may read it.
/// </summary>
[ApiController]
[Route("api/options")]
public sealed class OptionsController : ControllerBase
{
    private readonly IOptionMatrixProvider _matrices;
    private readonly IDeskService _desks;
    private readonly ICurrentUser _currentUser;

    public OptionsController(IOptionMatrixProvider matrices, IDeskService desks, ICurrentUser currentUser)
    {
        _matrices = matrices;
        _desks = desks;
        _currentUser = currentUser;
    }

    /// <summary>One product's matrix, or 404 if there are no option rules for that product.</summary>
    [HttpGet("{product}")]
    public async Task<ActionResult<OptionMatrixDto>> Get(string product, CancellationToken ct)
    {
        var user = _currentUser.Get();
        if (user is null)
        {
            return Unauthorized();
        }
        if (!user.IsAdmin && !await _desks.CanAccessAsync(ScreenDeskMap.CrossCommodities, user, ct))
        {
            return Forbid();
        }

        var matrix = _matrices.Find(product);
        if (matrix is null)
        {
            return NotFound($"There are no options rules for '{product}'.");
        }
        return OptionMatrixMapper.ToDto(matrix);
    }
}