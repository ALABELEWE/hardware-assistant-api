CREATE TABLE ai_usage (
                          id UUID PRIMARY KEY DEFAULT gen_random_uuid(),
                          merchant_id UUID NOT NULL REFERENCES users(id) ON DELETE CASCADE,
                          prompt_tokens INTEGER NOT NULL DEFAULT 0,
                          completion_tokens INTEGER NOT NULL DEFAULT 0,
                          total_tokens INTEGER NOT NULL DEFAULT 0,
                          estimated_cost DECIMAL(10,6) NOT NULL DEFAULT 0,
                          model_used VARCHAR(100),
                          analysis_id UUID,
                          created_at TIMESTAMP NOT NULL DEFAULT NOW()
);

CREATE INDEX idx_ai_usage_merchant ON ai_usage(merchant_id);
CREATE INDEX idx_ai_usage_created_at ON ai_usage(created_at);