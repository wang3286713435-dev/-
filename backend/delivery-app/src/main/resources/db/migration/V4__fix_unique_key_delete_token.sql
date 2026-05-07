ALTER TABLE masterdata_section_nodes
    ADD COLUMN delete_token BIGINT NOT NULL DEFAULT 0;

ALTER TABLE masterdata_section_nodes
    DROP INDEX uk_masterdata_section_project_code_deleted;

ALTER TABLE masterdata_section_nodes
    ADD UNIQUE KEY uk_masterdata_section_project_code_token (project_id, code, delete_token);

UPDATE masterdata_section_nodes
SET delete_token = id
WHERE deleted = 1;

ALTER TABLE masterdata_node_types
    ADD COLUMN delete_token BIGINT NOT NULL DEFAULT 0;

ALTER TABLE masterdata_node_types
    DROP INDEX uk_masterdata_node_type_project_code_deleted;

ALTER TABLE masterdata_node_types
    ADD UNIQUE KEY uk_masterdata_node_type_project_code_token (project_id, code, delete_token);

UPDATE masterdata_node_types
SET delete_token = id
WHERE deleted = 1;
