namespace Atlas.Web.Ice.Domain.Enumeration;

/// <summary>
/// The price fields the API itself reads out of an ICE record, by ICE's field number. Everything else passes
/// through to the browser untouched; the field list the grid uses is in .claude/CLAUDE.md, "Wire formats".
/// </summary>
public enum IceQuoteFieldIds
{
    Last = 19,
    Bid = 20,
    Ask = 21,
    Settlement = 273
}
