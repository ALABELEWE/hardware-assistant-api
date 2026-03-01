
-- ── Product Catalogue ─────────────────────────────────────────
CREATE TABLE products (
                          id                  UUID PRIMARY KEY DEFAULT gen_random_uuid(),
                          merchant_profile_id UUID NOT NULL REFERENCES merchant_profiles(id) ON DELETE CASCADE,
                          name                VARCHAR(200) NOT NULL,
                          category            VARCHAR(100),
                          unit                VARCHAR(50)   DEFAULT 'piece',
                          cost_price          NUMERIC(15,2),
                          selling_price       NUMERIC(15,2),
                          current_stock       NUMERIC(15,2) DEFAULT 0,
                          reorder_level       NUMERIC(15,2) DEFAULT 0,
                          is_active           BOOLEAN       DEFAULT TRUE,
                          created_at          TIMESTAMP     NOT NULL DEFAULT NOW(),
                          updated_at          TIMESTAMP     NOT NULL DEFAULT NOW()
);

CREATE INDEX idx_products_merchant  ON products(merchant_profile_id);
CREATE INDEX idx_products_category  ON products(merchant_profile_id, category);
CREATE INDEX idx_products_active    ON products(merchant_profile_id, is_active);

-- ── Sales Transactions ────────────────────────────────────────
CREATE TABLE sales_transactions (
                                    id                  UUID PRIMARY KEY DEFAULT gen_random_uuid(),
                                    merchant_profile_id UUID          NOT NULL REFERENCES merchant_profiles(id) ON DELETE CASCADE,
                                    product_id          UUID          REFERENCES products(id) ON DELETE SET NULL,
                                    product_name        VARCHAR(200)  NOT NULL,
                                    quantity            NUMERIC(15,2) NOT NULL CHECK (quantity > 0),
                                    unit_price          NUMERIC(15,2) NOT NULL CHECK (unit_price >= 0),
                                    total_amount        NUMERIC(15,2) GENERATED ALWAYS AS (quantity * unit_price) STORED,
                                    cost_price          NUMERIC(15,2),
                                    transaction_date    DATE          NOT NULL,
                                    payment_method      VARCHAR(50)   DEFAULT 'cash',
                                    notes               TEXT,
                                    source              VARCHAR(20)   DEFAULT 'manual',
                                    created_at          TIMESTAMP     NOT NULL DEFAULT NOW()
);

CREATE INDEX idx_sales_merchant ON sales_transactions(merchant_profile_id);
CREATE INDEX idx_sales_date     ON sales_transactions(merchant_profile_id, transaction_date DESC);
CREATE INDEX idx_sales_product  ON sales_transactions(product_id);

-- ── Expense Records ───────────────────────────────────────────
CREATE TABLE expense_records (
                                 id                  UUID PRIMARY KEY DEFAULT gen_random_uuid(),
                                 merchant_profile_id UUID          NOT NULL REFERENCES merchant_profiles(id) ON DELETE CASCADE,
                                 category            VARCHAR(100)  NOT NULL,
                                 description         VARCHAR(300),
                                 amount              NUMERIC(15,2) NOT NULL CHECK (amount > 0),
                                 expense_date        DATE          NOT NULL,
                                 payment_method      VARCHAR(50)   DEFAULT 'cash',
                                 notes               TEXT,
                                 source              VARCHAR(20)   DEFAULT 'manual',
                                 created_at          TIMESTAMP     NOT NULL DEFAULT NOW()
);

CREATE INDEX idx_expenses_merchant  ON expense_records(merchant_profile_id);
CREATE INDEX idx_expenses_date      ON expense_records(merchant_profile_id, expense_date DESC);
CREATE INDEX idx_expenses_category  ON expense_records(merchant_profile_id, category);

-- ── Inventory Snapshots ───────────────────────────────────────
CREATE TABLE inventory_snapshots (
                                     id                  UUID PRIMARY KEY DEFAULT gen_random_uuid(),
                                     merchant_profile_id UUID          NOT NULL REFERENCES merchant_profiles(id) ON DELETE CASCADE,
                                     product_id          UUID          REFERENCES products(id) ON DELETE SET NULL,
                                     product_name        VARCHAR(200)  NOT NULL,
                                     quantity_counted    NUMERIC(15,2) NOT NULL CHECK (quantity_counted >= 0),
                                     unit_cost           NUMERIC(15,2),
                                     total_value         NUMERIC(15,2) GENERATED ALWAYS AS (quantity_counted * COALESCE(unit_cost, 0)) STORED,
                                     snapshot_date       DATE          NOT NULL,
                                     notes               TEXT,
                                     created_at          TIMESTAMP     NOT NULL DEFAULT NOW()
);

CREATE INDEX idx_inventory_merchant ON inventory_snapshots(merchant_profile_id);
CREATE INDEX idx_inventory_date     ON inventory_snapshots(merchant_profile_id, snapshot_date DESC);
CREATE INDEX idx_inventory_product  ON inventory_snapshots(product_id);

-- ── CSV Upload Log ────────────────────────────────────────────
CREATE TABLE csv_upload_logs (
                                 id                  UUID PRIMARY KEY DEFAULT gen_random_uuid(),
                                 merchant_profile_id UUID         NOT NULL REFERENCES merchant_profiles(id) ON DELETE CASCADE,
                                 upload_type         VARCHAR(20)  NOT NULL,
                                 original_filename   VARCHAR(255),
                                 total_rows          INT          DEFAULT 0,
                                 processed_rows      INT          DEFAULT 0,
                                 failed_rows         INT          DEFAULT 0,
                                 status              VARCHAR(20)  DEFAULT 'processing',
                                 error_summary       TEXT,
                                 uploaded_at         TIMESTAMP    NOT NULL DEFAULT NOW(),
                                 completed_at        TIMESTAMP
);

CREATE INDEX idx_csv_uploads_merchant ON csv_upload_logs(merchant_profile_id);