using Atlas.Web.Core.Application.Ports;
using Atlas.Web.Core.Domain.Enumeration;
using Atlas.Web.Core.Domain.Models;
using System.Collections.Concurrent;
using System.Reactive.Linq;
using System.Reactive.Subjects;

namespace Atlas.Web.Core.Domain.Logic;

public sealed class PricingStore : IPricingStore, IDisposable
{
    private readonly ConcurrentDictionary<string, BehaviorSubject<PricingSnapshot>> _subjects = new();
    private readonly object _writeLock = new();
    private readonly ISymbolListParser _symbolListParser;

    public PricingStore(ISymbolListParser symbolListParser)
    {
        _symbolListParser = symbolListParser;
    }

    public void Update(PricingUpdate update)
    {
        lock (_writeLock)
        {
            var subject = GetOrCreateSubject(update.Symbol);
            var current = subject.Value;
            var merged = new Dictionary<int, string>(current.Fields);
            foreach (var (fieldId, value) in update.Fields)
            {
                merged[fieldId] = value;
            }
            subject.OnNext(new PricingSnapshot(update.Symbol, merged, DataStatus.Active));
        }
    }

    public void Reset(string symbol)
    {
        lock (_writeLock)
        {
            if (!_subjects.TryGetValue(symbol, out var subject))
            {
                return;
            }
            subject.OnNext(new PricingSnapshot(symbol, new Dictionary<int, string>(), DataStatus.Reset));
        }
    }

    public void Remove(string symbol)
    {
        lock (_writeLock)
        {
            if (!_subjects.TryRemove(symbol, out var subject))
            {
                return;
            }

            subject.OnNext(new PricingSnapshot(symbol, new Dictionary<int, string>(), DataStatus.Remove));

            // OnCompleted and Dispose are called on this symbol's subject only.
            // All other symbols are unaffected.
            subject.OnCompleted();
            subject.Dispose();

        }
    }

    private IObservable<PricingSnapshot> GetStreamSingle(string symbol)
    {
        // AsObservable() wraps the BehaviorSubject to prevent callers
        // from casting back and calling OnNext directly, bypassing the store's merge logic.
        return GetOrCreateSubject(symbol).AsObservable();
    }

    public IObservable<PricingSnapshot> GetStream(string symbols)
    {
        var symbolList = _symbolListParser.SplitSymbols(symbols);

        return symbolList
           .Select(symbol => GetStreamSingle(symbol))
           .Merge();
    }


    public PricingSnapshot CurrentSnapshot(string symbol)
    {
        if (_subjects.TryGetValue(symbol, out var subject))
        {
            return subject.Value;
        }
        return new PricingSnapshot(symbol, new Dictionary<int, string>(), DataStatus.Active);
    }

    public void Dispose()
    {
        foreach (var subject in _subjects.Values)
        {
            subject.OnCompleted();
        }
    }

    private BehaviorSubject<PricingSnapshot> GetOrCreateSubject(string symbol)
    {
        return _subjects.GetOrAdd(
            symbol,
            s => new BehaviorSubject<PricingSnapshot>(
                new PricingSnapshot(s, new Dictionary<int, string>(), DataStatus.Active)
            )
        );
    }
}
