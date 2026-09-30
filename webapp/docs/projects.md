# .NET projects

    Atlas.Web.Api ──► Atlas.Web.Ice ──► Atlas.Web.Core
          └────────────────────────────────►┘
    Atlas.Web.Data (desks and members in SQLite; referenced by Api)

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

## Atlas.Web.Data

Desks and their members in a SQLite file through EF Core: the entities, the migrations, the repositories. Owns its
entity classes; Core's records stay free of EF.

(A mock feed project that stood in for CEAPI was removed on 2026-09-25.)
