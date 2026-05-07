INSERT INTO core_users (id, username, password_hash, display_name, status)
VALUES (1, 'platform.admin', '{noop}Admin@123', '平台管理员', 'ACTIVE')
ON DUPLICATE KEY UPDATE
    password_hash = '{noop}Admin@123',
    display_name = '平台管理员',
    status = 'ACTIVE';

INSERT INTO core_users (id, username, password_hash, display_name, status)
VALUES (2, 'delivery.engineer', '{noop}Engineer@123', '交付工程师', 'ACTIVE')
ON DUPLICATE KEY UPDATE
    password_hash = '{noop}Engineer@123',
    display_name = '交付工程师',
    status = 'ACTIVE';

INSERT INTO core_roles (id, code, name)
VALUES (1, 'PROJECT_ADMIN', '项目管理员')
ON DUPLICATE KEY UPDATE
    name = '项目管理员';

INSERT INTO core_roles (id, code, name)
VALUES (2, 'DELIVERY_ENGINEER', '交付工程师')
ON DUPLICATE KEY UPDATE
    name = '交付工程师';

INSERT INTO core_roles (id, code, name)
VALUES (3, 'PROJECT_VIEWER', '查看者')
ON DUPLICATE KEY UPDATE
    name = '查看者';

INSERT INTO core_projects (
    id, code, name, industry_type, owner_org_name, design_org_name, construct_org_name, supervision_org_name, status
)
VALUES (
    1, 'SAMPLE-MEP-001', '机电交付样板项目', 'BUILDING_MEP', '业主单位', '设计单位', '施工单位', '监理单位', 'ACTIVE'
)
ON DUPLICATE KEY UPDATE
    name = '机电交付样板项目',
    industry_type = 'BUILDING_MEP',
    status = 'ACTIVE';

INSERT INTO core_projects (
    id, code, name, industry_type, owner_org_name, design_org_name, construct_org_name, supervision_org_name, status
)
VALUES (
    2, 'SAMPLE-MEP-002', '机电交付扩展项目', 'BUILDING_MEP', '业主单位二期', '设计单位二期', '施工单位二期', '监理单位二期', 'ACTIVE'
)
ON DUPLICATE KEY UPDATE
    name = '机电交付扩展项目',
    industry_type = 'BUILDING_MEP',
    status = 'ACTIVE';

INSERT INTO core_user_project_roles (id, user_id, project_id, role_id)
VALUES (1, 1, 1, 1)
ON DUPLICATE KEY UPDATE
    user_id = 1,
    project_id = 1,
    role_id = 1;

INSERT INTO core_user_project_roles (id, user_id, project_id, role_id)
VALUES (2, 1, 2, 1)
ON DUPLICATE KEY UPDATE
    user_id = 1,
    project_id = 2,
    role_id = 1;

INSERT INTO core_user_project_roles (id, user_id, project_id, role_id)
VALUES (3, 2, 1, 2)
ON DUPLICATE KEY UPDATE
    user_id = 2,
    project_id = 1,
    role_id = 2;

INSERT INTO core_permissions (id, code, name, module_code)
VALUES (1, 'WORKCENTER_HOME_VIEW', '查看项目首页', 'work-center')
ON DUPLICATE KEY UPDATE
    name = '查看项目首页',
    module_code = 'work-center';

INSERT INTO core_permissions (id, code, name, module_code)
VALUES (2, 'CORE_PROJECT_SWITCH', '切换项目', 'core')
ON DUPLICATE KEY UPDATE
    name = '切换项目',
    module_code = 'core';

INSERT INTO core_permissions (id, code, name, module_code)
VALUES (3, 'CORE_USER_READ', '查看当前用户', 'core')
ON DUPLICATE KEY UPDATE
    name = '查看当前用户',
    module_code = 'core';

INSERT INTO core_role_permissions (id, role_id, permission_id)
VALUES (1, 1, 1)
ON DUPLICATE KEY UPDATE
    role_id = 1,
    permission_id = 1;

INSERT INTO core_role_permissions (id, role_id, permission_id)
VALUES (2, 1, 2)
ON DUPLICATE KEY UPDATE
    role_id = 1,
    permission_id = 2;

INSERT INTO core_role_permissions (id, role_id, permission_id)
VALUES (3, 1, 3)
ON DUPLICATE KEY UPDATE
    role_id = 1,
    permission_id = 3;

INSERT INTO core_role_permissions (id, role_id, permission_id)
VALUES (4, 2, 1)
ON DUPLICATE KEY UPDATE
    role_id = 2,
    permission_id = 1;

INSERT INTO core_role_permissions (id, role_id, permission_id)
VALUES (5, 2, 2)
ON DUPLICATE KEY UPDATE
    role_id = 2,
    permission_id = 2;

INSERT INTO core_role_permissions (id, role_id, permission_id)
VALUES (6, 2, 3)
ON DUPLICATE KEY UPDATE
    role_id = 2,
    permission_id = 3;

INSERT INTO core_role_permissions (id, role_id, permission_id)
VALUES (7, 3, 1)
ON DUPLICATE KEY UPDATE
    role_id = 3,
    permission_id = 1;

INSERT INTO core_role_permissions (id, role_id, permission_id)
VALUES (8, 3, 3)
ON DUPLICATE KEY UPDATE
    role_id = 3,
    permission_id = 3;
