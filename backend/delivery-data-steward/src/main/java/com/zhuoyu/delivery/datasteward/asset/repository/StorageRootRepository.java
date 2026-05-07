package com.zhuoyu.delivery.datasteward.asset.repository;

import com.zhuoyu.delivery.datasteward.asset.dto.AssetDtos.StorageRoot;
import com.zhuoyu.delivery.shared.exception.BusinessException;
import java.sql.ResultSet;
import java.sql.SQLException;
import java.util.List;
import org.springframework.http.HttpStatus;
import org.springframework.jdbc.core.RowMapper;
import org.springframework.jdbc.core.namedparam.MapSqlParameterSource;
import org.springframework.jdbc.core.namedparam.NamedParameterJdbcTemplate;
import org.springframework.stereotype.Repository;

@Repository
public class StorageRootRepository {

    private static final RowMapper<StorageRoot> ROW_MAPPER = StorageRootRepository::map;

    private final NamedParameterJdbcTemplate jdbcTemplate;

    public StorageRootRepository(NamedParameterJdbcTemplate jdbcTemplate) {
        this.jdbcTemplate = jdbcTemplate;
    }

    public void upsert(String providerCode, String rootCode, String displayName, String rootPath, Long operatorId) {
        String sql = """
            INSERT INTO data_storage_roots (
                provider_code, root_code, display_name, root_path, created_by, updated_by
            ) VALUES (
                :providerCode, :rootCode, :displayName, :rootPath, :operatorId, :operatorId
            )
            ON DUPLICATE KEY UPDATE
                display_name = VALUES(display_name),
                root_path = VALUES(root_path),
                status = 'ACTIVE',
                updated_by = VALUES(updated_by),
                deleted = 0
            """;
        jdbcTemplate.update(sql, new MapSqlParameterSource()
            .addValue("providerCode", providerCode)
            .addValue("rootCode", rootCode)
            .addValue("displayName", displayName)
            .addValue("rootPath", rootPath)
            .addValue("operatorId", operatorId));
    }

    public StorageRoot requireRoot(String providerCode, String rootCode) {
        List<StorageRoot> rows = jdbcTemplate.query("""
            SELECT id, provider_code, root_code, display_name, root_path
            FROM data_storage_roots
            WHERE provider_code = :providerCode
              AND root_code = :rootCode
              AND status = 'ACTIVE'
              AND deleted = 0
            """, new MapSqlParameterSource()
            .addValue("providerCode", providerCode)
            .addValue("rootCode", rootCode), ROW_MAPPER);
        if (rows.isEmpty()) {
            throw new BusinessException("DATA_STORAGE_ROOT_NOT_FOUND", "存储根不存在或未启用", HttpStatus.NOT_FOUND);
        }
        return rows.getFirst();
    }

    private static StorageRoot map(ResultSet rs, int rowNum) throws SQLException {
        return new StorageRoot(
            rs.getLong("id"),
            rs.getString("provider_code"),
            rs.getString("root_code"),
            rs.getString("display_name"),
            rs.getString("root_path")
        );
    }
}
