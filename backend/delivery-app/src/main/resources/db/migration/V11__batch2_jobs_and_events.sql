-- V11: Batch 2 — async job task table + event stream table + checksum support

-- ============================================================
-- 1. data_asset_jobs — universal async job task table
-- ============================================================
CREATE TABLE IF NOT EXISTS data_asset_jobs (
    id              BIGINT PRIMARY KEY AUTO_INCREMENT,
    job_type        VARCHAR(32)  NOT NULL COMMENT 'NAS_SCAN | CHECKSUM_CALC | QUARANTINE_CLEANUP | PERMANENT_DELETE',
    status          VARCHAR(32)  NOT NULL DEFAULT 'PENDING' COMMENT 'PENDING | RUNNING | SUCCEEDED | FAILED | CANCELED',
    project_id      BIGINT       NULL COMMENT 'nullable for global jobs',
    target_type     VARCHAR(64)  NULL COMMENT 'entity type being acted upon: FILE_RESOURCE | SCAN_TASK | etc',
    target_id       BIGINT       NULL COMMENT 'entity ID being acted upon',
    request_payload JSON         NULL COMMENT 'job-specific request payload',
    progress_current INT NOT NULL DEFAULT 0 COMMENT 'current progress count',
    progress_total  INT NOT NULL DEFAULT 0 COMMENT 'total count for progress tracking',
    progress_percent DECIMAL(5,2) NOT NULL DEFAULT 0.00 COMMENT '0.00-100.00',
    progress_message VARCHAR(512) NULL COMMENT 'human-readable progress message',
    failure_reason  TEXT         NULL COMMENT 'detailed failure reason',
    retry_count     INT NOT NULL DEFAULT 0 COMMENT 'retry attempt count',
    max_retries     INT NOT NULL DEFAULT 3 COMMENT 'max retry attempts allowed',
    created_by      BIGINT       NULL,
    started_at      TIMESTAMP    NULL,
    completed_at    TIMESTAMP    NULL,
    created_at      TIMESTAMP    NOT NULL DEFAULT CURRENT_TIMESTAMP,
    updated_at      TIMESTAMP    NOT NULL DEFAULT CURRENT_TIMESTAMP ON UPDATE CURRENT_TIMESTAMP,
    deleted         TINYINT      NOT NULL DEFAULT 0,
    delete_token    BIGINT       NOT NULL DEFAULT 0,
    KEY idx_asset_job_type (job_type),
    KEY idx_asset_job_status (status),
    KEY idx_asset_job_project (project_id),
    KEY idx_asset_job_created (created_at DESC),
    KEY idx_asset_job_target (target_type, target_id)
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COLLATE=utf8mb4_unicode_ci;

-- ============================================================
-- 2. data_asset_events — event stream for incremental sync
-- ============================================================
CREATE TABLE IF NOT EXISTS data_asset_events (
    id              BIGINT PRIMARY KEY AUTO_INCREMENT,
    event_type      VARCHAR(64)  NOT NULL COMMENT 'event category: SCAN | FILE | CHECKSUM | PROJECT | MAPPING | REVIEW | JOB',
    project_id      BIGINT       NULL COMMENT 'nullable for global events',
    aggregate_type  VARCHAR(64)  NULL COMMENT 'entity type: PROJECT | FILE_RESOURCE | SCAN_TASK | SCAN_CANDIDATE | JOB',
    aggregate_id    VARCHAR(128) NULL COMMENT 'entity identifier',
    action_code     VARCHAR(64)  NOT NULL COMMENT 'specific action: create | update | start | success | fail',
    operator_id     BIGINT       NULL COMMENT 'acting user ID',
    source_type     VARCHAR(32)  NULL COMMENT 'origin: API | SCAN | REVIEW | SYSTEM',
    summary         VARCHAR(512) NULL COMMENT 'human-readable event summary',
    payload_json    JSON         NULL COMMENT 'event-specific payload',
    trace_id        VARCHAR(64)  NULL COMMENT 'request trace ID for correlation',
    created_at      TIMESTAMP    NOT NULL DEFAULT CURRENT_TIMESTAMP,
    KEY idx_asset_event_type (event_type),
    KEY idx_asset_event_project (project_id),
    KEY idx_asset_event_action (action_code),
    KEY idx_asset_event_aggregate (aggregate_type, aggregate_id),
    KEY idx_asset_event_created (created_at DESC)
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COLLATE=utf8mb4_unicode_ci;
