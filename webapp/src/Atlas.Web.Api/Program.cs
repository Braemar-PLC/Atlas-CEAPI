using Atlas.Web.Api.Configuration;
using Atlas.Web.Api.Factories;
using Atlas.Web.Api.Services;
using Atlas.Web.Core.Application.Ports;
using Atlas.Web.Core.Application.Services;
using Atlas.Web.Core.Domain.Logic;
using Atlas.Web.Ice;
using Atlas.Web.Ice.Adapters;
using Atlas.Web.Ice.Configuration;
using Atlas.Web.Ice.Domain.Services;
using Atlas.Web.Ice.WebSocket;

var builder = WebApplication.CreateBuilder(args);

builder.Services.AddControllers();
builder.Services.AddEndpointsApiExplorer();
builder.Services.AddSwaggerGen();

ConfigureDI(builder.Services);
ConfigureOptions(builder.Services, builder.Configuration);

var app = builder.Build();

if (app.Environment.IsDevelopment())
{
    app.UseSwagger();
    app.UseSwaggerUI();
}

app.UseHttpsRedirection();
app.UseAuthorization();
app.MapControllers();
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
}

static void ConfigureOptions(IServiceCollection services, IConfiguration configuration)
{
    services.AddOptions<IceOptions>()
        .Bind(configuration.GetSection(IceOptions.Section))
        .Validate(o => !string.IsNullOrEmpty(o.Endpoint), "Atlas.Web.Ice:Endpoint is required")
        .Validate(o => o.Port > 0, "Atlas.Web.Ice:Port is required")
        .ValidateOnStart();

    services.AddOptions<SseOptions>()
        .Bind(configuration.GetSection(SseOptions.Section))
        .Validate(o => o.HeartbeatInterval > TimeSpan.Zero, "Atlas.Web.Api:HeartbeatInterval is required")
        .ValidateOnStart();
}