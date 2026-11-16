# Auth service

Kotlin and Spring Boot starter for the auth service. The API routes are scaffolded; auth flows are not implemented yet and the controller stubs return `501 Not Implemented`.

The auth app runs Flyway migrations against the `auth` database during startup. Add subsequent versioned SQL migrations under `src/main/resources/db/migration` (for example, `V2__add_email_verification.sql`). Hibernate schema generation is disabled so Flyway remains the schema owner. OpenFGA continues to initialize its separate database with its own migrator.

## Run the full stack

From this directory, start the auth app and its supporting services:

```sh
docker compose up --build
```

This Compose file uses the `include` feature (Docker Compose 2.20 or newer).

To start only Postgres, the S3-compatible store, and OpenFGA:

```sh
docker compose -f compose.support.yaml up
```

Local endpoints:

- Auth app: `http://localhost:8080`
- Postgres: `localhost:5432` (`auth` database)
- S3 API: `http://localhost:8333` (`auth-files` bucket)
- OpenFGA HTTP API: `http://localhost:8082`
- OpenFGA gRPC: `localhost:8081`
- OpenFGA playground: `http://localhost:3001`

The Compose credentials are for local development only.

## API scaffold

- Swagger UI: `http://localhost:8080/swagger-ui/index.html`
- OpenAPI document: `http://localhost:8080/v3/api-docs`
- Password auth: `POST /api/v1/auth/register`, `/login`, `/refresh`, `/logout`
- Passkeys: registration options/response, authentication options/response, list, and delete under `/api/v1/auth/passkeys`

The controller stubs return `501 Not Implemented` when requests reach them; Spring Security may reject state-changing requests that do not include a valid CSRF token. The OpenAPI document describes bearer JWT protection for passkey management, but JWT validation is not wired yet.
