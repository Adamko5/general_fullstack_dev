# Test frontend

One browser-side React page served by Next.js. It uses OpenID Connect Authorization Code with PKCE against the local auth service. Next.js API routes and server-side auth handling are not used.

Run from this directory:

```sh
cp .env.example .env.local
pnpm install
pnpm dev
```

Open `http://localhost:3000`. Start the auth service first with Docker Compose from `auth/`. Create an account through `http://localhost:8080/swagger-ui/index.html`, then sign in from the test page. Tokens are kept in memory and disappear on reload. The client ID in `.env.local` must match an entry in `auth/oidc-clients.local.json`.
