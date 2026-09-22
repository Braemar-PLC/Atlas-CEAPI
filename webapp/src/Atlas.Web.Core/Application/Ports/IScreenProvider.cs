using Atlas.Web.Core.Domain.Models;

namespace Atlas.Web.Core.Application.Ports;

/// <summary>The desk screens as they stand today: which rows, in which order, and what each row needs from the feed.</summary>
public interface IScreenProvider
{
    /// <summary>All screens, built for today's date.</summary>
    IReadOnlyList<Screen> GetScreens();

    /// <summary>One screen by key ("ttf-flat", "ttf-spreads", "nbp"), or null if there is no such screen.</summary>
    Screen? Find(string key);

    /// <summary>
    /// Every symbol the feed relay should subscribe to: what the screens need today, plus what they will need
    /// after the next expiry - so a relay started today still has the right symbols once a strip rolls off.
    /// </summary>
    IReadOnlyList<string> SymbolsToSubscribe();
}
