using System.Text.Json;
using Atlas.Web.Core.Application.Ports;
using Microsoft.Extensions.Logging;

namespace Atlas.Web.Ice.Domain.Services;

/// <summary>
/// One line from the relay in, one price message to the store out: parse, interpret, hand on.
/// A line that fails at any step is logged and dropped, never thrown: an exception here would reach
/// <see cref="Adapters.IceReceiver"/>, which would close the WebSocket, and the relay then disconnects from ICE
/// - so one bad line would cost a full ICE reconnect (Known problems #3 in the root notes, fixed 2026-09-25).
/// </summary>
public sealed class IceMessageProcessor : IIceMessageProcessor
{
    private readonly IInboundParser _parser;
    private readonly IIceInterpreter _interpreter;
    private readonly IPriceMessageHandler _handler;
    private readonly ILogger<IceMessageProcessor> _logger;

    public IceMessageProcessor(
        IInboundParser parser,
        IIceInterpreter interpreter,
        IPriceMessageHandler handler,
        ILogger<IceMessageProcessor> logger)
    {
        _parser = parser;
        _interpreter = interpreter;
        _handler = handler;
        _logger = logger;
    }

    public void Process(string line)
    {
        try
        {
            using var doc = JsonDocument.Parse(line);
            if (!_parser.TryParse(doc.RootElement, out var envelope))
            {
                return;
            }
            var message = _interpreter.Interpret(envelope);
            if (message is not null)
            {
                _handler.Handle(message);
            }
        }
        catch (Exception e)
        {
            // Deliberately caught and logged rather than propagated: the connection must outlive a bad line.
            // The line itself is in the log so the fault can be reproduced.
            _logger.LogError(e, "A line from the feed relay could not be processed and was dropped: {Line}", line);
        }
    }
}
