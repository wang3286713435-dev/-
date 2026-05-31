export type BimThemeMode = 'dark' | 'light';

export type BimTone = 'normal' | 'warning' | 'danger' | 'focus' | 'muted';

export interface BimMetric {
  label: string;
  value: string;
  hint?: string;
  tone?: BimTone;
}

export interface BimChartBar {
  label: string;
  value: number;
  displayValue: string;
  color: string;
}

export interface BimLegendItem {
  label: string;
  value: string;
  color: string;
}

export interface BimOperationsSummary {
  equipmentCount: number;
  spaceObjectCount: number;
  systemObjectCount: number;
  componentPlaceholderCount: number;
  sectionNodeCount: number;
  linkedObjectCount: number;
  unlinkedObjectCount: number;
  byObjectType: BimChartBar[];
  byDiscipline: BimChartBar[];
  systems: BimSystemItem[];
  spaces: BimSpaceItem[];
  workItems: BimWorkItem[];
  unavailableModules: string[];
}

export interface BimSystemItem {
  id: number;
  code: string;
  name: string;
  discipline: string;
  linkedEquipmentCount: number;
  status: string;
  source: string;
}

export interface BimSpaceItem {
  id: number;
  parentId: number | null;
  code: string;
  name: string;
  level: number;
  path: string;
  objectCount: number;
  equipmentCount: number;
  systemCount: number;
}

export interface BimWorkItem {
  id: string;
  category: string;
  title: string;
  status: string;
  source: string;
  updatedAt: string;
}

export interface BimSceneNode {
  id: string;
  modelFileId: number;
  label: string;
  meta: string;
  modelFormat: string;
  versionNo: string;
  integrationStatus: string;
  status: Exclude<BimTone, 'muted'>;
  weight: number;
  previewStatus: string;
  previewMode: string;
  conversionStatus: string;
  viewerAvailable: boolean;
  statusLabel: string;
  actionHint: string;
}

export interface BimEmbeddedPreviewModel extends BimSceneNode {
  frameUrl: string;
  fileManagerUrl?: string;
  sizeLabel?: string;
}

export interface BimLightweightSummary {
  totalModelFiles: number;
  readyCount: number;
  pendingCount: number;
  failedCount: number;
  readyModels: BimEmbeddedPreviewModel[];
}

export interface BimTimelineEvent {
  id: string;
  title: string;
  time: string;
  tone: BimTone;
}

export interface BimCollaborationData {
  projectId: number;
  projectName: string;
  projectCode: string;
  projectStage: string;
  managerName: string;
  engineMode: string;
  statusLabel: string;
  viewerAvailable: boolean;
  viewerMessage: string;
  updatedAt: string;
  overviewMetrics: BimMetric[];
  progressPercent: number;
  progressMetrics: BimMetric[];
  issueMetrics: BimMetric[];
  modelBars: BimChartBar[];
  objectShares: BimLegendItem[];
  issueBars: BimChartBar[];
  sceneNodes: BimSceneNode[];
  todoMetrics: BimMetric[];
  events: BimTimelineEvent[];
  versionInfo: BimMetric[];
  safetyNotes: string[];
  nextActionText: string;
  operations: BimOperationsSummary;
}
