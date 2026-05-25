-- M2I: file ownership governance.
-- This table stores project-relative ownership metadata only.
-- It must not store raw NAS paths, storage_uri, file body evidence, or parser output.

CREATE TABLE IF NOT EXISTS data_file_ownership_assignments (
    id BIGINT PRIMARY KEY AUTO_INCREMENT,
    project_id BIGINT NOT NULL,
    file_id BIGINT NOT NULL,
    section_node_id BIGINT NULL,
    node_key VARCHAR(128) NOT NULL,
    node_label VARCHAR(255) NOT NULL,
    node_path VARCHAR(1024) NOT NULL,
    ownership_type VARCHAR(32) NOT NULL,
    status VARCHAR(32) NOT NULL DEFAULT 'SUGGESTED',
    confidence VARCHAR(16) NOT NULL DEFAULT 'MEDIUM',
    source VARCHAR(32) NOT NULL DEFAULT 'RULE',
    reason VARCHAR(1024) NULL,
    evidence_summary VARCHAR(1024) NULL,
    confirmed_by BIGINT NULL,
    confirmed_at TIMESTAMP NULL,
    rejected_by BIGINT NULL,
    rejected_at TIMESTAMP NULL,
    created_by BIGINT NULL,
    updated_by BIGINT NULL,
    created_at TIMESTAMP NOT NULL DEFAULT CURRENT_TIMESTAMP,
    updated_at TIMESTAMP NOT NULL DEFAULT CURRENT_TIMESTAMP ON UPDATE CURRENT_TIMESTAMP,
    deleted TINYINT NOT NULL DEFAULT 0,
    delete_token BIGINT NOT NULL DEFAULT 0,
    UNIQUE KEY uk_file_ownership_file_token (file_id, delete_token),
    KEY idx_file_ownership_project_status (project_id, status),
    KEY idx_file_ownership_project_node (project_id, node_key),
    KEY idx_file_ownership_section_node (section_node_id),
    CONSTRAINT fk_file_ownership_project FOREIGN KEY (project_id) REFERENCES core_projects (id),
    CONSTRAINT fk_file_ownership_file FOREIGN KEY (file_id) REFERENCES data_file_resources (id),
    CONSTRAINT fk_file_ownership_section FOREIGN KEY (section_node_id) REFERENCES masterdata_section_nodes (id)
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COLLATE=utf8mb4_unicode_ci;
