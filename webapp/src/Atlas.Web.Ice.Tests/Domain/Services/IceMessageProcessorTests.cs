using System.Text.Json;
using Atlas.Web.Core.Application.Models;
using Atlas.Web.Core.Application.Ports;
using Atlas.Web.Ice.Domain.Services;
using FluentAssertions;
using Microsoft.Extensions.Logging;
using Microsoft.Extensions.Logging.Abstractions;
using Moq;

namespace Atlas.Web.Ice.Tests.Domain.Services;

public class IceMessageProcessorTests
{
    /// <summary>Keeps what was logged, so a test can check a fault was reported and not swallowed.</summary>
    private sealed class CapturingLogger : ILogger<IceMessageProcessor>
    {
        public List<(LogLevel Level, string Message, Exception? Exception)> Entries { get; } = new();
        public IDisposable? BeginScope<TState>(TState state) where TState : notnull => null;
        public bool IsEnabled(LogLevel logLevel) => true;
        public void Log<TState>(LogLevel logLevel, EventId eventId, TState state, Exception? exception, Func<TState, Exception?, string> formatter) =>
            Entries.Add((logLevel, formatter(state, exception), exception));
    }

    private static IceMessageProcessor Build(
        IInboundParser? parser = null,
        IIceInterpreter? interpreter = null,
        IPriceMessageHandler? handler = null,
        ILogger<IceMessageProcessor>? logger = null) =>
        new(
            parser ?? new Mock<IInboundParser>().Object,
            interpreter ?? new Mock<IIceInterpreter>().Object,
            handler ?? new Mock<IPriceMessageHandler>().Object,
            logger ?? NullLogger<IceMessageProcessor>.Instance);

    private static readonly string ValidLine =
        """["update","TFM 26J-ICN",[[1,"72"],[19,"64.665"]]]""";

    [Fact]
    public void Process_ValidLine_CallsParser()
    {
        var parser = new Mock<IInboundParser>();
        IceEnvelope dummy = default!;
        parser.Setup(p => p.TryParse(It.IsAny<JsonElement>(), out dummy)).Returns(false);

        var sut = Build(parser: parser.Object);
        sut.Process(ValidLine);

        parser.Verify(p => p.TryParse(It.IsAny<JsonElement>(), out dummy), Times.Once);
    }

    [Fact]
    public void Process_WhenParserSucceeds_CallsInterpreter()
    {
        var envelope = new IceEnvelope("TFM 26J-ICN", [(19, "64.665")]);
        var parser = new Mock<IInboundParser>();
        parser.Setup(p => p.TryParse(It.IsAny<JsonElement>(), out envelope)).Returns(true);

        var interpreter = new Mock<IIceInterpreter>();
        interpreter.Setup(i => i.Interpret(envelope))
            .Returns(new PriceMessage("TFM 26J-ICN", [(19, "64.665")]));

        var sut = Build(parser: parser.Object, interpreter: interpreter.Object);
        sut.Process(ValidLine);

        interpreter.Verify(i => i.Interpret(envelope), Times.Once);
    }

    [Fact]
    public void Process_WhenParserReturnsFalse_DoesNotCallInterpreter()
    {
        var parser = new Mock<IInboundParser>();
        IceEnvelope dummy = default!;
        parser.Setup(p => p.TryParse(It.IsAny<JsonElement>(), out dummy)).Returns(false);

        var interpreter = new Mock<IIceInterpreter>();

        var sut = Build(parser: parser.Object, interpreter: interpreter.Object);
        sut.Process(ValidLine);

        interpreter.Verify(i => i.Interpret(It.IsAny<IceEnvelope>()), Times.Never);
    }

    [Fact]
    public void Process_WhenInterpreterReturnsMessage_CallsHandler()
    {
        var envelope = new IceEnvelope("TFM 26J-ICN", [(19, "64.665")]);
        var message = new PriceMessage("TFM 26J-ICN", [(19, "64.665")]);

        var parser = new Mock<IInboundParser>();
        parser.Setup(p => p.TryParse(It.IsAny<JsonElement>(), out envelope)).Returns(true);

        var interpreter = new Mock<IIceInterpreter>();
        interpreter.Setup(i => i.Interpret(envelope)).Returns(message);

        var handler = new Mock<IPriceMessageHandler>();

        var sut = Build(parser: parser.Object, interpreter: interpreter.Object, handler: handler.Object);
        sut.Process(ValidLine);

        handler.Verify(h => h.Handle(message), Times.Once);
    }

    [Fact]
    public void Process_WhenInterpreterReturnsNull_DoesNotCallHandler()
    {
        var envelope = new IceEnvelope("TFM 26J-ICN", [(330, "metadata only")]);
        var parser = new Mock<IInboundParser>();
        parser.Setup(p => p.TryParse(It.IsAny<JsonElement>(), out envelope)).Returns(true);

        var interpreter = new Mock<IIceInterpreter>();
        interpreter.Setup(i => i.Interpret(It.IsAny<IceEnvelope>())).Returns((PriceMessage?)null);

        var handler = new Mock<IPriceMessageHandler>();

        var sut = Build(parser: parser.Object, interpreter: interpreter.Object, handler: handler.Object);
        sut.Process(ValidLine);

        handler.Verify(h => h.Handle(It.IsAny<PriceMessage>()), Times.Never);
    }

    // One bad line must never reach IceReceiver as an exception: that drops the relay's connection, and the relay
    // then drops ICE, so every bad line would cost a full ICE reconnect (root notes, Known problems #3).

    [Fact]
    public void Process_WhenParserThrows_DoesNotThrow()
    {
        var parser = new Mock<IInboundParser>();
        IceEnvelope dummy = default!;
        parser.Setup(p => p.TryParse(It.IsAny<JsonElement>(), out dummy)).Throws<Exception>();
        var sut = Build(parser: parser.Object);

        var act = () => sut.Process(ValidLine);

        act.Should().NotThrow();
    }

    [Fact]
    public void Process_WhenInterpreterThrows_DoesNotThrow()
    {
        var envelope = new IceEnvelope("TFM 26J-ICN", [(19, "64.665")]);
        var parser = new Mock<IInboundParser>();
        parser.Setup(p => p.TryParse(It.IsAny<JsonElement>(), out envelope)).Returns(true);
        var interpreter = new Mock<IIceInterpreter>();
        interpreter.Setup(i => i.Interpret(It.IsAny<IceEnvelope>())).Throws<Exception>();
        var sut = Build(parser: parser.Object, interpreter: interpreter.Object);

        var act = () => sut.Process(ValidLine);

        act.Should().NotThrow();
    }

    [Fact]
    public void Process_WhenHandlerThrows_DoesNotThrow()
    {
        var envelope = new IceEnvelope("TFM 26J-ICN", [(19, "64.665")]);
        var message = new PriceMessage("TFM 26J-ICN", [(19, "64.665")]);
        var parser = new Mock<IInboundParser>();
        parser.Setup(p => p.TryParse(It.IsAny<JsonElement>(), out envelope)).Returns(true);
        var interpreter = new Mock<IIceInterpreter>();
        interpreter.Setup(i => i.Interpret(envelope)).Returns(message);
        var handler = new Mock<IPriceMessageHandler>();
        handler.Setup(h => h.Handle(It.IsAny<PriceMessage>())).Throws<Exception>();
        var sut = Build(parser: parser.Object, interpreter: interpreter.Object, handler: handler.Object);

        var act = () => sut.Process(ValidLine);

        act.Should().NotThrow();
    }

    [Fact]
    public void Process_InvalidJson_DoesNotThrow()
    {
        var sut = Build();

        var act = () => sut.Process("this is not json");

        act.Should().NotThrow();
    }

    [Fact]
    public void Process_ABadLine_IsLoggedAsAnErrorWithTheLine_NotSwallowed()
    {
        var logger = new CapturingLogger();
        var sut = Build(logger: logger);

        sut.Process("this is not json");

        var entry = logger.Entries.Should().ContainSingle().Subject;
        entry.Level.Should().Be(LogLevel.Error);
        entry.Message.Should().Contain("this is not json");
        entry.Exception.Should().BeAssignableTo<JsonException>();
    }

    [Fact]
    public void Process_AfterABadLine_StillHandlesTheNextGoodOne()
    {
        var envelope = new IceEnvelope("TFM 26J-ICN", [(19, "64.665")]);
        var message = new PriceMessage("TFM 26J-ICN", [(19, "64.665")]);
        var parser = new Mock<IInboundParser>();
        parser.Setup(p => p.TryParse(It.IsAny<JsonElement>(), out envelope)).Returns(true);
        var interpreter = new Mock<IIceInterpreter>();
        interpreter.Setup(i => i.Interpret(envelope)).Returns(message);
        var handler = new Mock<IPriceMessageHandler>();
        var sut = Build(parser: parser.Object, interpreter: interpreter.Object, handler: handler.Object);

        sut.Process("this is not json");
        sut.Process(ValidLine);

        handler.Verify(h => h.Handle(message), Times.Once);
    }
}
