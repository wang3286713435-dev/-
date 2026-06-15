UPDATE core_role_permissions rp
JOIN core_roles r ON r.id = rp.role_id AND r.deleted = 0
JOIN core_permissions p ON p.id = rp.permission_id AND p.deleted = 0
SET rp.deleted = 1
WHERE r.code = 'PROJECT_ADMIN'
  AND p.code IN ('CORE_USER_MANAGE', 'CORE_PROJECT_ROLE_MANAGE')
  AND rp.deleted = 0;

UPDATE core_permissions
SET name = '超级管理员工账号'
WHERE code = 'CORE_USER_MANAGE'
  AND deleted = 0;

UPDATE core_permissions
SET name = '超级管理员工项目权限'
WHERE code = 'CORE_PROJECT_ROLE_MANAGE'
  AND deleted = 0;
