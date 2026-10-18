CREATE TABLE brand_profiles (
    user_id       BIGINT PRIMARY KEY,
    company_name  VARCHAR(150) NOT NULL,
    industry      VARCHAR(80),
    website       VARCHAR(255),
    city          VARCHAR(80),
    state         VARCHAR(80),
    description   VARCHAR(2000),
    logo_url      VARCHAR(500),
    created_at    DATETIME(6) NOT NULL,
    updated_at    DATETIME(6) NOT NULL
);

CREATE TABLE influencer_profiles (
    user_id              BIGINT PRIMARY KEY,
    display_name         VARCHAR(120) NOT NULL,
    bio                  VARCHAR(2000),
    city                 VARCHAR(80)  NOT NULL,
    state                VARCHAR(80),
    country              VARCHAR(80)  NOT NULL DEFAULT 'India',
    photo_url            VARCHAR(500),
    instagram_handle     VARCHAR(80),
    instagram_followers  BIGINT       NOT NULL DEFAULT 0,
    youtube_channel      VARCHAR(120),
    youtube_subscribers  BIGINT       NOT NULL DEFAULT 0,
    twitter_handle       VARCHAR(80),
    twitter_followers    BIGINT       NOT NULL DEFAULT 0,
    engagement_rate      DOUBLE       NOT NULL DEFAULT 0,
    price_per_post       DECIMAL(12, 2) NOT NULL,
    rating               DOUBLE       NOT NULL DEFAULT 0,
    rating_count         INT          NOT NULL DEFAULT 0,
    completed_deals      INT          NOT NULL DEFAULT 0,
    verified             BOOLEAN      NOT NULL DEFAULT FALSE,
    created_at           DATETIME(6)  NOT NULL,
    updated_at           DATETIME(6)  NOT NULL
);

CREATE TABLE influencer_niches (
    user_id  BIGINT      NOT NULL,
    niche    VARCHAR(40) NOT NULL,
    PRIMARY KEY (user_id, niche),
    CONSTRAINT fk_niche_influencer FOREIGN KEY (user_id) REFERENCES influencer_profiles (user_id) ON DELETE CASCADE
);

CREATE TABLE campaigns (
    id                    BIGINT AUTO_INCREMENT PRIMARY KEY,
    brand_user_id         BIGINT        NOT NULL,
    brand_name            VARCHAR(150)  NOT NULL,
    title                 VARCHAR(150)  NOT NULL,
    description           VARCHAR(4000) NOT NULL,
    deliverables          VARCHAR(2000),
    niche                 VARCHAR(40)   NOT NULL,
    platform              VARCHAR(20)   NOT NULL,
    city                  VARCHAR(80),
    budget                DECIMAL(12, 2) NOT NULL,
    min_followers         BIGINT        NOT NULL DEFAULT 0,
    status                VARCHAR(20)   NOT NULL,
    start_date            DATE,
    end_date              DATE,
    application_deadline  DATE,
    created_at            DATETIME(6)   NOT NULL,
    updated_at            DATETIME(6)   NOT NULL,
    version               BIGINT        NOT NULL DEFAULT 0
);

CREATE INDEX idx_campaign_brand ON campaigns (brand_user_id);
CREATE INDEX idx_campaign_status_niche ON campaigns (status, niche);

CREATE TABLE deals (
    id                  BIGINT AUTO_INCREMENT PRIMARY KEY,
    campaign_id         BIGINT        NOT NULL,
    brand_user_id       BIGINT        NOT NULL,
    influencer_user_id  BIGINT        NOT NULL,
    initiated_by        VARCHAR(20)   NOT NULL,
    message             VARCHAR(2000),
    amount              DECIMAL(12, 2) NOT NULL,
    status              VARCHAR(20)   NOT NULL,
    payment_id          BIGINT,
    deliverable_url     VARCHAR(500),
    delivery_note       VARCHAR(2000),
    created_at          DATETIME(6)   NOT NULL,
    updated_at          DATETIME(6)   NOT NULL,
    version             BIGINT        NOT NULL DEFAULT 0,
    CONSTRAINT uk_deal_campaign_influencer UNIQUE (campaign_id, influencer_user_id),
    CONSTRAINT fk_deal_campaign FOREIGN KEY (campaign_id) REFERENCES campaigns (id)
);

CREATE INDEX idx_deal_brand_status ON deals (brand_user_id, status);
CREATE INDEX idx_deal_influencer_status ON deals (influencer_user_id, status);

CREATE TABLE deal_status_history (
    id           BIGINT AUTO_INCREMENT PRIMARY KEY,
    deal_id      BIGINT       NOT NULL,
    from_status  VARCHAR(20),
    to_status    VARCHAR(20)  NOT NULL,
    changed_by   BIGINT,
    note         VARCHAR(1000),
    created_at   DATETIME(6)  NOT NULL,
    CONSTRAINT fk_history_deal FOREIGN KEY (deal_id) REFERENCES deals (id) ON DELETE CASCADE
);

CREATE TABLE reviews (
    id                BIGINT AUTO_INCREMENT PRIMARY KEY,
    deal_id           BIGINT       NOT NULL,
    reviewer_user_id  BIGINT       NOT NULL,
    reviewee_user_id  BIGINT       NOT NULL,
    reviewer_name     VARCHAR(150),
    rating            INT          NOT NULL,
    comment           VARCHAR(2000),
    created_at        DATETIME(6)  NOT NULL,
    CONSTRAINT uk_review_deal_reviewer UNIQUE (deal_id, reviewer_user_id),
    CONSTRAINT fk_review_deal FOREIGN KEY (deal_id) REFERENCES deals (id)
);

CREATE INDEX idx_review_reviewee ON reviews (reviewee_user_id);
