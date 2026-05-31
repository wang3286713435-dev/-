import { http } from '@/app/http';
import type { ApiResponse } from '@/modules/core/api/types';
import type { ModelIntegration } from '@/modules/data-steward/api/dataSteward';

export interface VisualizationContext {
  projectId: number;
  publishedModelCount: number;
  managedObjectCount: number;
  models: Array<{
    id: number;
    name: string;
    versionNo: string;
    status: string;
    componentCount: number;
  }>;
  objects: Array<{
    id: number;
    code: string;
    name: string;
    objectType: string;
    sectionNodeId: number | null;
  }>;
}

export interface AdapterCommand {
  projectId: number;
  adapterCommand: string;
  status: string;
  [key: string]: unknown;
}

export interface DigitalTwinDashboard {
  project: {
    projectId: number;
    code: string;
    name: string;
    industryType: string | null;
    projectStage: string | null;
    projectManagerName: string | null;
    assetStatus: string | null;
    onboardingStatus: string | null;
  };
  assetSummary: {
    projectCount: number;
    fileCount: number;
    modelFileCount: number;
    drawingFileCount: number;
    totalSizeBytes: number;
    byFileKind: DistributionItem[];
    byDiscipline: DistributionItem[];
  };
  deliverySummary: {
    standardReady: boolean;
    totalRequired: number;
    completedCount: number;
    missingCount: number;
    draftCount: number;
    pendingReviewCount: number;
    approvedCount: number;
    rejectedCount: number;
    openRectificationCount: number;
    completionRate: number;
    approvedRate: number;
    nextActionCode: string;
    nextActionText: string;
    readinessIssues: string[];
  };
  qualitySummary: {
    riskSignalCount: number;
    pendingReviewCount: number;
    failedScanCount: number;
    runningScanCount: number;
    missingChecksumCount: number;
    missingDisciplineCount: number;
    missingVersionCount: number;
    zeroSizeFileCount: number;
    metrics: Array<{
      code: string;
      label: string;
      severity: string;
      count: number;
    }>;
  };
  modelSummary: {
    modelIntegrationCount: number;
    publishedModelCount: number;
    managedObjectCount: number;
    engineMode: string;
    engineConnected: boolean;
    lightweightStatus: string;
    viewerAvailable: boolean;
    statusLabel: string;
    actionHint: string;
    models: Array<{
      id: number;
      modelFileId: number;
      name: string;
      modelFormat: string;
      versionNo: string;
      status: string;
      componentCount: number;
      previewStatus: string;
      previewMode: string;
      conversionStatus: string;
      viewerAvailable: boolean;
      statusLabel: string;
      actionHint: string;
    }>;
    objects: Array<{
      id: number;
      code: string;
      name: string;
      objectType: string;
      sectionNodeId: number | null;
      discipline: string | null;
      status: string | null;
    }>;
  };
  operationsSummary: {
    equipmentCount: number;
    spaceObjectCount: number;
    systemObjectCount: number;
    componentPlaceholderCount: number;
    sectionNodeCount: number;
    linkedObjectCount: number;
    unlinkedObjectCount: number;
    byObjectType: DistributionItem[];
    byDiscipline: DistributionItem[];
    systems: Array<{
      id: number;
      code: string;
      name: string;
      discipline: string | null;
      linkedEquipmentCount: number;
      status: string | null;
      source: string;
    }>;
    spaces: Array<{
      id: number;
      parentId: number | null;
      code: string;
      name: string;
      level: number;
      path: string;
      objectCount: number;
      equipmentCount: number;
      systemCount: number;
    }>;
    workItems: Array<{
      id: string;
      category: string;
      title: string;
      status: string | null;
      source: string | null;
      updatedAt: string | null;
    }>;
    unavailableModules: string[];
  };
  activity: {
    latestAssetUpdatedAt: string | null;
    latestEventAt: string | null;
    recentEvents: Array<{
      id: number;
      projectId: number | null;
      actionCode: string;
      summary: string | null;
      createdAt: string;
    }>;
    scanTasks: Array<{
      id: number;
      projectId: number | null;
      projectCode: string | null;
      status: string;
      progressPercent: number | null;
      totalScanned: number;
      autoIngested: number;
      pendingReview: number;
      failedCount: number;
      updatedAt: string;
    }>;
  };
  safetyBoundary: {
    guarantees: string[];
    blockedOperations: string[];
    viewerMessage: string;
  };
}

export interface DistributionItem {
  code: string;
  label: string;
  count: number;
  totalSizeBytes: number;
}

export interface LightweightJobCreateResponse {
  projectId: number;
  integrationId: number | null;
  modelFileId: number;
  modelName: string;
  modelFormat: string;
  jobId: string;
  lightweightName: string | null;
  uniqueCode: string | null;
  modelAccessAddress: string | null;
  engineMode: string;
  taskCreated: boolean;
  taskStatus: string;
  progressPercent: number;
  statusLabel: string;
  actionHint: string;
  blockedReason: string | null;
  realUploadExecuted: boolean;
  realConversionExecuted: boolean;
  modelBodyRead: boolean;
  nasFileTouched: boolean;
  viewerAvailable: boolean;
  supportedOperations: string[];
  forbiddenOperations: string[];
}

export interface LightweightJobResponse {
  projectId: number;
  jobId: string;
  modelFileId: number;
  lightweightName: string | null;
  uniqueCode: string | null;
  modelAccessAddress: string | null;
  engineMode: string;
  taskStatus: string;
  progressPercent: number;
  statusLabel: string;
  blockedReason: string | null;
  viewerAvailable: boolean;
  realUploadExecuted: boolean;
  realConversionExecuted: boolean;
  lastErrorCode: string | null;
  lastErrorMessage: string | null;
  updatedAt: string | null;
}

export interface LightweightViewerTicketResponse {
  projectId: number;
  jobId: string;
  engineMode: string;
  viewerAvailable: boolean;
  ticketIssued: boolean;
  viewerTicket: string | null;
  expiresAt: string | null;
  launchUrl: string | null;
  lightweightName: string | null;
  modelAccessAddress: string | null;
  engineStaticBase: string | null;
  statusLabel: string;
  blockedReason: string | null;
  supportedOperations: string[];
  forbiddenOperations: string[];
}

export interface GlandarRvtPilotFile {
  projectId: number;
  fileId: number;
  assetUuid: string;
  fileName: string;
  modelFormat: string;
  sizeBytes: number;
  pilotRank: number;
  inPilot: boolean;
  latestJobId: string | null;
  lightweightName: string | null;
  taskStatus: string;
  progressPercent: number;
  viewerAvailable: boolean;
  statusLabel: string;
  actionHint: string;
  blockedReason: string | null;
  updatedAt: string | null;
}

export async function fetchVisualizationContext(projectId: number) {
  const { data } = await http.get<ApiResponse<VisualizationContext>>(
    `/api/visualization-adapter/projects/${projectId}/context`
  );
  return data.data;
}

export async function fetchDigitalTwinDashboard(projectId: number) {
  const { data } = await http.get<ApiResponse<DigitalTwinDashboard>>(
    `/api/visualization-adapter/projects/${projectId}/digital-twin-dashboard`
  );
  return data.data;
}

export async function publishModelViaAdapter(projectId: number, integrationId: number) {
  const { data } = await http.post<ApiResponse<ModelIntegration>>(
    `/api/visualization-adapter/projects/${projectId}/model-integrations/${integrationId}:publish`
  );
  return data.data;
}

export async function locateManagedObject(projectId: number, objectId: number) {
  const { data } = await http.post<ApiResponse<AdapterCommand>>(
    `/api/visualization-adapter/projects/${projectId}/managed-objects/${objectId}:locate`
  );
  return data.data;
}

export async function highlightManagedObject(projectId: number, objectId: number, color = '#2563eb') {
  const { data } = await http.post<ApiResponse<AdapterCommand>>(
    `/api/visualization-adapter/projects/${projectId}/managed-objects/${objectId}:highlight`,
    { color, durationSeconds: 5 }
  );
  return data.data;
}

export async function injectVisualizationContext(projectId: number, sectionNodeId?: number | null, managedObjectId?: number | null) {
  const { data } = await http.post<ApiResponse<AdapterCommand>>(
    `/api/visualization-adapter/projects/${projectId}/context:inject`,
    { sectionNodeId, managedObjectId, source: 'FRONTEND_WORKBENCH' }
  );
  return data.data;
}

export async function fetchGlandarRvtPilotFiles(projectId: number) {
  const { data } = await http.get<ApiResponse<GlandarRvtPilotFile[]>>(
    `/api/visualization-adapter/projects/${projectId}/glandar/rvt-pilot-files`
  );
  return data.data;
}

export async function submitGlandarRvtPilotFiles(projectId: number, force = false) {
  const { data } = await http.post<ApiResponse<GlandarRvtPilotFile[]>>(
    `/api/visualization-adapter/projects/${projectId}/glandar/rvt-pilot-files:submit`,
    {},
    { params: { force } }
  );
  return data.data;
}

export async function createFileLightweightJob(projectId: number, fileId: number, force = false) {
  const { data } = await http.post<ApiResponse<LightweightJobCreateResponse>>(
    `/api/visualization-adapter/projects/${projectId}/files/${fileId}/lightweight-jobs`,
    {},
    { params: { force } }
  );
  return data.data;
}

export async function fetchLightweightJob(projectId: number, jobId: string) {
  const { data } = await http.get<ApiResponse<LightweightJobResponse>>(
    `/api/visualization-adapter/projects/${projectId}/lightweight-jobs/${jobId}`
  );
  return data.data;
}

export async function issueLightweightViewerTicket(projectId: number, jobId: string) {
  const { data } = await http.post<ApiResponse<LightweightViewerTicketResponse>>(
    `/api/visualization-adapter/projects/${projectId}/lightweight-jobs/${jobId}:viewer-ticket`,
    {}
  );
  return data.data;
}
