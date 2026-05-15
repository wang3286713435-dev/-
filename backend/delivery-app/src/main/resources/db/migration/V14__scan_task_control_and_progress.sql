-- V14: NAS scan task control, progress, and reporting

ALTER TABLE data_asset_scan_tasks
    ADD COLUMN progress_current INT NOT NULL DEFAULT 0 AFTER progress_message,
    ADD COLUMN progress_total INT NOT NULL DEFAULT 0 AFTER progress_current,
    ADD COLUMN progress_percent DECIMAL(5,2) NOT NULL DEFAULT 0.00 AFTER progress_total,
    ADD COLUMN cancel_requested TINYINT NOT NULL DEFAULT 0 AFTER progress_percent,
    ADD COLUMN skipped_low_value INT NOT NULL DEFAULT 0 AFTER failed_count,
    ADD COLUMN skipped_directories INT NOT NULL DEFAULT 0 AFTER skipped_low_value,
    ADD COLUMN last_scanned_path VARCHAR(1024) NULL AFTER skipped_directories,
    ADD COLUMN skip_low_value_directories TINYINT NOT NULL DEFAULT 0 AFTER extensions,
    ADD COLUMN skip_directory_keywords VARCHAR(512) NULL AFTER skip_low_value_directories,
    ADD COLUMN scan_report_json JSON NULL AFTER failure_reason;

CREATE INDEX idx_asset_scan_task_cancel
    ON data_asset_scan_tasks (cancel_requested, status);
