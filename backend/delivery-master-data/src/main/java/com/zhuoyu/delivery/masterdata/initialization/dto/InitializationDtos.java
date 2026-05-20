package com.zhuoyu.delivery.masterdata.initialization.dto;

import com.zhuoyu.delivery.masterdata.status.dto.StandardStatusResponse;
import jakarta.validation.constraints.NotBlank;
import java.time.Instant;
import java.util.List;

public final class InitializationDtos {

    private InitializationDtos() {
    }

    public record InitializationStatusResponse(
        Long projectId,
        StandardStatusResponse standardStatus,
        String currentStep,
        Boolean ready,
        List<String> blockers,
        List<String> warnings,
        List<String> nextActions
    ) {
    }

    public record StandardTemplateSummaryResponse(
        String templateCode,
        String templateName,
        String industryType,
        String description,
        TemplateCounts counts
    ) {
    }

    public record StandardTemplateDetailResponse(
        String templateCode,
        String templateName,
        String industryType,
        String description,
        TemplateCounts counts,
        List<TemplateItemResponse> items
    ) {
    }

    public record TemplateItemResponse(
        String category,
        String code,
        String name,
        String parentCode,
        String targetCode,
        String fileKind,
        Boolean required
    ) {
    }

    public record TemplateCounts(
        Integer sectionNodes,
        Integer nodeTypes,
        Integer deliverableDefinitions,
        Integer deliverableTypes,
        Integer deliverableAttributes,
        Integer directoryTemplates
    ) {
        public static TemplateCounts zero() {
            return new TemplateCounts(0, 0, 0, 0, 0, 0);
        }
    }

    public record TemplatePreviewRequest(
        @NotBlank String templateCode
    ) {
    }

    public record TemplateApplyRequest(
        @NotBlank String templateCode,
        Boolean confirmApply
    ) {
    }

    public record TemplatePreviewResponse(
        String templateCode,
        String templateName,
        Boolean blocked,
        List<String> blockReasons,
        List<String> conflicts,
        TemplateCounts willCreate,
        TemplateCounts willSkip,
        List<TemplatePreviewItemResponse> items
    ) {
    }

    public record TemplatePreviewItemResponse(
        String category,
        String code,
        String name,
        String action,
        String reason
    ) {
    }

    public record TemplateApplyResponse(
        String templateCode,
        String templateName,
        TemplateCounts created,
        TemplateCounts skipped,
        Integer conflictCount,
        StandardStatusResponse standardStatus,
        List<String> nextActions
    ) {
    }

    public record OnboardingAssessmentResponse(
        Long projectId,
        Boolean assetCatalogOnly,
        String evidenceMode,
        String onboardingStatus,
        OnboardingAssetSummary assetSummary,
        StandardStatusResponse standardStatus,
        List<OnboardingEvidenceClue> evidenceClues,
        List<OnboardingGap> gaps,
        List<String> nextActions
    ) {
    }

    public record OnboardingAssetSummary(
        Integer fileCount,
        Integer modelFileCount,
        Integer drawingFileCount,
        Integer documentFileCount,
        Integer pathMappingCount,
        List<String> dominantFileKinds,
        Instant lastAssetSeenAt,
        Instant lastScanAt
    ) {
    }

    public record OnboardingEvidenceClue(
        String clueType,
        String label,
        String evidenceMode,
        Boolean assetCatalogOnly,
        String description
    ) {
    }

    public record OnboardingGap(
        String code,
        String severity,
        String description,
        String missingEvidenceReason
    ) {
    }

    public record OnboardingDraftPreviewResponse(
        Long projectId,
        Boolean dryRun,
        Boolean confirmedRequired,
        Boolean nasTouched,
        Boolean contentRead,
        String evidenceMode,
        String templateCode,
        String templateName,
        OnboardingAssetSummary assetSummary,
        TemplatePreviewResponse templatePreview,
        List<OnboardingDraftItem> draftItems,
        List<String> warnings
    ) {
    }

    public record OnboardingDraftItem(
        String category,
        String name,
        String reason,
        Boolean fromRealAssetClue,
        Boolean fromTemplateSkeleton,
        Boolean pendingConfirmation
    ) {
    }

    public record OnboardingApplyRequest(
        String templateCode,
        Boolean confirmed
    ) {
    }

    public record OnboardingApplyResponse(
        Long projectId,
        Boolean confirmed,
        Boolean nasTouched,
        Boolean contentRead,
        Boolean draftApplied,
        String evidenceMode,
        TemplateApplyResponse templateResult,
        List<String> nextActions
    ) {
    }
}
