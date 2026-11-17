# Auth service

Kotlin and Spring Boot starter for a central Spring Authorization Server and OpenID Connect provider. Password registration and passkey-management controller stubs return `501 Not Implemented`; the OIDC server and authentication flows are not configured yet.

The auth app runs Flyway migrations against the `auth` database during startup. Hibernate schema generation is disabled so Flyway remains the schema owner. V2 adds Spring Authorization Server and Spring Security WebAuthn storage, while preserving the earlier draft tables under `legacy_*` names. OpenFGA continues to initialize its separate database with its own migrator.

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
- Account registration: `POST /api/v1/auth/register`
- Passkey management: `GET /api/v1/auth/passkeys` and `DELETE /api/v1/auth/passkeys/{credentialId}`

Spring Authorization Server will own OIDC login, authorization, token, and logout protocol flows. Spring Security WebAuthn will provide passkey enrollment and login ceremonies. Their configuration is still on the TODO list. CSRF protection remains enabled.
