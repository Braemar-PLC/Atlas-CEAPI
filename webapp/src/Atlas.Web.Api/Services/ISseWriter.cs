namespace Atlas.Web.Api.Services;

public interface ISseWriter
{
    Task StreamAsync<T>(HttpResponse response, IAsyncEnumerable<T> items, CancellationToken ct);
}
