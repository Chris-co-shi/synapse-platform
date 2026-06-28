ALTER TABLE iam_user
    RENAME COLUMN version TO revision;

ALTER TABLE iam_user
    RENAME CONSTRAINT ck_iam_user_version TO ck_iam_user_revision;

ALTER TABLE iam_user_credential
    RENAME COLUMN version TO revision;

ALTER TABLE iam_user_credential
    RENAME CONSTRAINT ck_iam_user_credential_version TO ck_iam_user_credential_revision;

CREATE TABLE iam_refresh_session
(
    id                   VARCHAR(19)  PRIMARY KEY,
    family_id            VARCHAR(64)  NOT NULL,
    user_id              VARCHAR(19)  NOT NULL,
    client_id            VARCHAR(64)  NOT NULL,
    refresh_token_digest VARCHAR(64)  NOT NULL,
    access_token_digest  VARCHAR(64)  NOT NULL,
    status               VARCHAR(32)  NOT NULL,
    issued_at            TIMESTAMPTZ  NOT NULL,
    idle_expires_at      TIMESTAMPTZ  NOT NULL,
    absolute_expires_at  TIMESTAMPTZ  NOT NULL,
    revoked_at           TIMESTAMPTZ,
    revoke_reason        VARCHAR(64),
    replaced_by_id       VARCHAR(19),
    revision             INTEGER      NOT NULL DEFAULT 0,
    created_at           TIMESTAMPTZ  NOT NULL DEFAULT CURRENT_TIMESTAMP,
    updated_at           TIMESTAMPTZ  NOT NULL DEFAULT CURRENT_TIMESTAMP,
    created_by           VARCHAR(19),
    updated_by           VARCHAR(19),
    CONSTRAINT fk_iam_refresh_session_user
        FOREIGN KEY (user_id) REFERENCES iam_user (id),
    CONSTRAINT uk_iam_refresh_session_refresh_digest UNIQUE (refresh_token_digest),
    CONSTRAINT ck_iam_refresh_session_status CHECK (status IN ('ACTIVE', 'ROTATED', 'REVOKED', 'REUSE_DETECTED')),
    CONSTRAINT ck_iam_refresh_session_revision CHECK (revision >= 0)
);

CREATE INDEX idx_iam_refresh_session_user
    ON iam_refresh_session (user_id);

CREATE INDEX idx_iam_refresh_session_family
    ON iam_refresh_session (family_id);

COMMENT ON TABLE iam_refresh_session IS 'IAM Opaque Refresh Token 会话与 rotation 状态';
COMMENT ON COLUMN iam_refresh_session.refresh_token_digest IS 'Refresh Token SHA-256 摘要，不保存原始 Token';
COMMENT ON COLUMN iam_refresh_session.access_token_digest IS '当前会话对应 Access Token SHA-256 摘要，不保存原始 Token';
