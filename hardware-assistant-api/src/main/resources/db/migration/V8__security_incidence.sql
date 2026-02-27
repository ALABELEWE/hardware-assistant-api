

-- Security incident tracking + user ban support
-- User ban fields
ALTER TABLE users
    ADD COLUMN IF NOT EXISTS is_banned  BOOLEAN      NOT NULL DEFAULT FALSE,
    ADD COLUMN IF NOT EXISTS ban_reason VARCHAR(300),
    ADD COLUMN IF NOT EXISTS banned_at  TIMESTAMP;

-- Security incidents table
CREATE TABLE IF NOT EXISTS security_incidents (
                                                  id                UUID         PRIMARY KEY DEFAULT gen_random_uuid(),
                                                  user_id           UUID         REFERENCES users(id) ON DELETE SET NULL,
                                                  user_email        VARCHAR(200),
                                                  field_name        VARCHAR(100) NOT NULL,
                                                  flagged_input     VARCHAR(300),
                                                  matched_pattern   VARCHAR(200),
                                                  severity          VARCHAR(20)  NOT NULL CHECK (severity IN ('WARNING','BLOCKED','BANNED')),
                                                  user_attempt_count INT         NOT NULL DEFAULT 1,
                                                  ip_address        VARCHAR(50),
                                                  created_at        TIMESTAMP    NOT NULL DEFAULT NOW()
);

CREATE INDEX IF NOT EXISTS idx_incident_user       ON security_incidents(user_id);
CREATE INDEX IF NOT EXISTS idx_incident_created_at ON security_incidents(created_at);
CREATE INDEX IF NOT EXISTS idx_incident_severity   ON security_incidents(severity);
CREATE INDEX IF NOT EXISTS idx_incident_user_email ON security_incidents(user_email);