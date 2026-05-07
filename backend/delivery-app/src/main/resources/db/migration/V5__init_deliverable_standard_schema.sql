CREATE TABLE IF NOT EXISTS masterdata_deliverable_definitions (
    id BIGINT PRIMARY KEY AUTO_INCREMENT,
    project_id BIGINT NOT NULL,
    node_type_id BIGINT NOT NULL,
    code VARCHAR(64) NOT NULL,
    name VARCHAR(128) NOT NULL,
    category VARCHAR(64) NOT NULL DEFAULT 'DOCUMENT',
    required TINYINT NOT NULL DEFAULT 0,
    sort_order INT NOT NULL DEFAULT 0,
    status VARCHAR(32) NOT NULL DEFAULT 'ACTIVE',
    created_at TIMESTAMP NOT NULL DEFAULT CURRENT_TIMESTAMP,
    updated_at TIMESTAMP NOT NULL DEFAULT CURRENT_TIMESTAMP ON UPDATE CURRENT_TIMESTAMP,
    created_by BIGINT NULL,
    updated_by BIGINT NULL,
    deleted TINYINT NOT NULL DEFAULT 0,
    delete_token BIGINT NOT NULL DEFAULT 0,
    UNIQUE KEY uk_md_deliverable_def_project_code_token (project_id, code, delete_token),
    KEY idx_md_deliverable_def_project (project_id, sort_order),
    KEY idx_md_deliverable_def_node_type (project_id, node_type_id),
    CONSTRAINT fk_md_deliverable_def_project FOREIGN KEY (project_id) REFERENCES core_projects (id),
    CONSTRAINT fk_md_deliverable_def_node_type FOREIGN KEY (node_type_id) REFERENCES masterdata_node_types (id)
);

CREATE TABLE IF NOT EXISTS masterdata_deliverable_types (
    id BIGINT PRIMARY KEY AUTO_INCREMENT,
    project_id BIGINT NOT NULL,
    deliverable_definition_id BIGINT NOT NULL,
    code VARCHAR(64) NOT NULL,
    name VARCHAR(128) NOT NULL,
    file_kind VARCHAR(32) NOT NULL DEFAULT 'DOCUMENT',
    binding_strategy VARCHAR(32) NOT NULL DEFAULT 'SECTION_NODE',
    sort_order INT NOT NULL DEFAULT 0,
    status VARCHAR(32) NOT NULL DEFAULT 'ACTIVE',
    created_at TIMESTAMP NOT NULL DEFAULT CURRENT_TIMESTAMP,
    updated_at TIMESTAMP NOT NULL DEFAULT CURRENT_TIMESTAMP ON UPDATE CURRENT_TIMESTAMP,
    created_by BIGINT NULL,
    updated_by BIGINT NULL,
    deleted TINYINT NOT NULL DEFAULT 0,
    delete_token BIGINT NOT NULL DEFAULT 0,
    UNIQUE KEY uk_md_deliverable_type_project_code_token (project_id, code, delete_token),
    KEY idx_md_deliverable_type_project (project_id, sort_order),
    KEY idx_md_deliverable_type_def (project_id, deliverable_definition_id),
    CONSTRAINT fk_md_deliverable_type_project FOREIGN KEY (project_id) REFERENCES core_projects (id),
    CONSTRAINT fk_md_deliverable_type_def FOREIGN KEY (deliverable_definition_id) REFERENCES masterdata_deliverable_definitions (id)
);

CREATE TABLE IF NOT EXISTS masterdata_deliverable_attributes (
    id BIGINT PRIMARY KEY AUTO_INCREMENT,
    project_id BIGINT NOT NULL,
    deliverable_type_id BIGINT NOT NULL,
    code VARCHAR(64) NOT NULL,
    name VARCHAR(128) NOT NULL,
    value_type VARCHAR(32) NOT NULL DEFAULT 'TEXT',
    unit VARCHAR(32) NULL,
    required TINYINT NOT NULL DEFAULT 0,
    example_value VARCHAR(256) NULL,
    enum_options TEXT NULL,
    sort_order INT NOT NULL DEFAULT 0,
    status VARCHAR(32) NOT NULL DEFAULT 'ACTIVE',
    created_at TIMESTAMP NOT NULL DEFAULT CURRENT_TIMESTAMP,
    updated_at TIMESTAMP NOT NULL DEFAULT CURRENT_TIMESTAMP ON UPDATE CURRENT_TIMESTAMP,
    created_by BIGINT NULL,
    updated_by BIGINT NULL,
    deleted TINYINT NOT NULL DEFAULT 0,
    delete_token BIGINT NOT NULL DEFAULT 0,
    UNIQUE KEY uk_md_deliverable_attr_project_code_token (project_id, code, delete_token),
    KEY idx_md_deliverable_attr_project (project_id, sort_order),
    KEY idx_md_deliverable_attr_type (project_id, deliverable_type_id),
    CONSTRAINT fk_md_deliverable_attr_project FOREIGN KEY (project_id) REFERENCES core_projects (id),
    CONSTRAINT fk_md_deliverable_attr_type FOREIGN KEY (deliverable_type_id) REFERENCES masterdata_deliverable_types (id)
);

CREATE TABLE IF NOT EXISTS masterdata_directory_templates (
    id BIGINT PRIMARY KEY AUTO_INCREMENT,
    project_id BIGINT NOT NULL,
    template_type VARCHAR(32) NOT NULL DEFAULT 'DOCUMENT',
    name VARCHAR(128) NOT NULL,
    root_node_json TEXT NULL,
    source_type VARCHAR(32) NOT NULL DEFAULT 'MANUAL',
    sort_order INT NOT NULL DEFAULT 0,
    status VARCHAR(32) NOT NULL DEFAULT 'ACTIVE',
    created_at TIMESTAMP NOT NULL DEFAULT CURRENT_TIMESTAMP,
    updated_at TIMESTAMP NOT NULL DEFAULT CURRENT_TIMESTAMP ON UPDATE CURRENT_TIMESTAMP,
    created_by BIGINT NULL,
    updated_by BIGINT NULL,
    deleted TINYINT NOT NULL DEFAULT 0,
    delete_token BIGINT NOT NULL DEFAULT 0,
    UNIQUE KEY uk_md_dir_template_project_code_token (project_id, name, delete_token),
    KEY idx_md_dir_template_project (project_id, sort_order),
    CONSTRAINT fk_md_dir_template_project FOREIGN KEY (project_id) REFERENCES core_projects (id)
);

-- Permissions for deliverable standard management
INSERT INTO core_permissions (code, name, module_code)
SELECT 'MASTERDATA_DELIVERABLE_READ', '查看交付物标准', 'master-data'
WHERE NOT EXISTS (SELECT 1 FROM core_permissions WHERE code = 'MASTERDATA_DELIVERABLE_READ');

UPDATE core_permissions
SET name = '查看交付物标准', module_code = 'master-data'
WHERE code = 'MASTERDATA_DELIVERABLE_READ';

INSERT INTO core_permissions (code, name, module_code)
SELECT 'MASTERDATA_DELIVERABLE_MANAGE', '维护交付物标准', 'master-data'
WHERE NOT EXISTS (SELECT 1 FROM core_permissions WHERE code = 'MASTERDATA_DELIVERABLE_MANAGE');

UPDATE core_permissions
SET name = '维护交付物标准', module_code = 'master-data'
WHERE code = 'MASTERDATA_DELIVERABLE_MANAGE';

INSERT INTO core_permissions (code, name, module_code)
SELECT 'MASTERDATA_DIR_TEMPLATE_READ', '查看目录模板', 'master-data'
WHERE NOT EXISTS (SELECT 1 FROM core_permissions WHERE code = 'MASTERDATA_DIR_TEMPLATE_READ');

UPDATE core_permissions
SET name = '查看目录模板', module_code = 'master-data'
WHERE code = 'MASTERDATA_DIR_TEMPLATE_READ';

INSERT INTO core_permissions (code, name, module_code)
SELECT 'MASTERDATA_DIR_TEMPLATE_MANAGE', '维护目录模板', 'master-data'
WHERE NOT EXISTS (SELECT 1 FROM core_permissions WHERE code = 'MASTERDATA_DIR_TEMPLATE_MANAGE');

UPDATE core_permissions
SET name = '维护目录模板', module_code = 'master-data'
WHERE code = 'MASTERDATA_DIR_TEMPLATE_MANAGE';

-- Grant permissions to project admin role
INSERT INTO core_role_permissions (role_id, permission_id)
SELECT r.id, p.id
FROM core_roles r
JOIN core_permissions p ON p.code IN (
    'MASTERDATA_DELIVERABLE_READ',
    'MASTERDATA_DELIVERABLE_MANAGE',
    'MASTERDATA_DIR_TEMPLATE_READ',
    'MASTERDATA_DIR_TEMPLATE_MANAGE'
)
WHERE r.code = 'PROJECT_ADMIN'
  AND NOT EXISTS (
      SELECT 1
      FROM core_role_permissions rp
      WHERE rp.role_id = r.id AND rp.permission_id = p.id
  );

-- Grant read permissions to delivery engineer role
INSERT INTO core_role_permissions (role_id, permission_id)
SELECT r.id, p.id
FROM core_roles r
JOIN core_permissions p ON p.code IN (
    'MASTERDATA_DELIVERABLE_READ',
    'MASTERDATA_DIR_TEMPLATE_READ'
)
WHERE r.code = 'DELIVERY_ENGINEER'
  AND NOT EXISTS (
      SELECT 1
      FROM core_role_permissions rp
      WHERE rp.role_id = r.id AND rp.permission_id = p.id
  );
