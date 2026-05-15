-- V10: Batch 1 tail — historical confidence_level backfill & data cleanup
-- Backfills NULL confidence_level for batch-1-managed assets (NAS_SCAN, REVIEW)
-- without forcing a single confidence value on all historical files.

-- Strategy:
--   1. NAS_SCAN auto-ingested files → confidence_level = 'HIGH' (if review_status = 'APPROVED')
--   2. REVIEW-approved files → confidence_level = 'HIGH' (if review_status = 'APPROVED')
--   3. Everything else with batch-1 source → confidence_level = 'MEDIUM' (safe fallback)
--   4. PENDING review → confidence_level = 'LOW' (conservative, needs review)
-- Only touches rows where confidence_level IS NULL.

UPDATE data_file_resources
SET confidence_level = 'HIGH',
    last_verified_at = COALESCE(last_verified_at, NOW())
WHERE confidence_level IS NULL
  AND deleted = 0
  AND source_type IN ('NAS_SCAN', 'REVIEW')
  AND review_status = 'APPROVED';

UPDATE data_file_resources
SET confidence_level = 'LOW',
    last_verified_at = COALESCE(last_verified_at, NOW())
WHERE confidence_level IS NULL
  AND deleted = 0
  AND source_type IN ('NAS_SCAN', 'REVIEW')
  AND review_status = 'PENDING';

UPDATE data_file_resources
SET confidence_level = 'MEDIUM',
    last_verified_at = COALESCE(last_verified_at, NOW())
WHERE confidence_level IS NULL
  AND deleted = 0
  AND source_type IN ('NAS_SCAN', 'REVIEW');
