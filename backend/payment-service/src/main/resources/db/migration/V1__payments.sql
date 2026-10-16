CREATE TABLE payments (
    id                   BIGINT AUTO_INCREMENT PRIMARY KEY,
    deal_id              BIGINT         NOT NULL,
    brand_user_id        BIGINT         NOT NULL,
    influencer_user_id   BIGINT         NOT NULL,
    campaign_title       VARCHAR(150),
    amount               DECIMAL(12, 2) NOT NULL,
    platform_fee         DECIMAL(12, 2) NOT NULL,
    currency             VARCHAR(3)     NOT NULL,
    status               VARCHAR(20)    NOT NULL,
    provider             VARCHAR(20)    NOT NULL,
    provider_payment_id  VARCHAR(100),
    client_secret        VARCHAR(255),
    idempotency_key      VARCHAR(100)   NOT NULL,
    failure_reason       VARCHAR(500),
    payout_status        VARCHAR(20)    NOT NULL DEFAULT 'NOT_STARTED',
    payout_released_at   DATETIME(6),
    deal_synced          BOOLEAN        NOT NULL DEFAULT FALSE,
    created_at           DATETIME(6)    NOT NULL,
    updated_at           DATETIME(6)    NOT NULL,
    version              BIGINT         NOT NULL DEFAULT 0,
    CONSTRAINT uk_payment_idempotency_key UNIQUE (idempotency_key),
    CONSTRAINT uk_payment_provider_id UNIQUE (provider_payment_id)
);

CREATE INDEX idx_payment_deal ON payments (deal_id);
CREATE INDEX idx_payment_brand ON payments (brand_user_id);
CREATE INDEX idx_payment_influencer ON payments (influencer_user_id);
CREATE INDEX idx_payment_unsynced ON payments (status, deal_synced);

-- Stripe retries webhooks; storing processed event ids makes handling idempotent.
CREATE TABLE processed_webhook_events (
    event_id     VARCHAR(100) PRIMARY KEY,
    event_type   VARCHAR(100) NOT NULL,
    received_at  DATETIME(6)  NOT NULL
);
