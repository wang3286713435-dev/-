CREATE TABLE IF NOT EXISTS core_project_business_profiles (
    id BIGINT PRIMARY KEY AUTO_INCREMENT,
    project_id BIGINT NOT NULL,
    budget_amount DECIMAL(18, 2) NULL,
    contract_amount DECIMAL(18, 2) NULL,
    received_amount DECIMAL(18, 2) NULL,
    payment_status VARCHAR(32) NOT NULL DEFAULT 'UNSET',
    expected_payment_date DATE NULL,
    planned_start_date DATE NULL,
    planned_delivery_date DATE NULL,
    actual_delivery_date DATE NULL,
    currency_code VARCHAR(16) NOT NULL DEFAULT 'CNY',
    business_remark VARCHAR(1000) NULL,
    created_at TIMESTAMP NOT NULL DEFAULT CURRENT_TIMESTAMP,
    updated_at TIMESTAMP NOT NULL DEFAULT CURRENT_TIMESTAMP ON UPDATE CURRENT_TIMESTAMP,
    created_by BIGINT NULL,
    updated_by BIGINT NULL,
    deleted TINYINT NOT NULL DEFAULT 0,
    UNIQUE KEY uk_core_project_business_profiles_project (project_id),
    KEY idx_core_project_business_profiles_payment_status (payment_status),
    KEY idx_core_project_business_profiles_delivery_date (planned_delivery_date),
    CONSTRAINT fk_core_project_business_profiles_project
        FOREIGN KEY (project_id) REFERENCES core_projects (id)
);
