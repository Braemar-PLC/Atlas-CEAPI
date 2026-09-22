using System.Globalization;
using System.Reflection;
using System.Text.Json;
using Atlas.Web.Core.Application.Ports;
using Atlas.Web.Core.Domain.Enumeration;
using Atlas.Web.Core.Domain.Models;

namespace Atlas.Web.Ice.Catalogue;

/// <summary>
/// ICE's list of TTF and NBP gas contracts, read from ice-instruments.json.
///
/// That file is built into this assembly (an "embedded resource"), so it is always found, wherever the API or a
/// test runs from. It is generated from ICE's reference files by tools/ice-catalogue/build-catalogue.mjs -
/// never edit it by hand; re-run the script when ICE sends fresh files.
/// </summary>
public sealed class JsonInstrumentCatalogue : IInstrumentCatalogue
{
    private const string ResourceName = "Atlas.Web.Ice.Catalogue.ice-instruments.json";

    public IReadOnlyList<Instrument> Outrights { get; }
    public IReadOnlyList<SpreadInstrument> Spreads { get; }

    /// <summary>The date of the ICE files the catalogue was built from.</summary>
    public DateOnly AsOf { get; }

    private JsonInstrumentCatalogue(DateOnly asOf, IReadOnlyList<Instrument> outrights, IReadOnlyList<SpreadInstrument> spreads)
    {
        AsOf = asOf;
        Outrights = outrights;
        Spreads = spreads;
    }

    public static JsonInstrumentCatalogue LoadEmbedded()
    {
        using var stream = Assembly.GetExecutingAssembly().GetManifestResourceStream(ResourceName)
            ?? throw new InvalidOperationException(
                $"Embedded resource '{ResourceName}' is missing. Check the EmbeddedResource entry in Atlas.Web.Ice.csproj.");
        return Load(stream);
    }

    public static JsonInstrumentCatalogue Load(Stream json)
    {
        var options = new JsonSerializerOptions { PropertyNameCaseInsensitive = true };
        var file = JsonSerializer.Deserialize<CatalogueFile>(json, options)
            ?? throw new InvalidOperationException("The instrument catalogue is empty.");

        var outrights = file.Outrights
            .Select(o => new Instrument(
                o.Hub,
                Enum.Parse<StripKind>(o.Kind),
                o.Name,
                o.Symbol,
                // "2026-10" - the first delivery month. Day 1 makes it a date we can sort by.
                DateOnly.ParseExact(o.Start + "-01", "yyyy-MM-dd", CultureInfo.InvariantCulture),
                ParseDate(o.Expiry)))
            .ToList();

        var spreads = file.Spreads
            .Select(s => new SpreadInstrument(s.Hub, s.Name, s.Near, s.Far, s.Symbol, ParseDate(s.Expiry)))
            .ToList();

        return new JsonInstrumentCatalogue(ParseDate(file.AsOf), outrights, spreads);
    }

    private static DateOnly ParseDate(string value) =>
        DateOnly.ParseExact(value, "yyyy-MM-dd", CultureInfo.InvariantCulture);

    // The shape of ice-instruments.json. Private: nothing outside this class should depend on the file layout.
    private sealed record CatalogueFile(string AsOf, List<OutrightEntry> Outrights, List<SpreadEntry> Spreads);
    private sealed record OutrightEntry(string Hub, string Kind, string Name, string Symbol, string Start, string Expiry);
    private sealed record SpreadEntry(string Hub, string Name, string Near, string Far, string Symbol, string Expiry);
}
