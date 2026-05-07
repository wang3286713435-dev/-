CREATE TABLE IF NOT EXISTS data_file_resources (
    id BIGINT PRIMARY KEY AUTO_INCREMENT,
    project_id BIGINT NOT NULL,
    original_name VARCHAR(255) NOT NULL,
    file_kind VARCHAR(32) NOT NULL,
    mime_type VARCHAR(128) NULL,
    size_bytes BIGINT NOT NULL DEFAULT 0,
    storage_uri VARCHAR(512) NOT NULL,
    checksum VARCHAR(128) NULL,
    business_tag VARCHAR(64) NULL,
    version_no VARCHAR(32) NOT NULL DEFAULT 'V1',
    process_status VARCHAR(32) NOT NULL DEFAULT 'PENDING',
    processed_at TIMESTAMP NULL,
    created_at TIMESTAMP NOT NULL DEFAULT CURRENT_TIMESTAMP,
    updated_at TIMESTAMP NOT NULL DEFAULT CURRENT_TIMESTAMP ON UPDATE CURRENT_TIMESTAMP,
    created_by BIGINT NULL,
    updated_by BIGINT NULL,
    deleted TINYINT NOT NULL DEFAULT 0,
    delete_token BIGINT NOT NULL DEFAULT 0,
    UNIQUE KEY uk_data_file_project_uri_token (project_id, storage_uri, delete_token),
    KEY idx_data_file_project_kind (project_id, file_kind, process_status),
    CONSTRAINT fk_data_file_project FOREIGN KEY (project_id) REFERENCES core_projects (id)
);

CREATE TABLE IF NOT EXISTS data_model_integrations (
    id BIGINT PRIMARY KEY AUTO_INCREMENT,
    project_id BIGINT NOT NULL,
    name VARCHAR(128) NOT NULL,
    model_file_id BIGINT NOT NULL,
    version_no VARCHAR(32) NOT NULL DEFAULT 'V1',
    status VARCHAR(32) NOT NULL DEFAULT 'DRAFT',
    component_count INT NOT NULL DEFAULT 0,
    published_at TIMESTAMP NULL,
    published_by BIGINT NULL,
    adapter_payload_json TEXT NULL,
    created_at TIMESTAMP NOT NULL DEFAULT CURRENT_TIMESTAMP,
    updated_at TIMESTAMP NOT NULL DEFAULT CURRENT_TIMESTAMP ON UPDATE CURRENT_TIMESTAMP,
    created_by BIGINT NULL,
    updated_by BIGINT NULL,
    deleted TINYINT NOT NULL DEFAULT 0,
    delete_token BIGINT NOT NULL DEFAULT 0,
    UNIQUE KEY uk_data_model_project_name_token (project_id, name, delete_token),
    KEY idx_data_model_project_status (project_id, status),
    CONSTRAINT fk_data_model_project FOREIGN KEY (project_id) REFERENCES core_projects (id),
    CONSTRAINT fk_data_model_file FOREIGN KEY (model_file_id) REFERENCES data_file_resources (id)
);

CREATE TABLE IF NOT EXISTS data_managed_objects (
    id BIGINT PRIMARY KEY AUTO_INCREMENT,
    project_id BIGINT NOT NULL,
    model_integration_id BIGINT NOT NULL,
    section_node_id BIGINT NULL,
    code VARCHAR(64) NOT NULL,
    name VARCHAR(128) NOT NULL,
    object_type VARCHAR(64) NOT NULL DEFAULT 'EQUIPMENT',
    external_id VARCHAR(128) NULL,
    discipline VARCHAR(64) NULL,
    status VARCHAR(32) NOT NULL DEFAULT 'ACTIVE',
    properties_json TEXT NULL,
    created_at TIMESTAMP NOT NULL DEFAULT CURRENT_TIMESTAMP,
    updated_at TIMESTAMP NOT NULL DEFAULT CURRENT_TIMESTAMP ON UPDATE CURRENT_TIMESTAMP,
    created_by BIGINT NULL,
    updated_by BIGINT NULL,
    deleted TINYINT NOT NULL DEFAULT 0,
    delete_token BIGINT NOT NULL DEFAULT 0,
    UNIQUE KEY uk_data_object_project_code_token (project_id, code, delete_token),
    KEY idx_data_object_project_model (project_id, model_integration_id),
    KEY idx_data_object_project_section (project_id, section_node_id),
    CONSTRAINT fk_data_object_project FOREIGN KEY (project_id) REFERENCES core_projects (id),
    CONSTRAINT fk_data_object_model FOREIGN KEY (model_integration_id) REFERENCES data_model_integrations (id),
    CONSTRAINT fk_data_object_section FOREIGN KEY (section_node_id) REFERENCES masterdata_section_nodes (id)
);

CREATE TABLE IF NOT EXISTS work_delivery_bindings (
    id BIGINT PRIMARY KEY AUTO_INCREMENT,
    project_id BIGINT NOT NULL,
    view_type VARCHAR(32) NOT NULL,
    section_node_id BIGINT NULL,
    managed_object_id BIGINT NULL,
    deliverable_type_id BIGINT NOT NULL,
    file_resource_id BIGINT NOT NULL,
    binding_status VARCHAR(32) NOT NULL DEFAULT 'BOUND',
    review_status VARCHAR(32) NOT NULL DEFAULT 'PENDING',
    sort_order INT NOT NULL DEFAULT 0,
    remark VARCHAR(512) NULL,
    created_at TIMESTAMP NOT NULL DEFAULT CURRENT_TIMESTAMP,
    updated_at TIMESTAMP NOT NULL DEFAULT CURRENT_TIMESTAMP ON UPDATE CURRENT_TIMESTAMP,
    created_by BIGINT NULL,
    updated_by BIGINT NULL,
    deleted TINYINT NOT NULL DEFAULT 0,
    delete_token BIGINT NOT NULL DEFAULT 0,
    UNIQUE KEY uk_work_binding_file_type_token (project_id, view_type, deliverable_type_id, file_resource_id, delete_token),
    KEY idx_work_binding_project_view (project_id, view_type, sort_order),
    KEY idx_work_binding_project_section (project_id, section_node_id),
    KEY idx_work_binding_project_object (project_id, managed_object_id),
    CONSTRAINT fk_work_binding_project FOREIGN KEY (project_id) REFERENCES core_projects (id),
    CONSTRAINT fk_work_binding_section FOREIGN KEY (section_node_id) REFERENCES masterdata_section_nodes (id),
    CONSTRAINT fk_work_binding_object FOREIGN KEY (managed_object_id) REFERENCES data_managed_objects (id),
    CONSTRAINT fk_work_binding_deliverable_type FOREIGN KEY (deliverable_type_id) REFERENCES masterdata_deliverable_types (id),
    CONSTRAINT fk_work_binding_file FOREIGN KEY (file_resource_id) REFERENCES data_file_resources (id)
);

INSERT INTO core_permissions (code, name, module_code)
SELECT 'DATA_STEWARD_FILE_READ', '查看文件资源', 'data-steward'
WHERE NOT EXISTS (SELECT 1 FROM core_permissions WHERE code = 'DATA_STEWARD_FILE_READ');
UPDATE core_permissions SET name = '查看文件资源', module_code = 'data-steward' WHERE code = 'DATA_STEWARD_FILE_READ';

INSERT INTO core_permissions (code, name, module_code)
SELECT 'DATA_STEWARD_FILE_MANAGE', '维护文件资源', 'data-steward'
WHERE NOT EXISTS (SELECT 1 FROM core_permissions WHERE code = 'DATA_STEWARD_FILE_MANAGE');
UPDATE core_permissions SET name = '维护文件资源', module_code = 'data-steward' WHERE code = 'DATA_STEWARD_FILE_MANAGE';

INSERT INTO core_permissions (code, name, module_code)
SELECT 'DATA_STEWARD_MODEL_READ', '查看模型集成', 'data-steward'
WHERE NOT EXISTS (SELECT 1 FROM core_permissions WHERE code = 'DATA_STEWARD_MODEL_READ');
UPDATE core_permissions SET name = '查看模型集成', module_code = 'data-steward' WHERE code = 'DATA_STEWARD_MODEL_READ';

INSERT INTO core_permissions (code, name, module_code)
SELECT 'DATA_STEWARD_MODEL_MANAGE', '维护模型集成', 'data-steward'
WHERE NOT EXISTS (SELECT 1 FROM core_permissions WHERE code = 'DATA_STEWARD_MODEL_MANAGE');
UPDATE core_permissions SET name = '维护模型集成', module_code = 'data-steward' WHERE code = 'DATA_STEWARD_MODEL_MANAGE';

INSERT INTO core_permissions (code, name, module_code)
SELECT 'DATA_STEWARD_OBJECT_READ', '查看管理对象', 'data-steward'
WHERE NOT EXISTS (SELECT 1 FROM core_permissions WHERE code = 'DATA_STEWARD_OBJECT_READ');
UPDATE core_permissions SET name = '查看管理对象', module_code = 'data-steward' WHERE code = 'DATA_STEWARD_OBJECT_READ';

INSERT INTO core_permissions (code, name, module_code)
SELECT 'DATA_STEWARD_OBJECT_MANAGE', '维护管理对象', 'data-steward'
WHERE NOT EXISTS (SELECT 1 FROM core_permissions WHERE code = 'DATA_STEWARD_OBJECT_MANAGE');
UPDATE core_permissions SET name = '维护管理对象', module_code = 'data-steward' WHERE code = 'DATA_STEWARD_OBJECT_MANAGE';

INSERT INTO core_permissions (code, name, module_code)
SELECT 'WORKCENTER_DELIVERY_READ', '查看交付视图', 'work-center'
WHERE NOT EXISTS (SELECT 1 FROM core_permissions WHERE code = 'WORKCENTER_DELIVERY_READ');
UPDATE core_permissions SET name = '查看交付视图', module_code = 'work-center' WHERE code = 'WORKCENTER_DELIVERY_READ';

INSERT INTO core_permissions (code, name, module_code)
SELECT 'WORKCENTER_DELIVERY_MANAGE', '维护交付绑定', 'work-center'
WHERE NOT EXISTS (SELECT 1 FROM core_permissions WHERE code = 'WORKCENTER_DELIVERY_MANAGE');
UPDATE core_permissions SET name = '维护交付绑定', module_code = 'work-center' WHERE code = 'WORKCENTER_DELIVERY_MANAGE';

INSERT INTO core_permissions (code, name, module_code)
SELECT 'WORKCENTER_DASHBOARD_VIEW', '查看智慧大屏', 'work-center'
WHERE NOT EXISTS (SELECT 1 FROM core_permissions WHERE code = 'WORKCENTER_DASHBOARD_VIEW');
UPDATE core_permissions SET name = '查看智慧大屏', module_code = 'work-center' WHERE code = 'WORKCENTER_DASHBOARD_VIEW';

INSERT INTO core_permissions (code, name, module_code)
SELECT 'VISUALIZATION_WORKBENCH_VIEW', '查看三维工作台', 'visualization-adapter'
WHERE NOT EXISTS (SELECT 1 FROM core_permissions WHERE code = 'VISUALIZATION_WORKBENCH_VIEW');
UPDATE core_permissions SET name = '查看三维工作台', module_code = 'visualization-adapter' WHERE code = 'VISUALIZATION_WORKBENCH_VIEW';

INSERT INTO core_role_permissions (role_id, permission_id)
SELECT r.id, p.id
FROM core_roles r
JOIN core_permissions p ON p.code IN (
    'DATA_STEWARD_FILE_READ',
    'DATA_STEWARD_FILE_MANAGE',
    'DATA_STEWARD_MODEL_READ',
    'DATA_STEWARD_MODEL_MANAGE',
    'DATA_STEWARD_OBJECT_READ',
    'DATA_STEWARD_OBJECT_MANAGE',
    'WORKCENTER_DELIVERY_READ',
    'WORKCENTER_DELIVERY_MANAGE',
    'WORKCENTER_DASHBOARD_VIEW',
    'VISUALIZATION_WORKBENCH_VIEW'
)
WHERE r.code = 'PROJECT_ADMIN'
  AND NOT EXISTS (
      SELECT 1 FROM core_role_permissions rp
      WHERE rp.role_id = r.id AND rp.permission_id = p.id
  );

INSERT INTO core_role_permissions (role_id, permission_id)
SELECT r.id, p.id
FROM core_roles r
JOIN core_permissions p ON p.code IN (
    'DATA_STEWARD_FILE_READ',
    'DATA_STEWARD_FILE_MANAGE',
    'DATA_STEWARD_MODEL_READ',
    'DATA_STEWARD_MODEL_MANAGE',
    'DATA_STEWARD_OBJECT_READ',
    'DATA_STEWARD_OBJECT_MANAGE',
    'WORKCENTER_DELIVERY_READ',
    'WORKCENTER_DELIVERY_MANAGE',
    'WORKCENTER_DASHBOARD_VIEW',
    'VISUALIZATION_WORKBENCH_VIEW'
)
WHERE r.code = 'DELIVERY_ENGINEER'
  AND NOT EXISTS (
      SELECT 1 FROM core_role_permissions rp
      WHERE rp.role_id = r.id AND rp.permission_id = p.id
  );

INSERT INTO core_role_permissions (role_id, permission_id)
SELECT r.id, p.id
FROM core_roles r
JOIN core_permissions p ON p.code IN (
    'DATA_STEWARD_FILE_READ',
    'DATA_STEWARD_MODEL_READ',
    'DATA_STEWARD_OBJECT_READ',
    'WORKCENTER_DELIVERY_READ',
    'WORKCENTER_DASHBOARD_VIEW',
    'VISUALIZATION_WORKBENCH_VIEW'
)
WHERE r.code = 'PROJECT_VIEWER'
  AND NOT EXISTS (
      SELECT 1 FROM core_role_permissions rp
      WHERE rp.role_id = r.id AND rp.permission_id = p.id
  );
