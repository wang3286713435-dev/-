package com.zhuoyu.delivery.datasteward.asset.repository;

import com.zhuoyu.delivery.datasteward.asset.dto.AssetDtos.DisciplineResponse;
import java.sql.ResultSet;
import java.sql.SQLException;
import java.util.List;
import org.springframework.jdbc.core.RowMapper;
import org.springframework.jdbc.core.namedparam.MapSqlParameterSource;
import org.springframework.jdbc.core.namedparam.NamedParameterJdbcTemplate;
import org.springframework.stereotype.Repository;

@Repository
public class AssetDisciplineRepository {

    private static final RowMapper<DisciplineResponse> ROW_MAPPER = AssetDisciplineRepository::map;

    private final NamedParameterJdbcTemplate jdbcTemplate;

    public AssetDisciplineRepository(NamedParameterJdbcTemplate jdbcTemplate) {
        this.jdbcTemplate = jdbcTemplate;
    }

    public List<DisciplineResponse> listBuiltin() {
        return jdbcTemplate.query("""
            SELECT id, code, name, project_id, scope, sort_order
            FROM data_asset_disciplines
            WHERE scope = 'BUILTIN'
            ORDER BY sort_order
            """, new MapSqlParameterSource(), ROW_MAPPER);
    }

    public List<DisciplineResponse> listByProject(Long projectId) {
        return jdbcTemplate.query("""
            SELECT id, code, name, project_id, scope, sort_order
            FROM data_asset_disciplines
            WHERE scope = 'BUILTIN' OR project_id = :projectId
            ORDER BY scope DESC, sort_order
            """, new MapSqlParameterSource("projectId", projectId), ROW_MAPPER);
    }

    public boolean existsByCode(String code) {
        Integer count = jdbcTemplate.queryForObject("""
            SELECT COUNT(1) FROM data_asset_disciplines WHERE code = :code
            """, new MapSqlParameterSource("code", code), Integer.class);
        return count != null && count > 0;
    }

    private static DisciplineResponse map(ResultSet rs, int rowNum) throws SQLException {
        return new DisciplineResponse(
            rs.getLong("id"),
            rs.getString("code"),
            rs.getString("name"),
            rs.getObject("project_id", Long.class),
            rs.getString("scope"),
            rs.getInt("sort_order")
        );
    }
}
