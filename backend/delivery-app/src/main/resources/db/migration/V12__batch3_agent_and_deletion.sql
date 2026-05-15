-- V12: Batch 3 — agent API keys, annotations, delete requests, quarantine records

-- ============================================================
-- 1. data_agent_api_keys — agent API key management
-- ============================================================
CREATE TABLE IF NOT EXISTS data_agent_api_keys (
    id              BIGINT PRIMARY KEY AUTO_INCREMENT,
    key_name        VARCHAR(128)  NOT NULL COMMENT 'human-readable key name',
    key_prefix      VARCHAR(16)   NOT NULL COMMENT 'first 8 chars of plain key for identification',
    key_hash        VARCHAR(128)  NOT NULL COMMENT 'SHA-256 hash of full plain key',
    status          VARCHAR(16)   NOT NULL DEFAULT 'ACTIVE' COMMENT 'ACTIVE | REVOKED | EXPIRED',
    scope_type      VARCHAR(32)   NOT NULL DEFAULT 'SPECIFIC_PROJECTS' COMMENT 'ALL_PROJECTS | SPECIFIC_PROJECTS',
    expires_at      TIMESTAMP     NULL COMMENT 'expiration time, NULL = never',
    last_used_at    TIMESTAMP     NULL COMMENT 'last usage timestamp',
    last_used_ip    VARCHAR(64)   NULL COMMENT 'source IP of last usage',
    created_by      BIGINT        NOT NULL COMMENT 'user who created this key',
    revoked_by      BIGINT        NULL COMMENT 'user who revoked this key',
    revoked_at      TIMESTAMP     NULL COMMENT 'revocation timestamp',
    remark          VARCHAR(512)  NULL,
    created_at      TIMESTAMP     NOT NULL DEFAULT CURRENT_TIMESTAMP,
    updated_at      TIMESTAMP     NOT NULL DEFAULT CURRENT_TIMESTAMP ON UPDATE CURRENT_TIMESTAMP,
    deleted         TINYINT       NOT NULL DEFAULT 0,
    delete_token    BIGINT        NOT NULL DEFAULT 0,
    UNIQUE KEY uk_agent_key_hash (key_hash),
    KEY idx_agent_key_status (status),
    KEY idx_agent_key_created_by (created_by)
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COLLATE=utf8mb4_unicode_ci;

-- ============================================================
-- 2. data_agent_api_key_projects — authorized projects for SPECIFIC_PROJECTS keys
-- ============================================================
CREATE TABLE IF NOT EXISTS data_agent_api_key_projects (
    id              BIGINT PRIMARY KEY AUTO_INCREMENT,
    api_key_id      BIGINT        NOT NULL COMMENT 'FK to data_agent_api_keys.id',
    project_id      BIGINT        NOT NULL COMMENT 'FK to core_projects.id',
    created_at      TIMESTAMP     NOT NULL DEFAULT CURRENT_TIMESTAMP,
    KEY idx_akp_key (api_key_id),
    KEY idx_akp_project (project_id),
    CONSTRAINT fk_akp_key FOREIGN KEY (api_key_id) REFERENCES data_agent_api_keys(id) ON DELETE CASCADE
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COLLATE=utf8mb4_unicode_ci;

-- ============================================================
-- 3. data_agent_annotations — agent-submitted annotations (never directly modify formal assets)
-- ============================================================
CREATE TABLE IF NOT EXISTS data_agent_annotations (
    id              BIGINT PRIMARY KEY AUTO_INCREMENT,
    api_key_id      BIGINT        NOT NULL COMMENT 'FK to data_agent_api_keys.id',
    project_id      BIGINT        NOT NULL,
    target_type     VARCHAR(32)   NOT NULL COMMENT 'FILE_RESOURCE | MODEL_ASSET',
    target_id       BIGINT        NOT NULL,
    content         TEXT          NOT NULL COMMENT 'annotation text',
    status          VARCHAR(16)   NOT NULL DEFAULT 'SUBMITTED' COMMENT 'SUBMITTED | REVIEWED | DISMISSED',
    created_at      TIMESTAMP     NOT NULL DEFAULT CURRENT_TIMESTAMP,
    KEY idx_ann_key (api_key_id),
    KEY idx_ann_target (target_type, target_id),
    KEY idx_ann_project (project_id)
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COLLATE=utf8mb4_unicode_ci;

-- ============================================================
-- 4. data_asset_delete_requests — delete request with approval workflow
-- ============================================================
CREATE TABLE IF NOT EXISTS data_asset_delete_requests (
    id              BIGINT PRIMARY KEY AUTO_INCREMENT,
    request_no      VARCHAR(64)   NOT NULL COMMENT 'human-readable request number',
    project_id      BIGINT        NOT NULL,
    file_id         BIGINT        NOT NULL COMMENT 'FK to data_file_resources.id',
    delete_type     VARCHAR(16)   NOT NULL COMMENT 'LOGICAL | PHYSICAL',
    reason          TEXT          NOT NULL COMMENT 'reason for deletion',
    status          VARCHAR(16)   NOT NULL DEFAULT 'PENDING' COMMENT 'PENDING | APPROVED | REJECTED | CANCELED | EXECUTED',
    requested_by_type VARCHAR(16) NOT NULL DEFAULT 'USER' COMMENT 'USER | AGENT',
    requested_by    BIGINT        NOT NULL COMMENT 'user ID or agent key ID (prefixed for agent)',
    approved_by     BIGINT        NULL,
    approved_at     TIMESTAMP     NULL,
    rejected_by     BIGINT        NULL,
    rejected_at     TIMESTAMP     NULL,
    executed_by     BIGINT        NULL,
    executed_at     TIMESTAMP     NULL,
    failure_reason  TEXT          NULL,
    created_at      TIMESTAMP     NOT NULL DEFAULT CURRENT_TIMESTAMP,
    updated_at      TIMESTAMP     NOT NULL DEFAULT CURRENT_TIMESTAMP ON UPDATE CURRENT_TIMESTAMP,
    deleted         TINYINT       NOT NULL DEFAULT 0,
    delete_token    BIGINT        NOT NULL DEFAULT 0,
    UNIQUE KEY uk_delete_request_no (request_no),
    KEY idx_dr_status (status),
    KEY idx_dr_project (project_id),
    KEY idx_dr_file (file_id)
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COLLATE=utf8mb4_unicode_ci;

-- ============================================================
-- 5. data_asset_quarantine_records — physical deletion quarantine tracking
-- ============================================================
CREATE TABLE IF NOT EXISTS data_asset_quarantine_records (
    id                  BIGINT PRIMARY KEY AUTO_INCREMENT,
    delete_request_id   BIGINT        NOT NULL COMMENT 'FK to data_asset_delete_requests.id',
    project_id          BIGINT        NOT NULL,
    file_id             BIGINT        NOT NULL COMMENT 'FK to data_file_resources.id',
    original_path       VARCHAR(1024) NOT NULL COMMENT 'original NAS file path',
    quarantine_path     VARCHAR(1024) NOT NULL COMMENT 'quarantine storage path',
    status              VARCHAR(32)   NOT NULL DEFAULT 'QUARANTINED' COMMENT 'QUARANTINED | RESTORED | PERMANENT_DELETED | FAILED',
    quarantine_until    TIMESTAMP     NOT NULL COMMENT 'quarantine expiry (default +30 days)',
    requested_by_type   VARCHAR(16)   NOT NULL DEFAULT 'USER',
    requested_by        BIGINT        NOT NULL,
    approved_by         BIGINT        NULL,
    executed_by         BIGINT        NULL,
    restored_by         BIGINT        NULL,
    permanent_deleted_by BIGINT       NULL,
    failure_reason      TEXT          NULL,
    created_at          TIMESTAMP     NOT NULL DEFAULT CURRENT_TIMESTAMP,
    updated_at          TIMESTAMP     NOT NULL DEFAULT CURRENT_TIMESTAMP ON UPDATE CURRENT_TIMESTAMP,
    KEY idx_qr_status (status),
    KEY idx_qr_delete_request (delete_request_id),
    KEY idx_qr_project (project_id),
    KEY idx_qr_file (file_id),
    KEY idx_qr_quarantine_until (quarantine_until)
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COLLATE=utf8mb4_unicode_ci;

-- Extend data_asset_events.event_type to cover agent and deletion event types
-- (event_type is VARCHAR, so no DDL change needed — just documentation)
-- New event types: AGENT_KEY, AGENT_QUERY, AGENT_ANNOTATION, DELETE_REQUEST, DELETE_APPROVAL, DELETE_QUARANTINE, DELETE_RESTORE, DELETE_PERMANENT
