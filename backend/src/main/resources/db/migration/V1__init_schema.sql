-- =====================================================================
-- SmartSpender - V1 Initial Schema
-- PostgreSQL 16
--
-- Design notes:
--   - TIMESTAMPTZ everywhere so timezone is never ambiguous
--   - NUMERIC(14,2) for money — never FLOAT/DOUBLE
--   - ON DELETE CASCADE from users; SET NULL on transactions.category_id
--     so deleting a category leaves historical transactions intact
--   - name (not merchant) — works for "Starbucks" and "September Salary"
-- =====================================================================

-- ---------------------------------------------------------------------
-- USERS
-- ---------------------------------------------------------------------
CREATE TABLE users (
    id                    BIGSERIAL       PRIMARY KEY,
    email                 VARCHAR(255)    NOT NULL UNIQUE,
    password_hash         VARCHAR(255)    NOT NULL,
    display_name          VARCHAR(100)    NOT NULL,
    currency_pref         VARCHAR(3)      NOT NULL DEFAULT 'USD',
    email_verified        BOOLEAN         NOT NULL DEFAULT FALSE,
    verification_token    VARCHAR(64)     NULL,
    verification_token_expires_at TIMESTAMPTZ NULL,
    pending_email         VARCHAR(255)    NULL,
    pending_email_token   VARCHAR(64)     NULL,
    pending_email_token_expires_at TIMESTAMPTZ NULL,
    created_at            TIMESTAMPTZ     NOT NULL DEFAULT NOW(),
    updated_at            TIMESTAMPTZ     NOT NULL DEFAULT NOW()
);
CREATE INDEX idx_users_email ON users (email);
CREATE INDEX idx_users_verification_token ON users (verification_token);
CREATE INDEX idx_users_pending_email_token ON users (pending_email_token);

-- ---------------------------------------------------------------------
-- CATEGORIES
-- Every category belongs to exactly one user. Names AND colors are unique
-- per user, so a donut chart never confuses two slices.
-- ---------------------------------------------------------------------
CREATE TABLE categories (
    id          BIGSERIAL       PRIMARY KEY,
    user_id     BIGINT          NOT NULL,
    name        VARCHAR(80)     NOT NULL,
    icon        VARCHAR(50)     NOT NULL DEFAULT 'pi pi-tag',
    color       VARCHAR(7)      NOT NULL,
    is_default  BOOLEAN         NOT NULL DEFAULT FALSE,
    created_at  TIMESTAMPTZ     NOT NULL DEFAULT NOW(),

    CONSTRAINT fk_categories_user
        FOREIGN KEY (user_id) REFERENCES users (id) ON DELETE CASCADE,
    CONSTRAINT uq_categories_user_name  UNIQUE (user_id, name),
    CONSTRAINT uq_categories_user_color UNIQUE (user_id, color),
    CONSTRAINT chk_categories_color_format CHECK (color ~ '^#[0-9A-Fa-f]{6}$')
);
CREATE INDEX idx_categories_user ON categories (user_id);

-- ---------------------------------------------------------------------
-- TRANSACTIONS
-- name is the human label: "Starbucks", "September Salary", "Rent".
-- category_id is NOT NULL — every transaction is categorized.
-- ---------------------------------------------------------------------
CREATE TABLE transactions (
    id           BIGSERIAL       PRIMARY KEY,
    user_id      BIGINT          NOT NULL,
    category_id  BIGINT          NOT NULL,
    amount       NUMERIC(14, 2)  NOT NULL,
    type         VARCHAR(10)     NOT NULL,
    name         VARCHAR(150)    NOT NULL,
    notes        VARCHAR(500)    NULL,
    txn_date     DATE            NOT NULL,
    created_at   TIMESTAMPTZ     NOT NULL DEFAULT NOW(),
    updated_at   TIMESTAMPTZ     NOT NULL DEFAULT NOW(),

    CONSTRAINT fk_transactions_user
        FOREIGN KEY (user_id) REFERENCES users (id) ON DELETE CASCADE,
    CONSTRAINT fk_transactions_category
        FOREIGN KEY (category_id) REFERENCES categories (id) ON DELETE RESTRICT,
    CONSTRAINT chk_transactions_amount_positive CHECK (amount > 0),
    CONSTRAINT chk_transactions_type CHECK (type IN ('EXPENSE', 'INCOME')),
    CONSTRAINT chk_transactions_name_not_blank CHECK (LENGTH(TRIM(name)) > 0)
);
CREATE INDEX idx_transactions_user_date
    ON transactions (user_id, txn_date DESC);
CREATE INDEX idx_transactions_user_category_date
    ON transactions (user_id, category_id, txn_date);
CREATE INDEX idx_transactions_user_type_date
    ON transactions (user_id, type, txn_date);

-- ---------------------------------------------------------------------
-- BUDGETS
-- One envelope = one user + category + month + year.
-- ---------------------------------------------------------------------
CREATE TABLE budgets (
    id            BIGSERIAL       PRIMARY KEY,
    user_id       BIGINT          NOT NULL,
    category_id   BIGINT          NOT NULL,
    limit_amount  NUMERIC(14, 2)  NOT NULL,
    month         SMALLINT        NOT NULL,
    year          SMALLINT        NOT NULL,
    created_at    TIMESTAMPTZ     NOT NULL DEFAULT NOW(),
    updated_at    TIMESTAMPTZ     NOT NULL DEFAULT NOW(),

    CONSTRAINT fk_budgets_user
        FOREIGN KEY (user_id) REFERENCES users (id) ON DELETE CASCADE,
    CONSTRAINT fk_budgets_category
        FOREIGN KEY (category_id) REFERENCES categories (id) ON DELETE CASCADE,
    CONSTRAINT chk_budgets_limit_positive CHECK (limit_amount > 0),
    CONSTRAINT chk_budgets_month CHECK (month BETWEEN 1 AND 12),
    CONSTRAINT chk_budgets_year  CHECK (year BETWEEN 2000 AND 2100),
    CONSTRAINT uq_budgets_user_category_period
        UNIQUE (user_id, category_id, month, year)
);
CREATE INDEX idx_budgets_user_period ON budgets (user_id, year, month);