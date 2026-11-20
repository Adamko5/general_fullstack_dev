# Auth service

Kotlin and Spring Boot service for a central Spring Authorization Server and OpenID Connect provider. Account registration and email/password login use the `users` table. Spring Security WebAuthn provides passkey enrollment and login; passkey records use the JDBC repositories.

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
- Passkey registration page: `GET /webauthn/register` (requires login)
- Passkey registration: `POST /webauthn/register/options` and `POST /webauthn/register` (require login and CSRF)
- Passkey login: `POST /webauthn/authenticate/options` and `POST /login/webauthn` (public routes, still require CSRF)

Registration stores passwords with Spring Security's delegating password encoder and returns the created user's ID, name, and normalized email. Log in through the Spring Security form at `/login`, entering the account email as the username; the generated login page includes passkey login. The authenticated user's UUID is the OIDC subject. Passkey enrollment and management require an authenticated session. WebAuthn currently uses RP ID `localhost` and allows the auth and frontend development origins; set the deployed hostname and exact HTTPS origins before deployment. CSRF protection remains enabled, including on passkey ceremony POST requests. Client and authorization storage is in Postgres. The development CORS origin defaults to `http://localhost:3000`. The test frontend keeps tokens in memory, so a page reload signs it out. The RSA private key is local-only and ignored by Git; configure an HTTPS issuer and managed signing keys outside local development.

## Local OIDC client registry

`oidc-clients.local.json` defines every local OIDC client and its Spring Authorization Server settings. The auth app reads it during startup and adds each client whose `clientId` is not already in Postgres. Existing clients are deliberately left unchanged; editing this file does not update a client already seeded in the database.

The `test-frontend` entry uses the `none` client authentication method because browser code cannot keep a client secret private. It is a public client and requires PKCE. Its `allowedResourceUris` list controls which APIs it can request tokens for. The frontend sends one of those URIs with the OAuth `resource` parameter, and the access token's audience is set to the requested resource URI or URIs. Add an allowed URI for each API a client may use. For a confidential server-side client, set a client-secret authentication method such as `client_secret_basic` and put its plaintext value in `clientSecret`; the seeder encodes it before storing it in Postgres. Never put that secret in browser code or a `NEXT_PUBLIC_` variable.

This registry is a local development convenience only. OIDC client registration and its production management are outside this repository's scope; the local registry and any plaintext secrets in it must not be deployed.
