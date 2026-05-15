package com.zhuoyu.delivery.datasteward.asset.repository;

import com.zhuoyu.delivery.datasteward.asset.dto.AssetDtos.ImportRowError;
import java.sql.ResultSet;
import java.sql.SQLException;
import java.util.List;
import org.springframework.jdbc.core.RowMapper;
import org.springframework.jdbc.core.namedparam.MapSqlParameterSource;
import org.springframework.jdbc.core.namedparam.NamedParameterJdbcTemplate;
import org.springframework.stereotype.Repository;

@Repository
public class AssetImportRowRepository {

    private final NamedParameterJdbcTemplate jdbcTemplate;

    public AssetImportRowRepository(NamedParameterJdbcTemplate jdbcTemplate) {
        this.jdbcTemplate = jdbcTemplate;
    }

    public void insert(Long importJobId, int rowNo, String rawData, boolean success,
                        String targetType, Long targetId, String errorCode, String errorMessage) {
        jdbcTemplate.update("""
            INSERT INTO data_asset_import_rows (
                import_job_id, row_no, raw_data, success, target_type, target_id,
                error_code, error_message
            ) VALUES (
                :importJobId, :rowNo, :rawData, :success, :targetType, :targetId,
                :errorCode, :errorMessage
            )
            """, new MapSqlParameterSource()
            .addValue("importJobId", importJobId)
            .addValue("rowNo", rowNo)
            .addValue("rawData", rawData)
            .addValue("success", success ? 1 : 0)
            .addValue("targetType", targetType)
            .addValue("targetId", targetId)
            .addValue("errorCode", errorCode)
            .addValue("errorMessage", errorMessage));
    }

    public List<ImportRowError> findErrorsByJobId(Long importJobId) {
        return jdbcTemplate.query("""
            SELECT row_no, COALESCE(raw_data, '') AS raw_data, error_code, error_message
            FROM data_asset_import_rows
            WHERE import_job_id = :importJobId AND success = 0
            ORDER BY row_no
            """, new MapSqlParameterSource("importJobId", importJobId), (rs, rowNum) -> new ImportRowError(
            rs.getInt("row_no"),
            rs.getString("raw_data"),
            rs.getString("error_code"),
            rs.getString("error_message")
        ));
    }
}
