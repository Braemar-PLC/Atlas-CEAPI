using Atlas.Web.Core.Application.Ports;
using Atlas.Web.Ice.Configuration;
using Microsoft.Extensions.Options;

namespace Atlas.Web.Core.Domain.Logic
{
    public class IceSymbolListParser : ISymbolListParser
    {
        private readonly IceOptions _options;
        private readonly IScreenProvider? _screens;

        // `screens` is optional so the parser still works on its own (older tests build it with options only).
        // The API always supplies it.
        public IceSymbolListParser(IOptions<IceOptions> options, IScreenProvider? screens = null)
        {
            _options = options.Value;
            _screens = screens;
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

        /// <summary>
        /// A symbol may be asked for if a desk screen needs it (worked out by rule, so it rolls by itself) or if it
        /// has been added by hand to Ice:Symbols in appsettings.json.
        /// </summary>
        public bool IsValid(IEnumerable<string> symbols)
        {
            var allowed = new HashSet<string>(_options.Symbols);
            if (_screens is not null)
            {
                allowed.UnionWith(_screens.SymbolsToSubscribe());
            }
            return symbols.All(allowed.Contains);
        }
    }
}
