namespace Atlas.Web.Core.Domain.Enumeration;

/// <summary>
/// What length of delivery a strip covers. A "strip" is the desk's word for one tradable delivery period:
/// a single month (Oct26), a quarter (Q4 26), a gas season (Winter26 = October to March) or a calendar year (Cal 27).
/// </summary>
public enum StripKind
{
    Month,
    Quarter,
    Season,
    Cal
}
