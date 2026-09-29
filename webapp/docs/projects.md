# .NET projects

    Atlas.Web.Api ──► Atlas.Web.Ice ──► Atlas.Web.Core
          └────────────────────────────────►┘
    Atlas.Web.Ice.MockFeed (stands in for CEAPI; not referenced by the others)

## Atlas.Web.Core

The domain. Holds the current price of every subscribed contract and builds the desk screens.

- Responsible for: price state and its updates, the desk-screen rules, which symbols may be requested, the ports
  (interfaces) the other projects implement.
- Knows nothing of ICE, WebSockets, HTTP or JSON. References no other project.

## Atlas.Web.Ice

The adapter to CEAPI, the Java relay over ICE.

- Responsible for: the WebSocket connection to CEAPI, CEAPI's wire format, ICE field numbers and their translation
  into Core's terms, ICE's contract catalogue.
- The only project that knows about ICE.

## Atlas.Web.Api

The HTTP surface and composition root.

- Responsible for: HTTP endpoints and server-sent events, reading and validating requests, the outbound JSON
  contract, configuration and wiring the other projects together.
- Holds no domain logic and knows nothing of ICE's format.

## Atlas.Web.Ice.MockFeed

A development stand-in for CEAPI. Speaks CEAPI's wire format on its own port, so the API cannot tell it from the
real feed.
