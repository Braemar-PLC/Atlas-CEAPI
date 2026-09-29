using Atlas.Web.Api.Authentication;
using Atlas.Web.Api.Mappers;
using Atlas.Web.Api.Models.Inbound;
using Atlas.Web.Api.Models.Outbound;
using Atlas.Web.Core.Application;
using Atlas.Web.Core.Application.Ports;
using Microsoft.AspNetCore.Authorization;
using Microsoft.AspNetCore.Mvc;

namespace Atlas.Web.Api.Controllers;

/// <summary>Desks visible to members and admins; desk membership changes are admin-only.</summary>
[ApiController]
[Route("api/desks")]
public sealed class DesksController : ControllerBase
{
    private readonly IDeskService _desks;
    private readonly ICurrentUser _currentUser;

    public DesksController(IDeskService desks, ICurrentUser currentUser)
    {
        _desks = desks;
        _currentUser = currentUser;
    }

    /// <summary>Desks the signed-in user may access, in name order, without member emails.</summary>
    [HttpGet]
    public async Task<ActionResult<IReadOnlyList<DeskDto>>> GetAll(CancellationToken ct)
    {
        var user = _currentUser.Get();
        if (user is null)
        {
            return Unauthorized();
        }

        var desks = await _desks.ForUserAsync(user, ct);
        return desks.Select(DeskMapper.ToDto).ToList();
    }

    /// <summary>One desk with its members for admins; regular users cannot read other members' email addresses.</summary>
    [HttpGet("{key}")]
    public async Task<ActionResult<DeskDetailDto>> Get(string key, CancellationToken ct)
    {
        var user = _currentUser.Get();
        if (user is null)
        {
            return Unauthorized();
        }
        if (!user.IsAdmin)
        {
            return Forbid();
        }

        var desk = await _desks.FindAsync(key, ct);
        if (desk is null)
        {
            return NotFound($"There is no desk called '{key}'.");
        }
        return DeskMapper.ToDetailDto(desk);
    }

    /// <summary>Adds a desk. 409 if the key is taken; 400, with the reason, if the key, name or a member is not valid.</summary>
    [HttpPost]
    [Authorize(Policy = AuthPolicies.Admin)]
    public async Task<ActionResult<DeskDetailDto>> Create(DeskDetailDto desk, CancellationToken ct)
    {
        if (await _desks.FindAsync(desk.Key, ct) is not null)
        {
            return Conflict($"There is already a desk called '{desk.Key}'.");
        }
        try
        {
            var saved = await _desks.SaveAsync(desk.Key, desk.Name, desk.Description, desk.Members, ct);
            return CreatedAtAction(nameof(Get), new { key = saved.Desk.Key }, DeskMapper.ToDetailDto(saved));
        }
        catch (DeskValidationException e)
        {
            return BadRequest(e.Message);
        }
    }

    /// <summary>Changes a desk's name, description and members. 404 for an unknown key; 400, with the reason, for bad input.</summary>
    [HttpPut("{key}")]
    [Authorize(Policy = AuthPolicies.Admin)]
    public async Task<ActionResult<DeskDetailDto>> Update(string key, DeskWriteDto desk, CancellationToken ct)
    {
        if (await _desks.FindAsync(key, ct) is null)
        {
            return NotFound($"There is no desk called '{key}'.");
        }
        try
        {
            var saved = await _desks.SaveAsync(key, desk.Name, desk.Description, desk.Members, ct);
            return DeskMapper.ToDetailDto(saved);
        }
        catch (DeskValidationException e)
        {
            return BadRequest(e.Message);
        }
    }
}
