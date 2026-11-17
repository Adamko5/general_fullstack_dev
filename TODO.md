1. [x] Choose the central identity-provider approach.
    - Use Spring Authorization Server with OpenID Connect.
    - Future apps sign in with Authorization Code + PKCE and get their own client tokens.
    - Password and passkeys are authentication methods at the identity provider, not custom token endpoints.
    - Keep app roles out of the plan; use authenticated identity in Spring Security and OpenFGA for file-level permissions.

2. Configure Spring Authorization Server and OIDC.
    - Add the `@Order(1)` authorization-server filter chain ahead of the existing `@Order(2)` application chain, and explicitly enable OIDC.
    - Set a stable issuer and signing key, enable discovery/JWKS, and configure token lifetimes and refresh-token rotation.
    - Wire Spring Authorization Server's JDBC client, authorization, and consent repositories to the V2 Flyway tables; register a development OIDC client with Authorization Code + PKCE.
    - Configure the auth-server login session and CSRF behavior for browser flows.
    - V2 adds SAS and Spring Security WebAuthn storage and renames the previous draft token/passkey tables as `legacy_*`. Drop those legacy tables later after confirming they contain no data worth keeping.

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
    - Verify sign-in, callback handling, token validation, refresh, and SSO through the central auth-server session.

6. Add S3 upload and download endpoints.
    - Validate OIDC access tokens at the API boundary.
    - Check the user/resource relationship in OpenFGA before each S3 operation.
