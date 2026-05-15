-- V15: Nonstandard NAS directory governance list
-- Stores directories that are intentionally kept out of official asset views
-- until agent-assisted analysis and human confirmation are complete.

CREATE TABLE IF NOT EXISTS data_asset_nonstandard_directories (
    id BIGINT PRIMARY KEY AUTO_INCREMENT,
    provider_code VARCHAR(32) NOT NULL DEFAULT 'NAS',
    root_path VARCHAR(1024) NOT NULL,
    directory_name VARCHAR(255) NOT NULL,
    nas_path VARCHAR(1024) NOT NULL,
    nas_path_hash CHAR(64) GENERATED ALWAYS AS (SHA2(nas_path, 256)) STORED,
    directory_type VARCHAR(32) NOT NULL DEFAULT 'UNKNOWN',
    risk_type VARCHAR(64) NOT NULL,
    governance_status VARCHAR(32) NOT NULL DEFAULT 'PENDING_AGENT',
    suggested_project_code VARCHAR(128) NULL,
    suggested_project_name VARCHAR(255) NULL,
    duplicate_base_code VARCHAR(64) NULL,
    standard_folders_json JSON NULL,
    review_reason VARCHAR(512) NULL,
    agent_suggestion TEXT NULL,
    manual_decision VARCHAR(512) NULL,
    decision_reason VARCHAR(512) NULL,
    owner_name VARCHAR(128) NULL,
    decided_by BIGINT NULL,
    decided_at TIMESTAMP NULL,
    created_at TIMESTAMP NOT NULL DEFAULT CURRENT_TIMESTAMP,
    updated_at TIMESTAMP NOT NULL DEFAULT CURRENT_TIMESTAMP ON UPDATE CURRENT_TIMESTAMP,
    created_by BIGINT NULL,
    updated_by BIGINT NULL,
    deleted TINYINT NOT NULL DEFAULT 0,
    delete_token BIGINT NOT NULL DEFAULT 0,
    UNIQUE KEY uk_nonstandard_nas_path_token (nas_path_hash, delete_token),
    KEY idx_nonstandard_status (governance_status),
    KEY idx_nonstandard_risk (risk_type),
    KEY idx_nonstandard_created_by (created_by),
    KEY idx_nonstandard_updated (updated_at DESC)
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COLLATE=utf8mb4_unicode_ci;
