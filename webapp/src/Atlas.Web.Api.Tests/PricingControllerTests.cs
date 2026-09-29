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
        IStreamEventFactory? eventFactory = null,
        CurrentUser? user = null,
        IScreenProvider? screens = null,
        IDeskService? desks = null)
    {
        eventFactory ??= new Mock<IStreamEventFactory>().Object;
        user ??= new CurrentUser("admin@braemar.com", "Atlas Admin", true);
        var currentUser = new Mock<ICurrentUser>();
        currentUser.Setup(service => service.Get()).Returns(user);
        var controller = new PricingController(
            store,
            screens ?? new Mock<IScreenProvider>().Object,
            desks ?? new Mock<IDeskService>().Object,
            currentUser.Object,
            sseFactory,
            eventFactory);
        controller.ControllerContext = new ControllerContext { HttpContext = new DefaultHttpContext() };
        return controller;
    }

    private static Mock<ISseWriterFactory> FactoryThatCompletes()
    {
        var writer = new Mock<ISseWriter>();
        writer.Setup(w => w.StreamAsync(
            It.IsAny<HttpResponse>(),
            It.IsAny<IObservable<StreamEvent<PricingStateDto>>>(),
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
    public async Task Stream_PassesRequestCancellationTokenToWriter()
    {
        // The writer ends the stream when this token is cancelled (the client disconnected); see SseWriterTests.
        var store = StoreReturning("SYM", new Subject<PricingSnapshot>());
        var writer = new Mock<ISseWriter>();
        writer.Setup(w => w.StreamAsync(
            It.IsAny<HttpResponse>(),
            It.IsAny<IObservable<StreamEvent<PricingStateDto>>>(),
            It.IsAny<CancellationToken>()))
            .Returns(Task.CompletedTask);
        var sseFactory = new Mock<ISseWriterFactory>();
        sseFactory.Setup(f => f.Create()).Returns(writer.Object);
        using var cts = new CancellationTokenSource();
        var sut = BuildController(store.Object, sseFactory.Object);

        await sut.Stream("SYM", cts.Token);

        writer.Verify(w => w.StreamAsync(
            It.IsAny<HttpResponse>(),
            It.IsAny<IObservable<StreamEvent<PricingStateDto>>>(),
            cts.Token));
    }

    [Fact]
    public async Task Stream_RegularUserCannotAccessSymbolFromAnotherDesk()
    {
        var symbol = "API2 26V-ICN";
        var screen = new Screen("coal-api2", "Coal API2", new[]
        {
            new ScreenRow("ARA", "Oct26", "Months", RowSource.Quoted, symbol, null, null),
        });
        var screens = new Mock<IScreenProvider>();
        screens.Setup(provider => provider.GetScreens()).Returns(new[] { screen });
        var desks = new Mock<IDeskService>();
        desks.Setup(service => service.ForUserAsync(
                It.IsAny<CurrentUser>(),
                It.IsAny<CancellationToken>()))
            .ReturnsAsync(new[] { new Desk("natural-gas", "Natural Gas", "") });
        var factory = FactoryThatCompletes();
        var user = new CurrentUser("member@braemar.com", "Member", false);
        var sut = BuildController(
            new Mock<IPricingStore>().Object,
            factory.Object,
            user: user,
            screens: screens.Object,
            desks: desks.Object);

        var result = await sut.Stream(symbol, CancellationToken.None);

        result.Should().BeOfType<ForbidResult>();
        factory.Verify(service => service.Create(), Times.Never);
    }
}
