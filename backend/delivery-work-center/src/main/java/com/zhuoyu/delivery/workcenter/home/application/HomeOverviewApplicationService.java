package com.zhuoyu.delivery.workcenter.home.application;

import com.zhuoyu.delivery.core.project.application.ProjectAccessApplicationService;
import com.zhuoyu.delivery.workcenter.delivery.DeliveryBindingRepository;
import com.zhuoyu.delivery.workcenter.home.dto.HomeOverviewResponse;
import java.util.List;
import org.springframework.stereotype.Service;

@Service
public class HomeOverviewApplicationService {

    private final ProjectAccessApplicationService projectAccessApplicationService;
    private final DeliveryBindingRepository deliveryBindingRepository;

    public HomeOverviewApplicationService(
        ProjectAccessApplicationService projectAccessApplicationService,
        DeliveryBindingRepository deliveryBindingRepository
    ) {
        this.projectAccessApplicationService = projectAccessApplicationService;
        this.deliveryBindingRepository = deliveryBindingRepository;
    }

    public HomeOverviewResponse getOverview(Long userId, Long projectId) {
        var project = projectAccessApplicationService.requireAccessibleProject(userId, projectId);
        var summary = deliveryBindingRepository.dashboardSummary(projectId);
        int deliveryTotal = summary.documentBindingCount() + summary.drawingBindingCount();
        int fileTotal = summary.fileCount();
        int standardTotal = summary.sectionNodeCount() + summary.deliverableDefinitionCount();
        return new HomeOverviewResponse(
            project.id(),
            project.code(),
            project.name(),
            List.of(
                new HomeOverviewResponse.MetricItem("标准底座", standardTotal, "项"),
                new HomeOverviewResponse.MetricItem("文件资源", fileTotal, "份"),
                new HomeOverviewResponse.MetricItem("交付绑定", deliveryTotal, "条")
            ),
            List.of(
                "项目/部位/标准/文件/模型/对象已经进入同一业务上下文",
                "文档与图纸交付视图共享标准底座，按文件类型独立验收",
                "三维适配层保持可插拔，当前先输出项目与对象上下文"
            )
        );
    }
}
