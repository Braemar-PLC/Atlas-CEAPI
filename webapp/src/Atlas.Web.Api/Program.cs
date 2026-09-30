using Atlas.Web.Api.Authentication;
using Atlas.Web.Api.Configuration;
using Atlas.Web.Api.Factories;
using Atlas.Web.Api.Services;
using Atlas.Web.Core.Application.Ports;
using Atlas.Web.Core.Application.Services;
using Atlas.Web.Core.Domain.Logic;
using Atlas.Web.Core.Domain.Models;
using Atlas.Web.Data;
using Atlas.Web.Data.Configuration;
using Atlas.Web.Data.Repositories;
using Atlas.Web.Ice;
using Atlas.Web.Ice.Adapters;
using Atlas.Web.Ice.Catalogue;
using Atlas.Web.Ice.Configuration;
using Atlas.Web.Ice.Domain.Services;
using Atlas.Web.Ice.WebSocket;
using Microsoft.AspNetCore.HttpOverrides;
using Microsoft.EntityFrameworkCore;
using Microsoft.Extensions.Options;

var builder = WebApplication.CreateBuilder(args);

builder.Services.AddControllers();
builder.Services.AddEndpointsApiExplorer();
builder.Services.AddSwaggerGen();
builder.Services.Configure<ForwardedHeadersOptions>(options =>
{
    options.ForwardedHeaders = ForwardedHeaders.XForwardedFor | ForwardedHeaders.XForwardedProto;
    options.ForwardLimit = 1;
    options.KnownIPNetworks.Clear();
    options.KnownProxies.Clear();
});

ConfigureDI(builder.Services);
ConfigureOptions(builder.Services, builder.Configuration);
builder.Services.AddEasyAuth(builder.Configuration);
ConfigureDatabase(builder.Services, builder.Configuration, builder.Environment.ContentRootPath);

var app = builder.Build();

// Bring the database file up to date before anything can ask for a desk.
using (var scope = app.Services.CreateScope())
{
    await DatabaseStartup.InitialiseAsync(scope.ServiceProvider.GetRequiredService<AtlasDbContext>(), CancellationToken.None);
}

if (app.Environment.IsDevelopment())
{
    app.UseSwagger();
    app.UseSwaggerUI();
}

app.UseForwardedHeaders();
app.UseHttpsRedirection();
app.UseDefaultFiles();
app.UseStaticFiles();
app.UseAuthentication();
app.UseAuthorization();
app.MapControllers();
// The SPA shell holds no data and must load for signed-out users so it can show its sign-in page;
// API controllers remain covered by the authenticated fallback policy.
app.MapFallbackToFile("index.html").AllowAnonymous();
app.Run();

static void ConfigureDI(IServiceCollection services)
{
    services.AddSingleton<IPricingStore, PricingStore>();
    services.AddSingleton(TimeProvider.System);
    services.AddSingleton<IFeedHealthStore>(sp =>
    {
        var options = sp.GetRequiredService<IOptions<FeedHealthOptions>>().Value;
        return new FeedHealthStore(sp.GetRequiredService<TimeProvider>(), options.StaleAfter);
    });
    services.AddSingleton<IPriceMessageHandler, PriceMessageHandler>();
    services.AddSingleton<IInboundParser, IceParser>();
    services.AddSingleton<IIceInterpreter, IceInterpreter>();
    services.AddSingleton<IIceMessageProcessor, IceMessageProcessor>();
    services.AddSingleton<ISymbolListParser, IceSymbolListParser>();
    services.AddSingleton<IWebSocketClientFactory, WebSocketClientFactory>();
    services.AddHostedService<IceReceiver>();
    services.AddSingleton<ISseWriterFactory, SseWriterFactory>();
    services.AddSingleton<IStreamEventFactory, StreamEventFactory>();

    // Desk screens: ICE's contract list + the desk's counts (the "Screens" section of appsettings.json) + today's
    // date give the rows of each screen; the option chains also need the futures' prices, to pick the strikes
    // around them. Core knows nothing about configuration, so the pieces are joined here.
    services.AddSingleton<IInstrumentCatalogue>(_ => JsonInstrumentCatalogue.LoadEmbedded());
    services.AddSingleton(sp => new Lazy<IPricingStore>(sp.GetRequiredService<IPricingStore>));
    services.AddSingleton<IReferencePrices, IceReferencePrices>();
    services.AddSingleton<ScreenProvider>(sp =>
    {
        var catalogue = sp.GetRequiredService<IInstrumentCatalogue>();
        var rules = sp.GetRequiredService<IOptions<ScreenRules>>().Value;
        return new ScreenProvider(
            new ScreenBuilder(catalogue, rules),
            new OptionChainBuilder(catalogue, rules, sp.GetRequiredService<IReferencePrices>()),
            TimeProvider.System);
    });
    // The one provider answers both the screens and the calculator's matrix, so it is registered once and
    // handed out under each port.
    services.AddSingleton<IScreenProvider>(sp => sp.GetRequiredService<ScreenProvider>());
    services.AddSingleton<IOptionMatrixProvider>(sp => sp.GetRequiredService<ScreenProvider>());
}

// Desks and members live in a SQLite file; where it is comes from the "Database" section. The Data project owns the
// schema and the repository, Core owns the rules, so the pieces are joined here.
static void ConfigureDatabase(IServiceCollection services, IConfiguration configuration, string contentRoot)
{
    services.AddOptions<DatabaseOptions>()
        .Bind(configuration.GetSection(DatabaseOptions.Section))
        .Validate(o => !string.IsNullOrWhiteSpace(o.Path), $"{DatabaseOptions.Section}:Path is required")
        .ValidateOnStart();

    services.AddDbContext<AtlasDbContext>((sp, options) =>
    {
        var path = sp.GetRequiredService<IOptions<DatabaseOptions>>().Value.FullPath(contentRoot);
        options.UseSqlite(DatabaseStartup.ConnectionString(path));
    });
    services.AddScoped<IDeskRepository, EfDeskRepository>();
    services.AddScoped<IDeskService, DeskService>();
    services.AddScoped<IMeService, MeService>();
}

static void ConfigureOptions(IServiceCollection services, IConfiguration configuration)
{
    services.AddOptions<IceOptions>()
        .Bind(configuration.GetSection(IceOptions.Section))
        .Validate(o => !string.IsNullOrEmpty(o.Endpoint), $"{IceOptions.Section}:Endpoint is required")
        .Validate(o => o.Port > 0, $"{IceOptions.Section}:Port is required")
        .ValidateOnStart();

    services.AddOptions<SseOptions>()
        .Bind(configuration.GetSection(SseOptions.Section))
        .Validate(o => o.HeartbeatInterval > TimeSpan.Zero, "Atlas.Web.Api:HeartbeatInterval is required")
        .ValidateOnStart();

    services.AddOptions<FeedHealthOptions>()
        .Bind(configuration.GetSection(FeedHealthOptions.Section))
        .Validate(o => o.StaleAfter > TimeSpan.FromSeconds(5),
            $"{FeedHealthOptions.Section}:StaleAfter must exceed the five-second CEAPI heartbeat")
        .ValidateOnStart();

    // Without a month count every screen would be silently empty, so refuse to start instead.
    services.AddOptions<ScreenRules>()
        .Bind(configuration.GetSection(ScreenRules.Section))
        .Validate(o => o.Flat.Months > 0, $"{ScreenRules.Section}:Flat:Months is required")
        .Validate(o => o.Hubs.Values.All(h => h.Flat is null || h.Flat.Months > 0),
            $"{ScreenRules.Section}:Hubs:<hub>:Flat:Months must be above 0 when a hub has counts of its own")
        // An option chain with no expiries, a band narrower than what is shown, or no price to centre on until the
        // feed answers would be an empty or blank screen - refuse to start instead.
        .Validate(o => o.Options.Values.All(r =>
                r.Months + r.Quarters + r.Seasons + r.Cals > 0
                && r.StrikesEachSide >= 0 && r.SubscribeStrikesEachSide >= r.StrikesEachSide
                && r.StripStrikesEachSide >= 0 && r.SubscribeStripStrikesEachSide >= r.StripStrikesEachSide
                && r.SeedPrice > 0),
            $"{ScreenRules.Section}:Options:<product> needs at least one of Months/Quarters/Seasons/Cals above 0, the Subscribe bands at least as wide as the shown ones, and SeedPrice above 0")
        .ValidateOnStart();
}