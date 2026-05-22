-- M2B: project-level NAS write trial switch and scoped writable roots.
-- This table stores only project-relative roots and role/account allow lists.
-- It must not contain raw NAS physical paths.

CREATE TABLE IF NOT EXISTS data_nas_write_trial_configs (
    id BIGINT PRIMARY KEY AUTO_INCREMENT,
    project_id BIGINT NOT NULL,
    enabled TINYINT NOT NULL DEFAULT 0,
    allowed_relative_roots_json JSON NOT NULL,
    allowed_role_codes_json JSON NOT NULL,
    allowed_user_ids_json JSON NOT NULL,
    trial_mode_notice VARCHAR(512) NULL,
    created_at TIMESTAMP NOT NULL DEFAULT CURRENT_TIMESTAMP,
    updated_at TIMESTAMP NOT NULL DEFAULT CURRENT_TIMESTAMP ON UPDATE CURRENT_TIMESTAMP,
    created_by BIGINT NULL,
    updated_by BIGINT NULL,
    deleted TINYINT NOT NULL DEFAULT 0,
    delete_token BIGINT NOT NULL DEFAULT 0,
    UNIQUE KEY uk_nas_write_trial_project_token (project_id, delete_token),
    KEY idx_nas_write_trial_project_enabled (project_id, enabled),
    CONSTRAINT fk_nas_write_trial_project FOREIGN KEY (project_id) REFERENCES core_projects (id)
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COLLATE=utf8mb4_unicode_ci;
