-- USERS
CREATE TABLE users (
                       id         UUID         PRIMARY KEY DEFAULT gen_random_uuid(),
                       email      VARCHAR(255) NOT NULL UNIQUE,
                       password   VARCHAR(255) NOT NULL,
                       role       VARCHAR(20)  NOT NULL DEFAULT 'MERCHANT',
                       enabled    BOOLEAN      NOT NULL DEFAULT TRUE,
                       created_at TIMESTAMP    NOT NULL DEFAULT NOW()
);

-- MERCHANT PROFILES
CREATE TABLE merchant_profiles (
                                   id            UUID         PRIMARY KEY DEFAULT gen_random_uuid(),
                                   user_id       UUID         NOT NULL UNIQUE REFERENCES users(id) ON DELETE CASCADE,
                                   business_name VARCHAR(255) NOT NULL,
                                   location      VARCHAR(255),
                                   customer_type VARCHAR(100),
                                   phone_number  VARCHAR(20),
                                   products      TEXT,
                                   price_range   VARCHAR(100),
                                   created_at    TIMESTAMP    NOT NULL DEFAULT NOW()
);

-- BUSINESS ANALYSES
CREATE TABLE business_analyses (
                                   id                  UUID      PRIMARY KEY DEFAULT gen_random_uuid(),
                                   merchant_profile_id UUID      NOT NULL REFERENCES merchant_profiles(id) ON DELETE CASCADE,
                                   ai_response_json    TEXT      NOT NULL,
                                   created_at          TIMESTAMP NOT NULL DEFAULT NOW()
);

-- SMS LOGS
CREATE TABLE sms_logs (
                          id                  UUID         PRIMARY KEY DEFAULT gen_random_uuid(),
                          merchant_profile_id UUID         REFERENCES merchant_profiles(id),
                          phone_number        VARCHAR(20)  NOT NULL,
                          message             TEXT         NOT NULL,
                          status              VARCHAR(20)  NOT NULL DEFAULT 'PENDING',
                          sent_at             TIMESTAMP    NOT NULL DEFAULT NOW()
);

-- SUBSCRIPTIONS
CREATE TABLE subscriptions (
                               id                  UUID         PRIMARY KEY DEFAULT gen_random_uuid(),
                               user_id             UUID         NOT NULL UNIQUE REFERENCES users(id) ON DELETE CASCADE,
                               plan_name           VARCHAR(100) NOT NULL,
                               status              VARCHAR(20)  NOT NULL DEFAULT 'INACTIVE',
                               payment_provider_id VARCHAR(255),
                               renewal_date        TIMESTAMP,
                               created_at          TIMESTAMP    NOT NULL DEFAULT NOW()
);