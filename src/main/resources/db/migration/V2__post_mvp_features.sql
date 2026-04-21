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

CREATE INDEX idx_refresh_tokens_user_id ON refresh_tokens (user_id);
CREATE INDEX idx_documents_created_at ON documents (created_at DESC);
CREATE INDEX idx_admin_logs_admin_id ON admin_logs (admin_id);
