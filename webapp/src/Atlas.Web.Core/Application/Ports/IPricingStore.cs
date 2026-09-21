using Atlas.Web.Core.Domain.Models;

namespace Atlas.Web.Core.Application.Ports;

/// <summary>
/// Canonical source of truth for pricing state.
/// BehaviorSubject semantics: every subscriber immediately receives
/// the current snapshot, then all subsequent state changes.
/// </summary>
public interface IPricingStore
{
    void Update(PricingUpdate update);
    void Reset(string symbol);
    void Remove(string symbol);
    IObservable<PricingSnapshot> GetStream(string symbol);
    PricingSnapshot CurrentSnapshot(string symbol);
}
