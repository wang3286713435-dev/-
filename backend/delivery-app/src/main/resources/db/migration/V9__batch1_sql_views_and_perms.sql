-- V9: Batch 1 SQL Views & permissions seed
-- Provides stable read models for enterprise agent and new permission codes

-- ============================================
-- SQL Views for enterprise agent stable read
-- ============================================

CREATE OR REPLACE VIEW ProjectAssetView AS
SELECT
    p.id                  AS project_id,
    p.code                AS project_code,
    p.name                AS project_name,
    p.project_stage       AS project_stage,
    COALESCE(
        (SELECT GROUP_CONCAT(DISTINCT d.name ORDER BY d.sort_order SEPARATOR ', ')
         FROM data_asset_disciplines d
         WHERE d.scope = 'BUILTIN'
            OR d.project_id = p.id),
        ''
    )                     AS discipline_scope,
    p.project_manager_name AS manager_name,
    p.owner_org_name      AS owner_org_name,
    p.asset_status        AS asset_status,
    COUNT(f.id)           AS model_file_count,
    COALESCE(SUM(f.size_bytes), 0) AS total_size_bytes,
    MAX(f.updated_at)     AS last_asset_updated_at
FROM core_projects p
LEFT JOIN data_file_resources f
    ON f.project_id = p.id
    AND f.deleted = 0
    AND f.file_kind = 'MODEL'
WHERE p.deleted = 0
GROUP BY p.id, p.code, p.name, p.project_stage,
         p.project_manager_name, p.owner_org_name, p.asset_status;

CREATE OR REPLACE VIEW FileAssetView AS
SELECT
    f.id                AS file_id,
    f.project_id        AS project_id,
    p.code              AS project_code,
    p.name              AS project_name,
    f.original_name     AS file_name,
    LOWER(SUBSTRING_INDEX(f.original_name, '.', -1)) AS file_ext,
    f.file_kind         AS file_kind,
    f.discipline        AS discipline,
    f.version_no        AS version_no,
    f.size_bytes        AS size_bytes,
    f.checksum          AS checksum,
    f.storage_provider  AS storage_provider,
    f.storage_uri       AS storage_path,
    f.logical_path      AS logical_path,
    f.source_type       AS source_type,
    f.process_status    AS process_status,
    f.created_at        AS created_at,
    f.updated_at        AS updated_at
FROM data_file_resources f
JOIN core_projects p ON p.id = f.project_id AND p.deleted = 0
WHERE f.deleted = 0;

CREATE OR REPLACE VIEW ModelAssetView AS
SELECT
    mi.id               AS model_id,
    mi.model_file_id    AS file_id,
    p.code              AS project_code,
    mi.name             AS model_name,
    LOWER(SUBSTRING_INDEX(f.original_name, '.', -1)) AS model_format,
    f.discipline        AS discipline,
    mi.version_no       AS version_no,
    0                   AS preview_available,
    'NOT_REQUIRED'      AS lightweight_status,
    'NOT_REQUIRED'      AS component_index_status,
    f.storage_uri       AS storage_path,
    mi.updated_at       AS updated_at
FROM data_model_integrations mi
JOIN data_file_resources f ON f.id = mi.model_file_id AND f.deleted = 0
JOIN core_projects p ON p.id = mi.project_id AND p.deleted = 0
WHERE mi.deleted = 0;

CREATE OR REPLACE VIEW AuditEventView AS
SELECT
    a.id            AS event_id,
    a.project_id    AS project_id,
    a.module_code   AS module_code,
    a.action_code   AS action_code,
    a.target_type   AS target_type,
    a.target_id     AS target_id,
    a.operator_id   AS operator_id,
    a.details_json  AS summary,
    a.created_at    AS created_at
FROM core_audit_logs a;

-- ============================================
-- New permissions for batch 1 scan & review
-- ============================================

INSERT INTO core_permissions (code, name, module_code)
SELECT 'DATA_STEWARD_ASSET_SCAN', '执行NAS扫描任务', 'data-steward'
WHERE NOT EXISTS (SELECT 1 FROM core_permissions WHERE code = 'DATA_STEWARD_ASSET_SCAN');
UPDATE core_permissions SET name = '执行NAS扫描任务', module_code = 'data-steward' WHERE code = 'DATA_STEWARD_ASSET_SCAN';

INSERT INTO core_permissions (code, name, module_code)
SELECT 'DATA_STEWARD_ASSET_REVIEW', '审核BIM资产', 'data-steward'
WHERE NOT EXISTS (SELECT 1 FROM core_permissions WHERE code = 'DATA_STEWARD_ASSET_REVIEW');
UPDATE core_permissions SET name = '审核BIM资产', module_code = 'data-steward' WHERE code = 'DATA_STEWARD_ASSET_REVIEW';

-- Assign SCAN and REVIEW to PROJECT_ADMIN and DELIVERY_ENGINEER
INSERT INTO core_role_permissions (role_id, permission_id)
SELECT r.id, p.id
FROM core_roles r
JOIN core_permissions p ON p.code IN (
    'DATA_STEWARD_ASSET_SCAN',
    'DATA_STEWARD_ASSET_REVIEW'
)
WHERE r.code IN ('PROJECT_ADMIN', 'DELIVERY_ENGINEER')
  AND NOT EXISTS (
      SELECT 1 FROM core_role_permissions rp
      WHERE rp.role_id = r.id AND rp.permission_id = p.id
  );

-- PROJECT_VIEWER does NOT get SCAN or REVIEW (read-only)
