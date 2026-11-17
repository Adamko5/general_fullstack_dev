1. Decide token delivery and refresh-token format.
    The README currently says both tokens are JWTs. The schema can store a hash of either, though I recommend a short-lived JWT access token plus an opaque rotating refresh token.


2. Configure JWT issuance/validation and the security filter chain, then settle the CSRF token approach for the browser.


3. Implement password registration/login and session/refresh/logout services.


4. Implement the two WebAuthn ceremonies: enrollment for authenticated users, and public challenge/assertion endpoints for login.


5. Add the S3 endpoints. They should authenticate the user, ask OpenFGA about that user and file, then perform the S3 operation.


