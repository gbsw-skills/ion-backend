-- Consolidated schema for manual table creation.
-- Source of truth remains Flyway migrations under db/migration.

CREATE TABLE users
(
    id            BIGSERIAL PRIMARY KEY,
    username      VARCHAR(50)  NOT NULL UNIQUE,
    password_hash VARCHAR(255) NOT NULL,
    role          VARCHAR(20)  NOT NULL,
    display_name  VARCHAR(100) NOT NULL,
    created_at    TIMESTAMPTZ  NOT NULL DEFAULT NOW(),
    updated_at    TIMESTAMPTZ  NOT NULL DEFAULT NOW()
);

CREATE TABLE chat_sessions
(
    id             UUID PRIMARY KEY DEFAULT gen_random_uuid(),
    user_id        BIGINT       NOT NULL REFERENCES users (id),
    title          VARCHAR(200),
    created_at     TIMESTAMPTZ  NOT NULL DEFAULT NOW(),
    last_active_at TIMESTAMPTZ  NOT NULL DEFAULT NOW()
);

CREATE TABLE chat_messages
(
    id         BIGSERIAL PRIMARY KEY,
    session_id UUID        NOT NULL REFERENCES chat_sessions (id) ON DELETE CASCADE,
    role       VARCHAR(20) NOT NULL,
    content    TEXT        NOT NULL,
    created_at TIMESTAMPTZ NOT NULL DEFAULT NOW()
);

CREATE TABLE notices
(
    id           BIGSERIAL PRIMARY KEY,
    title        VARCHAR(300) NOT NULL,
    content      TEXT         NOT NULL,
    author_id    BIGINT REFERENCES users (id),
    published_at TIMESTAMPTZ,
    created_at   TIMESTAMPTZ  NOT NULL DEFAULT NOW()
);

CREATE TABLE refresh_tokens
(
    id         BIGSERIAL PRIMARY KEY,
    user_id    BIGINT        NOT NULL REFERENCES users (id) ON DELETE CASCADE,
    token      VARCHAR(1000) NOT NULL UNIQUE,
    expires_at TIMESTAMPTZ   NOT NULL,
    created_at TIMESTAMPTZ   NOT NULL DEFAULT NOW()
);

CREATE TABLE documents
(
    id          BIGSERIAL PRIMARY KEY,
    title       VARCHAR(300) NOT NULL,
    file_path   VARCHAR(500) NOT NULL,
    file_type   VARCHAR(20),
    uploaded_by BIGINT REFERENCES users (id),
    created_at  TIMESTAMPTZ  NOT NULL DEFAULT NOW()
);

CREATE TABLE admin_logs
(
    id          BIGSERIAL PRIMARY KEY,
    admin_id    BIGINT       NOT NULL REFERENCES users (id),
    action      VARCHAR(100) NOT NULL,
    target_type VARCHAR(50),
    target_id   BIGINT,
    created_at  TIMESTAMPTZ  NOT NULL DEFAULT NOW()
);

CREATE TABLE llm_endpoint_configs
(
    id            BIGSERIAL PRIMARY KEY,
    name          VARCHAR(100)  NOT NULL,
    base_url      VARCHAR(500)  NOT NULL,
    api_key       VARCHAR(1000) NOT NULL,
    model         VARCHAR(200)  NOT NULL,
    system_prompt TEXT          NOT NULL,
    temperature   NUMERIC(3, 2) NOT NULL,
    max_tokens    INTEGER       NOT NULL,
    enabled       BOOLEAN       NOT NULL DEFAULT TRUE,
    is_default    BOOLEAN       NOT NULL DEFAULT FALSE,
    created_at    TIMESTAMPTZ   NOT NULL DEFAULT NOW(),
    updated_at    TIMESTAMPTZ   NOT NULL DEFAULT NOW()
);

CREATE INDEX idx_chat_sessions_user_id ON chat_sessions (user_id);
CREATE INDEX idx_chat_messages_session_id ON chat_messages (session_id);
CREATE INDEX idx_chat_messages_created_at ON chat_messages (created_at DESC);
CREATE INDEX idx_notices_published_at ON notices (published_at DESC);
CREATE INDEX idx_refresh_tokens_user_id ON refresh_tokens (user_id);
CREATE INDEX idx_documents_created_at ON documents (created_at DESC);
CREATE INDEX idx_admin_logs_admin_id ON admin_logs (admin_id);
CREATE UNIQUE INDEX uq_llm_endpoint_configs_name ON llm_endpoint_configs (name);
CREATE UNIQUE INDEX uq_llm_endpoint_configs_default_true
    ON llm_endpoint_configs (is_default)
    WHERE is_default = TRUE;
CREATE INDEX idx_llm_endpoint_configs_enabled ON llm_endpoint_configs (enabled);
