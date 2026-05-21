<template>
  <section class="mvp-page employee-page">
    <div class="mvp-page__header">
      <div>
        <h1>员工权限</h1>
        <p>管理员工账号状态，并按项目分配查看者、交付工程师或项目管理员角色。</p>
      </div>
      <div class="mvp-page__actions">
        <el-input
          v-model.trim="filters.keyword"
          class="employee-search"
          clearable
          placeholder="姓名、手机号"
          :prefix-icon="Search"
          @keyup.enter="loadEmployees"
          @clear="loadEmployees"
        />
        <el-select v-model="filters.status" class="employee-status-filter" clearable placeholder="全部状态" @change="loadEmployees">
          <el-option label="启用" value="ACTIVE" />
          <el-option label="停用" value="DISABLED" />
        </el-select>
        <el-button :icon="Refresh" @click="loadEmployees">刷新</el-button>
      </div>
    </div>

    <el-alert
      type="info"
      :closable="false"
      show-icon
      title="项目授权采用全量覆盖：保存时，表格里的项目角色就是该员工最终可访问范围。"
    />

    <el-table v-loading="loading" :data="employees" class="master-table" empty-text="暂无员工账号">
      <el-table-column prop="displayName" label="姓名" min-width="140" show-overflow-tooltip />
      <el-table-column prop="phoneNumber" label="手机号" width="150" show-overflow-tooltip>
        <template #default="{ row }">{{ row.phoneNumber || row.username }}</template>
      </el-table-column>
      <el-table-column prop="departmentName" label="部门" min-width="150" show-overflow-tooltip>
        <template #default="{ row }">{{ row.departmentName || '-' }}</template>
      </el-table-column>
      <el-table-column label="状态" width="100">
        <template #default="{ row }">
          <el-tag size="small" :type="statusTagType(row.status)">{{ statusText(row.status) }}</el-tag>
        </template>
      </el-table-column>
      <el-table-column prop="projectCount" label="项目数" width="100" align="right" />
      <el-table-column label="最近登录" width="170">
        <template #default="{ row }">{{ formatDate(row.lastLoginAt) }}</template>
      </el-table-column>
      <el-table-column label="操作" width="300" fixed="right">
        <template #default="{ row }">
          <el-button text :icon="Edit" @click="openEmployee(row.userId)">查看/编辑权限</el-button>
          <el-button
            text
            :type="row.status === 'ACTIVE' ? 'warning' : 'success'"
            :disabled="row.userId === authStore.currentUser?.userId"
            @click="toggleStatus(row)"
          >
            {{ row.status === 'ACTIVE' ? '停用' : '启用' }}
          </el-button>
          <el-button
            text
            type="danger"
            :icon="Delete"
            :disabled="row.userId === authStore.currentUser?.userId"
            @click="deleteAccount(row)"
          >
            删除
          </el-button>
        </template>
      </el-table-column>
    </el-table>

    <el-drawer v-model="drawerVisible" title="员工项目授权" size="760px">
      <div v-if="detail" class="employee-drawer">
        <el-descriptions :column="2" border>
          <el-descriptions-item label="姓名">{{ detail.displayName }}</el-descriptions-item>
          <el-descriptions-item label="手机号">{{ detail.phoneNumber || detail.username }}</el-descriptions-item>
          <el-descriptions-item label="部门">{{ detail.departmentName || '-' }}</el-descriptions-item>
          <el-descriptions-item label="状态">
            <el-tag size="small" :type="statusTagType(detail.status)">{{ statusText(detail.status) }}</el-tag>
          </el-descriptions-item>
        </el-descriptions>

        <div class="employee-drawer__toolbar">
          <strong>项目授权</strong>
          <el-button type="primary" :icon="Plus" @click="addAssignment">添加项目</el-button>
        </div>

        <el-table :data="assignmentDrafts" class="master-table" empty-text="暂无项目授权">
          <el-table-column label="项目" min-width="260">
            <template #default="{ row, $index }">
              <el-select v-model="row.projectId" filterable placeholder="选择项目" class="employee-project-select">
                <el-option
                  v-for="project in assignableProjects"
                  :key="project.id"
                  :label="`${project.code} ${project.name}`"
                  :value="project.id"
                  :disabled="isProjectSelected(project.id, $index)"
                />
              </el-select>
            </template>
          </el-table-column>
          <el-table-column label="角色" min-width="180">
            <template #default="{ row }">
              <el-select v-model="row.roleCode" placeholder="选择角色" class="employee-role-select">
                <el-option
                  v-for="role in roleOptions"
                  :key="role.code"
                  :label="`${role.name}：${role.description}`"
                  :value="role.code"
                />
              </el-select>
            </template>
          </el-table-column>
          <el-table-column label="操作" width="100" fixed="right">
            <template #default="{ $index }">
              <el-button text type="danger" :icon="Delete" @click="removeAssignment($index)">删除</el-button>
            </template>
          </el-table-column>
        </el-table>

        <div class="employee-drawer__footer">
          <el-button @click="drawerVisible = false">取消</el-button>
          <el-button type="primary" :loading="saving" @click="saveAssignments">保存授权</el-button>
        </div>
      </div>
    </el-drawer>
  </section>
</template>

<script setup lang="ts">
import { computed, onMounted, reactive, ref } from 'vue';
import { ElMessage, ElMessageBox } from 'element-plus';
import { Delete, Edit, Plus, Refresh, Search } from '@element-plus/icons-vue';

import {
  deleteEmployee,
  fetchAssignableProjects,
  fetchEmployeeDetail,
  fetchEmployees,
  fetchProjectRoleOptions,
  updateEmployeeProjectRoles,
  updateEmployeeStatus
} from '@/modules/core/api/employees';
import type {
  AssignableProject,
  EmployeeDetail,
  EmployeeSummary,
  ProjectRoleOption
} from '@/modules/core/api/types';
import { useAuthStore } from '@/stores/auth';

type EmployeeStatus = 'ACTIVE' | 'DISABLED';
type ProjectRoleCode = ProjectRoleOption['code'];

interface AssignmentDraft {
  projectId: number | null;
  roleCode: ProjectRoleCode;
}

const authStore = useAuthStore();

const filters = reactive({
  keyword: '',
  status: '' as '' | EmployeeStatus
});
const loading = ref(false);
const saving = ref(false);
const drawerVisible = ref(false);
const employees = ref<EmployeeSummary[]>([]);
const detail = ref<EmployeeDetail | null>(null);
const assignableProjects = ref<AssignableProject[]>([]);
const roleOptions = ref<ProjectRoleOption[]>([]);
const assignmentDrafts = ref<AssignmentDraft[]>([]);

const firstAvailableProject = computed(() => {
  const selected = new Set(assignmentDrafts.value.map((item) => item.projectId).filter(Boolean));
  return assignableProjects.value.find((project) => !selected.has(project.id)) ?? null;
});

onMounted(async () => {
  await Promise.all([loadEmployees(), loadOptions()]);
});

async function loadEmployees() {
  loading.value = true;
  try {
    employees.value = await fetchEmployees({
      keyword: filters.keyword || undefined,
      status: filters.status
    });
  } catch (error) {
    ElMessage.error(error instanceof Error ? error.message : '员工列表加载失败');
  } finally {
    loading.value = false;
  }
}

async function loadOptions() {
  try {
    const [projects, roles] = await Promise.all([fetchAssignableProjects(), fetchProjectRoleOptions()]);
    assignableProjects.value = projects;
    roleOptions.value = roles;
  } catch (error) {
    ElMessage.error(error instanceof Error ? error.message : '授权选项加载失败');
  }
}

async function openEmployee(userId: number) {
  try {
    detail.value = await fetchEmployeeDetail(userId);
    assignmentDrafts.value = detail.value.projectRoles.map((item) => ({
      projectId: item.projectId,
      roleCode: item.roleCode
    }));
    drawerVisible.value = true;
  } catch (error) {
    ElMessage.error(error instanceof Error ? error.message : '员工详情加载失败');
  }
}

function addAssignment() {
  const project = firstAvailableProject.value;
  if (!project) {
    ElMessage.info('没有更多可授权项目');
    return;
  }
  assignmentDrafts.value.push({
    projectId: project.id,
    roleCode: 'PROJECT_VIEWER'
  });
}

function removeAssignment(index: number) {
  assignmentDrafts.value.splice(index, 1);
}

function isProjectSelected(projectId: number, rowIndex: number) {
  return assignmentDrafts.value.some((item, index) => index !== rowIndex && item.projectId === projectId);
}

async function saveAssignments() {
  if (!detail.value) return;
  const invalid = assignmentDrafts.value.find((item) => !item.projectId || !item.roleCode);
  if (invalid) {
    ElMessage.error('请补齐项目和角色');
    return;
  }
  saving.value = true;
  try {
    const updated = await updateEmployeeProjectRoles(detail.value.userId, {
      assignments: assignmentDrafts.value.map((item) => ({
        projectId: Number(item.projectId),
        roleCode: item.roleCode
      }))
    });
    detail.value = updated;
    assignmentDrafts.value = updated.projectRoles.map((item) => ({
      projectId: item.projectId,
      roleCode: item.roleCode
    }));
    await loadEmployees();
    ElMessage.success('项目授权已保存');
  } catch (error) {
    ElMessage.error(error instanceof Error ? error.message : '项目授权保存失败');
  } finally {
    saving.value = false;
  }
}

async function toggleStatus(row: EmployeeSummary) {
  const nextStatus: EmployeeStatus = row.status === 'ACTIVE' ? 'DISABLED' : 'ACTIVE';
  const action = nextStatus === 'DISABLED' ? '停用' : '启用';
  try {
    await ElMessageBox.confirm(
      nextStatus === 'DISABLED' ? '停用后该员工不能继续登录平台。' : '启用后该员工可重新登录平台。',
      `${action}员工`,
      { type: nextStatus === 'DISABLED' ? 'warning' : 'info' }
    );
    await updateEmployeeStatus(row.userId, nextStatus);
    await loadEmployees();
    if (detail.value?.userId === row.userId) {
      detail.value = await fetchEmployeeDetail(row.userId);
    }
    ElMessage.success(`已${action}员工`);
  } catch (error) {
    if (error !== 'cancel') {
      ElMessage.error(error instanceof Error ? error.message : `${action}失败`);
    }
  }
}

async function deleteAccount(row: EmployeeSummary) {
  try {
    await ElMessageBox.confirm(
      '删除后该员工不能登录，已有项目授权也会失效。平台只做账号软删除，不删除项目数据或 NAS 文件。',
      '删除员工账号',
      { type: 'warning', confirmButtonText: '删除账号', cancelButtonText: '取消' }
    );
    await deleteEmployee(row.userId);
    if (detail.value?.userId === row.userId) {
      drawerVisible.value = false;
      detail.value = null;
    }
    await loadEmployees();
    ElMessage.success('员工账号已删除');
  } catch (error) {
    if (error !== 'cancel') {
      ElMessage.error(error instanceof Error ? error.message : '删除失败');
    }
  }
}

function statusText(status: EmployeeStatus) {
  return status === 'ACTIVE' ? '启用' : '停用';
}

function statusTagType(status: EmployeeStatus) {
  return status === 'ACTIVE' ? 'success' : 'info';
}

function formatDate(value: string | null) {
  if (!value) return '-';
  const date = new Date(value);
  if (Number.isNaN(date.getTime())) return '-';
  return date.toLocaleString('zh-CN', { hour12: false });
}
</script>
