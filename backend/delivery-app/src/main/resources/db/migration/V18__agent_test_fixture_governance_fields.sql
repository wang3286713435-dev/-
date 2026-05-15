-- V18: Allow explicit non-sensitive AGENT_TEST fixtures to prove v1.1 governance fields.
-- Default production asset behavior remains UNKNOWN / catalog_only.

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
    MAX(f.updated_at)     AS last_asset_updated_at,
    CONCAT(
        'SOURCE_SYSTEM:delivery_platform',
        ',SOURCE_VIEW:ProjectAssetView',
        ',ASSET_KIND:PROJECT',
        ',PROJECT:', p.id,
        ',CONFIDENTIALITY:',
        CASE WHEN UPPER(COALESCE(p.asset_source, '')) = 'AGENT_TEST' THEN 'INTERNAL' ELSE 'UNKNOWN' END,
        ',INDEX_ELIGIBILITY:',
        CASE WHEN UPPER(COALESCE(p.asset_source, '')) = 'AGENT_TEST' THEN 'preview_allowed' ELSE 'catalog_only' END
    )                     AS permission_tags,
    CASE
        WHEN UPPER(COALESCE(p.asset_source, '')) = 'AGENT_TEST'
            THEN 'INTERNAL'
        ELSE 'UNKNOWN'
    END                   AS confidentiality_level,
    COALESCE(MAX(f.last_verified_at), MAX(f.updated_at), p.updated_at) AS last_seen_at,
    CASE
        WHEN UPPER(COALESCE(p.asset_status, p.status, '')) = 'ARCHIVED'
          OR UPPER(COALESCE(p.status, '')) = 'ARCHIVED'
            THEN 'archived'
        WHEN COALESCE(p.asset_status, p.status) IS NULL
            THEN 'unknown'
        ELSE 'active'
    END                   AS lifecycle_status,
    CASE
        WHEN UPPER(COALESCE(p.asset_source, '')) = 'AGENT_TEST'
            THEN 'preview_allowed'
        ELSE 'catalog_only'
    END                   AS index_eligibility
FROM core_projects p
LEFT JOIN data_file_resources f
    ON f.project_id = p.id
    AND f.deleted = 0
    AND f.file_kind = 'MODEL'
WHERE p.deleted = 0
GROUP BY p.id, p.code, p.name, p.project_stage,
         p.project_manager_name, p.owner_org_name, p.asset_status,
         p.status, p.updated_at, p.asset_source;

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
