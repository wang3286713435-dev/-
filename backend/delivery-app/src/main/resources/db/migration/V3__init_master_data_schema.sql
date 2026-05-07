CREATE TABLE IF NOT EXISTS masterdata_section_nodes (
    id BIGINT PRIMARY KEY AUTO_INCREMENT,
    project_id BIGINT NOT NULL,
    parent_id BIGINT NULL,
    code VARCHAR(64) NOT NULL,
    name VARCHAR(128) NOT NULL,
    level INT NOT NULL,
    path VARCHAR(512) NOT NULL,
    sort_order INT NOT NULL DEFAULT 0,
    status VARCHAR(32) NOT NULL DEFAULT 'ACTIVE',
    created_at TIMESTAMP NOT NULL DEFAULT CURRENT_TIMESTAMP,
    updated_at TIMESTAMP NOT NULL DEFAULT CURRENT_TIMESTAMP ON UPDATE CURRENT_TIMESTAMP,
    created_by BIGINT NULL,
    updated_by BIGINT NULL,
    deleted TINYINT NOT NULL DEFAULT 0,
    UNIQUE KEY uk_masterdata_section_project_code_deleted (project_id, code, deleted),
    KEY idx_masterdata_section_project_parent (project_id, parent_id, sort_order),
    KEY idx_masterdata_section_project_path (project_id, path),
    CONSTRAINT fk_masterdata_section_project FOREIGN KEY (project_id) REFERENCES core_projects (id),
    CONSTRAINT fk_masterdata_section_parent FOREIGN KEY (parent_id) REFERENCES masterdata_section_nodes (id)
);

CREATE TABLE IF NOT EXISTS masterdata_node_types (
    id BIGINT PRIMARY KEY AUTO_INCREMENT,
    project_id BIGINT NOT NULL,
    code VARCHAR(64) NOT NULL,
    name VARCHAR(128) NOT NULL,
    scope_level INT NOT NULL DEFAULT 1,
    sort_order INT NOT NULL DEFAULT 0,
    status VARCHAR(32) NOT NULL DEFAULT 'ACTIVE',
    locked TINYINT NOT NULL DEFAULT 0,
    locked_at TIMESTAMP NULL,
    locked_by BIGINT NULL,
    created_at TIMESTAMP NOT NULL DEFAULT CURRENT_TIMESTAMP,
    updated_at TIMESTAMP NOT NULL DEFAULT CURRENT_TIMESTAMP ON UPDATE CURRENT_TIMESTAMP,
    created_by BIGINT NULL,
    updated_by BIGINT NULL,
    deleted TINYINT NOT NULL DEFAULT 0,
    UNIQUE KEY uk_masterdata_node_type_project_code_deleted (project_id, code, deleted),
    KEY idx_masterdata_node_type_project (project_id, sort_order),
    CONSTRAINT fk_masterdata_node_type_project FOREIGN KEY (project_id) REFERENCES core_projects (id)
);

INSERT INTO core_permissions (code, name, module_code)
SELECT 'MASTERDATA_SECTION_READ', '查看工程管理部位', 'master-data'
WHERE NOT EXISTS (SELECT 1 FROM core_permissions WHERE code = 'MASTERDATA_SECTION_READ');

UPDATE core_permissions
SET name = '查看工程管理部位', module_code = 'master-data'
WHERE code = 'MASTERDATA_SECTION_READ';

INSERT INTO core_permissions (code, name, module_code)
SELECT 'MASTERDATA_SECTION_MANAGE', '维护工程管理部位', 'master-data'
WHERE NOT EXISTS (SELECT 1 FROM core_permissions WHERE code = 'MASTERDATA_SECTION_MANAGE');

UPDATE core_permissions
SET name = '维护工程管理部位', module_code = 'master-data'
WHERE code = 'MASTERDATA_SECTION_MANAGE';

INSERT INTO core_permissions (code, name, module_code)
SELECT 'MASTERDATA_NODE_TYPE_READ', '查看节点类型', 'master-data'
WHERE NOT EXISTS (SELECT 1 FROM core_permissions WHERE code = 'MASTERDATA_NODE_TYPE_READ');

UPDATE core_permissions
SET name = '查看节点类型', module_code = 'master-data'
WHERE code = 'MASTERDATA_NODE_TYPE_READ';

INSERT INTO core_permissions (code, name, module_code)
SELECT 'MASTERDATA_NODE_TYPE_MANAGE', '维护节点类型', 'master-data'
WHERE NOT EXISTS (SELECT 1 FROM core_permissions WHERE code = 'MASTERDATA_NODE_TYPE_MANAGE');

UPDATE core_permissions
SET name = '维护节点类型', module_code = 'master-data'
WHERE code = 'MASTERDATA_NODE_TYPE_MANAGE';

INSERT INTO core_role_permissions (role_id, permission_id)
SELECT r.id, p.id
FROM core_roles r
JOIN core_permissions p ON p.code IN (
    'MASTERDATA_SECTION_READ',
    'MASTERDATA_SECTION_MANAGE',
    'MASTERDATA_NODE_TYPE_READ',
    'MASTERDATA_NODE_TYPE_MANAGE'
)
WHERE r.code = 'PROJECT_ADMIN'
  AND NOT EXISTS (
      SELECT 1
      FROM core_role_permissions rp
      WHERE rp.role_id = r.id AND rp.permission_id = p.id
  );

INSERT INTO core_role_permissions (role_id, permission_id)
SELECT r.id, p.id
FROM core_roles r
JOIN core_permissions p ON p.code IN (
    'MASTERDATA_SECTION_READ',
    'MASTERDATA_NODE_TYPE_READ'
)
WHERE r.code = 'DELIVERY_ENGINEER'
  AND NOT EXISTS (
      SELECT 1
      FROM core_role_permissions rp
      WHERE rp.role_id = r.id AND rp.permission_id = p.id
  );
