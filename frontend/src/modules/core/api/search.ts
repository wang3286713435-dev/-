import { http } from '@/app/http';
import type { ApiResponse } from '@/modules/core/api/types';

export type GlobalSearchGroupType = 'PROJECT' | 'FILE' | 'MODEL' | 'DELIVERY' | 'RECTIFICATION';

export interface GlobalSearchItem {
  type: GlobalSearchGroupType;
  id: string;
  projectId: number;
  projectCode: string;
  projectName: string;
  title: string;
  subtitle: string;
  status: string;
  routeName: string;
  routeParams: Record<string, string | number | boolean>;
  routeQuery: Record<string, string | number | boolean>;
}

export interface GlobalSearchGroup {
  type: GlobalSearchGroupType;
  label: string;
  count: number;
  items: GlobalSearchItem[];
}

export interface GlobalSearchResponse {
  keyword: string;
  projectId: number | null;
  totalCount: number;
  groups: GlobalSearchGroup[];
}

export async function fetchGlobalSearch(params: {
  keyword: string;
  projectId?: number | null;
  limit?: number;
}) {
  const { data } = await http.get<ApiResponse<GlobalSearchResponse>>('/api/core/search/global', {
    params: {
      keyword: params.keyword,
      projectId: params.projectId ?? undefined,
      limit: params.limit ?? 5
    }
  });
  return data.data;
}
