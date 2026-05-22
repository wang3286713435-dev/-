-- M2C: delivery package draft and archive directory manifest.
-- Stores only catalog-derived checklist snapshots and semantic archive paths.
-- It must not store raw NAS paths, storage_uri, or copied package artifacts.

CREATE TABLE IF NOT EXISTS work_delivery_package_drafts (
    id BIGINT PRIMARY KEY AUTO_INCREMENT,
    project_id BIGINT NOT NULL,
    view_type VARCHAR(16) NOT NULL,
    target_type VARCHAR(16) NOT NULL DEFAULT 'SECTION',
    total_count INT NOT NULL DEFAULT 0,
    ready_count INT NOT NULL DEFAULT 0,
    blocked_count INT NOT NULL DEFAULT 0,
    missing_count INT NOT NULL DEFAULT 0,
    pending_review_count INT NOT NULL DEFAULT 0,
    rejected_count INT NOT NULL DEFAULT 0,
    conversion_required_count INT NOT NULL DEFAULT 0,
    unsupported_preview_count INT NOT NULL DEFAULT 0,
    dry_run TINYINT NOT NULL DEFAULT 1,
    physical_package_generated TINYINT NOT NULL DEFAULT 0,
    nas_file_copied TINYINT NOT NULL DEFAULT 0,
    created_by BIGINT NULL,
    created_at TIMESTAMP NOT NULL DEFAULT CURRENT_TIMESTAMP,
    deleted TINYINT NOT NULL DEFAULT 0,
    delete_token BIGINT NOT NULL DEFAULT 0,
    KEY idx_delivery_package_draft_project_created (project_id, created_at),
    KEY idx_delivery_package_draft_view_target (project_id, view_type, target_type),
    CONSTRAINT fk_delivery_package_draft_project FOREIGN KEY (project_id) REFERENCES core_projects (id)
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COLLATE=utf8mb4_unicode_ci;

CREATE TABLE IF NOT EXISTS work_delivery_package_draft_items (
    id BIGINT PRIMARY KEY AUTO_INCREMENT,
    draft_id BIGINT NOT NULL,
    project_id BIGINT NOT NULL,
    sort_order INT NOT NULL DEFAULT 0,
    target_type VARCHAR(16) NOT NULL,
    target_id BIGINT NULL,
    target_name VARCHAR(255) NULL,
    deliverable_definition_id BIGINT NULL,
    deliverable_definition_name VARCHAR(255) NULL,
    deliverable_type_id BIGINT NULL,
    deliverable_type_name VARCHAR(255) NULL,
    binding_id BIGINT NULL,
    file_id BIGINT NULL,
    file_name VARCHAR(512) NULL,
    file_kind VARCHAR(32) NULL,
    version_no VARCHAR(64) NULL,
    review_status VARCHAR(32) NULL,
    preview_status VARCHAR(32) NULL,
    export_status VARCHAR(32) NOT NULL,
    block_reason VARCHAR(1024) NULL,
    archive_directory_path VARCHAR(1024) NOT NULL,
    created_at TIMESTAMP NOT NULL DEFAULT CURRENT_TIMESTAMP,
    KEY idx_delivery_package_draft_item_draft (draft_id, sort_order),
    KEY idx_delivery_package_draft_item_project_status (project_id, export_status),
    CONSTRAINT fk_delivery_package_draft_item_draft FOREIGN KEY (draft_id) REFERENCES work_delivery_package_drafts (id),
    CONSTRAINT fk_delivery_package_draft_item_project FOREIGN KEY (project_id) REFERENCES core_projects (id)
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COLLATE=utf8mb4_unicode_ci;
