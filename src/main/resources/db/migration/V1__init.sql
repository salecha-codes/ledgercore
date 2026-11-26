CREATE TABLE accounts (
    id                UUID PRIMARY KEY,
    owner_name        VARCHAR(120) NOT NULL,
    currency          VARCHAR(3)   NOT NULL,
    account_type      VARCHAR(16)  NOT NULL,
    status            VARCHAR(16)  NOT NULL,
    available_balance BIGINT       NOT NULL DEFAULT 0,
    held_balance      BIGINT       NOT NULL DEFAULT 0,
    created_at        TIMESTAMP WITH TIME ZONE NOT NULL,

    CONSTRAINT chk_account_type CHECK (account_type IN ('USER', 'TREASURY')),
    CONSTRAINT chk_account_status CHECK (status IN ('ACTIVE', 'FROZEN')),
    CONSTRAINT chk_account_currency CHECK (currency IN ('INR', 'USD', 'JPY', 'BHD')),
    CONSTRAINT chk_non_negative_user_balance CHECK (
        account_type = 'TREASURY' OR (available_balance >= 0 AND held_balance >= 0)
    )
);

CREATE INDEX idx_accounts_type_currency ON accounts (account_type, currency);

INSERT INTO accounts (id, owner_name, currency, account_type, status, available_balance, held_balance, created_at)
VALUES
    ('00000000-0000-0000-0000-000000000001', 'SYSTEM_TREASURY_INR', 'INR', 'TREASURY', 'ACTIVE', 0, 0, CURRENT_TIMESTAMP),
    ('00000000-0000-0000-0000-000000000002', 'SYSTEM_TREASURY_USD', 'USD', 'TREASURY', 'ACTIVE', 0, 0, CURRENT_TIMESTAMP),
    ('00000000-0000-0000-0000-000000000003', 'SYSTEM_TREASURY_JPY', 'JPY', 'TREASURY', 'ACTIVE', 0, 0, CURRENT_TIMESTAMP),
    ('00000000-0000-0000-0000-000000000004', 'SYSTEM_TREASURY_BHD', 'BHD', 'TREASURY', 'ACTIVE', 0, 0, CURRENT_TIMESTAMP);
