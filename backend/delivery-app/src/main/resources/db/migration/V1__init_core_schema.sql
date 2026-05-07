CREATE TABLE IF NOT EXISTS core_users (
    id BIGINT PRIMARY KEY AUTO_INCREMENT,
    username VARCHAR(64) NOT NULL,
    password_hash VARCHAR(255) NOT NULL,
    display_name VARCHAR(128) NOT NULL,
    status VARCHAR(32) NOT NULL DEFAULT 'ACTIVE',
    created_at TIMESTAMP NOT NULL DEFAULT CURRENT_TIMESTAMP,
    updated_at TIMESTAMP NOT NULL DEFAULT CURRENT_TIMESTAMP ON UPDATE CURRENT_TIMESTAMP,
    created_by BIGINT NULL,
    updated_by BIGINT NULL,
    deleted TINYINT NOT NULL DEFAULT 0,
    UNIQUE KEY uk_core_users_username (username)
);

CREATE TABLE IF NOT EXISTS core_roles (
    id BIGINT PRIMARY KEY AUTO_INCREMENT,
    code VARCHAR(64) NOT NULL,
    name VARCHAR(128) NOT NULL,
    created_at TIMESTAMP NOT NULL DEFAULT CURRENT_TIMESTAMP,
    updated_at TIMESTAMP NOT NULL DEFAULT CURRENT_TIMESTAMP ON UPDATE CURRENT_TIMESTAMP,
    created_by BIGINT NULL,
    updated_by BIGINT NULL,
    deleted TINYINT NOT NULL DEFAULT 0,
    UNIQUE KEY uk_core_roles_code (code)
);

CREATE TABLE IF NOT EXISTS core_projects (
    id BIGINT PRIMARY KEY AUTO_INCREMENT,
    code VARCHAR(64) NOT NULL,
    name VARCHAR(255) NOT NULL,
    industry_type VARCHAR(64) NOT NULL,
    owner_org_name VARCHAR(128) NULL,
    design_org_name VARCHAR(128) NULL,
    construct_org_name VARCHAR(128) NULL,
    supervision_org_name VARCHAR(128) NULL,
    status VARCHAR(32) NOT NULL DEFAULT 'ACTIVE',
    created_at TIMESTAMP NOT NULL DEFAULT CURRENT_TIMESTAMP,
    updated_at TIMESTAMP NOT NULL DEFAULT CURRENT_TIMESTAMP ON UPDATE CURRENT_TIMESTAMP,
    created_by BIGINT NULL,
    updated_by BIGINT NULL,
    deleted TINYINT NOT NULL DEFAULT 0,
    UNIQUE KEY uk_core_projects_code (code)
);

CREATE TABLE IF NOT EXISTS core_user_project_roles (
    id BIGINT PRIMARY KEY AUTO_INCREMENT,
    user_id BIGINT NOT NULL,
    project_id BIGINT NOT NULL,
    role_id BIGINT NOT NULL,
    created_at TIMESTAMP NOT NULL DEFAULT CURRENT_TIMESTAMP,
    updated_at TIMESTAMP NOT NULL DEFAULT CURRENT_TIMESTAMP ON UPDATE CURRENT_TIMESTAMP,
    created_by BIGINT NULL,
    updated_by BIGINT NULL,
    deleted TINYINT NOT NULL DEFAULT 0,
    UNIQUE KEY uk_core_upr_user_project_role (user_id, project_id, role_id),
    CONSTRAINT fk_core_upr_user FOREIGN KEY (user_id) REFERENCES core_users (id),
    CONSTRAINT fk_core_upr_project FOREIGN KEY (project_id) REFERENCES core_projects (id),
    CONSTRAINT fk_core_upr_role FOREIGN KEY (role_id) REFERENCES core_roles (id)
);

CREATE TABLE IF NOT EXISTS core_permissions (
    id BIGINT PRIMARY KEY AUTO_INCREMENT,
    code VARCHAR(128) NOT NULL,
    name VARCHAR(128) NOT NULL,
    module_code VARCHAR(64) NOT NULL,
    created_at TIMESTAMP NOT NULL DEFAULT CURRENT_TIMESTAMP,
    updated_at TIMESTAMP NOT NULL DEFAULT CURRENT_TIMESTAMP ON UPDATE CURRENT_TIMESTAMP,
    created_by BIGINT NULL,
    updated_by BIGINT NULL,
    deleted TINYINT NOT NULL DEFAULT 0,
    UNIQUE KEY uk_core_permissions_code (code)
);

CREATE TABLE IF NOT EXISTS core_role_permissions (
    id BIGINT PRIMARY KEY AUTO_INCREMENT,
    role_id BIGINT NOT NULL,
    permission_id BIGINT NOT NULL,
    created_at TIMESTAMP NOT NULL DEFAULT CURRENT_TIMESTAMP,
    updated_at TIMESTAMP NOT NULL DEFAULT CURRENT_TIMESTAMP ON UPDATE CURRENT_TIMESTAMP,
    created_by BIGINT NULL,
    updated_by BIGINT NULL,
    deleted TINYINT NOT NULL DEFAULT 0,
    UNIQUE KEY uk_core_role_permissions_role_perm (role_id, permission_id),
    CONSTRAINT fk_core_rp_role FOREIGN KEY (role_id) REFERENCES core_roles (id),
    CONSTRAINT fk_core_rp_permission FOREIGN KEY (permission_id) REFERENCES core_permissions (id)
);

CREATE TABLE IF NOT EXISTS core_audit_logs (
    id BIGINT PRIMARY KEY AUTO_INCREMENT,
    project_id BIGINT NULL,
    module_code VARCHAR(64) NOT NULL,
    action_code VARCHAR(128) NOT NULL,
    target_type VARCHAR(64) NOT NULL,
    target_id VARCHAR(128) NULL,
    operator_id BIGINT NULL,
    trace_id VARCHAR(64) NULL,
    details_json JSON NULL,
    created_at TIMESTAMP NOT NULL DEFAULT CURRENT_TIMESTAMP
);
