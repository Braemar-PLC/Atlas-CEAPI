using Atlas.Web.Core.Application.Ports;
using Atlas.Web.Ice.Configuration;
using Microsoft.Extensions.Options;

namespace Atlas.Web.Core.Domain.Logic
{
    public class IceSymbolListParser : ISymbolListParser
    {
        private readonly IceOptions _options;
        public IceSymbolListParser(IOptions<IceOptions> options)
        {
            _options = options.Value;
        }

        public IEnumerable<string> SplitSymbols(string symbols)
        {
            var rtn = symbols
                .Split(',', StringSplitOptions.RemoveEmptyEntries)
                .Select(s => s.Replace("\"", "").Trim())
                .Where(s => s.Length > 0)
                .ToList();


            if (!IsValid(rtn))
            {
                throw new Exception("");
            }
            return rtn;
        }

        public bool IsValid(IEnumerable<string> symbols)
        {
            return symbols.All(_options.Symbols.Contains);
        }
    }
}
