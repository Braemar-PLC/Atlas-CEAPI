using Atlas.Web.Core.Application.Models;
using Atlas.Web.Ice;

namespace Atlas.Web.Ice;

/// <summary>
/// Interprets an IceEnvelope — reads ICE-specific metadata flags,
/// filters pricing fields, and produces a normalised PriceMessage for Core.
/// Returns null when the message should be ignored entirely.
/// </summary>
public interface IIceInterpreter
{
    PriceMessage? Interpret(IceEnvelope envelope);
}
