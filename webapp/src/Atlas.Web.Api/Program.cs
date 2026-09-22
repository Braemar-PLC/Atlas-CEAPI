using Atlas.Web.Api.Configuration;
using Atlas.Web.Api.Factories;
using Atlas.Web.Api.Services;
using Atlas.Web.Core.Application.Ports;
using Atlas.Web.Core.Application.Services;
using Atlas.Web.Core.Domain.Logic;
using Atlas.Web.Core.Domain.Models;
using Atlas.Web.Ice;
using Atlas.Web.Ice.Adapters;
using Atlas.Web.Ice.Catalogue;
using Atlas.Web.Ice.Configuration;
using Atlas.Web.Ice.Domain.Services;
using Atlas.Web.Ice.WebSocket;
using Microsoft.AspNetCore.HttpOverrides;
using Microsoft.Extensions.Options;

var builder = WebApplication.CreateBuilder(args);

builder.Services.AddControllers();
builder.Services.AddEndpointsApiExplorer();
builder.Services.AddSwaggerGen();

// Azure App Service terminates TLS at its front end and forwards plain HTTP to the container, so without this
// the app would see every request as HTTP and UseHttpsRedirection below would redirect-loop. App Service's edge
// isn't a fixed, known proxy address, so the known-network/proxy allow-lists are cleared to trust its headers.
builder.Services.Configure<ForwardedHeadersOptions>(options =>
{
    options.ForwardedHeaders = ForwardedHeaders.XForwardedFor | ForwardedHeaders.XForwardedProto;
    options.KnownNetworks.Clear();
    options.KnownProxies.Clear();
});

ConfigureDI(builder.Services);
ConfigureOptions(builder.Services, builder.Configuration);

var app = builder.Build();

if (app.Environment.IsDevelopment())
{
    app.UseSwagger();
    app.UseSwaggerUI();
}

app.UseForwardedHeaders();
app.UseHttpsRedirection();

// Serve the built frontend SPA (frontend/webapp/dist, copied into wwwroot/ at build time) from this same
// App Service, so the browser and the API share one origin with no CORS/proxy setup required.
app.UseDefaultFiles();
app.UseStaticFiles();

app.UseAuthorization();
app.MapControllers();

// Any request that isn't an API route or a static asset falls back to index.html so TanStack Router can
// handle client-side routes (e.g. /admin, /natgas) on a full page load or refresh.
app.MapFallbackToFile("index.html");

app.Run();

static void ConfigureDI(IServiceCollection services)
{
    services.AddSingleton<IPricingStore, PricingStore>();
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
    // date give the rows of each screen. Core knows nothing about configuration, so the pieces are joined here.
    services.AddSingleton<IInstrumentCatalogue>(_ => JsonInstrumentCatalogue.LoadEmbedded());
    services.AddSingleton<IScreenProvider>(sp => new ScreenProvider(
        new ScreenBuilder(
            sp.GetRequiredService<IInstrumentCatalogue>(),
            sp.GetRequiredService<IOptions<ScreenRules>>().Value),
        TimeProvider.System));
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

    // Without a month count every screen would be silently empty, so refuse to start instead.
    services.AddOptions<ScreenRules>()
        .Bind(configuration.GetSection(ScreenRules.Section))
        .Validate(o => o.Flat.Months > 0, $"{ScreenRules.Section}:Flat:Months is required")
        .ValidateOnStart();
}