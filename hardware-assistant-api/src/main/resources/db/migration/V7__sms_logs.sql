CREATE TABLE IF NOT EXISTS sms_logs (
                                        id UUID PRIMARY KEY DEFAULT gen_random_uuid(),
                                        merchant_id UUID,
                                        phone_number VARCHAR(20) NOT NULL,
                                        message TEXT NOT NULL,
                                        status VARCHAR(20) NOT NULL DEFAULT 'PENDING',
                                        cost DECIMAL(10,4),
                                        attempts INTEGER NOT NULL DEFAULT 0,
                                        analysis_id UUID,
                                        error_message TEXT,
                                        created_at TIMESTAMP NOT NULL DEFAULT NOW(),
                                        sent_at TIMESTAMP
);

DO $$
    BEGIN
        IF NOT EXISTS (
            SELECT 1 FROM pg_indexes
            WHERE tablename = 'sms_logs' AND indexname = 'idx_sms_logs_merchant'
        ) THEN
            CREATE INDEX idx_sms_logs_merchant ON sms_logs(merchant_id);
        END IF;
    END $$;