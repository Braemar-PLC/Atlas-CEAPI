using Atlas.Web.Core.Application.Ports;

namespace Atlas.Web.Core.Tests.Domain.Services;

public sealed class FakeSymbolListParser : ISymbolListParser
{
    public IEnumerable<string> SplitSymbols(string symbols)
    {
        return symbols
            .Split(',', StringSplitOptions.RemoveEmptyEntries)
            .Select(s => s.Trim())
            .ToList();
    }

    public bool IsValid(IEnumerable<string> symbols)
    {
        return true; // domain tests do not care about validity
    }
}
