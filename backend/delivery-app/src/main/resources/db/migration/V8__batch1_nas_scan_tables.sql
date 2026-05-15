-- V8: Batch 1 data base & NAS scan tables
-- Adds: project path mappings, scan tasks, scan candidates, disciplines, import rows
-- Extends data_file_resources with batch 1 columns

ALTER TABLE data_file_resources
    ADD COLUMN logical_path VARCHAR(1024) NULL AFTER storage_uri,
    ADD COLUMN review_status VARCHAR(32) NOT NULL DEFAULT 'PENDING' AFTER process_status,
    ADD COLUMN confidence_level VARCHAR(32) NULL AFTER review_status;

ALTER TABLE data_file_resources
    MODIFY COLUMN storage_provider VARCHAR(32) NOT NULL DEFAULT 'NAS';

CREATE INDEX idx_data_file_review_status
    ON data_file_resources (project_id, review_status);

CREATE TABLE IF NOT EXISTS data_asset_project_path_mappings (
    id BIGINT PRIMARY KEY AUTO_INCREMENT,
    project_id BIGINT NOT NULL,
    provider_code VARCHAR(32) NOT NULL DEFAULT 'NAS',
    nas_path VARCHAR(1024) NOT NULL,
    nas_path_hash CHAR(64) GENERATED ALWAYS AS (SHA2(nas_path, 256)) STORED,
    match_strategy VARCHAR(32) NOT NULL DEFAULT 'PREFIX',
    enabled TINYINT NOT NULL DEFAULT 1,
    sort_order INT NOT NULL DEFAULT 0,
    remark VARCHAR(512) NULL,
    created_at TIMESTAMP NOT NULL DEFAULT CURRENT_TIMESTAMP,
    updated_at TIMESTAMP NOT NULL DEFAULT CURRENT_TIMESTAMP ON UPDATE CURRENT_TIMESTAMP,
    created_by BIGINT NULL,
    updated_by BIGINT NULL,
    deleted TINYINT NOT NULL DEFAULT 0,
    delete_token BIGINT NOT NULL DEFAULT 0,
    UNIQUE KEY uk_asset_path_project_nas_token (project_id, nas_path_hash, delete_token),
    KEY idx_asset_path_project (project_id, enabled),
    CONSTRAINT fk_asset_path_project FOREIGN KEY (project_id) REFERENCES core_projects (id)
);

CREATE TABLE IF NOT EXISTS data_asset_scan_tasks (
    id BIGINT PRIMARY KEY AUTO_INCREMENT,
    root_code VARCHAR(64) NOT NULL,
    root_path VARCHAR(1024) NOT NULL,
    project_id BIGINT NULL,
    project_code VARCHAR(64) NULL,
    `recursive` TINYINT NOT NULL DEFAULT 1,
    extensions VARCHAR(512) NULL,
    status VARCHAR(32) NOT NULL DEFAULT 'PENDING',
    progress_message VARCHAR(512) NULL,
    total_scanned INT NOT NULL DEFAULT 0,
    auto_ingested INT NOT NULL DEFAULT 0,
    pending_review INT NOT NULL DEFAULT 0,
    failed_count INT NOT NULL DEFAULT 0,
    failure_reason TEXT NULL,
    started_at TIMESTAMP NULL,
    completed_at TIMESTAMP NULL,
    created_at TIMESTAMP NOT NULL DEFAULT CURRENT_TIMESTAMP,
    updated_at TIMESTAMP NOT NULL DEFAULT CURRENT_TIMESTAMP ON UPDATE CURRENT_TIMESTAMP,
    created_by BIGINT NULL,
    updated_by BIGINT NULL,
    deleted TINYINT NOT NULL DEFAULT 0,
    KEY idx_asset_scan_task_status (status),
    KEY idx_asset_scan_task_project (project_id),
    KEY idx_asset_scan_task_created (created_at DESC)
);

CREATE TABLE IF NOT EXISTS data_asset_scan_candidates (
    id BIGINT PRIMARY KEY AUTO_INCREMENT,
    scan_task_id BIGINT NOT NULL,
    matched_project_id BIGINT NULL,
    matched_project_code VARCHAR(64) NULL,
    raw_path VARCHAR(1024) NOT NULL,
    file_name VARCHAR(255) NOT NULL,
    file_ext VARCHAR(32) NOT NULL,
    size_bytes BIGINT NOT NULL DEFAULT 0,
    last_modified_at TIMESTAMP NULL,
    detected_file_kind VARCHAR(32) NULL,
    detected_discipline VARCHAR(64) NULL,
    detected_version_no VARCHAR(32) NOT NULL DEFAULT 'V1',
    confidence_level VARCHAR(32) NOT NULL DEFAULT 'LOW',
    review_status VARCHAR(32) NOT NULL DEFAULT 'PENDING',
    review_message VARCHAR(512) NULL,
    reviewed_by BIGINT NULL,
    reviewed_at TIMESTAMP NULL,
    created_file_resource_id BIGINT NULL,
    created_at TIMESTAMP NOT NULL DEFAULT CURRENT_TIMESTAMP,
    updated_at TIMESTAMP NOT NULL DEFAULT CURRENT_TIMESTAMP ON UPDATE CURRENT_TIMESTAMP,
    created_by BIGINT NULL,
    updated_by BIGINT NULL,
    deleted TINYINT NOT NULL DEFAULT 0,
    delete_token BIGINT NOT NULL DEFAULT 0,
    KEY idx_asset_candidate_scan (scan_task_id),
    KEY idx_asset_candidate_review (review_status),
    KEY idx_asset_candidate_project (matched_project_id),
    CONSTRAINT fk_asset_candidate_scan FOREIGN KEY (scan_task_id) REFERENCES data_asset_scan_tasks (id)
);

CREATE TABLE IF NOT EXISTS data_asset_disciplines (
    id BIGINT PRIMARY KEY AUTO_INCREMENT,
    code VARCHAR(64) NOT NULL,
    name VARCHAR(128) NOT NULL,
    project_id BIGINT NULL,
    project_id_fix BIGINT GENERATED ALWAYS AS (COALESCE(project_id, 0)) STORED,
    scope VARCHAR(32) NOT NULL DEFAULT 'BUILTIN',
    sort_order INT NOT NULL DEFAULT 0,
    created_at TIMESTAMP NOT NULL DEFAULT CURRENT_TIMESTAMP,
    UNIQUE KEY uk_asset_discipline_code_project (code, project_id_fix)
);

INSERT INTO data_asset_disciplines (code, name, scope, sort_order) VALUES
    ('ARCHITECTURE', '建筑', 'BUILTIN', 1),
    ('STRUCTURE', '结构', 'BUILTIN', 2),
    ('PLUMBING', '给排水', 'BUILTIN', 3),
    ('HVAC', '暖通', 'BUILTIN', 4),
    ('ELECTRICAL', '电气', 'BUILTIN', 5),
    ('FIRE_PROTECTION', '消防', 'BUILTIN', 6),
    ('INTELLIGENT', '智能化', 'BUILTIN', 7),
    ('GENERAL', '综合', 'BUILTIN', 8),
    ('OTHER', '其他', 'BUILTIN', 9)
ON DUPLICATE KEY UPDATE name = VALUES(name), sort_order = VALUES(sort_order);

CREATE TABLE IF NOT EXISTS data_asset_import_rows (
    id BIGINT PRIMARY KEY AUTO_INCREMENT,
    import_job_id BIGINT NOT NULL,
    row_no INT NOT NULL DEFAULT 0,
    raw_data TEXT NULL,
    success TINYINT NOT NULL DEFAULT 1,
    target_type VARCHAR(32) NULL,
    target_id BIGINT NULL,
    error_code VARCHAR(64) NULL,
    error_message VARCHAR(512) NULL,
    created_at TIMESTAMP NOT NULL DEFAULT CURRENT_TIMESTAMP,
    KEY idx_asset_import_row_job (import_job_id),
    CONSTRAINT fk_asset_import_row_job FOREIGN KEY (import_job_id) REFERENCES data_asset_import_jobs (id)
);
