using System.Text.Json;

namespace Atlas.Web.Ice.Adapters;

public sealed class IceParser : IInboundParser
{
    public bool TryParse(JsonElement root, out IceEnvelope envelope)
    {
        envelope = default!;
        try
        {
            if (root.ValueKind != JsonValueKind.Array || root.GetArrayLength() < 3)
                return false;

            // "refresh" is the full snapshot CEAPI sends on subscribe; "update" is a delta.
            // Both carry the same payload, and PricingStore merges fields, so they parse alike.
            var messageType = root[0].GetString();
            if (!string.Equals(messageType, "update", StringComparison.OrdinalIgnoreCase) &&
                !string.Equals(messageType, "refresh", StringComparison.OrdinalIgnoreCase))
                return false;

            var symbol = root[1].GetString();
            if (string.IsNullOrEmpty(symbol))
                return false;

            var fieldsElement = root[2];
            if (fieldsElement.ValueKind != JsonValueKind.Array)
                return false;

            var fields = new List<(int FieldId, string Value)>();
            foreach (var pair in fieldsElement.EnumerateArray())
            {
                if (pair.ValueKind != JsonValueKind.Array || pair.GetArrayLength() < 2)
                    return false;

                fields.Add((pair[0].GetInt32(), pair[1].ToString()));
            }

            envelope = new IceEnvelope(symbol, fields);
            return true;
        }
        catch
        {
            return false;
        }
    }
}