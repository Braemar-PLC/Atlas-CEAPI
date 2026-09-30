using System.Globalization;
using System.Reflection;
using System.Text.Json;
using Atlas.Web.Core.Application.Ports;
using Atlas.Web.Core.Domain.Enumeration;
using Atlas.Web.Core.Domain.Models;

namespace Atlas.Web.Ice.Catalogue;

/// <summary>
/// ICE's contract list, read from ice-instruments.json (the futures and their spreads) and ice-options.json
/// (the monthly options the options desk prices).
///
/// Both files are built into this assembly (an "embedded resource"), so they are always found, wherever the API
/// or a test runs from. They are generated from ICE's reference files by tools/ice-catalogue/build-catalogue.mjs -
/// never edit them by hand; re-run the script when ICE sends fresh files. The options file is the larger of the
/// two and most callers never need it, so it is read on first use.
/// </summary>
public sealed class JsonInstrumentCatalogue : IInstrumentCatalogue
{
    private const string ResourceName = "Atlas.Web.Ice.Catalogue.ice-instruments.json";
    private const string OptionsResourceName = "Atlas.Web.Ice.Catalogue.ice-options.json";

    private readonly Lazy<IReadOnlyList<OptionInstrument>> _options;

    public IReadOnlyList<Instrument> Outrights { get; }
    public IReadOnlyList<SpreadInstrument> Spreads { get; }
    public IReadOnlyList<OptionInstrument> Options => _options.Value;

    /// <summary>The date of the ICE files the catalogue was built from.</summary>
    public DateOnly AsOf { get; }

    private JsonInstrumentCatalogue(
        DateOnly asOf,
        IReadOnlyList<Instrument> outrights,
        IReadOnlyList<SpreadInstrument> spreads,
        Lazy<IReadOnlyList<OptionInstrument>> options)
    {
        AsOf = asOf;
        Outrights = outrights;
        Spreads = spreads;
        _options = options;
    }

    public static JsonInstrumentCatalogue LoadEmbedded()
    {
        using var stream = OpenEmbedded(ResourceName);
        return Load(stream, () => OpenEmbedded(OptionsResourceName));
    }

    /// <param name="json">The futures file.</param>
    /// <param name="openOptions">Opens the options file when it is first asked for; null means there are no options.</param>
    public static JsonInstrumentCatalogue Load(Stream json, Func<Stream>? openOptions = null)
    {
        var file = JsonSerializer.Deserialize<CatalogueFile>(json, SerializerOptions)
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

        var options = new Lazy<IReadOnlyList<OptionInstrument>>(() =>
        {
            if (openOptions is null)
            {
                return new List<OptionInstrument>();
            }
            using var stream = openOptions();
            return LoadOptions(stream);
        });

        return new JsonInstrumentCatalogue(ParseDate(file.AsOf), outrights, spreads, options);
    }

    public static IReadOnlyList<OptionInstrument> LoadOptions(Stream json)
    {
        var file = JsonSerializer.Deserialize<OptionsFile>(json, SerializerOptions)
            ?? throw new InvalidOperationException("The options catalogue is empty.");

        return file.Options
            .Select(o => new OptionInstrument(
                o.Product, Enum.Parse<StripKind>(o.Kind), o.Expiry, ParseDate(o.ExpiryDate), o.Underlying, o.Strike,
                ParseRight(o.Right), o.Symbol))
            .ToList();
    }

    private static readonly JsonSerializerOptions SerializerOptions = new() { PropertyNameCaseInsensitive = true };

    private static Stream OpenEmbedded(string resourceName) =>
        Assembly.GetExecutingAssembly().GetManifestResourceStream(resourceName)
            ?? throw new InvalidOperationException(
                $"Embedded resource '{resourceName}' is missing. Check the EmbeddedResource entries in Atlas.Web.Ice.csproj.");

    private static DateOnly ParseDate(string value) =>
        DateOnly.ParseExact(value, "yyyy-MM-dd", CultureInfo.InvariantCulture);

    private static OptionRight ParseRight(string value) => value switch
    {
        "C" => OptionRight.Call,
        "P" => OptionRight.Put,
        _ => throw new InvalidOperationException($"'{value}' is not an option right; expected C or P."),
    };

    // The shape of the two files. Private: nothing outside this class should depend on the file layout.
    private sealed record CatalogueFile(string AsOf, List<OutrightEntry> Outrights, List<SpreadEntry> Spreads);
    private sealed record OutrightEntry(string Hub, string Kind, string Name, string Symbol, string Start, string Expiry);
    private sealed record SpreadEntry(string Hub, string Name, string Near, string Far, string Symbol, string Expiry);
    private sealed record OptionsFile(string AsOf, List<OptionEntry> Options);
    private sealed record OptionEntry(
        string Product, string Kind, string Expiry, string ExpiryDate, string Underlying, decimal Strike, string Right, string Symbol);
}
