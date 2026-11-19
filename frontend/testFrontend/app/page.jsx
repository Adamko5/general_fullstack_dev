"use client";

import { InMemoryWebStorage, UserManager, WebStorageStateStore } from "oidc-client-ts";
import { useEffect, useRef, useState } from "react";

function createUserManager() {
  const settings = {
    authority: process.env.NEXT_PUBLIC_AUTH_ISSUER,
    client_id: process.env.NEXT_PUBLIC_AUTH_CLIENT_ID,
    redirect_uri: process.env.NEXT_PUBLIC_AUTH_REDIRECT_URI,
    post_logout_redirect_uri: process.env.NEXT_PUBLIC_AUTH_POST_LOGOUT_REDIRECT_URI,
  };
  const missingSettings = Object.entries(settings)
    .filter(([, value]) => !value)
    .map(([name]) => name);

  if (missingSettings.length > 0) {
    throw new Error(`Missing frontend OIDC settings: ${missingSettings.join(", ")}`);
  }

  return new UserManager({
    ...settings,
    response_type: "code",
    scope: "openid",
    userStore: new WebStorageStateStore({ store: new InMemoryWebStorage() }),
    stateStore: new WebStorageStateStore({ store: window.sessionStorage }),
    automaticSilentRenew: false,
    loadUserInfo: false,
  });
}

export default function Home() {
  const managerRef = useRef(null);
  const initializationRef = useRef(null);
  const [user, setUser] = useState(null);
  const [status, setStatus] = useState("Connecting to the identity provider…");
  const [error, setError] = useState("");

  function getUserManager() {
    managerRef.current ??= createUserManager();
    return managerRef.current;
  }

  useEffect(() => {
    let cancelled = false;

    initializationRef.current ??= (async () => {
      const manager = getUserManager();
      const query = new URLSearchParams(window.location.search);

      if (query.has("code") || query.has("error")) {
        try {
          await manager.signinRedirectCallback();
        } finally {
          window.history.replaceState({}, document.title, window.location.pathname);
        }
      }

      return manager.getUser();
    })();

    initializationRef.current
      .then((currentUser) => {
        if (!cancelled) {
          setUser(currentUser);
          setStatus(currentUser ? "Signed in" : "Signed out");
        }
      })
      .catch((cause) => {
        if (!cancelled) {
          setError(cause instanceof Error ? cause.message : "OIDC sign-in failed.");
          setStatus("Could not complete sign-in");
        }
      });

    return () => {
      cancelled = true;
    };
  }, []);

  async function signIn() {
    setError("");
    try {
      await getUserManager().signinRedirect();
    } catch (cause) {
      setError(cause instanceof Error ? cause.message : "Could not start sign-in.");
    }
  }

  async function signOut() {
    setError("");
    try {
      await getUserManager().signoutRedirect();
    } catch (cause) {
      setError(cause instanceof Error ? cause.message : "Could not start sign-out.");
    }
  }

  return (
    <main className="page-shell">
      <title>Test Frontend</title>
      <section className="panel" aria-labelledby="page-title">
        <p className="eyebrow">OIDC test client</p>
        <h1 id="page-title">Test Frontend</h1>
        <p className="intro">
          A single React page served by Next.js. Sign in through the local auth service to check
          the authorization-code flow.
        </p>

        <div className="status-row">
          <span className={`status-dot ${user ? "is-signed-in" : ""}`} aria-hidden="true" />
          <span>{status}</span>
        </div>

        {error && <p className="error-message" role="alert">{error}</p>}

        {user ? (
          <>
            <div className="identity-card">
              <p className="identity-label">Authenticated subject</p>
              <code>{user.profile.sub}</code>
            </div>
            <details>
              <summary>OpenID Connect claims</summary>
              <pre>{JSON.stringify(user.profile, null, 2)}</pre>
            </details>
            <button className="button button-secondary" onClick={signOut}>
              Sign out
            </button>
          </>
        ) : (
          <button className="button" onClick={signIn} disabled={status.startsWith("Connecting")}>
            Sign in
          </button>
        )}

        <p className="footnote">
          Tokens are held in memory for this test app and are cleared when the page reloads.
        </p>
      </section>
    </main>
  );
}
