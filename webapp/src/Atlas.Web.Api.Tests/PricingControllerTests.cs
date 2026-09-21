using System.Reactive.Linq;
using System.Reactive.Subjects;
using FluentAssertions;
using Atlas.Web.Api.Controllers;
using Atlas.Web.Api.Factories;
using Atlas.Web.Api.Services;
using Atlas.Web.Core.Application.Ports;
using Atlas.Web.Core.Domain.Enumeration;
using Atlas.Web.Core.Domain.Models;
using Microsoft.AspNetCore.Http;
using Microsoft.AspNetCore.Mvc;
using Moq;
using Atlas.Web.Api.Models.Outbound;

namespace Atlas.Web.Api.Tests;

public class PricingControllerTests
{
    private static PricingController BuildController(
        IPricingStore store,
        ISseWriterFactory sseFactory,
        IStreamEventFactory? eventFactory = null)
    {
        eventFactory ??= new Mock<IStreamEventFactory>().Object;
        var controller = new PricingController(store, sseFactory, eventFactory);
        controller.ControllerContext = new ControllerContext { HttpContext = new DefaultHttpContext() };
        return controller;
    }

    private static Mock<ISseWriterFactory> FactoryThatCompletes()
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

    private static Mock<IPricingStore> StoreReturning(string symbol, IObservable<PricingSnapshot> observable)
    {
        var store = new Mock<IPricingStore>(MockBehavior.Strict);
        store.Setup(s => s.GetStream(symbol)).Returns(observable);
        return store;
    }

    [Fact]
    public async Task Stream_DelegatesToSseWriter()
    {
        var store = StoreReturning("SYM", Observable.Empty<PricingSnapshot>());
        var factory = FactoryThatCompletes();
        var sut = BuildController(store.Object, factory.Object);
        await sut.Stream("SYM", CancellationToken.None);
        factory.Verify(f => f.Create(), Times.Once);
    }

    [Fact]
    public async Task Stream_CallsGetStream_WithCorrectSymbol()
    {
        var store = StoreReturning("EURUSD", Observable.Empty<PricingSnapshot>());
        var factory = FactoryThatCompletes();
        var sut = BuildController(store.Object, factory.Object);
        await sut.Stream("EURUSD", CancellationToken.None);
        store.Verify(s => s.GetStream("EURUSD"), Times.Once);
    }

    [Fact]
    public async Task Stream_CreatesNewWriterPerCall()
    {
        var store = StoreReturning("SYM", Observable.Empty<PricingSnapshot>());
        var factory = FactoryThatCompletes();
        var sut = BuildController(store.Object, factory.Object);
        await sut.Stream("SYM", CancellationToken.None);
        await sut.Stream("SYM", CancellationToken.None);
        factory.Verify(f => f.Create(), Times.Exactly(2));
    }

    [Fact]
    public async Task Stream_Cancellation_ThrowsOperationCanceledException()
    {
        var store = StoreReturning("SYM", new Subject<PricingSnapshot>());
        var writer = new Mock<ISseWriter>();
        writer.Setup(w => w.StreamAsync(
            It.IsAny<HttpResponse>(),
            It.IsAny<IAsyncEnumerable<StreamEvent<PricingStateDto>>>(),
            It.IsAny<CancellationToken>()))
            .Returns<HttpResponse, IAsyncEnumerable<StreamEvent<PricingStateDto>>, CancellationToken>(
                async (_, items, ct) => { await foreach (var __ in items.WithCancellation(ct)) { } });
        var sseFactory = new Mock<ISseWriterFactory>();
        sseFactory.Setup(f => f.Create()).Returns(writer.Object);
        var cts = new CancellationTokenSource();
        cts.Cancel();
        var sut = BuildController(store.Object, sseFactory.Object);
        var act = async () => await sut.Stream("SYM", cts.Token);
        await act.Should().ThrowAsync<OperationCanceledException>();
    }
}
