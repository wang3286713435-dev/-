import { http } from '@/app/http';
import type { ApiResponse } from '@/modules/core/api/types';

// ---- Review ----

export interface ReviewRecordItem {
  id: number;
  bindingId: number;
  action: string;
  comment: string | null;
  reviewerUserId: number;
  createdAt: string;
}

// ---- Rectification ----

export interface RectificationItem {
  id: number;
  projectId: number;
  sourceType: string;
  sourceId: number;
  bindingId: number;
  title: string;
  description: string | null;
  reason: string;
  status: string;
  severity: string;
  assigneeUserId: number | null;
  resolutionNote: string | null;
  dueDate: string | null;
  resolvedAt: string | null;
  closedAt: string | null;
  createdBy: number | null;
  updatedBy: number | null;
  createdAt: string;
  updatedAt: string;
  bindingViewType: string;
  bindingFileName: string | null;
  bindingDeliverableTypeName: string | null;
  bindingSectionNodeName: string | null;
}

export interface RectificationPayload {
  title?: string;
  description?: string;
  reason?: string;
  severity?: string;
  assigneeUserId?: number | null;
  dueDate?: string | null;
}

export interface DeliveryBindingPayload {
  viewType: string;
  sectionNodeId?: number | null;
  managedObjectId?: number | null;
  deliverableTypeId: number;
  fileResourceId: number;
  bindingStatus?: string;
  reviewStatus?: string;
  sortOrder?: number;
  remark?: string;
}

// ---- batch delivery binding ----

export interface BatchDeliveryBindingPayload {
  viewType: string;
  sectionNodeId?: number | null;
  managedObjectId?: number | null;
  deliverableTypeId: number;
  fileResourceIds: number[];
  bindingStatus?: string;
  reviewStatus?: string;
  remark?: string;
}

export interface BatchBindingRowResult {
  fileResourceId: number;
  status: 'CREATED' | 'SKIPPED' | 'FAILED';
  bindingId: number | null;
  message: string;
}

export interface BatchDeliveryBindingResponse {
  projectId: number;
  viewType: string;
  requestedCount: number;
  createdCount: number;
  skippedCount: number;
  failedCount: number;
  createdBindings: DeliveryBinding[];
  results: BatchBindingRowResult[];
}

export interface DeliveryBinding {
  id: number;
  projectId: number;
  viewType: string;
  sectionNodeId: number | null;
  sectionNodeName: string | null;
  managedObjectId: number | null;
  managedObjectName: string | null;
  deliverableTypeId: number;
  deliverableTypeName: string;
  deliverableDefinitionName: string;
  fileResourceId: number;
  fileName: string;
  fileKind: string;
  versionNo: string;
  processStatus: string;
  bindingStatus: string;
  reviewStatus: string;
  sortOrder: number;
  remark: string | null;
}

export interface DeliveryView {
  projectId: number;
  viewType: string;
  totalCount: number;
  boundCount: number;
  rows: DeliveryBinding[];
}

export interface DashboardSummary {
  projectId: number;
  sectionNodeCount: number;
  deliverableDefinitionCount: number;
  fileCount: number;
  documentFileCount: number;
  drawingFileCount: number;
  modelFileCount: number;
  modelIntegrationCount: number;
  publishedModelCount: number;
  managedObjectCount: number;
  documentBindingCount: number;
  drawingBindingCount: number;
}

export interface DeliveryCompletenessRow {
  targetType: string;
  targetId: number;
  targetCode: string;
  targetName: string;
  deliverableDefinitionId: number;
  deliverableDefinitionCode: string;
  deliverableDefinitionName: string;
  deliverableTypeId: number;
  deliverableTypeCode: string;
  deliverableTypeName: string;
  fileKind: string;
  required: boolean;
  completed: boolean;
  bindingId: number | null;
  fileResourceId: number | null;
  fileName: string | null;
  versionNo: string | null;
  reviewStatus: string | null;
  missingReason: string | null;
}

export interface DeliveryCompleteness {
  projectId: number;
  viewType: string;
  targetType: string;
  standardReady: boolean;
  readinessIssues: string[];
  totalRequired: number;
  completedCount: number;
  missingCount: number;
  completionRate: number;
  rows: DeliveryCompletenessRow[];
}

export interface DeliveryPackageViewSummary {
  totalRequired: number;
  boundCount: number;
  missingCount: number;
  pendingReviewCount: number;
  approvedCount: number;
  rejectedCount: number;
  completionRate: number;
  reviewReadyCount: number;
}

export interface DeliveryPackageSummaryRow {
  deliverableDefinitionId: number | null;
  deliverableDefinitionName: string | null;
  deliverableTypeId: number | null;
  deliverableTypeName: string | null;
  targetType: string;
  targetId: number | null;
  targetName: string | null;
  bindingId: number | null;
  fileResourceId: number | null;
  fileName: string | null;
  fileKind: string;
  versionNo: string | null;
  reviewStatus: string | null;
  readinessStatus: string;
}

export interface DeliveryPackageSummaryResponse {
  projectId: number;
  documentSummary: DeliveryPackageViewSummary;
  drawingSummary: DeliveryPackageViewSummary;
  totalRowCount: number;
  rows: DeliveryPackageSummaryRow[];
}

export async function createDeliveryBinding(projectId: number, payload: DeliveryBindingPayload) {
  const { data } = await http.post<ApiResponse<DeliveryBinding>>(
    `/api/work-center/projects/${projectId}/delivery-bindings`,
    payload
  );
  return data.data;
}

export async function createBatchDeliveryBinding(projectId: number, payload: BatchDeliveryBindingPayload) {
  const { data } = await http.post<ApiResponse<BatchDeliveryBindingResponse>>(
    `/api/work-center/projects/${projectId}/delivery-bindings:batch`,
    payload
  );
  return data.data;
}

export async function fetchDeliveryPackageSummary(
  projectId: number,
  viewType?: string,
  targetType = 'SECTION'
) {
  const { data } = await http.get<ApiResponse<DeliveryPackageSummaryResponse>>(
    `/api/work-center/projects/${projectId}/delivery-package/summary`,
    { params: { viewType, targetType } }
  );
  return data.data;
}

export async function fetchDeliveryView(projectId: number, viewType: string) {
  const { data } = await http.get<ApiResponse<DeliveryView>>(
    `/api/work-center/projects/${projectId}/delivery-views`,
    { params: { viewType } }
  );
  return data.data;
}

export async function fetchDashboardSummary(projectId: number) {
  const { data } = await http.get<ApiResponse<DashboardSummary>>(
    `/api/work-center/projects/${projectId}/dashboard/summary`
  );
  return data.data;
}

export async function fetchDeliveryCompleteness(
  projectId: number,
  viewType: string,
  targetType = 'SECTION',
  onlyMissing = false
) {
  const { data } = await http.get<ApiResponse<DeliveryCompleteness>>(
    `/api/work-center/projects/${projectId}/delivery-completeness`,
    { params: { viewType, targetType, onlyMissing } }
  );
  return data.data;
}

// ---- Review actions ----

export async function submitForReview(projectId: number, bindingId: number) {
  const { data } = await http.post<ApiResponse<DeliveryBinding>>(
    `/api/work-center/projects/${projectId}/delivery-bindings/${bindingId}:submit-review`
  );
  return data.data;
}

export async function approveBinding(projectId: number, bindingId: number) {
  const { data } = await http.post<ApiResponse<DeliveryBinding>>(
    `/api/work-center/projects/${projectId}/delivery-bindings/${bindingId}:approve`
  );
  return data.data;
}

export async function rejectBinding(projectId: number, bindingId: number, reason: string) {
  const { data } = await http.post<ApiResponse<DeliveryBinding>>(
    `/api/work-center/projects/${projectId}/delivery-bindings/${bindingId}:reject`,
    { reason }
  );
  return data.data;
}

export async function fetchReviewRecords(projectId: number, bindingId: number) {
  const { data } = await http.get<ApiResponse<ReviewRecordItem[]>>(
    `/api/work-center/projects/${projectId}/delivery-bindings/${bindingId}/review-records`
  );
  return data.data;
}

// ---- Rectification actions ----

export async function fetchRectifications(projectId: number, status?: string) {
  const { data } = await http.get<ApiResponse<RectificationItem[]>>(
    `/api/work-center/projects/${projectId}/rectifications`,
    { params: status ? { status } : {} }
  );
  return data.data;
}

export async function fetchRectification(projectId: number, rectificationId: number) {
  const { data } = await http.get<ApiResponse<RectificationItem>>(
    `/api/work-center/projects/${projectId}/rectifications/${rectificationId}`
  );
  return data.data;
}

export async function updateRectification(projectId: number, rectificationId: number, payload: RectificationPayload) {
  const { data } = await http.patch<ApiResponse<RectificationItem>>(
    `/api/work-center/projects/${projectId}/rectifications/${rectificationId}`,
    payload
  );
  return data.data;
}

export async function resolveRectification(projectId: number, rectificationId: number, resolutionNote: string) {
  const { data } = await http.post<ApiResponse<RectificationItem>>(
    `/api/work-center/projects/${projectId}/rectifications/${rectificationId}:resolve`,
    { resolutionNote }
  );
  return data.data;
}

export async function closeRectification(projectId: number, rectificationId: number) {
  const { data } = await http.post<ApiResponse<RectificationItem>>(
    `/api/work-center/projects/${projectId}/rectifications/${rectificationId}:close`
  );
  return data.data;
}

export async function reopenRectification(projectId: number, rectificationId: number) {
  const { data } = await http.post<ApiResponse<RectificationItem>>(
    `/api/work-center/projects/${projectId}/rectifications/${rectificationId}:reopen`
  );
  return data.data;
}

// ---- CSV exports ----

function downloadCsv(url: string, filename: string) {
  http.get(url, { responseType: 'blob' }).then(({ data }) => {
    const blob = new Blob([data], { type: 'text/csv;charset=UTF-8' });
    const link = document.createElement('a');
    link.href = URL.createObjectURL(blob);
    link.download = filename;
    link.click();
    URL.revokeObjectURL(link.href);
  });
}

export function exportDeliveryCompletenessCsv(projectId: number) {
  downloadCsv(
    `/api/work-center/projects/${projectId}/reports/delivery-completeness.csv`,
    `delivery-completeness-${projectId}.csv`
  );
}

export function exportReviewSummaryCsv(projectId: number) {
  downloadCsv(
    `/api/work-center/projects/${projectId}/reports/review-summary.csv`,
    `review-summary-${projectId}.csv`
  );
}

export function exportRectificationsCsv(projectId: number) {
  downloadCsv(
    `/api/work-center/projects/${projectId}/reports/rectifications.csv`,
    `rectifications-${projectId}.csv`
  );
}

// ---- export precheck ----

export interface ExportPrecheckRow {
  deliverableDefinitionId: number | null;
  deliverableDefinitionName: string | null;
  deliverableTypeId: number | null;
  deliverableTypeName: string | null;
  targetType: string;
  targetId: number | null;
  targetName: string | null;
  bindingId: number | null;
  fileResourceId: number | null;
  fileName: string | null;
  fileKind: string;
  versionNo: string | null;
  fileExt: string | null;
  reviewStatus: string | null;
  readinessStatus: string | null;
  previewStatus: string;
  previewMode: string;
  conversionStatus: string;
  conversionRequired: boolean;
  downloadOnly: boolean;
  statusLabel: string | null;
  actionHint: string | null;
  riskLevel: string | null;
  exportStatus: string;
  blockReason: string | null;
}

export interface ExportPrecheckResponse {
  projectId: number;
  viewType: string;
  targetType: string;
  dryRun: boolean;
  packageGenerated: boolean;
  totalCount: number;
  readyCount: number;
  blockedCount: number;
  missingCount: number;
  pendingReviewCount: number;
  rejectedCount: number;
  conversionRequiredCount: number;
  unsupportedPreviewCount: number;
  rows: ExportPrecheckRow[];
}

export async function fetchExportPrecheck(
  projectId: number,
  viewType?: string,
  targetType = 'SECTION'
) {
  const { data } = await http.get<ApiResponse<ExportPrecheckResponse>>(
    `/api/work-center/projects/${projectId}/delivery-package/export-precheck`,
    { params: { viewType, targetType } }
  );
  return data.data;
}

// ---- Agent delivery governance ----

export interface AgentGovernanceStandardStatus {
  hasSectionTree: boolean;
  hasNodeTypes: boolean;
  nodeTypesLocked: boolean;
  deliverableStandardReady: boolean;
  sectionNodeCount: number;
  nodeTypeCount: number;
  deliverableDefinitionCount: number;
  deliverableTypeCount: number;
  directoryTemplateCount: number;
}

export interface AgentGovernanceDeliveryStatus {
  viewType: string;
  totalRequired: number;
  completedCount: number;
  missingCount: number;
  completionRate: number;
  pendingReviewCount: number;
  rejectedCount: number;
}

export interface AgentGovernanceExportPrecheckSummary {
  totalCount: number;
  readyCount: number;
  blockedCount: number;
  missingCount: number;
  pendingReviewCount: number;
  rejectedCount: number;
  conversionRequiredCount: number;
  unsupportedPreviewCount: number;
}

export interface AgentGovernanceOverview {
  projectId: number;
  standardStatus: AgentGovernanceStandardStatus;
  documentDelivery: AgentGovernanceDeliveryStatus;
  drawingDelivery: AgentGovernanceDeliveryStatus;
  pendingReviewCount: number;
  rejectedCount: number;
  rectificationPendingCount: number;
  packageStatus: string;
  exportPrecheckSummary: AgentGovernanceExportPrecheckSummary;
  summaryText: string;
  nextActions: string[];
}

export interface AgentGovernanceMissingItem {
  missingItemKey: string;
  viewType: string;
  targetType: string;
  targetId: number;
  targetName: string;
  deliverableDefinitionId: number;
  deliverableDefinitionName: string;
  deliverableTypeId: number;
  deliverableTypeName: string;
  fileKind: string;
  missingReason: string;
  expectedFileKind: string;
  explanation: string;
}

export interface AgentGovernanceMissingItemsResponse {
  projectId: number;
  targetType: string;
  totalCount: number;
  rows: AgentGovernanceMissingItem[];
}

export interface AgentBindingRecommendationRequest {
  viewType: string;
  targetType?: string;
  limitPerMissingItem?: number;
}

export interface AgentBindingRecommendation {
  recommendationId: string;
  missingItemKey: string;
  viewType: string;
  targetType: string;
  targetId: number;
  targetName: string;
  deliverableTypeId: number;
  deliverableTypeName: string;
  fileResourceId: number;
  fileName: string;
  fileKind: string;
  fileExt: string;
  versionNo: string;
  processStatus: string;
  previewStatus: string;
  statusLabel: string;
  recommendationReason: string;
  confidence: 'HIGH' | 'MEDIUM' | 'LOW';
  riskWarnings: string[];
  metadataGovernanceRequired: boolean;
}

export interface AgentBindingRecommendationResponse {
  projectId: number;
  viewType: string;
  targetType: string;
  totalCount: number;
  rows: AgentBindingRecommendation[];
}

export interface ApplyAgentRecommendationItem {
  recommendationId: string;
  missingItemKey: string;
  targetType: string;
  targetId: number;
  deliverableTypeId: number;
  fileResourceId: number;
}

export interface ApplyAgentRecommendationsRequest {
  confirmed: boolean;
  viewType: string;
  targetType?: string;
  items: ApplyAgentRecommendationItem[];
}

export interface ApplyAgentRecommendationRowResult {
  recommendationId: string | null;
  missingItemKey: string | null;
  fileResourceId: number;
  bindingId: number | null;
  status: 'CREATED' | 'SKIPPED' | 'FAILED';
  message: string;
}

export interface ApplyAgentRecommendationsResponse {
  projectId: number;
  viewType: string;
  requestedCount: number;
  createdCount: number;
  skippedCount: number;
  failedCount: number;
  results: ApplyAgentRecommendationRowResult[];
}

export async function fetchAgentGovernanceOverview(projectId: number) {
  const { data } = await http.get<ApiResponse<AgentGovernanceOverview>>(
    `/api/work-center/projects/${projectId}/agent-governance/overview`
  );
  return data.data;
}

export async function fetchAgentGovernanceMissingItems(
  projectId: number,
  viewType?: string,
  targetType = 'SECTION'
) {
  const { data } = await http.get<ApiResponse<AgentGovernanceMissingItemsResponse>>(
    `/api/work-center/projects/${projectId}/agent-governance/missing-items`,
    { params: { viewType, targetType } }
  );
  return data.data;
}

export async function recommendAgentGovernanceBindings(
  projectId: number,
  payload: AgentBindingRecommendationRequest
) {
  const { data } = await http.post<ApiResponse<AgentBindingRecommendationResponse>>(
    `/api/work-center/projects/${projectId}/agent-governance/recommend-bindings`,
    payload
  );
  return data.data;
}

export async function applyAgentGovernanceRecommendations(
  projectId: number,
  payload: ApplyAgentRecommendationsRequest
) {
  const { data } = await http.post<ApiResponse<ApplyAgentRecommendationsResponse>>(
    `/api/work-center/projects/${projectId}/agent-governance/recommendations:apply`,
    payload
  );
  return data.data;
}
