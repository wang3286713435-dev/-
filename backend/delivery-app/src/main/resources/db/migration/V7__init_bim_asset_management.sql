ALTER TABLE core_projects
    ADD COLUMN project_stage VARCHAR(64) NULL AFTER industry_type,
    ADD COLUMN project_manager_name VARCHAR(128) NULL AFTER project_stage,
    ADD COLUMN asset_status VARCHAR(32) NOT NULL DEFAULT 'ACTIVE' AFTER project_manager_name,
    ADD COLUMN asset_source VARCHAR(64) NULL AFTER asset_status;

ALTER TABLE data_file_resources
    ADD COLUMN storage_provider VARCHAR(32) NOT NULL DEFAULT 'METADATA' AFTER storage_uri,
    ADD COLUMN storage_key TEXT NULL AFTER storage_provider,
    ADD COLUMN source_path_digest VARCHAR(128) NULL AFTER storage_key,
    ADD COLUMN discipline VARCHAR(64) NULL AFTER business_tag,
    ADD COLUMN source_type VARCHAR(32) NOT NULL DEFAULT 'MANUAL' AFTER discipline,
    ADD COLUMN last_verified_at TIMESTAMP NULL AFTER source_type;

CREATE INDEX idx_data_file_project_provider
    ON data_file_resources (project_id, storage_provider, file_kind, process_status);

CREATE INDEX idx_data_file_project_discipline
    ON data_file_resources (project_id, discipline);

CREATE TABLE IF NOT EXISTS data_storage_roots (
    id BIGINT PRIMARY KEY AUTO_INCREMENT,
    provider_code VARCHAR(32) NOT NULL,
    root_code VARCHAR(64) NOT NULL,
    display_name VARCHAR(128) NOT NULL,
    root_path VARCHAR(1024) NOT NULL,
    status VARCHAR(32) NOT NULL DEFAULT 'ACTIVE',
    created_at TIMESTAMP NOT NULL DEFAULT CURRENT_TIMESTAMP,
    updated_at TIMESTAMP NOT NULL DEFAULT CURRENT_TIMESTAMP ON UPDATE CURRENT_TIMESTAMP,
    created_by BIGINT NULL,
    updated_by BIGINT NULL,
    deleted TINYINT NOT NULL DEFAULT 0,
    UNIQUE KEY uk_data_storage_root_code (provider_code, root_code)
);

CREATE TABLE IF NOT EXISTS data_asset_import_jobs (
    id BIGINT PRIMARY KEY AUTO_INCREMENT,
    job_type VARCHAR(32) NOT NULL,
    source_name VARCHAR(255) NULL,
    status VARCHAR(32) NOT NULL DEFAULT 'PENDING',
    total_count INT NOT NULL DEFAULT 0,
    success_count INT NOT NULL DEFAULT 0,
    failure_count INT NOT NULL DEFAULT 0,
    report_json JSON NULL,
    created_at TIMESTAMP NOT NULL DEFAULT CURRENT_TIMESTAMP,
    updated_at TIMESTAMP NOT NULL DEFAULT CURRENT_TIMESTAMP ON UPDATE CURRENT_TIMESTAMP,
    created_by BIGINT NULL,
    updated_by BIGINT NULL,
    deleted TINYINT NOT NULL DEFAULT 0
);

INSERT INTO core_permissions (code, name, module_code)
SELECT 'DATA_STEWARD_ASSET_READ', '查看BIM资产台账', 'data-steward'
WHERE NOT EXISTS (SELECT 1 FROM core_permissions WHERE code = 'DATA_STEWARD_ASSET_READ');
UPDATE core_permissions SET name = '查看BIM资产台账', module_code = 'data-steward' WHERE code = 'DATA_STEWARD_ASSET_READ';

INSERT INTO core_permissions (code, name, module_code)
SELECT 'DATA_STEWARD_ASSET_MANAGE', '维护BIM资产台账', 'data-steward'
WHERE NOT EXISTS (SELECT 1 FROM core_permissions WHERE code = 'DATA_STEWARD_ASSET_MANAGE');
UPDATE core_permissions SET name = '维护BIM资产台账', module_code = 'data-steward' WHERE code = 'DATA_STEWARD_ASSET_MANAGE';

INSERT INTO core_role_permissions (role_id, permission_id)
SELECT r.id, p.id
FROM core_roles r
JOIN core_permissions p ON p.code IN (
    'DATA_STEWARD_ASSET_READ',
    'DATA_STEWARD_ASSET_MANAGE'
)
WHERE r.code IN ('PROJECT_ADMIN', 'DELIVERY_ENGINEER')
  AND NOT EXISTS (
      SELECT 1 FROM core_role_permissions rp
      WHERE rp.role_id = r.id AND rp.permission_id = p.id
  );

INSERT INTO core_role_permissions (role_id, permission_id)
SELECT r.id, p.id
FROM core_roles r
JOIN core_permissions p ON p.code = 'DATA_STEWARD_ASSET_READ'
WHERE r.code = 'PROJECT_VIEWER'
  AND NOT EXISTS (
      SELECT 1 FROM core_role_permissions rp
      WHERE rp.role_id = r.id AND rp.permission_id = p.id
  );
