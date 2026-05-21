import { http } from '@/app/http';
import type { ApiResponse } from '@/modules/core/api/types';

export interface FileResource {
  id: number;
  projectId: number;
  originalName: string;
  fileKind: string;
  mimeType: string | null;
  sizeBytes: number;
  storageUri: string;
  checksum: string | null;
  businessTag: string | null;
  versionNo: string;
  processStatus: string;
  processedAt: string | null;
}

export interface FileResourcePayload {
  originalName: string;
  fileKind: string;
  mimeType?: string;
  sizeBytes?: number;
  storageUri: string;
  checksum?: string;
  businessTag?: string;
  versionNo?: string;
  processStatus?: string;
}

export interface FileResourcePageQuery {
  fileKind?: string;
  keyword?: string;
  processStatus?: string;
  pageNo?: number;
  pageSize?: number;
}

export interface FileResourcePageResult {
  page: number;
  pageSize: number;
  total: number;
  rows: FileResource[];
}

export interface ModelIntegration {
  id: number;
  projectId: number;
  name: string;
  modelFileId: number;
  versionNo: string;
  status: string;
  componentCount: number;
  publishedAt: string | null;
  publishedBy: number | null;
  adapterPayloadJson: string | null;
}

export interface ModelIntegrationPayload {
  name: string;
  modelFileId: number;
  versionNo?: string;
  componentCount?: number;
  adapterPayloadJson?: string;
}

export interface BimLightweightStatus {
  projectId: number;
  integrationId: number;
  modelFileId: number;
  modelName: string;
  modelFormat: string;
  integrationStatus: string;
  engineMode: string;
  engineConnected: boolean;
  lightweightStatus: string;
  viewerAvailable: boolean;
  taskStatus: string;
  conversionRequired: boolean;
  componentIndexStatus: string;
  previewMode: string;
  statusLabel: string;
  actionHint: string;
  blockedReason: string;
  supportedOperations: string[];
  forbiddenOperations: string[];
}

export interface BimLightweightPlan {
  projectId: number;
  integrationId: number;
  modelFileId: number;
  modelName: string;
  modelFormat: string;
  engineMode: string;
  dryRun: boolean;
  taskCreated: boolean;
  engineBindingRequired: boolean;
  realConversionExecuted: boolean;
  nasFileTouched: boolean;
  viewerAvailable: boolean;
  requiredConditions: string[];
  futureSteps: string[];
  riskWarnings: string[];
  supportedOperations: string[];
  forbiddenOperations: string[];
}

export interface ManagedObject {
  id: number;
  projectId: number;
  modelIntegrationId: number;
  sectionNodeId: number | null;
  code: string;
  name: string;
  objectType: string;
  externalId: string | null;
  discipline: string | null;
  status: string;
  propertiesJson: string | null;
}

export interface ManagedObjectPayload {
  modelIntegrationId: number;
  sectionNodeId?: number | null;
  code: string;
  name: string;
  objectType?: string;
  externalId?: string;
  discipline?: string;
  status?: string;
  propertiesJson?: string;
}

export interface AssetProject {
  projectId: number;
  code: string;
  name: string;
  industryType: string | null;
  projectStage: string | null;
  projectManagerName: string | null;
  assetStatus: string;
  assetSource: string | null;
  modelCount: number | null;
  totalSizeBytes: number | null;
  lastModelUpdatedAt: string | null;
  projectSource?: string | null;
  projectCategory?: string | null;
  onboardingStatus?: string | null;
  fileCount?: number | null;
  dominantFileKinds?: string[];
  lastScanAt?: string | null;
  hasMasterData?: boolean;
  hasDeliveryStandard?: boolean;
  governanceReady?: boolean;
}

export interface AssetCapacityByFileKind {
  fileKind: string;
  fileCount: number;
  totalSizeBytes: number;
}

export interface AssetCapacityByDiscipline {
  discipline: string;
  fileCount: number;
  totalSizeBytes: number;
}

export interface AssetCapacityByProject {
  projectId: number;
  projectCode: string;
  projectName: string;
  fileCount: number;
  totalSizeBytes: number;
}

export interface AssetStatistics {
  projectCount: number;
  fileCount: number;
  modelFileCount: number;
  drawingFileCount: number;
  totalSizeBytes: number;
  byFileKind: AssetCapacityByFileKind[];
  byDiscipline: AssetCapacityByDiscipline[];
  topProjects: AssetCapacityByProject[];
  lastUpdatedAt: string | null;
}

export interface AssetQualityMetric {
  code: string;
  label: string;
  severity: string;
  count: number;
  description: string;
}

export interface AssetQualityProjectRisk {
  projectId: number;
  projectCode: string;
  projectName: string;
  missingChecksumCount: number;
  missingConfidenceCount: number;
  missingDisciplineCount: number;
  missingVersionCount: number;
  missingStoragePathCount: number;
  zeroSizeFileCount: number;
  pendingReviewCount: number;
  failedScanCount: number;
  totalRiskCount: number;
}

export interface AssetQualityEvent {
  id: number;
  eventType: string;
  projectId: number | null;
  aggregateType: string | null;
  aggregateId: string | null;
  actionCode: string;
  operatorId: number | null;
  sourceType: string | null;
  summary: string | null;
  payloadJson: string | null;
  traceId: string | null;
  createdAt: string;
}

export interface AssetQualityOverview {
  riskSignalCount: number;
  pendingReviewCount: number;
  failedScanCount: number;
  runningScanCount: number;
  missingChecksumCount: number;
  missingConfidenceCount: number;
  missingDisciplineCount: number;
  missingVersionCount: number;
  missingStoragePathCount: number;
  zeroSizeFileCount: number;
  nonstandardPendingCount: number;
  nonstandardApprovedCount: number;
  latestAssetUpdatedAt: string | null;
  latestEventAt: string | null;
  metrics: AssetQualityMetric[];
  topRiskProjects: AssetQualityProjectRisk[];
  recentEvents: AssetQualityEvent[];
}

export interface FileAsset {
  fileId: number;
  projectId: number;
  projectCode: string;
  projectName: string;
  fileName: string;
  fileExt: string;
  fileKind: string;
  discipline: string | null;
  versionNo: string;
  sizeBytes: number;
  checksum: string | null;
  storageProvider: string;
  storagePath: string;
  logicalPath: string | null;
  sourceType: string | null;
  processStatus: string;
  reviewStatus: string;
  confidenceLevel: string | null;
  createdAt: string;
  updatedAt: string;
  permissionTags?: string[];
  projectScope?: string | null;
  confidentialityLevel?: string | null;
  lastSeenAt?: string | null;
  lifecycleStatus?: string | null;
  indexEligibility?: string | null;
}

export interface FileAssetQuery {
  projectId?: number;
  fileKind?: string;
  discipline?: string;
  fileName?: string;
  fileExt?: string;
  sourceType?: string;
  keyword?: string;
  assetSource?: string;
  qualityIssue?: string;
  pageNo?: number;
  pageSize?: number;
}

export interface FileAssetPageResult {
  page: number;
  pageSize: number;
  total: number;
  rows: FileAsset[];
}

export interface FileAssetMetadataPayload {
  fileKind?: string;
  discipline?: string;
  versionNo?: string;
  confidenceLevel?: string;
  reviewStatus?: string;
}

export interface FilePreview {
  fileId: number;
  projectId: number;
  projectCode: string;
  projectName: string;
  fileName: string;
  fileExt: string;
  fileKind: string;
  previewStatus: string;
  previewMode: string;
  previewAvailable: boolean;
  conversionStatus: string;
  conversionRequired: boolean;
  message: string;
  supportedActions: string[];
  downloadOnly: boolean;
  statusLabel: string;
  actionHint: string;
  riskLevel: string;
  previewAllowed: boolean;
  downloadAllowed: boolean;
  accessPolicyMessage: string;
  viewerRoute: string;
  updatedAt: string | null;
}

export interface FileAccessTicket {
  ticketId: number;
  ticket: string;
  accessUrl: string;
  expiresAt: string;
  action: 'PREVIEW' | 'DOWNLOAD';
  fileId: number;
  fileName: string;
  previewable: boolean;
  downloadable: boolean;
  message: string;
}

export interface AssetDiscipline {
  id: number;
  code: string;
  name: string;
  projectId: number | null;
  scope: string;
  sortOrder: number;
}

export interface AssetJob {
  id: number;
  jobType: string;
  status: string;
  projectId: number | null;
  targetType: string | null;
  targetId: number | null;
  requestPayload: string | null;
  progressCurrent: number | null;
  progressTotal: number | null;
  progressPercent: number | null;
  progressMessage: string | null;
  failureReason: string | null;
  retryCount: number;
  maxRetries: number;
  createdBy: number | null;
  startedAt: string | null;
  completedAt: string | null;
  createdAt: string;
  updatedAt: string;
}

export interface AssetJobQuery {
  jobType?: string;
  status?: string;
  projectId?: number;
  limit?: number;
}

export interface AssetScanTask {
  id: number;
  rootCode: string;
  rootPath: string;
  projectId: number | null;
  projectCode: string | null;
  recursive: boolean;
  extensions: string | null;
  skipLowValueDirectories: boolean;
  skipDirectoryKeywords: string | null;
  status: string;
  progressMessage: string | null;
  progressCurrent: number | null;
  progressTotal: number | null;
  progressPercent: number | null;
  cancelRequested: boolean;
  totalScanned: number;
  autoIngested: number;
  pendingReview: number;
  failedCount: number;
  skippedLowValue: number;
  skippedDirectories: number;
  lastScannedPath: string | null;
  failureReason: string | null;
  scanReportJson: string | null;
  startedAt: string | null;
  completedAt: string | null;
  createdAt: string;
  updatedAt: string;
  createdBy: number | null;
}

export interface AssetScanReport {
  scanTaskId: number;
  rootCode: string;
  rootPath: string;
  projectId: number | null;
  projectCode: string | null;
  status: string;
  progressCurrent: number | null;
  progressTotal: number | null;
  progressPercent: number | null;
  progressMessage: string | null;
  totalScanned: number;
  autoIngested: number;
  pendingReview: number;
  failedCount: number;
  skippedLowValue: number;
  skippedDirectories: number;
  lastScannedPath: string | null;
  failureReason: string | null;
  scanReportJson: string | null;
  startedAt: string | null;
  completedAt: string | null;
}

export interface AssetScanPayload {
  rootCode: string;
  rootPath: string;
  projectId?: number | null;
  projectCode?: string;
  recursive?: boolean;
  extensions?: string[];
  skipLowValueDirectories?: boolean;
  skipDirectoryKeywords?: string[];
}

export interface AssetPathMapping {
  id: number;
  projectId: number;
  projectCode: string;
  projectName: string;
  providerCode: string;
  nasPath: string;
  matchStrategy: string;
  enabled: boolean;
  sortOrder: number | null;
  remark: string | null;
  createdAt: string;
}

export interface NonstandardDirectory {
  id: number;
  providerCode: string;
  rootPath: string;
  directoryName: string;
  nasPath: string;
  directoryType: string;
  riskType: string;
  governanceStatus: string;
  suggestedProjectCode: string | null;
  suggestedProjectName: string | null;
  duplicateBaseCode: string | null;
  standardFoldersJson: string | null;
  reviewReason: string | null;
  agentSuggestion: string | null;
  manualDecision: string | null;
  decisionReason: string | null;
  ownerName: string | null;
  decidedBy: number | null;
  decidedAt: string | null;
  createdAt: string;
  updatedAt: string;
  createdBy: number | null;
}

export interface NonstandardDirectoryQuery {
  governanceStatus?: string;
  riskType?: string;
  keyword?: string;
  limit?: number;
}

export interface NonstandardDirectoryDiscoverPayload {
  rootPath: string;
  providerCode?: string;
  deferredProjectCodes?: string[];
  ownerName?: string;
}

export interface NonstandardDirectoryDiscoverResult {
  rootPath: string;
  discoveredCount: number;
  createdOrUpdatedCount: number;
  rows: NonstandardDirectory[];
}

export interface NonstandardDirectoryUpdatePayload {
  governanceStatus?: string;
  riskType?: string;
  suggestedProjectCode?: string;
  suggestedProjectName?: string;
  reviewReason?: string;
  agentSuggestion?: string;
  manualDecision?: string;
  decisionReason?: string;
  ownerName?: string;
}

export async function createFileResource(projectId: number, payload: FileResourcePayload) {
  const { data } = await http.post<ApiResponse<FileResource>>(
    `/api/data-steward/projects/${projectId}/file-resources`,
    payload
  );
  return data.data;
}

export async function fetchFileResources(projectId: number, fileKind?: string) {
  const { data } = await http.get<ApiResponse<FileResource[]>>(
    `/api/data-steward/projects/${projectId}/file-resources`,
    { params: fileKind ? { fileKind } : undefined }
  );
  return data.data;
}

export async function fetchFileResourcesPage(
  projectId: number,
  query: FileResourcePageQuery = {},
  signal?: AbortSignal
): Promise<FileResourcePageResult> {
  const params = Object.fromEntries(
    Object.entries(query).filter(([, value]) => value !== undefined && value !== null && value !== '')
  );
  const { data } = await http.get<ApiResponse<{ pageNo: number; pageSize: number; total: number; items: FileResource[] }>>(
    `/api/data-steward/projects/${projectId}/file-resources`,
    { params, signal }
  );
  return {
    page: data.data?.pageNo ?? query.pageNo ?? 1,
    pageSize: data.data?.pageSize ?? query.pageSize ?? 50,
    total: data.data?.total ?? 0,
    rows: data.data?.items ?? []
  };
}

export async function processFileResource(projectId: number, fileId: number, processStatus = 'PROCESSED') {
  const { data } = await http.patch<ApiResponse<FileResource>>(
    `/api/data-steward/projects/${projectId}/file-resources/${fileId}:process`,
    { processStatus }
  );
  return data.data;
}

export async function createModelIntegration(projectId: number, payload: ModelIntegrationPayload) {
  const { data } = await http.post<ApiResponse<ModelIntegration>>(
    `/api/data-steward/projects/${projectId}/model-integrations`,
    payload
  );
  return data.data;
}

export async function fetchModelIntegrations(projectId: number) {
  const { data } = await http.get<ApiResponse<ModelIntegration[]>>(
    `/api/data-steward/projects/${projectId}/model-integrations`
  );
  return data.data;
}

export async function publishModelIntegration(projectId: number, integrationId: number) {
  const { data } = await http.patch<ApiResponse<ModelIntegration>>(
    `/api/data-steward/projects/${projectId}/model-integrations/${integrationId}:publish`
  );
  return data.data;
}

export async function fetchBimLightweightStatus(projectId: number, integrationId: number) {
  const { data } = await http.get<ApiResponse<BimLightweightStatus>>(
    `/api/visualization-adapter/projects/${projectId}/model-integrations/${integrationId}/lightweight-status`
  );
  return data.data;
}

export async function fetchBimLightweightPlan(projectId: number, integrationId: number) {
  const { data } = await http.get<ApiResponse<BimLightweightPlan>>(
    `/api/visualization-adapter/projects/${projectId}/model-integrations/${integrationId}/lightweight-plan`
  );
  return data.data;
}

export async function createManagedObject(projectId: number, payload: ManagedObjectPayload) {
  const { data } = await http.post<ApiResponse<ManagedObject>>(
    `/api/data-steward/projects/${projectId}/managed-objects`,
    payload
  );
  return data.data;
}

export async function fetchManagedObjects(projectId: number) {
  const { data } = await http.get<ApiResponse<ManagedObject[]>>(
    `/api/data-steward/projects/${projectId}/managed-objects`
  );
  return data.data;
}

export async function updateManagedObject(projectId: number, objectId: number, payload: ManagedObjectPayload) {
  const { data } = await http.patch<ApiResponse<ManagedObject>>(
    `/api/data-steward/projects/${projectId}/managed-objects/${objectId}`,
    payload
  );
  return data.data;
}

export async function deleteManagedObject(projectId: number, objectId: number) {
  await http.delete<ApiResponse<null>>(`/api/data-steward/projects/${projectId}/managed-objects/${objectId}`);
}

export async function fetchAssetProjects(keyword?: string, assetSource?: string) {
  const { data } = await http.get<ApiResponse<AssetProject[]>>('/api/data-steward/assets/projects', {
    params: {
      ...(keyword ? { keyword } : {}),
      ...(assetSource ? { assetSource } : {})
    }
  });
  return data.data;
}

export async function fetchAssetStatistics(projectId?: number, assetSource?: string) {
  const { data } = await http.get<ApiResponse<AssetStatistics>>('/api/data-steward/assets/statistics', {
    params: {
      ...(projectId ? { projectId } : {}),
      ...(assetSource ? { assetSource } : {})
    }
  });
  return data.data;
}

export async function fetchAssetQualityOverview(projectId?: number, assetSource?: string) {
  const { data } = await http.get<ApiResponse<AssetQualityOverview>>(
    '/api/data-steward/assets/quality/overview',
    {
      params: {
        ...(projectId ? { projectId } : {}),
        ...(assetSource ? { assetSource } : {})
      }
    }
  );
  return data.data;
}

export async function fetchAssetDisciplines(projectId?: number) {
  const { data } = await http.get<ApiResponse<AssetDiscipline[]>>('/api/data-steward/assets/disciplines', {
    params: projectId ? { projectId } : undefined
  });
  return data.data;
}

export async function fetchFileAssets(params: FileAssetQuery = {}) {
  const normalized = Object.fromEntries(
    Object.entries(params).filter(([, value]) => value !== undefined && value !== null && value !== '')
  );
  const { data } = await http.get<ApiResponse<FileAsset[]>>('/api/data-steward/assets/files', {
    params: normalized
  });
  return data.data;
}

export async function fetchFileAssetsPage(params: FileAssetQuery = {}): Promise<FileAssetPageResult> {
  const normalized = Object.fromEntries(
    Object.entries(params).filter(([, value]) => value !== undefined && value !== null && value !== '')
  );
  const { data } = await http.get<ApiResponse<{ pageNo: number; pageSize: number; total: number; items: FileAsset[] }>>(
    '/api/data-steward/assets/files:page',
    { params: normalized }
  );
  return {
    page: data.data?.pageNo ?? params.pageNo ?? 1,
    pageSize: data.data?.pageSize ?? params.pageSize ?? 50,
    total: data.data?.total ?? 0,
    rows: data.data?.items ?? []
  };
}

export async function fetchFileAsset(fileId: number) {
  const { data } = await http.get<ApiResponse<FileAsset>>(`/api/data-steward/assets/files/${fileId}`);
  return data.data;
}

export async function fetchFilePreview(fileId: number) {
  const { data } = await http.get<ApiResponse<FilePreview>>(`/api/data-steward/assets/files/${fileId}/preview`);
  return data.data;
}

export async function createFileAccessTicket(fileId: number, action: 'PREVIEW' | 'DOWNLOAD') {
  const { data } = await http.post<ApiResponse<FileAccessTicket>>(
    `/api/data-steward/assets/files/${fileId}/access-tickets`,
    { action }
  );
  return data.data;
}

export async function updateFileAssetMetadata(fileId: number, payload: FileAssetMetadataPayload) {
  const { data } = await http.patch<ApiResponse<FileAsset>>(
    `/api/data-steward/assets/files/${fileId}/metadata`,
    payload
  );
  return data.data;
}

export async function createChecksumJob(fileId: number) {
  const { data } = await http.post<ApiResponse<AssetJob>>('/api/data-steward/assets/checksum-jobs', { fileId });
  return data.data;
}

export async function createBatchChecksumJobs(projectId: number) {
  const { data } = await http.post<ApiResponse<number>>('/api/data-steward/assets/checksum-jobs/batch', { projectId });
  return data.data ?? 0;
}

export async function fetchAssetJobs(params: AssetJobQuery = {}) {
  const normalized = Object.fromEntries(
    Object.entries(params).filter(([, value]) => value !== undefined && value !== null && value !== '')
  );
  const { data } = await http.get<ApiResponse<AssetJob[]>>('/api/data-steward/assets/jobs', {
    params: normalized
  });
  return data.data;
}

export async function fetchAssetJob(jobId: number) {
  const { data } = await http.get<ApiResponse<AssetJob>>(`/api/data-steward/assets/jobs/${jobId}`);
  return data.data;
}

export async function retryAssetJob(jobId: number) {
  await http.post<ApiResponse<null>>(`/api/data-steward/assets/jobs/${jobId}:retry`);
}

export async function fetchAssetScanTasks() {
  const { data } = await http.get<ApiResponse<AssetScanTask[]>>('/api/data-steward/assets/nas-scans');
  return data.data;
}

export async function createAssetScan(payload: AssetScanPayload) {
  const { data } = await http.post<ApiResponse<AssetScanTask>>('/api/data-steward/assets/nas-scans', payload);
  return data.data;
}

export async function runAssetScan(scanTaskId: number) {
  const { data } = await http.post<ApiResponse<AssetScanTask>>(
    `/api/data-steward/assets/nas-scans/${scanTaskId}:run`
  );
  return data.data;
}

export async function cancelAssetScan(scanTaskId: number) {
  const { data } = await http.post<ApiResponse<AssetScanTask>>(
    `/api/data-steward/assets/nas-scans/${scanTaskId}:cancel`
  );
  return data.data;
}

export async function resumeAssetScan(scanTaskId: number) {
  const { data } = await http.post<ApiResponse<AssetScanTask>>(
    `/api/data-steward/assets/nas-scans/${scanTaskId}:resume`
  );
  return data.data;
}

export async function fetchAssetScanReport(scanTaskId: number) {
  const { data } = await http.get<ApiResponse<AssetScanReport>>(
    `/api/data-steward/assets/nas-scans/${scanTaskId}/report`
  );
  return data.data;
}

export async function fetchAssetPathMappings(projectId?: number) {
  const { data } = await http.get<ApiResponse<AssetPathMapping[]>>('/api/data-steward/assets/path-mappings', {
    params: projectId ? { projectId, enabled: true } : { enabled: true }
  });
  return data.data;
}

export async function fetchNonstandardDirectories(params: NonstandardDirectoryQuery = {}) {
  const normalized = Object.fromEntries(
    Object.entries(params).filter(([, value]) => value !== undefined && value !== null && value !== '')
  );
  const { data } = await http.get<ApiResponse<NonstandardDirectory[]>>(
    '/api/data-steward/assets/nonstandard-directories',
    { params: normalized }
  );
  return data.data;
}

export async function discoverNonstandardDirectories(payload: NonstandardDirectoryDiscoverPayload) {
  const { data } = await http.post<ApiResponse<NonstandardDirectoryDiscoverResult>>(
    '/api/data-steward/assets/nonstandard-directories:discover',
    payload
  );
  return data.data;
}

export async function updateNonstandardDirectory(directoryId: number, payload: NonstandardDirectoryUpdatePayload) {
  const { data } = await http.patch<ApiResponse<NonstandardDirectory>>(
    `/api/data-steward/assets/nonstandard-directories/${directoryId}`,
    payload
  );
  return data.data;
}

// ===== phase2 batch1: readonly catalog =====

export interface CatalogProject {
  projectId: number;
  projectCode: string;
  projectName: string;
  projectStage: string | null;
  assetSource: string | null;
  fileCount: number;
  totalSizeBytes: number;
  confidenceLevel: string | null;
}

export interface CatalogDirectory {
  directoryPath: string;
  projectId: number;
  projectCode: string;
  fileCount: number;
  totalSizeBytes: number;
}

export interface CatalogFile {
  fileId: number;
  projectId: number;
  projectCode: string;
  projectName: string;
  fileName: string;
  fileExt: string;
  fileKind: string;
  disciplineCode: string | null;
  disciplineName: string | null;
  version: string;
  sizeBytes: number;
  checksum: string | null;
  status: string;
  confidenceLevel: string | null;
  storageProvider: string;
  logicalPath: string | null;
  storagePathVisible: boolean;
  storagePathVisibilityReason: string;
  qualityFlags: string[];
  lastVerifiedAt: string | null;
  updatedAt: string | null;
  agentReadable: boolean;
  agentReadReason: string;
  agentContractView: string[];
}

export interface CatalogFileDetail extends CatalogFile {
  storagePath: string | null;
}

export interface CatalogEventSummary {
  eventId: number;
  eventType: string;
  actionCode: string;
  summary: string | null;
  createdAt: string;
}

export interface AuditContext {
  fileId: number;
  totalEventCount: number;
  recentEvents: CatalogEventSummary[];
}

export interface PermissionEvidence {
  type: string;
  label: string;
  value: string;
  sensitive: boolean;
}

export interface PermissionProof {
  allowed: boolean;
  decision: string;
  actorType: string;
  projectScope: string | null;
  reasonCode: string;
  reasonText: string;
  evidence: PermissionEvidence[];
  traceId: string;
  checkedAt: string;
}

export interface HermesCapabilities {
  agentName: string;
  mode: string;
  contractVersion: string;
  supports: {
    catalogQuery: boolean;
    missingEvidence: boolean;
    operationPlanDraft: boolean;
    documentContentAnswer: boolean;
    dbCrud: boolean;
    nasCrud: boolean;
    fullBimParse: boolean;
    productionRollout: boolean;
  };
  safety: {
    failClosed: boolean;
    requiresProjectScope: boolean;
    requiresCitationForContentAnswer: boolean;
  };
  authorityHealth: HermesAuthorityHealth;
}

export interface HermesAuthorityHealth {
  safetyHealth: string;
  capabilityHealth: string;
  architectureAuthorityHealth: string;
  mode: string;
}

export interface HermesHealth {
  status: string;
  hermesAvailable: boolean;
  mode: string;
  contractVersion: string;
  gatewayEnabled: boolean;
  readonly: boolean;
  runtimeWriteEnabled: boolean;
  agentAnswerIntegrationEnabled: boolean;
  unavailableReason: string;
  checkedAt: string;
  authorityHealth: HermesAuthorityHealth;
}

export interface HermesChatRequest {
  pageType: string;
  projectId: number;
  assetId?: number;
  sourceView?: string;
  currentRoute?: string;
  projectCode?: string;
  projectName?: string;
  pageTitle?: string;
  sessionId?: string;
  threadId?: string;
  previousResponseId?: string;
  sanitizedContextRefs?: Record<string, unknown>[];
  question: string;
}

export interface HermesCitation {
  citationType: string;
  sourceView: string;
  assetRef: string;
  projectRef: string;
  displayLabel: string;
  safeToOpen: boolean;
}

export interface HermesPermissionResult {
  permissionStatus: string;
  projectScopeChecked: boolean;
  permissionTagsChecked: boolean;
  failClosedApplied: boolean;
  reasonCode: string | null;
}

export interface HermesMissingEvidence {
  reason: string;
  message: string;
}

export interface HermesPathHint {
  displayPath: string;
  pathHint: string;
  provider: string;
  matchStrategy: string;
}

export interface HermesOperationAction {
  actionType: string;
  status: string;
}

export interface HermesOperationPlan {
  available: boolean;
  requiresHumanApproval: boolean;
  actions: HermesOperationAction[];
}

export interface HermesTrace {
  requestId: string;
  agentMode: string;
  productionRollout: boolean;
}

export interface HermesChatResponse {
  responseId: string;
  status: 'ok' | 'denied' | 'missing_evidence' | 'catalog_only' | 'error' | string;
  evidenceMode: 'catalog_only' | 'missing_evidence' | string;
  assetCatalogOnly: boolean;
  queryId: string;
  traceId: string;
  sourceView: string;
  fileId: number | null;
  modelId: number | null;
  pathHints: HermesPathHint[];
  answer: string;
  citations: HermesCitation[];
  permission: HermesPermissionResult;
  missingEvidence: HermesMissingEvidence[];
  operationPlan: HermesOperationPlan;
  trace: HermesTrace;
  sessionRef: string;
  threadRef: string;
  previousResponseRef: string;
  authorityHealth: HermesAuthorityHealth;
  safeMemoryCandidates: Record<string, unknown>[];
  sanitizedContextRefs: Record<string, unknown>[];
}

const HERMES_CHAT_TIMEOUT_MS = 45_000;

export interface CatalogSearchRequest {
  query: string;
  projectFilters?: string[];
  filters?: {
    assetKind?: string[];
    fileExt?: string[];
    lifecycleStatus?: string[];
    indexEligibility?: string[];
  };
  page?: {
    limit?: number;
    cursor?: string | null;
  };
}

export interface CatalogSearchResult {
  assetRef: string;
  assetKind: string;
  sourceView: string;
  fileId: number;
  modelId: number | null;
  projectId: number;
  projectCode: string;
  projectName: string;
  fileName: string;
  displayPath: string;
  pathHint: string;
  fileExt: string;
  disciplineCode: string | null;
  version: string | null;
  sizeBucket: string;
  lifecycleStatus: string;
  indexEligibility: string;
  contentEvidenceAvailable: boolean;
  missingEvidence: string[];
  updatedAt: string | null;
}

export interface CatalogSearchResponse {
  queryId: string;
  traceId: string;
  assetCatalogOnly: boolean;
  permissionDecision: string;
  evidenceMode: string;
  results: CatalogSearchResult[];
  nextCursor: string | null;
  safety: {
    rawRowsOutput: boolean;
    trueNasPathOutput: boolean;
    secretPrinted: boolean;
  };
  authorityHealth: HermesAuthorityHealth;
}

export interface CatalogFilesQuery {
  projectId?: number;
  keyword?: string;
  directoryPath?: string;
  fileExt?: string;
  fileKind?: string;
  disciplineCode?: string;
  version?: string;
  qualityIssue?: string;
  page?: number;
  pageSize?: number;
}

export async function fetchCatalogProjects(assetSource?: string) {
  const { data } = await http.get<ApiResponse<CatalogProject[]>>('/api/data-steward/catalog/projects', {
    params: assetSource ? { assetSource } : undefined
  });
  return data.data;
}

export async function fetchCatalogDirectories(projectId: number) {
  const { data } = await http.get<ApiResponse<CatalogDirectory[]>>(
    '/api/data-steward/catalog/directories',
    { params: { projectId } }
  );
  return data.data;
}

export async function fetchCatalogFiles(params: CatalogFilesQuery = {}) {
  const normalized = Object.fromEntries(
    Object.entries(params).filter(([, value]) => value !== undefined && value !== null && value !== '')
  );
  const resp = await http.get<ApiResponse<{ pageNo: number; pageSize: number; total: number; items: CatalogFile[] }>>(
    '/api/data-steward/catalog/files',
    { params: normalized }
  );
  return {
    page: resp.data.data?.pageNo ?? 1,
    pageSize: resp.data.data?.pageSize ?? 20,
    total: resp.data.data?.total ?? 0,
    rows: resp.data.data?.items ?? []
  };
}

export async function fetchCatalogFileDetail(fileId: number) {
  const { data } = await http.get<ApiResponse<CatalogFileDetail>>(
    `/api/data-steward/catalog/files/${fileId}`
  );
  return data.data;
}

export async function fetchFileAuditContext(fileId: number) {
  const { data } = await http.get<ApiResponse<AuditContext>>(
    `/api/data-steward/catalog/files/${fileId}/audit-context`
  );
  return data.data;
}

export async function fetchFilePermissionProof(fileId: number) {
  const { data } = await http.get<ApiResponse<PermissionProof>>(
    `/api/data-steward/catalog/files/${fileId}/permission-proof`
  );
  return data.data;
}

export async function checkPermissionProofs(fileIds: number[], actorType = 'USER') {
  const { data } = await http.post<ApiResponse<PermissionProof[]>>(
    '/api/data-steward/catalog/permission-proofs:check',
    { fileIds, actorType }
  );
  return data.data;
}

export async function fetchHermesCapabilities() {
  const { data } = await http.get<ApiResponse<HermesCapabilities>>('/api/data-steward/hermes/capabilities');
  return data.data;
}

export async function askHermes(request: HermesChatRequest) {
  const { data } = await http.post<ApiResponse<HermesChatResponse>>('/api/data-steward/chat', request, {
    timeout: HERMES_CHAT_TIMEOUT_MS
  });
  return data.data;
}

export async function fetchHermesHealth() {
  const { data } = await http.get<ApiResponse<HermesHealth>>('/api/data-steward/hermes/health');
  return data.data;
}

export async function searchCatalogPreview(request: CatalogSearchRequest) {
  const { data } = await http.post<ApiResponse<CatalogSearchResponse>>('/api/data-steward/catalog/search', request);
  return data.data;
}
