CREATE TABLE users (
    id UUID PRIMARY KEY DEFAULT gen_random_uuid(),
    name TEXT NOT NULL,
    email TEXT NOT NULL,
    password_hash TEXT,
    email_verified_at TIMESTAMPTZ,
    created_at TIMESTAMPTZ NOT NULL DEFAULT now(),
    updated_at TIMESTAMPTZ NOT NULL DEFAULT now(),
    disabled_at TIMESTAMPTZ,
    CONSTRAINT users_name_not_blank CHECK (btrim(name) <> ''),
    CONSTRAINT users_email_not_blank CHECK (btrim(email) <> '')
);

CREATE UNIQUE INDEX users_email_case_insensitive_unique_idx
    ON users (lower(email));

CREATE TABLE auth_sessions (
    id UUID PRIMARY KEY DEFAULT gen_random_uuid(),
    user_id UUID NOT NULL REFERENCES users (id) ON DELETE CASCADE,
    created_at TIMESTAMPTZ NOT NULL DEFAULT now(),
    last_refresh_at TIMESTAMPTZ NOT NULL DEFAULT now(),
    expires_at TIMESTAMPTZ NOT NULL,
    revoked_at TIMESTAMPTZ,
    CONSTRAINT auth_sessions_expiry_after_creation CHECK (expires_at > created_at),
    CONSTRAINT auth_sessions_revoked_after_creation
        CHECK (revoked_at IS NULL OR revoked_at >= created_at)
);

CREATE INDEX auth_sessions_user_created_at_idx
    ON auth_sessions (user_id, created_at DESC);

CREATE TABLE refresh_tokens (
    id UUID PRIMARY KEY DEFAULT gen_random_uuid(),
    session_id UUID NOT NULL REFERENCES auth_sessions (id) ON DELETE CASCADE,
    token_hash BYTEA NOT NULL UNIQUE,
    issued_at TIMESTAMPTZ NOT NULL DEFAULT now(),
    expires_at TIMESTAMPTZ NOT NULL,
    consumed_at TIMESTAMPTZ,
    replaced_by_token_id UUID REFERENCES refresh_tokens (id) ON DELETE SET NULL,
    CONSTRAINT refresh_tokens_sha256_hash_length
        CHECK (octet_length(token_hash) = 32),
    CONSTRAINT refresh_tokens_expiry_after_issue CHECK (expires_at > issued_at),
    CONSTRAINT refresh_tokens_consumed_after_issue
        CHECK (consumed_at IS NULL OR consumed_at >= issued_at),
    CONSTRAINT refresh_tokens_not_replaced_by_self
        CHECK (replaced_by_token_id IS NULL OR replaced_by_token_id <> id),
    CONSTRAINT refresh_tokens_replacement_unique UNIQUE (replaced_by_token_id)
);

CREATE INDEX refresh_tokens_session_issued_at_idx
    ON refresh_tokens (session_id, issued_at DESC);

CREATE UNIQUE INDEX refresh_tokens_one_unconsumed_per_session_idx
    ON refresh_tokens (session_id)
    WHERE consumed_at IS NULL;

CREATE TABLE webauthn_credentials (
    id UUID PRIMARY KEY DEFAULT gen_random_uuid(),
    user_id UUID NOT NULL REFERENCES users (id) ON DELETE CASCADE,
    credential_id BYTEA NOT NULL UNIQUE,
    public_key BYTEA NOT NULL,
    sign_count BIGINT NOT NULL DEFAULT 0,
    transports TEXT[] NOT NULL DEFAULT ARRAY[]::TEXT[],
    label TEXT NOT NULL DEFAULT 'Passkey',
    aaguid UUID,
    backup_eligible BOOLEAN NOT NULL DEFAULT FALSE,
    backup_state BOOLEAN NOT NULL DEFAULT FALSE,
    created_at TIMESTAMPTZ NOT NULL DEFAULT now(),
    last_used_at TIMESTAMPTZ,
    CONSTRAINT webauthn_credentials_id_length
        CHECK (octet_length(credential_id) BETWEEN 1 AND 1023),
    CONSTRAINT webauthn_credentials_sign_count_nonnegative CHECK (sign_count >= 0),
    CONSTRAINT webauthn_credentials_label_not_blank CHECK (btrim(label) <> ''),
    CONSTRAINT webauthn_credentials_backup_state_requires_eligibility
        CHECK (NOT backup_state OR backup_eligible)
);

CREATE INDEX webauthn_credentials_user_created_at_idx
    ON webauthn_credentials (user_id, created_at DESC);

CREATE TABLE webauthn_challenges (
    id UUID PRIMARY KEY DEFAULT gen_random_uuid(),
    challenge BYTEA NOT NULL UNIQUE,
    purpose TEXT NOT NULL,
    user_id UUID REFERENCES users (id) ON DELETE CASCADE,
    created_at TIMESTAMPTZ NOT NULL DEFAULT now(),
    expires_at TIMESTAMPTZ NOT NULL,
    consumed_at TIMESTAMPTZ,
    CONSTRAINT webauthn_challenges_purpose
        CHECK (purpose IN ('registration', 'authentication')),
    CONSTRAINT webauthn_registration_challenge_has_user
        CHECK (purpose <> 'registration' OR user_id IS NOT NULL),
    CONSTRAINT webauthn_challenges_expiry_after_creation
        CHECK (expires_at > created_at),
    CONSTRAINT webauthn_challenges_consumed_after_creation
        CHECK (consumed_at IS NULL OR consumed_at >= created_at)
);

CREATE INDEX webauthn_challenges_pending_expiry_idx
    ON webauthn_challenges (expires_at)
    WHERE consumed_at IS NULL;
