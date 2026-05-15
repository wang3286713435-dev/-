-- V13: Make ModelAssetView cover NAS-scanned model file assets.
-- Batch 1 real NAS pilot showed that enterprise agents must find model files
-- even before those files enter model integration / lightweight preview flows.

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
WHERE mi.deleted = 0

UNION ALL

SELECT
    f.id                AS model_id,
    f.id                AS file_id,
    p.code              AS project_code,
    f.original_name     AS model_name,
    LOWER(SUBSTRING_INDEX(f.original_name, '.', -1)) AS model_format,
    f.discipline        AS discipline,
    f.version_no        AS version_no,
    0                   AS preview_available,
    'NOT_REQUIRED'      AS lightweight_status,
    'NOT_REQUIRED'      AS component_index_status,
    f.storage_uri       AS storage_path,
    f.updated_at        AS updated_at
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
