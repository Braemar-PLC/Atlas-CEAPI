namespace Atlas.Web.Api.Services;

public interface ISseWriter
{
    Task StreamAsync<T>(HttpResponse response, IObservable<T> items, CancellationToken ct);
}
