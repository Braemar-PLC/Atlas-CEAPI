using System.Text.Json;
using Atlas.Web.Core.Application.Ports;

namespace Atlas.Web.Ice.Domain.Services;

public sealed class IceMessageProcessor : IIceMessageProcessor
{
    private readonly IInboundParser _parser;
    private readonly IIceInterpreter _interpreter;
    private readonly IPriceMessageHandler _handler;

    public IceMessageProcessor(
        IInboundParser parser,
        IIceInterpreter interpreter,
        IPriceMessageHandler handler)
    {
        _parser = parser;
        _interpreter = interpreter;
        _handler = handler;
    }

    public void Process(string line)
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
}
