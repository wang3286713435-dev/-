import { http } from '@/app/http';
import type { ApiResponse } from '@/modules/core/api/types';

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

export async function createDeliveryBinding(projectId: number, payload: DeliveryBindingPayload) {
  const { data } = await http.post<ApiResponse<DeliveryBinding>>(
    `/api/work-center/projects/${projectId}/delivery-bindings`,
    payload
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
