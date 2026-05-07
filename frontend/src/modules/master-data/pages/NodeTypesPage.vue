<template>
  <section class="master-data-page">
    <div class="master-data-page__header">
      <div>
        <h1>节点类型</h1>
        <p>{{ projectLabel }}</p>
      </div>
      <div class="master-data-page__actions">
        <el-button :icon="Refresh" @click="loadPage">刷新</el-button>
        <el-button :disabled="!nodeTypes.length" @click="handleLockAll">全部锁定</el-button>
        <el-button type="primary" :icon="Plus" @click="openCreateDialog">新增节点类型</el-button>
      </div>
    </div>

    <StandardStatusPanel :status="standardStatus" />

    <el-alert
      class="node-type-lock"
      :type="lockStatus?.allNodeTypesLocked ? 'success' : 'info'"
      :closable="false"
      show-icon
    >
      <template #title>
        节点类型锁定状态：{{ lockStatus?.allNodeTypesLocked ? '全部锁定' : '仍可维护' }}
      </template>
    </el-alert>

    <el-table v-loading="loading" :data="nodeTypes" class="master-table" empty-text="暂无节点类型">
      <el-table-column prop="name" label="名称" min-width="180" />
      <el-table-column prop="code" label="编码" width="180" />
      <el-table-column prop="scopeLevel" label="适用层级" width="110" />
      <el-table-column prop="sortOrder" label="排序" width="90" />
      <el-table-column prop="status" label="状态" width="110">
        <template #default="{ row }">
          <el-tag :type="row.status === 'ACTIVE' ? 'success' : 'info'">{{ row.status }}</el-tag>
        </template>
      </el-table-column>
      <el-table-column prop="locked" label="锁定" width="110">
        <template #default="{ row }">
          <el-tag :type="row.locked ? 'success' : 'warning'">{{ row.locked ? '已锁定' : '未锁定' }}</el-tag>
        </template>
      </el-table-column>
      <el-table-column label="操作" width="190" fixed="right">
        <template #default="{ row }">
          <el-button text :disabled="row.locked" @click="openEditDialog(row)">编辑</el-button>
          <el-button text type="primary" :disabled="row.locked" @click="handleLock(row)">锁定</el-button>
        </template>
      </el-table-column>
    </el-table>

    <el-dialog v-model="dialogVisible" :title="editingNodeType ? '编辑节点类型' : '新增节点类型'" width="520px">
      <el-form label-position="top" class="master-form">
        <el-form-item label="编码">
          <el-input v-model="form.code" maxlength="64" />
        </el-form-item>
        <el-form-item label="名称">
          <el-input v-model="form.name" maxlength="128" />
        </el-form-item>
        <el-form-item label="适用层级">
          <el-input-number v-model="form.scopeLevel" :min="1" controls-position="right" />
        </el-form-item>
        <el-form-item label="排序">
          <el-input-number v-model="form.sortOrder" :min="0" controls-position="right" />
        </el-form-item>
        <el-form-item label="状态">
          <el-segmented v-model="form.status" :options="statusOptions" />
        </el-form-item>
      </el-form>
      <template #footer>
        <el-button @click="dialogVisible = false">取消</el-button>
        <el-button type="primary" :loading="saving" @click="handleSave">保存</el-button>
      </template>
    </el-dialog>
  </section>
</template>

<script setup lang="ts">
import { computed, reactive, ref, watch } from 'vue';
import { ElMessage, ElMessageBox } from 'element-plus';
import { Plus, Refresh } from '@element-plus/icons-vue';

import StandardStatusPanel from '@/modules/master-data/components/StandardStatusPanel.vue';
import {
  createNodeType,
  fetchNodeTypeLockStatus,
  fetchNodeTypes,
  fetchStandardStatus,
  lockAllNodeTypes,
  lockNodeType,
  updateNodeType,
  type NodeType,
  type NodeTypeLockStatus,
  type StandardStatus
} from '@/modules/master-data/api/masterData';
import { useAuthStore } from '@/stores/auth';

const authStore = useAuthStore();
const loading = ref(false);
const saving = ref(false);
const dialogVisible = ref(false);
const nodeTypes = ref<NodeType[]>([]);
const standardStatus = ref<StandardStatus | null>(null);
const lockStatus = ref<NodeTypeLockStatus | null>(null);
const editingNodeType = ref<NodeType | null>(null);

const form = reactive({
  code: '',
  name: '',
  scopeLevel: 1,
  sortOrder: 0,
  status: 'ACTIVE'
});

const statusOptions = [
  { label: '启用', value: 'ACTIVE' },
  { label: '停用', value: 'DISABLED' }
];

const currentProjectId = computed(() => authStore.currentProjectId);
const projectLabel = computed(() => {
  const project = authStore.currentUser?.currentProject;
  return project ? `${project.code} | ${project.name}` : '等待项目上下文';
});

watch(
  currentProjectId,
  () => {
    loadPage();
  },
  { immediate: true }
);

async function loadPage() {
  if (!currentProjectId.value) {
    return;
  }
  loading.value = true;
  try {
    const [status, types, lock] = await Promise.all([
      fetchStandardStatus(currentProjectId.value),
      fetchNodeTypes(currentProjectId.value),
      fetchNodeTypeLockStatus(currentProjectId.value)
    ]);
    standardStatus.value = status;
    nodeTypes.value = types;
    lockStatus.value = lock;
  } catch (error) {
    ElMessage.error(error instanceof Error ? error.message : '节点类型加载失败');
  } finally {
    loading.value = false;
  }
}

function openCreateDialog() {
  editingNodeType.value = null;
  Object.assign(form, {
    code: '',
    name: '',
    scopeLevel: 1,
    sortOrder: 0,
    status: 'ACTIVE'
  });
  dialogVisible.value = true;
}

function openEditDialog(nodeType: NodeType) {
  editingNodeType.value = nodeType;
  Object.assign(form, {
    code: nodeType.code,
    name: nodeType.name,
    scopeLevel: nodeType.scopeLevel,
    sortOrder: nodeType.sortOrder,
    status: nodeType.status
  });
  dialogVisible.value = true;
}

async function handleSave() {
  if (!currentProjectId.value) {
    return;
  }
  saving.value = true;
  try {
    const payload = {
      code: form.code,
      name: form.name,
      scopeLevel: form.scopeLevel,
      sortOrder: form.sortOrder,
      status: form.status
    };
    if (editingNodeType.value) {
      await updateNodeType(currentProjectId.value, editingNodeType.value.id, payload);
      ElMessage.success('节点类型已更新');
    } else {
      await createNodeType(currentProjectId.value, payload);
      ElMessage.success('节点类型已创建');
    }
    dialogVisible.value = false;
    await loadPage();
  } catch (error) {
    ElMessage.error(error instanceof Error ? error.message : '保存失败');
  } finally {
    saving.value = false;
  }
}

async function handleLock(nodeType: NodeType) {
  if (!currentProjectId.value) {
    return;
  }
  try {
    await ElMessageBox.confirm(`确认锁定“${nodeType.name}”？`, '锁定节点类型', {
      type: 'warning',
      confirmButtonText: '锁定',
      cancelButtonText: '取消'
    });
    await lockNodeType(currentProjectId.value, nodeType.id);
    ElMessage.success('节点类型已锁定');
    await loadPage();
  } catch (error) {
    if (error instanceof Error) {
      ElMessage.error(error.message);
    }
  }
}

async function handleLockAll() {
  if (!currentProjectId.value) {
    return;
  }
  try {
    await ElMessageBox.confirm('确认锁定当前项目全部节点类型？', '全部锁定', {
      type: 'warning',
      confirmButtonText: '全部锁定',
      cancelButtonText: '取消'
    });
    await lockAllNodeTypes(currentProjectId.value);
    ElMessage.success('节点类型已全部锁定');
    await loadPage();
  } catch (error) {
    if (error instanceof Error) {
      ElMessage.error(error.message);
    }
  }
}
</script>
