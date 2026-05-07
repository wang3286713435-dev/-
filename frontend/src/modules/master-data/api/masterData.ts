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
