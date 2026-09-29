using Atlas.Web.Api.Models.Outbound;
using Atlas.Web.Core.Application.Models;

namespace Atlas.Web.Api.Mappers;

public static class MeMapper
{
    public static MeDto ToDto(Me me) =>
        new(me.Name, me.Email, me.IsAdmin, me.Desks.Select(DeskMapper.ToDto).ToList(), me.HomeDeskKey);
}
