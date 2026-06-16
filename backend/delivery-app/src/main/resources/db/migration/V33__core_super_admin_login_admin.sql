UPDATE core_users
SET username = CONCAT('admin.legacy.', id),
    status = 'DISABLED',
    deleted = 1
WHERE username = 'admin'
  AND id <> 1;

UPDATE core_users
SET username = 'admin',
    password_hash = '{noop}123456',
    display_name = '超级管理员',
    status = 'ACTIVE',
    deleted = 0
WHERE id = 1;
