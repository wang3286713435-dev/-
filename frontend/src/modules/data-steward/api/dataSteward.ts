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
