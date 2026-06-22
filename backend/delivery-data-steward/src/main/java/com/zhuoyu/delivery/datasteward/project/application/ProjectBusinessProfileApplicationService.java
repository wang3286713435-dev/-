package com.zhuoyu.delivery.datasteward.project.application;

import com.zhuoyu.delivery.core.audit.application.AuditLogApplicationService;
import com.zhuoyu.delivery.datasteward.asset.dto.AssetDtos.ProjectBusinessProfileResponse;
import com.zhuoyu.delivery.datasteward.asset.dto.AssetDtos.ProjectBusinessProfileUpdateRequest;
import com.zhuoyu.delivery.datasteward.asset.dto.AssetDtos.ProjectMembersSummaryResponse;
import com.zhuoyu.delivery.shared.exception.BusinessException;
import java.math.BigDecimal;
import java.math.RoundingMode;
import java.time.LocalDate;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;
import java.util.Set;
import org.springframework.http.HttpStatus;
import org.springframework.jdbc.core.namedparam.MapSqlParameterSource;
import org.springframework.jdbc.core.namedparam.NamedParameterJdbcTemplate;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

@Service
public class ProjectBusinessProfileApplicationService {

    private static final String MODULE_CODE = "data-steward";
    private static final Set<String> PAYMENT_STATUSES = Set.of(
        "UNSET", "NOT_STARTED", "PARTIAL", "COMPLETED", "OVERDUE"
    );

    private final NamedParameterJdbcTemplate jdbcTemplate;
    private final AuditLogApplicationService auditLogApplicationService;

    public ProjectBusinessProfileApplicationService(
        NamedParameterJdbcTemplate jdbcTemplate,
        AuditLogApplicationService auditLogApplicationService
    ) {
        this.jdbcTemplate = jdbcTemplate;
        this.auditLogApplicationService = auditLogApplicationService;
    }

    public ProjectBusinessProfileResponse getBusinessProfile(Long userId, Long projectId) {
        ProjectCore project = requireActiveProject(projectId);
        requireProjectReadable(userId, projectId);
        ProfileRow profile = findProfile(projectId);
        ProjectMembersSummaryResponse membersSummary = membersSummary(userId, projectId);
        boolean editable = canEditProject(userId, projectId);
        return response(project, profile, membersSummary, editable);
    }

    public ProjectMembersSummaryResponse membersSummary(Long userId, Long projectId) {
        requireActiveProject(projectId);
        requireProjectReadable(userId, projectId);
        return jdbcTemplate.query("""
            SELECT COUNT(DISTINCT upr.user_id) AS member_count,
                   COUNT(DISTINCT CASE WHEN r.code = 'PROJECT_ADMIN' THEN upr.user_id END) AS project_admin_count,
                   COUNT(DISTINCT CASE WHEN r.code = 'DELIVERY_ENGINEER' THEN upr.user_id END) AS delivery_engineer_count,
                   COUNT(DISTINCT CASE WHEN r.code = 'PROJECT_VIEWER' THEN upr.user_id END) AS viewer_count
            FROM core_user_project_roles upr
            JOIN core_users u ON u.id = upr.user_id
                AND u.deleted = 0
                AND u.status = 'ACTIVE'
            JOIN core_roles r ON r.id = upr.role_id AND r.deleted = 0
            WHERE upr.project_id = :projectId
              AND upr.deleted = 0
            """, new MapSqlParameterSource("projectId", projectId), (rs, rowNum) ->
            new ProjectMembersSummaryResponse(
                rs.getInt("member_count"),
                rs.getInt("project_admin_count"),
                rs.getInt("delivery_engineer_count"),
                rs.getInt("viewer_count")
            )
        ).stream().findFirst().orElse(new ProjectMembersSummaryResponse(0, 0, 0, 0));
    }

    @Transactional
    public ProjectBusinessProfileResponse updateBusinessProfile(
        Long userId,
        Long projectId,
        ProjectBusinessProfileUpdateRequest request
    ) {
        ProjectCore project = requireActiveProject(projectId);
        requireProjectEditable(userId, projectId);
        NormalizedProfile normalized = normalize(request);
        jdbcTemplate.update("""
            INSERT INTO core_project_business_profiles (
                project_id, budget_amount, contract_amount, received_amount, payment_status,
                expected_payment_date, planned_start_date, planned_delivery_date, actual_delivery_date,
                currency_code, business_remark, created_by, updated_by, deleted
            ) VALUES (
                :projectId, :budgetAmount, :contractAmount, :receivedAmount, :paymentStatus,
                :expectedPaymentDate, :plannedStartDate, :plannedDeliveryDate, :actualDeliveryDate,
                :currencyCode, :businessRemark, :operatorId, :operatorId, 0
            )
            ON DUPLICATE KEY UPDATE
                budget_amount = VALUES(budget_amount),
                contract_amount = VALUES(contract_amount),
                received_amount = VALUES(received_amount),
                payment_status = VALUES(payment_status),
                expected_payment_date = VALUES(expected_payment_date),
                planned_start_date = VALUES(planned_start_date),
                planned_delivery_date = VALUES(planned_delivery_date),
                actual_delivery_date = VALUES(actual_delivery_date),
                currency_code = VALUES(currency_code),
                business_remark = VALUES(business_remark),
                updated_by = VALUES(updated_by),
                deleted = 0
            """, new MapSqlParameterSource()
            .addValue("projectId", projectId)
            .addValue("budgetAmount", normalized.budgetAmount())
            .addValue("contractAmount", normalized.contractAmount())
            .addValue("receivedAmount", normalized.receivedAmount())
            .addValue("paymentStatus", normalized.paymentStatus())
            .addValue("expectedPaymentDate", normalized.expectedPaymentDate())
            .addValue("plannedStartDate", normalized.plannedStartDate())
            .addValue("plannedDeliveryDate", normalized.plannedDeliveryDate())
            .addValue("actualDeliveryDate", normalized.actualDeliveryDate())
            .addValue("currencyCode", normalized.currencyCode())
            .addValue("businessRemark", normalized.businessRemark())
            .addValue("operatorId", userId));
        auditLogApplicationService.record(projectId, MODULE_CODE, "project.business-profile.update",
            "PROJECT_BUSINESS_PROFILE", String.valueOf(projectId), userId, auditDetails(normalized));
        return response(project, findProfile(projectId), membersSummary(userId, projectId), true);
    }

    private ProjectBusinessProfileResponse response(
        ProjectCore project,
        ProfileRow profile,
        ProjectMembersSummaryResponse membersSummary,
        boolean editable
    ) {
        BigDecimal receivedAmount = profile == null ? null : profile.receivedAmount();
        BigDecimal contractAmount = profile == null ? null : profile.contractAmount();
        return new ProjectBusinessProfileResponse(
            project.projectId(),
            project.projectCode(),
            project.projectName(),
            profile == null ? null : profile.budgetAmount(),
            contractAmount,
            receivedAmount,
            paymentProgressPercent(receivedAmount, contractAmount),
            profile == null ? "UNSET" : profile.paymentStatus(),
            profile == null ? null : profile.expectedPaymentDate(),
            profile == null ? null : profile.plannedStartDate(),
            profile == null ? null : profile.plannedDeliveryDate(),
            profile == null ? null : profile.actualDeliveryDate(),
            profile == null ? "CNY" : profile.currencyCode(),
            profile == null ? null : profile.businessRemark(),
            membersSummary,
            editable
        );
    }

    private ProjectCore requireActiveProject(Long projectId) {
        if (projectId == null) {
            throw new BusinessException("CORE_PROJECT_ID_REQUIRED", "项目ID不能为空", HttpStatus.BAD_REQUEST);
        }
        List<ProjectCore> rows = jdbcTemplate.query("""
            SELECT id, code, name
            FROM core_projects
            WHERE id = :projectId
              AND deleted = 0
              AND status = 'ACTIVE'
              AND COALESCE(asset_status, 'ACTIVE') <> 'ARCHIVED'
            LIMIT 1
            """, new MapSqlParameterSource("projectId", projectId),
            (rs, rowNum) -> new ProjectCore(
                rs.getLong("id"),
                rs.getString("code"),
                rs.getString("name")
            ));
        return rows.stream().findFirst()
            .orElseThrow(() -> new BusinessException("CORE_PROJECT_NOT_FOUND", "项目不存在或已归档", HttpStatus.NOT_FOUND));
    }

    private ProfileRow findProfile(Long projectId) {
        return jdbcTemplate.query("""
            SELECT budget_amount, contract_amount, received_amount, payment_status,
                   expected_payment_date, planned_start_date, planned_delivery_date, actual_delivery_date,
                   currency_code, business_remark
            FROM core_project_business_profiles
            WHERE project_id = :projectId
              AND deleted = 0
            LIMIT 1
            """, new MapSqlParameterSource("projectId", projectId), (rs, rowNum) ->
            new ProfileRow(
                rs.getBigDecimal("budget_amount"),
                rs.getBigDecimal("contract_amount"),
                rs.getBigDecimal("received_amount"),
                rs.getString("payment_status"),
                localDate(rs.getDate("expected_payment_date")),
                localDate(rs.getDate("planned_start_date")),
                localDate(rs.getDate("planned_delivery_date")),
                localDate(rs.getDate("actual_delivery_date")),
                rs.getString("currency_code"),
                rs.getString("business_remark")
            )
        ).stream().findFirst().orElse(null);
    }

    private void requireProjectReadable(Long userId, Long projectId) {
        if (isSuperAdmin(userId) || hasProjectRole(userId, projectId, null)) {
            return;
        }
        throw new BusinessException("PROJECT_ACCESS_DENIED", "当前账号无项目权限", HttpStatus.FORBIDDEN);
    }

    private void requireProjectEditable(Long userId, Long projectId) {
        if (canEditProject(userId, projectId)) {
            return;
        }
        throw new BusinessException("PROJECT_BUSINESS_PROFILE_EDIT_FORBIDDEN",
            "只有项目管理员可以维护项目经营信息", HttpStatus.FORBIDDEN);
    }

    private boolean canEditProject(Long userId, Long projectId) {
        return isSuperAdmin(userId) || hasProjectRole(userId, projectId, "PROJECT_ADMIN");
    }

    private boolean hasProjectRole(Long userId, Long projectId, String roleCode) {
        if (userId == null || projectId == null) {
            return false;
        }
        MapSqlParameterSource params = new MapSqlParameterSource()
            .addValue("userId", userId)
            .addValue("projectId", projectId)
            .addValue("roleCode", roleCode);
        String roleFilter = roleCode == null ? "" : " AND r.code = :roleCode";
        Integer count = jdbcTemplate.queryForObject("""
            SELECT COUNT(1)
            FROM core_user_project_roles upr
            JOIN core_roles r ON r.id = upr.role_id AND r.deleted = 0
            WHERE upr.user_id = :userId
              AND upr.project_id = :projectId
              AND upr.deleted = 0
            """ + roleFilter, params, Integer.class);
        return count != null && count > 0;
    }

    private boolean isSuperAdmin(Long userId) {
        if (userId == null) {
            return false;
        }
        List<String> rows = jdbcTemplate.query("""
            SELECT username
            FROM core_users
            WHERE id = :userId
              AND status = 'ACTIVE'
              AND deleted = 0
            LIMIT 1
            """, new MapSqlParameterSource("userId", userId), (rs, rowNum) -> rs.getString("username"));
        String username = rows.isEmpty() ? null : rows.getFirst();
        return "admin".equalsIgnoreCase(username);
    }

    private NormalizedProfile normalize(ProjectBusinessProfileUpdateRequest request) {
        if (request == null) {
            throw new BusinessException("PROJECT_BUSINESS_PROFILE_REQUEST_REQUIRED",
                "项目经营信息不能为空", HttpStatus.BAD_REQUEST);
        }
        BigDecimal budgetAmount = normalizeAmount(request.budgetAmount(), "预算金额不能为负数");
        BigDecimal contractAmount = normalizeAmount(request.contractAmount(), "合同金额不能为负数");
        BigDecimal receivedAmount = normalizeAmount(request.receivedAmount(), "已回款金额不能为负数");
        if (contractAmount != null && receivedAmount != null && receivedAmount.compareTo(contractAmount) > 0) {
            throw new BusinessException("PROJECT_BUSINESS_PROFILE_RECEIVED_EXCEEDS_CONTRACT",
                "已回款金额不能大于合同金额", HttpStatus.BAD_REQUEST);
        }
        String paymentStatus = normalizePaymentStatus(request.paymentStatus());
        String currencyCode = normalizeCurrency(request.currencyCode());
        String businessRemark = normalizeRemark(request.businessRemark());
        return new NormalizedProfile(
            budgetAmount,
            contractAmount,
            receivedAmount,
            paymentStatus,
            request.expectedPaymentDate(),
            request.plannedStartDate(),
            request.plannedDeliveryDate(),
            request.actualDeliveryDate(),
            currencyCode,
            businessRemark
        );
    }

    private BigDecimal normalizeAmount(BigDecimal value, String message) {
        if (value == null) {
            return null;
        }
        if (value.compareTo(BigDecimal.ZERO) < 0) {
            throw new BusinessException("PROJECT_BUSINESS_PROFILE_AMOUNT_INVALID", message, HttpStatus.BAD_REQUEST);
        }
        return value.setScale(2, RoundingMode.HALF_UP);
    }

    private String normalizePaymentStatus(String value) {
        if (value == null || value.isBlank()) {
            return "UNSET";
        }
        String normalized = value.trim().toUpperCase();
        if (!PAYMENT_STATUSES.contains(normalized)) {
            throw new BusinessException("PROJECT_BUSINESS_PROFILE_PAYMENT_STATUS_INVALID",
                "回款状态只能是 UNSET / NOT_STARTED / PARTIAL / COMPLETED / OVERDUE", HttpStatus.BAD_REQUEST);
        }
        return normalized;
    }

    private String normalizeCurrency(String value) {
        if (value == null || value.isBlank()) {
            return "CNY";
        }
        String normalized = value.trim().toUpperCase();
        if (normalized.length() > 16) {
            throw new BusinessException("PROJECT_BUSINESS_PROFILE_CURRENCY_TOO_LONG",
                "币种代码不能超过 16 个字符", HttpStatus.BAD_REQUEST);
        }
        return normalized;
    }

    private String normalizeRemark(String value) {
        if (value == null || value.isBlank()) {
            return null;
        }
        String normalized = value.trim();
        if (normalized.length() > 1000) {
            throw new BusinessException("PROJECT_BUSINESS_PROFILE_REMARK_TOO_LONG",
                "经营备注不能超过 1000 个字符", HttpStatus.BAD_REQUEST);
        }
        return normalized;
    }

    private BigDecimal paymentProgressPercent(BigDecimal receivedAmount, BigDecimal contractAmount) {
        if (receivedAmount == null || contractAmount == null || contractAmount.compareTo(BigDecimal.ZERO) <= 0) {
            return BigDecimal.ZERO.setScale(2, RoundingMode.HALF_UP);
        }
        return receivedAmount
            .multiply(BigDecimal.valueOf(100))
            .divide(contractAmount, 2, RoundingMode.HALF_UP);
    }

    private LocalDate localDate(java.sql.Date date) {
        return date == null ? null : date.toLocalDate();
    }

    private Map<String, Object> auditDetails(NormalizedProfile profile) {
        Map<String, Object> details = new LinkedHashMap<>();
        details.put("budgetAmount", profile.budgetAmount());
        details.put("contractAmount", profile.contractAmount());
        details.put("receivedAmount", profile.receivedAmount());
        details.put("paymentStatus", profile.paymentStatus());
        details.put("plannedDeliveryDate", profile.plannedDeliveryDate());
        details.put("currencyCode", profile.currencyCode());
        return details;
    }

    private record ProjectCore(Long projectId, String projectCode, String projectName) {
    }

    private record ProfileRow(
        BigDecimal budgetAmount,
        BigDecimal contractAmount,
        BigDecimal receivedAmount,
        String paymentStatus,
        LocalDate expectedPaymentDate,
        LocalDate plannedStartDate,
        LocalDate plannedDeliveryDate,
        LocalDate actualDeliveryDate,
        String currencyCode,
        String businessRemark
    ) {
    }

    private record NormalizedProfile(
        BigDecimal budgetAmount,
        BigDecimal contractAmount,
        BigDecimal receivedAmount,
        String paymentStatus,
        LocalDate expectedPaymentDate,
        LocalDate plannedStartDate,
        LocalDate plannedDeliveryDate,
        LocalDate actualDeliveryDate,
        String currencyCode,
        String businessRemark
    ) {
    }
}
