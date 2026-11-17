# Auth service

Kotlin and Spring Boot scaffold for a central Spring Authorization Server and OpenID Connect provider. OIDC endpoints and a local PKCE client are configured. Database-backed account authentication and WebAuthn are still TODOs; the account registration and passkey-management controller stubs return `501 Not Implemented`.

The auth app runs Flyway migrations against the `auth` database during startup. Hibernate schema generation is disabled so Flyway remains the schema owner. V2 adds Spring Authorization Server and Spring Security WebAuthn storage, while preserving the earlier draft tables under `legacy_*` names. OpenFGA continues to initialize its separate database with its own migrator.

## Run the full stack

Generate a local RSA signing-key pair once from PowerShell:

```powershell
.\scripts\generate-dev-jwt-keys.ps1
```

Then start the auth app and its supporting services from this directory:

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
- OIDC discovery: `http://localhost:8080/.well-known/openid-configuration`
- OIDC JWK set: `http://localhost:8080/oauth2/jwks`

The Compose credentials are for local development only. Override the local S3 credentials with `S3_ACCESS_KEY` and `S3_SECRET_KEY` environment variables when needed.

## API scaffold

- Swagger UI: `http://localhost:8080/swagger-ui/index.html`
- OpenAPI document: `http://localhost:8080/v3/api-docs`
- CSRF token: `GET /api/v1/auth/csrf` (send the returned token in its named header on browser POST requests)
- Account registration: `POST /api/v1/auth/register`
- Passkey management: `GET /api/v1/auth/passkeys` and `DELETE /api/v1/auth/passkeys/{credentialId}`

The local OIDC client is `local-nextjs`, a public PKCE client with `http://localhost:3000/oidc/callback` as a placeholder redirect URI. Client and authorization storage is in Postgres. The development CORS origin defaults to `http://localhost:3000`. Until Step 3 connects the `users` table, Spring Boot's temporary development user can be used for the login page; its generated password is printed in the startup logs. CSRF protection remains enabled. Passkey login/enrollment is not enabled until Step 4. The frontend's token-storage approach is still open. The RSA private key is local-only and ignored by Git; configure an HTTPS issuer and managed signing keys outside local development.
