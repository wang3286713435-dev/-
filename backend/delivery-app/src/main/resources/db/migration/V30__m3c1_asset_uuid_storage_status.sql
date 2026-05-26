-- M3C-1: stable public asset UUID and storage status contract.
-- Numeric file_id remains the internal key; asset_uuid is the stable, non-path public asset identifier.

ALTER TABLE data_file_resources
    ADD COLUMN asset_uuid CHAR(36) NULL AFTER id;

UPDATE data_file_resources
SET asset_uuid = UUID()
WHERE asset_uuid IS NULL OR asset_uuid = '';

ALTER TABLE data_file_resources
    MODIFY COLUMN asset_uuid CHAR(36) NOT NULL DEFAULT (UUID());

CREATE UNIQUE INDEX uk_data_file_resources_asset_uuid
    ON data_file_resources (asset_uuid);

CREATE OR REPLACE VIEW FileAssetView AS
SELECT
    f.id                AS file_id,
    f.asset_uuid        AS asset_uuid,
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
    f.updated_at        AS updated_at,
    CONCAT(
        'SOURCE_SYSTEM:delivery_platform',
        ',SOURCE_VIEW:FileAssetView',
        ',ASSET_KIND:FILE',
        ',PROJECT:', f.project_id,
        ',CONFIDENTIALITY:',
        CASE WHEN UPPER(COALESCE(f.source_type, '')) = 'AGENT_TEST' THEN 'INTERNAL' ELSE 'UNKNOWN' END,
        ',INDEX_ELIGIBILITY:',
        CASE
            WHEN UPPER(COALESCE(f.source_type, '')) = 'AGENT_TEST'
             AND LOWER(SUBSTRING_INDEX(f.original_name, '.', -1)) IN ('txt', 'pdf', 'doc', 'docx')
                THEN 'full_text_allowed'
            WHEN UPPER(COALESCE(f.source_type, '')) = 'AGENT_TEST'
                THEN 'preview_allowed'
            ELSE 'catalog_only'
        END
    )                   AS permission_tags,
    CASE
        WHEN UPPER(COALESCE(f.source_type, '')) = 'AGENT_TEST'
            THEN 'INTERNAL'
        ELSE 'UNKNOWN'
    END                 AS confidentiality_level,
    COALESCE(f.last_verified_at, f.updated_at) AS last_seen_at,
    CASE
        WHEN UPPER(COALESCE(f.process_status, '')) IN ('ARCHIVED')
            THEN 'archived'
        WHEN UPPER(COALESCE(f.process_status, '')) IN ('DELETE_REQUESTED', 'DELETED', 'PENDING_DELETE')
            THEN 'deleted_candidate'
        WHEN f.last_verified_at IS NULL AND UPPER(COALESCE(f.source_type, '')) IN ('NAS_SCAN', 'REVIEW')
            THEN 'stale_unverified'
        WHEN f.process_status IS NULL
            THEN 'unknown'
        ELSE 'active'
    END                 AS lifecycle_status,
    CASE
        WHEN UPPER(COALESCE(f.source_type, '')) = 'AGENT_TEST'
         AND LOWER(SUBSTRING_INDEX(f.original_name, '.', -1)) IN ('txt', 'pdf', 'doc', 'docx')
            THEN 'full_text_allowed'
        WHEN UPPER(COALESCE(f.source_type, '')) = 'AGENT_TEST'
            THEN 'preview_allowed'
        ELSE 'catalog_only'
    END                 AS index_eligibility
FROM data_file_resources f
JOIN core_projects p ON p.id = f.project_id AND p.deleted = 0
WHERE f.deleted = 0;

CREATE OR REPLACE VIEW ModelAssetView AS
SELECT
    mi.id               AS model_id,
    mi.model_file_id    AS file_id,
    f.asset_uuid        AS asset_uuid,
    mi.project_id       AS project_id,
    p.code              AS project_code,
    mi.name             AS model_name,
    LOWER(SUBSTRING_INDEX(f.original_name, '.', -1)) AS model_format,
    f.discipline        AS discipline,
    mi.version_no       AS version_no,
    0                   AS preview_available,
    'NOT_REQUIRED'      AS lightweight_status,
    'NOT_REQUIRED'      AS component_index_status,
    f.storage_uri       AS storage_path,
    mi.updated_at       AS updated_at,
    CONCAT(
        'SOURCE_SYSTEM:delivery_platform',
        ',SOURCE_VIEW:ModelAssetView',
        ',ASSET_KIND:MODEL',
        ',PROJECT:', mi.project_id,
        ',CONFIDENTIALITY:UNKNOWN',
        ',INDEX_ELIGIBILITY:catalog_only'
    )                   AS permission_tags,
    'UNKNOWN'           AS confidentiality_level,
    COALESCE(f.last_verified_at, mi.updated_at, f.updated_at) AS last_seen_at,
    CASE
        WHEN UPPER(COALESCE(mi.status, '')) = 'ARCHIVED'
            THEN 'archived'
        WHEN UPPER(COALESCE(f.process_status, '')) IN ('DELETE_REQUESTED', 'DELETED', 'PENDING_DELETE')
            THEN 'deleted_candidate'
        WHEN f.last_verified_at IS NULL AND UPPER(COALESCE(f.source_type, '')) IN ('NAS_SCAN', 'REVIEW')
            THEN 'stale_unverified'
        WHEN mi.status IS NULL
            THEN 'unknown'
        ELSE 'active'
    END                 AS lifecycle_status,
    'catalog_only'      AS index_eligibility
FROM data_model_integrations mi
JOIN data_file_resources f ON f.id = mi.model_file_id AND f.deleted = 0
JOIN core_projects p ON p.id = mi.project_id AND p.deleted = 0
WHERE mi.deleted = 0

UNION ALL

SELECT
    f.id                AS model_id,
    f.id                AS file_id,
    f.asset_uuid        AS asset_uuid,
    f.project_id        AS project_id,
    p.code              AS project_code,
    f.original_name     AS model_name,
    LOWER(SUBSTRING_INDEX(f.original_name, '.', -1)) AS model_format,
    f.discipline        AS discipline,
    f.version_no        AS version_no,
    0                   AS preview_available,
    'NOT_REQUIRED'      AS lightweight_status,
    'NOT_REQUIRED'      AS component_index_status,
    f.storage_uri       AS storage_path,
    f.updated_at        AS updated_at,
    CONCAT(
        'SOURCE_SYSTEM:delivery_platform',
        ',SOURCE_VIEW:ModelAssetView',
        ',ASSET_KIND:MODEL',
        ',PROJECT:', f.project_id,
        ',CONFIDENTIALITY:UNKNOWN',
        ',INDEX_ELIGIBILITY:catalog_only'
    )                   AS permission_tags,
    'UNKNOWN'           AS confidentiality_level,
    COALESCE(f.last_verified_at, f.updated_at) AS last_seen_at,
    CASE
        WHEN UPPER(COALESCE(f.process_status, '')) IN ('ARCHIVED')
            THEN 'archived'
        WHEN UPPER(COALESCE(f.process_status, '')) IN ('DELETE_REQUESTED', 'DELETED', 'PENDING_DELETE')
            THEN 'deleted_candidate'
        WHEN f.last_verified_at IS NULL AND UPPER(COALESCE(f.source_type, '')) IN ('NAS_SCAN', 'REVIEW')
            THEN 'stale_unverified'
        WHEN f.process_status IS NULL
            THEN 'unknown'
        ELSE 'active'
    END                 AS lifecycle_status,
    'catalog_only'      AS index_eligibility
FROM data_file_resources f
JOIN core_projects p ON p.id = f.project_id AND p.deleted = 0
WHERE f.deleted = 0
  AND f.file_kind = 'MODEL'
  AND NOT EXISTS (
      SELECT 1
      FROM data_model_integrations mi
      WHERE mi.model_file_id = f.id
        AND mi.deleted = 0
  );
