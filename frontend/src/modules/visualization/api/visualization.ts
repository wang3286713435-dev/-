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

export async function fetchVisualizationContext(projectId: number) {
  const { data } = await http.get<ApiResponse<VisualizationContext>>(
    `/api/visualization-adapter/projects/${projectId}/context`
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
