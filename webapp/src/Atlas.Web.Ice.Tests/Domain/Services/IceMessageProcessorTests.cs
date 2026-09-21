using System.Text.Json;
using Atlas.Web.Core.Application.Models;
using Atlas.Web.Core.Application.Ports;
using Atlas.Web.Ice.Domain.Services;
using FluentAssertions;
using Moq;

namespace Atlas.Web.Ice.Tests.Domain.Services;

public class IceMessageProcessorTests
{
    private static IceMessageProcessor Build(
        IInboundParser? parser = null,
        IIceInterpreter? interpreter = null,
        IPriceMessageHandler? handler = null) =>
        new(
            parser ?? new Mock<IInboundParser>().Object,
            interpreter ?? new Mock<IIceInterpreter>().Object,
            handler ?? new Mock<IPriceMessageHandler>().Object);

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

    [Fact]
    public void Process_WhenParserThrows_DoesNotThrow()
    {
        throw new NotImplementedException();
        // var parser = new Mock<IInboundParser>();
        // IceEnvelope dummy = default!;
        // parser.Setup(p => p.TryParse(It.IsAny<JsonElement>(), out dummy)).Throws<Exception>();
        // var sut = Build(parser: parser.Object);
        // var act = () => sut.Process(ValidLine);
        // act.Should().NotThrow();
    }

    [Fact]
    public void Process_WhenInterpreterThrows_DoesNotThrow()
    {
        throw new NotImplementedException();
        // var envelope = new IceEnvelope("TFM 26J-ICN", [(19, "64.665")]);
        // var parser = new Mock<IInboundParser>();
        // parser.Setup(p => p.TryParse(It.IsAny<JsonElement>(), out envelope)).Returns(true);
        // var interpreter = new Mock<IIceInterpreter>();
        // interpreter.Setup(i => i.Interpret(It.IsAny<IceEnvelope>())).Throws<Exception>();
        // var sut = Build(parser: parser.Object, interpreter: interpreter.Object);
        // var act = () => sut.Process(ValidLine);
        // act.Should().NotThrow();
    }

    [Fact]
    public void Process_WhenHandlerThrows_DoesNotThrow()
    {
        throw new NotImplementedException();
        // var envelope = new IceEnvelope("TFM 26J-ICN", [(19, "64.665")]);
        // var message = new PriceMessage("TFM 26J-ICN", [(19, "64.665")]);
        // var parser = new Mock<IInboundParser>();
        // parser.Setup(p => p.TryParse(It.IsAny<JsonElement>(), out envelope)).Returns(true);
        // var interpreter = new Mock<IIceInterpreter>();
        // interpreter.Setup(i => i.Interpret(envelope)).Returns(message);
        // var handler = new Mock<IPriceMessageHandler>();
        // handler.Setup(h => h.Handle(It.IsAny<PriceMessage>())).Throws<Exception>();
        // var sut = Build(parser: parser.Object, interpreter: interpreter.Object, handler: handler.Object);
        // var act = () => sut.Process(ValidLine);
        // act.Should().NotThrow();
    }

    [Fact]
    public void Process_InvalidJson_DoesNotThrow()
    {
        throw new NotImplementedException();
        // var sut = Build();
        // var act = () => sut.Process("this is not json");
        // act.Should().NotThrow();
    }
}
