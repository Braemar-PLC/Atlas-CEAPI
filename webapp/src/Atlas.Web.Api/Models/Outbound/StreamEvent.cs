namespace Atlas.Web.Api.Models.Outbound;

public sealed record StreamEvent<T>(
    StreamEventMetadata Metadata,
    T Data
);
