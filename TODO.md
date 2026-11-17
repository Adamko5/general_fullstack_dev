1. [x] Choose the central identity-provider approach.
    - Use Spring Authorization Server with OpenID Connect.
    - Future apps sign in with Authorization Code + PKCE and get their own client tokens.
    - Password and passkeys are authentication methods at the identity provider, not custom token endpoints.
    - Keep app roles out of the plan; use authenticated identity in Spring Security and OpenFGA for file-level permissions.
    - The earlier direct API refresh-cookie design is superseded by OIDC token responses; choose SPA versus BFF token storage for each client in Step 5.

2. [x] Configure the Spring Authorization Server/OIDC foundation.
    - Added the ordered authorization-server and application filter chains and enabled OIDC.
    - Configured a stable issuer, persistent RSA signing key, discovery/JWKS, short-lived self-contained access tokens, and rotating refresh tokens.
    - Wired JDBC client, authorization, and consent persistence and seeded the local `local-nextjs` PKCE client.
    - Kept the auth-server login session and CSRF protection session-backed; added a CSRF bootstrap endpoint and local frontend CORS origin.
    - V2 adds SAS and Spring Security WebAuthn storage and renames the previous draft token/passkey tables as `legacy_*`. Drop those legacy tables later after confirming they contain no data worth keeping.
    - Before issuing real tokens, decide whether SAS's standard JDBC authorization store may retain token values at rest or whether a custom hashed-token persistence service is required; the old `token_hash` table did not do this.

3. Connect account registration and password authentication.
    - Keep account registration as an application endpoint.
    - Add a `UserDetailsService` and `PasswordEncoder` backed by the existing `users` table; use Spring Security form login for the OIDC authorization flow.
    - Make the authenticated user ID the stable OIDC subject; do not add application roles.

4. Add passkeys as an alternative way to authenticate.
    - Configure Spring Security WebAuthn with the real RP ID and allowed browser origins.
    - Use the JDBC credential repositories and V2 tables already scaffolded.
    - Require an existing login to enroll or manage passkeys; allow a passkey assertion to authenticate before continuing the OIDC authorization flow.

5. Register the first frontend as an OIDC client.
    - Choose the client type and exact redirect/logout URLs.
    - Decide whether the frontend is a public SPA or a Next.js BFF, and define its token-storage approach.
    - Verify sign-in, callback handling, token validation, refresh, and SSO through the central auth-server session.

6. Add S3 upload and download endpoints.
    - Validate OIDC access tokens at the API boundary.
    - Check the user/resource relationship in OpenFGA before each S3 operation.
