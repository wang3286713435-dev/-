-- Phase 2 Batch 4: file preview/download permission separation and access tickets

-- Permissions for file preview and download
INSERT INTO core_permissions (code, name, module_code)
SELECT 'DATA_STEWARD_FILE_PREVIEW', '文件预览', 'data-steward'
WHERE NOT EXISTS (SELECT 1 FROM core_permissions WHERE code = 'DATA_STEWARD_FILE_PREVIEW');

INSERT INTO core_permissions (code, name, module_code)
SELECT 'DATA_STEWARD_FILE_DOWNLOAD', '文件下载', 'data-steward'
WHERE NOT EXISTS (SELECT 1 FROM core_permissions WHERE code = 'DATA_STEWARD_FILE_DOWNLOAD');

-- Grant preview+download to PROJECT_ADMIN
INSERT INTO core_role_permissions (role_id, permission_id)
SELECT r.id, p.id
FROM core_roles r
JOIN core_permissions p ON p.code IN ('DATA_STEWARD_FILE_PREVIEW', 'DATA_STEWARD_FILE_DOWNLOAD')
WHERE r.code = 'PROJECT_ADMIN'
  AND NOT EXISTS (
      SELECT 1 FROM core_role_permissions rp
      WHERE rp.role_id = r.id AND rp.permission_id = p.id
  );

-- Grant preview+download to DELIVERY_ENGINEER
INSERT INTO core_role_permissions (role_id, permission_id)
SELECT r.id, p.id
FROM core_roles r
JOIN core_permissions p ON p.code IN ('DATA_STEWARD_FILE_PREVIEW', 'DATA_STEWARD_FILE_DOWNLOAD')
WHERE r.code = 'DELIVERY_ENGINEER'
  AND NOT EXISTS (
      SELECT 1 FROM core_role_permissions rp
      WHERE rp.role_id = r.id AND rp.permission_id = p.id
  );

-- Grant preview only (no download) to PROJECT_VIEWER
INSERT INTO core_role_permissions (role_id, permission_id)
SELECT r.id, p.id
FROM core_roles r
JOIN core_permissions p ON p.code = 'DATA_STEWARD_FILE_PREVIEW'
WHERE r.code = 'PROJECT_VIEWER'
  AND NOT EXISTS (
      SELECT 1 FROM core_role_permissions rp
      WHERE rp.role_id = r.id AND rp.permission_id = p.id
  );

-- Short-lived access tickets table
CREATE TABLE IF NOT EXISTS data_file_access_tickets (
    id BIGINT PRIMARY KEY AUTO_INCREMENT,
    ticket VARCHAR(64) NOT NULL,
    file_id BIGINT NOT NULL,
    project_id BIGINT NOT NULL,
    user_id BIGINT NOT NULL,
    action VARCHAR(16) NOT NULL COMMENT 'PREVIEW or DOWNLOAD',
    status VARCHAR(16) NOT NULL DEFAULT 'ACTIVE' COMMENT 'ACTIVE/USED/EXPIRED/REVOKED',
    expires_at TIMESTAMP NOT NULL,
    used_at TIMESTAMP NULL,
    created_at TIMESTAMP NOT NULL DEFAULT CURRENT_TIMESTAMP,
    CONSTRAINT fk_access_ticket_file FOREIGN KEY (file_id) REFERENCES data_file_resources (id),
    CONSTRAINT fk_access_ticket_project FOREIGN KEY (project_id) REFERENCES core_projects (id),
    UNIQUE KEY uk_access_ticket (ticket),
    KEY idx_access_ticket_file (file_id),
    KEY idx_access_ticket_user (user_id),
    KEY idx_access_ticket_status_expires (status, expires_at)
);
