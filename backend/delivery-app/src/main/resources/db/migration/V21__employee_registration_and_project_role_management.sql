ALTER TABLE core_users
    ADD COLUMN phone_number VARCHAR(32) NULL AFTER username,
    ADD COLUMN department_name VARCHAR(128) NULL AFTER display_name,
    ADD COLUMN last_login_at TIMESTAMP NULL AFTER status,
    ADD UNIQUE KEY uk_core_users_phone_number (phone_number);

INSERT INTO core_permissions (code, name, module_code)
SELECT 'CORE_USER_MANAGE', '管理员工账号', 'core'
WHERE NOT EXISTS (SELECT 1 FROM core_permissions WHERE code = 'CORE_USER_MANAGE');
UPDATE core_permissions SET name = '管理员工账号', module_code = 'core' WHERE code = 'CORE_USER_MANAGE';

INSERT INTO core_permissions (code, name, module_code)
SELECT 'CORE_PROJECT_ROLE_MANAGE', '管理员工项目权限', 'core'
WHERE NOT EXISTS (SELECT 1 FROM core_permissions WHERE code = 'CORE_PROJECT_ROLE_MANAGE');
UPDATE core_permissions SET name = '管理员工项目权限', module_code = 'core' WHERE code = 'CORE_PROJECT_ROLE_MANAGE';

INSERT INTO core_role_permissions (role_id, permission_id)
SELECT r.id, p.id
FROM core_roles r
JOIN core_permissions p ON p.code IN ('CORE_USER_MANAGE', 'CORE_PROJECT_ROLE_MANAGE')
WHERE r.code = 'PROJECT_ADMIN'
  AND NOT EXISTS (
      SELECT 1
      FROM core_role_permissions rp
      WHERE rp.role_id = r.id
        AND rp.permission_id = p.id
  );
