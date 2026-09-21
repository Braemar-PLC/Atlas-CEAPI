using Atlas.Web.Core.Application.Models;
using Atlas.Web.Core.Domain.Enumeration;
using Atlas.Web.Ice.Domain.Enumeration;

namespace Atlas.Web.Ice.Adapters;

public sealed class IceInterpreter : IIceInterpreter
{
    private static readonly HashSet<int> MetaFieldIds =
        Enum.GetValues<IceResponseMetaFieldIds>().Select(e => (int)e).ToHashSet();

    private static readonly int RecordResetFieldId = (int)IceResponseMetaFieldIds.RecordReset;

    public PriceMessage? Interpret(IceEnvelope envelope)
    {
        if (IsReset(envelope))
        {
            return new PriceMessage(envelope.Symbol, [], DataStatus.Reset);
        }

        var pricingFields = envelope.Fields
            .Where(f => !MetaFieldIds.Contains(f.FieldId))
            .ToList();

        if (!pricingFields.Any())
        {
            return null;
        }

        return new PriceMessage(envelope.Symbol, pricingFields, DataStatus.Active);
    }

    private static bool IsReset(IceEnvelope envelope) =>
        envelope.Fields.Any(f => f.FieldId == RecordResetFieldId && f.Value == "0");
}
