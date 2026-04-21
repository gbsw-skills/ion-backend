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

CREATE INDEX idx_chat_sessions_user_id ON chat_sessions (user_id);
CREATE INDEX idx_chat_messages_session_id ON chat_messages (session_id);
CREATE INDEX idx_chat_messages_created_at ON chat_messages (created_at DESC);
CREATE INDEX idx_notices_published_at ON notices (published_at DESC);
