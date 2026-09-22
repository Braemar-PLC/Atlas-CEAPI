using System.Reactive.Linq;
using Atlas.Web.Api.Controllers;
using Atlas.Web.Api.Factories;
using Atlas.Web.Api.Models.Outbound;
using Atlas.Web.Api.Services;
using Atlas.Web.Core.Application.Ports;
using Atlas.Web.Core.Domain.Enumeration;
using Atlas.Web.Core.Domain.Models;
using FluentAssertions;
using Microsoft.AspNetCore.Http;
using Microsoft.AspNetCore.Mvc;
using Moq;

namespace Atlas.Web.Api.Tests;

public class ScreensControllerTests
{
    private static readonly Screen Flat = new("ttf-flat", "Nat Gas TTF Flat Price", new[]
    {
        new ScreenRow("TTF", "Oct26", "Months", RowSource.Quoted, "TFM 26V-ICN", null, null),
        new ScreenRow("TTF", "Oct26/Q1 27", "Spreads", RowSource.Computed, null,
            new ScreenLeg("Oct26", "TFM 26V-ICN"), new ScreenLeg("Q1 27", "TFMQ 27F-ICN")),
    });

    private static Mock<IScreenProvider> Screens()
    {
        var screens = new Mock<IScreenProvider>();
        screens.Setup(s => s.GetScreens()).Returns(new[] { Flat });
        screens.Setup(s => s.Find("ttf-flat")).Returns(Flat);
        screens.Setup(s => s.SymbolsToSubscribe()).Returns(new[] { "TFM 26V-ICN", "TFM 26V:TFM26X-ICN" });
        return screens;
    }

    private static Mock<ISseWriterFactory> WriterFactory()
    {
        var writer = new Mock<ISseWriter>();
        writer.Setup(w => w.StreamAsync(
                It.IsAny<HttpResponse>(),
                It.IsAny<IAsyncEnumerable<StreamEvent<PricingStateDto>>>(),
                It.IsAny<CancellationToken>()))
            .Returns(Task.CompletedTask);
        var factory = new Mock<ISseWriterFactory>();
        factory.Setup(f => f.Create()).Returns(writer.Object);
        return factory;
    }

    private static ScreensController Controller(
        Mock<IScreenProvider>? screens = null,
        Mock<IPricingStore>? store = null,
        Mock<ISseWriterFactory>? sse = null)
    {
        var controller = new ScreensController(
            (screens ?? Screens()).Object,
            (store ?? new Mock<IPricingStore>()).Object,
            (sse ?? WriterFactory()).Object,
            new Mock<IStreamEventFactory>().Object);
        controller.ControllerContext = new ControllerContext { HttpContext = new DefaultHttpContext() };
        return controller;
    }

    [Fact]
    public void Get_KnownScreen_ReturnsItsRowsInOrder()
    {
        var result = Controller().Get("ttf-flat");

        result.Value!.Title.Should().Be("Nat Gas TTF Flat Price");
        result.Value.Rows.Select(r => r.Label).Should().Equal("Oct26", "Oct26/Q1 27");
    }

    [Fact]
    public void Get_QuotedRow_CarriesItsSymbolAndTheWordQuoted()
    {
        var row = Controller().Get("ttf-flat").Value!.Rows[0];

        row.Source.Should().Be("quoted");
        row.Symbol.Should().Be("TFM 26V-ICN");
        row.Near.Should().BeNull();
    }

    [Fact]
    public void Get_ComputedRow_CarriesBothLegsAndNoSymbol()
    {
        var row = Controller().Get("ttf-flat").Value!.Rows[1];

        row.Source.Should().Be("computed");
        row.Symbol.Should().BeNull();
        row.Near.Should().Be(new ScreenLegDto("Oct26", "TFM 26V-ICN"));
        row.Far.Should().Be(new ScreenLegDto("Q1 27", "TFMQ 27F-ICN"));
    }

    [Fact]
    public void Get_UnknownScreen_Returns404NamingTheScreen()
    {
        var result = Controller().Get("coal");

        result.Result.Should().BeOfType<NotFoundObjectResult>()
            .Which.Value.Should().Be("There is no screen called 'coal'.");
    }

    [Fact]
    public void GetAll_ReturnsEveryScreen()
    {
        Controller().GetAll().Select(s => s.Key).Should().Equal("ttf-flat");
    }

    [Fact]
    public void Symbols_ReturnsOneCommaSeparatedLineOfPlainText_AsCeapiExpects()
    {
        var result = Controller().Symbols();

        result.ContentType.Should().Be("text/plain");
        result.Content.Should().Be("TFM 26V-ICN,TFM 26V:TFM26X-ICN");
    }

    [Fact]
    public async Task Stream_KnownScreen_StreamsItsSymbolsIncludingTheLegsOfComputedRows()
    {
        var store = new Mock<IPricingStore>();
        store.Setup(s => s.GetStream(It.IsAny<string>())).Returns(Observable.Empty<PricingSnapshot>());

        await Controller(store: store).Stream("ttf-flat", CancellationToken.None);

        store.Verify(s => s.GetStream("TFM 26V-ICN,TFMQ 27F-ICN"), Times.Once);
    }

    [Fact]
    public async Task Stream_UnknownScreen_Returns404WithoutOpeningAStream()
    {
        var sse = WriterFactory();
        var sut = Controller(sse: sse);

        await sut.Stream("coal", CancellationToken.None);

        sut.Response.StatusCode.Should().Be(StatusCodes.Status404NotFound);
        sse.Verify(f => f.Create(), Times.Never);
    }
}
