-- M2A: controlled NAS write foundation
-- This batch stores only project-relative display paths in operation/quarantine tables.
-- Physical NAS paths remain confined to existing path mappings and file metadata.

CREATE TABLE IF NOT EXISTS data_nas_directory_records (
    id BIGINT PRIMARY KEY AUTO_INCREMENT,
    project_id BIGINT NOT NULL,
    relative_path VARCHAR(1024) NOT NULL,
    relative_path_hash CHAR(64) GENERATED ALWAYS AS (SHA2(relative_path, 256)) STORED,
    display_name VARCHAR(255) NOT NULL,
    parent_relative_path VARCHAR(1024) NULL,
    status VARCHAR(32) NOT NULL DEFAULT 'ACTIVE',
    quarantine_record_id BIGINT NULL,
    created_at TIMESTAMP NOT NULL DEFAULT CURRENT_TIMESTAMP,
    updated_at TIMESTAMP NOT NULL DEFAULT CURRENT_TIMESTAMP ON UPDATE CURRENT_TIMESTAMP,
    created_by BIGINT NULL,
    updated_by BIGINT NULL,
    deleted TINYINT NOT NULL DEFAULT 0,
    delete_token BIGINT NOT NULL DEFAULT 0,
    UNIQUE KEY uk_nas_directory_project_path_token (project_id, relative_path_hash, delete_token),
    KEY idx_nas_directory_project_status (project_id, status),
    CONSTRAINT fk_nas_directory_project FOREIGN KEY (project_id) REFERENCES core_projects (id)
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COLLATE=utf8mb4_unicode_ci;

CREATE TABLE IF NOT EXISTS data_nas_quarantine_records (
    id BIGINT PRIMARY KEY AUTO_INCREMENT,
    project_id BIGINT NOT NULL,
    target_type VARCHAR(32) NOT NULL,
    file_id BIGINT NULL,
    directory_id BIGINT NULL,
    original_relative_path VARCHAR(1024) NOT NULL,
    quarantine_relative_path VARCHAR(1024) NOT NULL,
    display_name VARCHAR(255) NOT NULL,
    status VARCHAR(32) NOT NULL DEFAULT 'QUARANTINED',
    reason VARCHAR(512) NULL,
    quarantine_until TIMESTAMP NOT NULL,
    created_at TIMESTAMP NOT NULL DEFAULT CURRENT_TIMESTAMP,
    updated_at TIMESTAMP NOT NULL DEFAULT CURRENT_TIMESTAMP ON UPDATE CURRENT_TIMESTAMP,
    created_by BIGINT NULL,
    restored_by BIGINT NULL,
    restored_at TIMESTAMP NULL,
    failure_reason VARCHAR(512) NULL,
    KEY idx_nas_quarantine_project_status (project_id, status),
    KEY idx_nas_quarantine_file (file_id),
    KEY idx_nas_quarantine_directory (directory_id),
    CONSTRAINT fk_nas_quarantine_project FOREIGN KEY (project_id) REFERENCES core_projects (id)
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COLLATE=utf8mb4_unicode_ci;

CREATE TABLE IF NOT EXISTS data_nas_operation_records (
    id BIGINT PRIMARY KEY AUTO_INCREMENT,
    project_id BIGINT NOT NULL,
    operation_type VARCHAR(48) NOT NULL,
    target_type VARCHAR(32) NOT NULL,
    target_id BIGINT NULL,
    file_id BIGINT NULL,
    directory_id BIGINT NULL,
    quarantine_record_id BIGINT NULL,
    source_path_hash CHAR(64) NULL,
    target_path_hash CHAR(64) NULL,
    source_display_path VARCHAR(1024) NULL,
    target_display_path VARCHAR(1024) NULL,
    status VARCHAR(32) NOT NULL,
    message VARCHAR(512) NULL,
    failure_reason VARCHAR(512) NULL,
    trace_id VARCHAR(64) NULL,
    created_at TIMESTAMP NOT NULL DEFAULT CURRENT_TIMESTAMP,
    created_by BIGINT NULL,
    KEY idx_nas_operation_project_created (project_id, created_at),
    KEY idx_nas_operation_trace (trace_id),
    CONSTRAINT fk_nas_operation_project FOREIGN KEY (project_id) REFERENCES core_projects (id)
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COLLATE=utf8mb4_unicode_ci;
