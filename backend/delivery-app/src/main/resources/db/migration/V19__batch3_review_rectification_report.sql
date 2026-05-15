CREATE TABLE IF NOT EXISTS work_review_records (
    id BIGINT PRIMARY KEY AUTO_INCREMENT,
    project_id BIGINT NOT NULL,
    binding_id BIGINT NOT NULL,
    action VARCHAR(32) NOT NULL COMMENT 'SUBMITTED/APPROVED/REJECTED',
    comment VARCHAR(1024) NULL,
    reviewer_user_id BIGINT NOT NULL,
    created_at TIMESTAMP NOT NULL DEFAULT CURRENT_TIMESTAMP,
    CONSTRAINT fk_review_record_project FOREIGN KEY (project_id) REFERENCES core_projects (id),
    CONSTRAINT fk_review_record_binding FOREIGN KEY (binding_id) REFERENCES work_delivery_bindings (id),
    KEY idx_review_record_binding (binding_id),
    KEY idx_review_record_project (project_id)
);

CREATE TABLE IF NOT EXISTS work_rectifications (
    id BIGINT PRIMARY KEY AUTO_INCREMENT,
    project_id BIGINT NOT NULL,
    source_type VARCHAR(32) NOT NULL DEFAULT 'DELIVERY_BINDING',
    source_id BIGINT NOT NULL,
    binding_id BIGINT NOT NULL,
    title VARCHAR(256) NOT NULL,
    description VARCHAR(1024) NULL,
    reason VARCHAR(1024) NOT NULL COMMENT '驳回原因',
    status VARCHAR(32) NOT NULL DEFAULT 'OPEN' COMMENT 'OPEN/IN_PROGRESS/RESOLVED/CLOSED/REOPENED',
    severity VARCHAR(32) NOT NULL DEFAULT 'NORMAL' COMMENT 'MINOR/NORMAL/MAJOR/CRITICAL',
    assignee_user_id BIGINT NULL,
    resolution_note VARCHAR(1024) NULL,
    due_date DATE NULL,
    resolved_at TIMESTAMP NULL,
    closed_at TIMESTAMP NULL,
    created_at TIMESTAMP NOT NULL DEFAULT CURRENT_TIMESTAMP,
    updated_at TIMESTAMP NOT NULL DEFAULT CURRENT_TIMESTAMP ON UPDATE CURRENT_TIMESTAMP,
    created_by BIGINT NULL,
    updated_by BIGINT NULL,
    deleted TINYINT NOT NULL DEFAULT 0,
    CONSTRAINT fk_rectification_project FOREIGN KEY (project_id) REFERENCES core_projects (id),
    CONSTRAINT fk_rectification_binding FOREIGN KEY (binding_id) REFERENCES work_delivery_bindings (id),
    KEY idx_rectification_project (project_id),
    KEY idx_rectification_binding (binding_id),
    KEY idx_rectification_status (project_id, status)
);

-- Permissions for review management
INSERT INTO core_permissions (code, name, module_code)
SELECT 'WORKCENTER_REVIEW_MANAGE', '管理交付审核', 'work-center'
WHERE NOT EXISTS (SELECT 1 FROM core_permissions WHERE code = 'WORKCENTER_REVIEW_MANAGE');
UPDATE core_permissions SET name = '管理交付审核', module_code = 'work-center' WHERE code = 'WORKCENTER_REVIEW_MANAGE';

-- Permissions for rectification management
INSERT INTO core_permissions (code, name, module_code)
SELECT 'WORKCENTER_RECTIFICATION_READ', '查看整改项', 'work-center'
WHERE NOT EXISTS (SELECT 1 FROM core_permissions WHERE code = 'WORKCENTER_RECTIFICATION_READ');
UPDATE core_permissions SET name = '查看整改项', module_code = 'work-center' WHERE code = 'WORKCENTER_RECTIFICATION_READ';

INSERT INTO core_permissions (code, name, module_code)
SELECT 'WORKCENTER_RECTIFICATION_MANAGE', '维护整改项', 'work-center'
WHERE NOT EXISTS (SELECT 1 FROM core_permissions WHERE code = 'WORKCENTER_RECTIFICATION_MANAGE');
UPDATE core_permissions SET name = '维护整改项', module_code = 'work-center' WHERE code = 'WORKCENTER_RECTIFICATION_MANAGE';

-- Permissions for report export
INSERT INTO core_permissions (code, name, module_code)
SELECT 'WORKCENTER_REPORT_EXPORT', '导出交付报表', 'work-center'
WHERE NOT EXISTS (SELECT 1 FROM core_permissions WHERE code = 'WORKCENTER_REPORT_EXPORT');
UPDATE core_permissions SET name = '导出交付报表', module_code = 'work-center' WHERE code = 'WORKCENTER_REPORT_EXPORT';

-- Grant new permissions to PROJECT_ADMIN
INSERT INTO core_role_permissions (role_id, permission_id)
SELECT r.id, p.id
FROM core_roles r
JOIN core_permissions p ON p.code IN (
    'WORKCENTER_REVIEW_MANAGE',
    'WORKCENTER_RECTIFICATION_READ',
    'WORKCENTER_RECTIFICATION_MANAGE',
    'WORKCENTER_REPORT_EXPORT'
)
WHERE r.code = 'PROJECT_ADMIN'
  AND NOT EXISTS (
      SELECT 1 FROM core_role_permissions rp
      WHERE rp.role_id = r.id AND rp.permission_id = p.id
  );

-- Grant to DELIVERY_ENGINEER
INSERT INTO core_role_permissions (role_id, permission_id)
SELECT r.id, p.id
FROM core_roles r
JOIN core_permissions p ON p.code IN (
    'WORKCENTER_REVIEW_MANAGE',
    'WORKCENTER_RECTIFICATION_READ',
    'WORKCENTER_RECTIFICATION_MANAGE',
    'WORKCENTER_REPORT_EXPORT'
)
WHERE r.code = 'DELIVERY_ENGINEER'
  AND NOT EXISTS (
      SELECT 1 FROM core_role_permissions rp
      WHERE rp.role_id = r.id AND rp.permission_id = p.id
  );

-- Grant read-only to PROJECT_VIEWER
INSERT INTO core_role_permissions (role_id, permission_id)
SELECT r.id, p.id
FROM core_roles r
JOIN core_permissions p ON p.code IN (
    'WORKCENTER_RECTIFICATION_READ',
    'WORKCENTER_REPORT_EXPORT'
)
WHERE r.code = 'PROJECT_VIEWER'
  AND NOT EXISTS (
      SELECT 1 FROM core_role_permissions rp
      WHERE rp.role_id = r.id AND rp.permission_id = p.id
  );
