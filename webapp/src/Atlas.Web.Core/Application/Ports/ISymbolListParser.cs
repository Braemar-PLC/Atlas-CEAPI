
namespace Atlas.Web.Core.Application.Ports
{
    public interface ISymbolListParser
    {
        bool IsValid(IEnumerable<string> symbols);

        IEnumerable<string> SplitSymbols(string symbols);
    }
}
