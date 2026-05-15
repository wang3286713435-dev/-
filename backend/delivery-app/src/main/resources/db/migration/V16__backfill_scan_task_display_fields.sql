-- V16: backfill scan task display fields for historical tasks

UPDATE data_asset_scan_tasks task
JOIN core_projects project ON project.id = task.project_id AND project.deleted = 0
SET task.project_code = project.code
WHERE task.deleted = 0
  AND task.project_id IS NOT NULL
  AND (task.project_code IS NULL OR task.project_code = '');

UPDATE data_asset_scan_tasks
SET progress_current = total_scanned,
    progress_total = total_scanned,
    progress_percent = 100.00,
    progress_message = COALESCE(NULLIF(progress_message, ''), '扫描完成'),
    completed_at = COALESCE(completed_at, updated_at)
WHERE deleted = 0
  AND status = 'SUCCEEDED'
  AND COALESCE(progress_percent, 0.00) < 100.00;
