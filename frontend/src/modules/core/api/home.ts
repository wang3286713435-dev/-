import { http } from '@/app/http';
import type { ApiResponse, HomeOverview } from './types';

export async function fetchHomeOverview(projectId: number) {
  const { data } = await http.get<ApiResponse<HomeOverview>>(
    `/api/work-center/projects/${projectId}/home/overview`
  );
  return data.data;
}
