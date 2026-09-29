using Atlas.Web.Api.Models.Outbound;
using Atlas.Web.Core.Application.Models;
using Atlas.Web.Core.Domain.Models;

namespace Atlas.Web.Api.Mappers;

public static class DeskMapper
{
    public static DeskDto ToDto(Desk desk) => new(desk.Key, desk.Name, desk.Description);

    public static DeskDetailDto ToDetailDto(DeskWithMembers desk) =>
        new(desk.Desk.Key, desk.Desk.Name, desk.Desk.Description, desk.Members);
}
