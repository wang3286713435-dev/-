import { http } from '@/app/http';
import type { ApiResponse } from '@/modules/core/api/types';

export interface SectionNode {
  id: number;
  projectId: number;
  parentId: number | null;
  code: string;
  name: string;
  level: number;
  path: string;
  sortOrder: number;
  status: string;
  children: SectionNode[];
}

export interface SectionNodePayload {
  parentId?: number | null;
  code: string;
  name: string;
  sortOrder: number;
  status: string;
}

export interface NodeType {
  id: number;
  projectId: number;
  code: string;
  name: string;
  scopeLevel: number;
  sortOrder: number;
  status: string;
  locked: boolean;
  lockedAt: string | null;
  lockedBy: number | null;
}

export interface NodeTypePayload {
  code: string;
  name: string;
  scopeLevel: number;
  sortOrder: number;
  status: string;
}

export interface NodeTypeLockStatus {
  projectId: number;
  nodeTypeId: number | null;
  locked: boolean;
  lockedAt: string | null;
  lockedBy: number | null;
  hasNodeTypes: boolean;
  allNodeTypesLocked: boolean;
  nodeTypeCount: number;
}

export interface StandardStatus {
  projectId: number;
  hasSectionTree: boolean;
  hasNodeTypes: boolean;
  nodeTypesLocked: boolean;
  sectionNodeCount: number;
  nodeTypeCount: number;
  hasDeliverableDefinitions: boolean;
  hasDeliverableTypes: boolean;
  hasDeliverableAttributes: boolean;
  hasDirectoryTemplates: boolean;
  deliverableStandardReady: boolean;
  deliverableDefinitionCount: number;
  deliverableTypeCount: number;
  deliverableAttributeCount: number;
  directoryTemplateCount: number;
}

export interface InitializationStatus {
  projectId: number;
  standardStatus: StandardStatus;
  currentStep: string;
  ready: boolean;
  blockers: string[];
  warnings: string[];
  nextActions: string[];
}

export interface TemplateCounts {
  sectionNodes: number;
  nodeTypes: number;
  deliverableDefinitions: number;
  deliverableTypes: number;
  deliverableAttributes: number;
  directoryTemplates: number;
}

export interface StandardTemplateSummary {
  templateCode: string;
  templateName: string;
  industryType: string;
  description: string;
  counts: TemplateCounts;
}

export interface StandardTemplateDetail extends StandardTemplateSummary {
  items: StandardTemplateItem[];
}

export interface StandardTemplateItem {
  category: string;
  code: string;
  name: string;
  parentCode: string | null;
  targetCode: string | null;
  fileKind: string | null;
  required: boolean | null;
}

export interface TemplatePreviewItem {
  category: string;
  code: string;
  name: string;
  action: 'CREATE' | 'SKIP' | 'CONFLICT';
  reason: string;
}

export interface TemplatePreview {
  templateCode: string;
  templateName: string;
  blocked: boolean;
  blockReasons: string[];
  conflicts: string[];
  willCreate: TemplateCounts;
  willSkip: TemplateCounts;
  items: TemplatePreviewItem[];
}

export interface TemplateApplyResult {
  templateCode: string;
  templateName: string;
  created: TemplateCounts;
  skipped: TemplateCounts;
  conflictCount: number;
  standardStatus: StandardStatus;
  nextActions: string[];
}

export interface OnboardingAssetSummary {
  fileCount: number;
  modelFileCount: number;
  drawingFileCount: number;
  documentFileCount: number;
  pathMappingCount: number;
  dominantFileKinds: string[];
  lastAssetSeenAt: string | null;
  lastScanAt: string | null;
}

export interface OnboardingEvidenceClue {
  clueType: string;
  label: string;
  evidenceMode: string;
  assetCatalogOnly: boolean;
  description: string;
}

export interface OnboardingGap {
  code: string;
  severity: string;
  description: string;
  missingEvidenceReason: string;
}

export interface OnboardingAssessment {
  projectId: number;
  assetCatalogOnly: boolean;
  evidenceMode: string;
  onboardingStatus: string;
  assetSummary: OnboardingAssetSummary;
  standardStatus: StandardStatus;
  evidenceClues: OnboardingEvidenceClue[];
  gaps: OnboardingGap[];
  nextActions: string[];
}

export interface OnboardingDraftItem {
  category: string;
  name: string;
  reason: string;
  fromRealAssetClue: boolean;
  fromTemplateSkeleton: boolean;
  pendingConfirmation: boolean;
}

export interface OnboardingDraftPreview {
  projectId: number;
  dryRun: boolean;
  confirmedRequired: boolean;
  nasTouched: boolean;
  contentRead: boolean;
  evidenceMode: string;
  templateCode: string;
  templateName: string;
  assetSummary: OnboardingAssetSummary;
  templatePreview: TemplatePreview;
  draftItems: OnboardingDraftItem[];
  warnings: string[];
}

export interface OnboardingApplyResult {
  projectId: number;
  confirmed: boolean;
  nasTouched: boolean;
  contentRead: boolean;
  draftApplied: boolean;
  evidenceMode: string;
  templateResult: TemplateApplyResult;
  nextActions: string[];
}

export interface DeliverableDefinition {
  id: number;
  projectId: number;
  nodeTypeId: number;
  code: string;
  name: string;
  category: string;
  required: boolean;
  sortOrder: number;
  status: string;
}

export interface DeliverableDefinitionPayload {
  nodeTypeId: number;
  code: string;
  name: string;
  category?: string;
  required?: boolean;
  sortOrder?: number;
  status?: string;
}

export interface DeliverableType {
  id: number;
  projectId: number;
  deliverableDefinitionId: number;
  code: string;
  name: string;
  fileKind: string;
  bindingStrategy: string;
  sortOrder: number;
  status: string;
}

export interface DeliverableTypePayload {
  deliverableDefinitionId: number;
  code: string;
  name: string;
  fileKind?: string;
  bindingStrategy?: string;
  sortOrder?: number;
  status?: string;
}

export interface DeliverableAttribute {
  id: number;
  projectId: number;
  deliverableTypeId: number;
  code: string;
  name: string;
  valueType: string;
  unit: string | null;
  required: boolean;
  exampleValue: string | null;
  enumOptions: string | null;
  sortOrder: number;
  status: string;
}

export interface DeliverableAttributePayload {
  deliverableTypeId: number;
  code: string;
  name: string;
  valueType?: string;
  unit?: string;
  required?: boolean;
  exampleValue?: string;
  enumOptions?: string;
  sortOrder?: number;
  status?: string;
}

export interface DirectoryTemplate {
  id: number;
  projectId: number;
  templateType: string;
  name: string;
  rootNodeJson: string | null;
  sourceType: string;
  sortOrder: number;
  status: string;
}

export interface DirectoryTemplatePayload {
  templateType: string;
  name: string;
  rootNodeJson?: string;
  sourceType?: string;
  sortOrder?: number;
  status?: string;
}

export async function fetchStandardStatus(projectId: number) {
  const { data } = await http.get<ApiResponse<StandardStatus>>(
    `/api/master-data/projects/${projectId}/standard-status`
  );
  return data.data;
}

export async function fetchInitializationStatus(projectId: number) {
  const { data } = await http.get<ApiResponse<InitializationStatus>>(
    `/api/master-data/projects/${projectId}/initialization/status`
  );
  return data.data;
}

export async function fetchStandardTemplates() {
  const { data } = await http.get<ApiResponse<StandardTemplateSummary[]>>('/api/master-data/standard-templates');
  return data.data;
}

export async function fetchStandardTemplateDetail(templateCode: string) {
  const { data } = await http.get<ApiResponse<StandardTemplateDetail>>(
    `/api/master-data/standard-templates/${templateCode}`
  );
  return data.data;
}

export async function previewStandardTemplate(projectId: number, templateCode: string) {
  const { data } = await http.post<ApiResponse<TemplatePreview>>(
    `/api/master-data/projects/${projectId}/initialization:preview-template`,
    { templateCode }
  );
  return data.data;
}

export async function applyStandardTemplate(projectId: number, templateCode: string) {
  const { data } = await http.post<ApiResponse<TemplateApplyResult>>(
    `/api/master-data/projects/${projectId}/initialization:apply-template`,
    { templateCode, confirmApply: true }
  );
  return data.data;
}

export async function fetchOnboardingAssessment(projectId: number) {
  const { data } = await http.get<ApiResponse<OnboardingAssessment>>(
    `/api/master-data/projects/${projectId}/onboarding/assessment`
  );
  return data.data;
}

export async function fetchOnboardingPreview(projectId: number, templateCode?: string) {
  const { data } = await http.get<ApiResponse<OnboardingDraftPreview>>(
    `/api/master-data/projects/${projectId}/onboarding/preview`,
    { params: templateCode ? { templateCode } : undefined }
  );
  return data.data;
}

export async function applyOnboardingDraft(projectId: number, templateCode: string) {
  const { data } = await http.post<ApiResponse<OnboardingApplyResult>>(
    `/api/master-data/projects/${projectId}/onboarding/apply`,
    { templateCode, confirmed: true }
  );
  return data.data;
}

export async function createSectionNode(projectId: number, payload: SectionNodePayload) {
  const { data } = await http.post<ApiResponse<SectionNode>>(
    `/api/master-data/projects/${projectId}/section-nodes`,
    payload
  );
  return data.data;
}

export async function fetchSectionTree(projectId: number) {
  const { data } = await http.get<ApiResponse<SectionNode[]>>(
    `/api/master-data/projects/${projectId}/section-nodes/tree`
  );
  return data.data;
}

export async function updateSectionNode(projectId: number, nodeId: number, payload: SectionNodePayload) {
  const { data } = await http.patch<ApiResponse<SectionNode>>(
    `/api/master-data/projects/${projectId}/section-nodes/${nodeId}`,
    payload
  );
  return data.data;
}

export async function deleteSectionNode(projectId: number, nodeId: number) {
  await http.delete<ApiResponse<null>>(`/api/master-data/projects/${projectId}/section-nodes/${nodeId}`);
}

export async function createNodeType(projectId: number, payload: NodeTypePayload) {
  const { data } = await http.post<ApiResponse<NodeType>>(
    `/api/master-data/projects/${projectId}/node-types`,
    payload
  );
  return data.data;
}

export async function fetchNodeTypes(projectId: number) {
  const { data } = await http.get<ApiResponse<NodeType[]>>(`/api/master-data/projects/${projectId}/node-types`);
  return data.data;
}

export async function updateNodeType(projectId: number, nodeTypeId: number, payload: NodeTypePayload) {
  const { data } = await http.patch<ApiResponse<NodeType>>(
    `/api/master-data/projects/${projectId}/node-types/${nodeTypeId}`,
    payload
  );
  return data.data;
}

export async function lockNodeType(projectId: number, nodeTypeId: number) {
  const { data } = await http.post<ApiResponse<NodeType>>(
    `/api/master-data/projects/${projectId}/node-types/${nodeTypeId}:lock`
  );
  return data.data;
}

export async function lockAllNodeTypes(projectId: number) {
  const { data } = await http.post<ApiResponse<NodeTypeLockStatus>>(
    `/api/master-data/projects/${projectId}/node-types:lock`
  );
  return data.data;
}

export async function fetchNodeTypeLockStatus(projectId: number) {
  const { data } = await http.get<ApiResponse<NodeTypeLockStatus>>(
    `/api/master-data/projects/${projectId}/node-types/lock-status`
  );
  return data.data;
}

// Deliverable Definitions
export async function createDeliverableDefinition(projectId: number, payload: DeliverableDefinitionPayload) {
  const { data } = await http.post<ApiResponse<DeliverableDefinition>>(
    `/api/master-data/projects/${projectId}/deliverable-definitions`,
    payload
  );
  return data.data;
}

export async function fetchDeliverableDefinitions(projectId: number) {
  const { data } = await http.get<ApiResponse<DeliverableDefinition[]>>(
    `/api/master-data/projects/${projectId}/deliverable-definitions`
  );
  return data.data;
}

export async function updateDeliverableDefinition(projectId: number, definitionId: number, payload: Partial<DeliverableDefinitionPayload>) {
  const { data } = await http.patch<ApiResponse<DeliverableDefinition>>(
    `/api/master-data/projects/${projectId}/deliverable-definitions/${definitionId}`,
    payload
  );
  return data.data;
}

export async function deleteDeliverableDefinition(projectId: number, definitionId: number) {
  await http.delete<ApiResponse<null>>(`/api/master-data/projects/${projectId}/deliverable-definitions/${definitionId}`);
}

// Deliverable Types
export async function createDeliverableType(projectId: number, payload: DeliverableTypePayload) {
  const { data } = await http.post<ApiResponse<DeliverableType>>(
    `/api/master-data/projects/${projectId}/deliverable-types`,
    payload
  );
  return data.data;
}

export async function fetchDeliverableTypes(projectId: number, definitionId?: number) {
  const params = definitionId ? { definitionId } : {};
  const { data } = await http.get<ApiResponse<DeliverableType[]>>(
    `/api/master-data/projects/${projectId}/deliverable-types`,
    { params }
  );
  return data.data;
}

export async function updateDeliverableType(projectId: number, typeId: number, payload: Partial<DeliverableTypePayload>) {
  const { data } = await http.patch<ApiResponse<DeliverableType>>(
    `/api/master-data/projects/${projectId}/deliverable-types/${typeId}`,
    payload
  );
  return data.data;
}

export async function deleteDeliverableType(projectId: number, typeId: number) {
  await http.delete<ApiResponse<null>>(`/api/master-data/projects/${projectId}/deliverable-types/${typeId}`);
}

// Deliverable Attributes
export async function createDeliverableAttribute(projectId: number, payload: DeliverableAttributePayload) {
  const { data } = await http.post<ApiResponse<DeliverableAttribute>>(
    `/api/master-data/projects/${projectId}/deliverable-attributes`,
    payload
  );
  return data.data;
}

export async function fetchDeliverableAttributes(projectId: number, typeId?: number) {
  const params = typeId ? { typeId } : {};
  const { data } = await http.get<ApiResponse<DeliverableAttribute[]>>(
    `/api/master-data/projects/${projectId}/deliverable-attributes`,
    { params }
  );
  return data.data;
}

export async function updateDeliverableAttribute(projectId: number, attributeId: number, payload: Partial<DeliverableAttributePayload>) {
  const { data } = await http.patch<ApiResponse<DeliverableAttribute>>(
    `/api/master-data/projects/${projectId}/deliverable-attributes/${attributeId}`,
    payload
  );
  return data.data;
}

export async function deleteDeliverableAttribute(projectId: number, attributeId: number) {
  await http.delete<ApiResponse<null>>(`/api/master-data/projects/${projectId}/deliverable-attributes/${attributeId}`);
}

// Directory Templates
export async function createDirectoryTemplate(projectId: number, payload: DirectoryTemplatePayload) {
  const { data } = await http.post<ApiResponse<DirectoryTemplate>>(
    `/api/master-data/projects/${projectId}/directory-templates`,
    payload
  );
  return data.data;
}

export async function fetchDirectoryTemplates(projectId: number) {
  const { data } = await http.get<ApiResponse<DirectoryTemplate[]>>(
    `/api/master-data/projects/${projectId}/directory-templates`
  );
  return data.data;
}

export async function updateDirectoryTemplate(projectId: number, templateId: number, payload: Partial<DirectoryTemplatePayload>) {
  const { data } = await http.patch<ApiResponse<DirectoryTemplate>>(
    `/api/master-data/projects/${projectId}/directory-templates/${templateId}`,
    payload
  );
  return data.data;
}

export async function deleteDirectoryTemplate(projectId: number, templateId: number) {
  await http.delete<ApiResponse<null>>(`/api/master-data/projects/${projectId}/directory-templates/${templateId}`);
}
