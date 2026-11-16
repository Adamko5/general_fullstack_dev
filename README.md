# Overview

The point of this repo is to practice everything regarding full stack development.

# Goals

1. Implement auth
2. Implement many variations of backends to learn cool features of Spring
3. Implement various designs of the same frontend to learn cool frontend design patterns (generic app with a header, footer and content in between)

# Contents

1. **custom auth implementation** (JWT tokens: access + refresh)
    - refresh token rotation + revocation
    - passkeys (WebAuthn)
    - resource-level authorization + CSRF protection
      - OpenFGA based access to S3 resources
      - this also means, there will be NO DATABASE data and access - the DB is only for auth management, not for the app's logic or resources
2. **frontend variations**
    - to explore frontend variations so i can try multiple designs
    - default framework is Next.js
      - might use Next server with hardcoded data as an API for the frontend
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
