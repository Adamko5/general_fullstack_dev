# Implemented

## **Central auth service** (**Spring Authorization Server + OpenID Connect**)
   - **SSO**
     - Authorization Code + PKCE
   - **WebAuthn**
     - password and passkey login options
   - **Resource-level authorization**
     - **OpenFGA** based access to **S3 resources**
     - CSRF protection

## Storage service (S3)

   - **Spring MVC**
     - xx
   - **WebFlux** + **Reactor**

# TODO

2. **frontend variations**
    - to explore frontend variations so i can try multiple designs
    - default framework is Next.js
      - wont use Next.js functionalities except just the next server that actually starts the React app.
    - enum of possible topics:
      - professional, clean, practical SaaS design
      - immersive design for a visual experience (something like web to present a new apartment complex + select apartments and such)
      - View Transitions for the floor plan
      - streaming UI (React Server Components)
3. **backend variations**
    - to explore backend architectures (default stack is Kotlin + Spring)
    - ways of setting up Spring projects and tools used for stuff (like controller advice or whatever)
    - maybe different languages + frameworks like Express?
    - enum of possible topics:
      - DDD
      - Hexagonal
      - BfF on Next server (Kotlin backend with like 2 endpoints + Next Server BfF that combines the result + frontend)
      - Layered with Express (typescript)
      - Layered with Scala
4. complex across stack (frontend + backend)
    - to explore design patterns, standard app features, behaviors...
    - enum of possible topics:
      - slugs
      - frontend caching
      - gRPC endpoints
      - various API request parameters/bodies/whatever content
      - debouncing of inputs
      - idempotency keys
      - offline collaboration (CRDTs)
      - SQLite in the browser (WASM)
      - server-driven forms (Kotlin JSON + React)
      - durable workflows (timed holds)
      - ...
5. docker compose so someone viewing this can run it locally
