package com.zhuoyu.delivery.workcenter.report;

import com.zhuoyu.delivery.core.audit.application.AuditLogApplicationService;
import com.zhuoyu.delivery.core.auth.application.SecurityPrincipalAccessor;
import com.zhuoyu.delivery.core.project.application.ProjectContextApplicationService;
import com.zhuoyu.delivery.workcenter.delivery.DeliveryBindingRepository;
import jakarta.servlet.http.HttpServletResponse;
import java.io.IOException;
import java.io.PrintWriter;
import java.time.LocalDateTime;
import java.time.format.DateTimeFormatter;
import java.util.List;
import java.util.Map;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

@RestController
@RequestMapping("/api/work-center/projects/{projectId}/reports")
public class ReportController {

    private static final String MODULE_CODE = "work-center";

    private final SecurityPrincipalAccessor securityPrincipalAccessor;
    private final ProjectContextApplicationService projectContextApplicationService;
    private final DeliveryBindingRepository deliveryBindingRepository;
    private final AuditLogApplicationService auditLogApplicationService;

    public ReportController(
        SecurityPrincipalAccessor securityPrincipalAccessor,
        ProjectContextApplicationService projectContextApplicationService,
        DeliveryBindingRepository deliveryBindingRepository,
        AuditLogApplicationService auditLogApplicationService
    ) {
        this.securityPrincipalAccessor = securityPrincipalAccessor;
        this.projectContextApplicationService = projectContextApplicationService;
        this.deliveryBindingRepository = deliveryBindingRepository;
        this.auditLogApplicationService = auditLogApplicationService;
    }

    @GetMapping("/delivery-completeness.csv")
    public void exportDeliveryCompleteness(@PathVariable Long projectId, HttpServletResponse response) throws IOException {
        var principal = securityPrincipalAccessor.requireCurrentPrincipal();
        projectContextApplicationService.requireCurrentProject(principal, projectId);
        auditLogApplicationService.record(projectId, MODULE_CODE, "work.report.export", "CSV",
            "delivery-completeness", principal.userId(), Map.of("projectId", projectId));

        response.setContentType("text/csv;charset=UTF-8");
        response.setHeader("Content-Disposition", "attachment; filename=\"delivery-completeness-" + projectId + ".csv\"");
        PrintWriter w = response.getWriter();
        w.println("﻿项目ID,视图类型,绑定ID,文件名,审核状态,挂接状态,交付物类型,挂接目标");
        List<Map<String, Object>> rows = deliveryBindingRepository.findCompletenessCsvData(projectId);
        for (var row : rows) {
            w.println(csvLine(
                row.get("project_id"), row.get("view_type"), row.get("binding_id"), row.get("file_name"),
                row.get("review_status"), row.get("binding_status"), row.get("deliverable_type_name"), row.get("target_name")
            ));
        }
        w.flush();
    }

    @GetMapping("/review-summary.csv")
    public void exportReviewSummary(@PathVariable Long projectId, HttpServletResponse response) throws IOException {
        var principal = securityPrincipalAccessor.requireCurrentPrincipal();
        projectContextApplicationService.requireCurrentProject(principal, projectId);
        auditLogApplicationService.record(projectId, MODULE_CODE, "work.report.export", "CSV",
            "review-summary", principal.userId(), Map.of("projectId", projectId));

        response.setContentType("text/csv;charset=UTF-8");
        response.setHeader("Content-Disposition", "attachment; filename=\"review-summary-" + projectId + ".csv\"");
        PrintWriter w = response.getWriter();
        w.println("﻿绑定ID,视图类型,审核状态,文件名,交付物类型,审核次数,最近审核时间");
        List<Map<String, Object>> rows = deliveryBindingRepository.findReviewSummaryCsvData(projectId);
        for (var row : rows) {
            Object lastReview = row.get("last_review_at");
            String lastReviewStr = lastReview != null ? lastReview.toString() : "";
            w.println(csvLine(
                row.get("binding_id"), row.get("view_type"), row.get("review_status"), row.get("file_name"),
                row.get("deliverable_type_name"), row.get("review_count"), lastReviewStr
            ));
        }
        w.flush();
    }

    @GetMapping("/rectifications.csv")
    public void exportRectifications(@PathVariable Long projectId, HttpServletResponse response) throws IOException {
        var principal = securityPrincipalAccessor.requireCurrentPrincipal();
        projectContextApplicationService.requireCurrentProject(principal, projectId);
        auditLogApplicationService.record(projectId, MODULE_CODE, "work.report.export", "CSV",
            "rectifications", principal.userId(), Map.of("projectId", projectId));

        response.setContentType("text/csv;charset=UTF-8");
        response.setHeader("Content-Disposition", "attachment; filename=\"rectifications-" + projectId + ".csv\"");
        PrintWriter w = response.getWriter();
        w.println("﻿整改ID,标题,原因,状态,严重程度,来源文件,处理说明,创建时间,解决时间,关闭时间");
        List<Map<String, Object>> rows = deliveryBindingRepository.findRectificationsCsvData(projectId);
        for (var row : rows) {
            w.println(csvLine(
                row.get("id"), row.get("title"), row.get("reason"), row.get("status"), row.get("severity"),
                row.get("binding_file_name"), row.get("resolution_note"),
                row.get("created_at"), row.get("resolved_at"), row.get("closed_at")
            ));
        }
        w.flush();
    }

    private static String csvLine(Object... values) {
        StringBuilder sb = new StringBuilder();
        for (int i = 0; i < values.length; i++) {
            if (i > 0) sb.append(',');
            String v = values[i] == null ? "" : values[i].toString();
            if (v.contains(",") || v.contains("\"") || v.contains("\n")) {
                sb.append('"').append(v.replace("\"", "\"\"")).append('"');
            } else {
                sb.append(v);
            }
        }
        return sb.toString();
    }
}
