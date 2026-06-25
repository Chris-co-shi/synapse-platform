CREATE TABLE iam_user
(
    id                  VARCHAR(19)  PRIMARY KEY,
    username            VARCHAR(64)  NOT NULL,
    normalized_username VARCHAR(64)  NOT NULL,
    display_name        VARCHAR(128) NOT NULL,
    status              VARCHAR(32)  NOT NULL,
    version             INTEGER      NOT NULL DEFAULT 0,
    created_at          TIMESTAMPTZ  NOT NULL DEFAULT CURRENT_TIMESTAMP,
    updated_at          TIMESTAMPTZ  NOT NULL DEFAULT CURRENT_TIMESTAMP,
    created_by          VARCHAR(19),
    updated_by          VARCHAR(19),
    CONSTRAINT ck_iam_user_status CHECK (status IN ('ACTIVE', 'DISABLED')),
    CONSTRAINT ck_iam_user_version CHECK (version >= 0)
);

CREATE UNIQUE INDEX uk_iam_user_username
    ON iam_user (username);

CREATE UNIQUE INDEX uk_iam_user_normalized_username
    ON iam_user (normalized_username);

CREATE TABLE iam_user_credential
(
    id                  VARCHAR(19)  PRIMARY KEY,
    user_id             VARCHAR(19)  NOT NULL,
    password_hash       VARCHAR(255) NOT NULL,
    password_changed_at TIMESTAMPTZ  NOT NULL,
    failed_attempts     INTEGER      NOT NULL DEFAULT 0,
    locked_until        TIMESTAMPTZ,
    version             INTEGER      NOT NULL DEFAULT 0,
    created_at          TIMESTAMPTZ  NOT NULL DEFAULT CURRENT_TIMESTAMP,
    updated_at          TIMESTAMPTZ  NOT NULL DEFAULT CURRENT_TIMESTAMP,
    created_by          VARCHAR(19),
    updated_by          VARCHAR(19),
    CONSTRAINT fk_iam_user_credential_user
        FOREIGN KEY (user_id) REFERENCES iam_user (id),
    CONSTRAINT uk_iam_user_credential_user UNIQUE (user_id),
    CONSTRAINT ck_iam_user_credential_failed_attempts CHECK (failed_attempts >= 0),
    CONSTRAINT ck_iam_user_credential_version CHECK (version >= 0)
);

COMMENT ON TABLE iam_user IS 'IAM 用户身份主体';
COMMENT ON COLUMN iam_user.normalized_username IS '用于登录查询和唯一约束的规范化用户名';
COMMENT ON TABLE iam_user_credential IS 'IAM 用户密码凭据与认证失败状态';
COMMENT ON COLUMN iam_user_credential.password_hash IS 'PasswordEncoder 生成的不可逆密码哈希';
