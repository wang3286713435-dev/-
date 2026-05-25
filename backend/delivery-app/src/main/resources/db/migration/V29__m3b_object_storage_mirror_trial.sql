-- M3B: small-sample object storage mirror migration.
-- Batch records are API-facing summaries; per-file rows stay in data_object_migration_tasks.

CREATE TABLE IF NOT EXISTS data_object_migration_task_batches (
    id BIGINT PRIMARY KEY AUTO_INCREMENT,
    project_id BIGINT NOT NULL,
    target_provider VARCHAR(32) NOT NULL DEFAULT 'MINIO',
    task_status VARCHAR(32) NOT NULL DEFAULT 'PENDING',
    storage_state VARCHAR(32) NOT NULL DEFAULT 'MIGRATION_PENDING',
    total_count INT NOT NULL DEFAULT 0,
    success_count INT NOT NULL DEFAULT 0,
    failure_count INT NOT NULL DEFAULT 0,
    skipped_count INT NOT NULL DEFAULT 0,
    failure_reason VARCHAR(1024) NULL,
    requested_by BIGINT NULL,
    started_at TIMESTAMP NULL,
    completed_at TIMESTAMP NULL,
    created_by BIGINT NULL,
    updated_by BIGINT NULL,
    created_at TIMESTAMP NOT NULL DEFAULT CURRENT_TIMESTAMP,
    updated_at TIMESTAMP NOT NULL DEFAULT CURRENT_TIMESTAMP ON UPDATE CURRENT_TIMESTAMP,
    deleted TINYINT NOT NULL DEFAULT 0,
    delete_token BIGINT NOT NULL DEFAULT 0,
    KEY idx_object_migration_batch_project_status (project_id, task_status),
    KEY idx_object_migration_batch_requested_by (requested_by),
    CONSTRAINT fk_object_migration_batch_project FOREIGN KEY (project_id) REFERENCES core_projects (id)
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COLLATE=utf8mb4_unicode_ci;

ALTER TABLE data_object_migration_tasks
    ADD COLUMN task_batch_id BIGINT NULL AFTER id,
    ADD KEY idx_object_migration_task_batch (task_batch_id),
    ADD CONSTRAINT fk_object_migration_task_batch
        FOREIGN KEY (task_batch_id) REFERENCES data_object_migration_task_batches (id);
