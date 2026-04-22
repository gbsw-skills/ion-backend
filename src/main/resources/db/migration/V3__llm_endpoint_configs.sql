CREATE TABLE llm_endpoint_configs
(
    id            BIGSERIAL PRIMARY KEY,
    name          VARCHAR(100) NOT NULL,
    base_url      VARCHAR(500) NOT NULL,
    api_key       VARCHAR(1000) NOT NULL,
    model         VARCHAR(200) NOT NULL,
    system_prompt TEXT         NOT NULL,
    temperature   NUMERIC(3, 2) NOT NULL,
    max_tokens    INTEGER      NOT NULL,
    enabled       BOOLEAN      NOT NULL DEFAULT TRUE,
    is_default    BOOLEAN      NOT NULL DEFAULT FALSE,
    created_at    TIMESTAMPTZ  NOT NULL DEFAULT NOW(),
    updated_at    TIMESTAMPTZ  NOT NULL DEFAULT NOW()
);

CREATE UNIQUE INDEX uq_llm_endpoint_configs_name ON llm_endpoint_configs (name);
CREATE UNIQUE INDEX uq_llm_endpoint_configs_default_true
    ON llm_endpoint_configs (is_default)
    WHERE is_default = TRUE;
CREATE INDEX idx_llm_endpoint_configs_enabled ON llm_endpoint_configs (enabled);
