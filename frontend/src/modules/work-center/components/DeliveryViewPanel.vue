<template>
  <section class="mvp-page">
    <div class="mvp-page__header">
      <div>
        <h1>{{ title }}</h1>
        <p>{{ projectLabel }}</p>
      </div>
      <div class="mvp-page__actions">
        <el-button :icon="Refresh" @click="loadPage">刷新</el-button>
        <el-button type="primary" :icon="Link" @click="openCreateDialog">新增挂接</el-button>
      </div>
    </div>

    <section class="mvp-stat-row">
      <article class="mvp-stat">
        <span>视图类型</span>
        <strong>{{ view?.viewType ?? viewType }}</strong>
      </article>
      <article class="mvp-stat">
        <span>挂接总数</span>
        <strong>{{ view?.totalCount ?? 0 }}</strong>
      </article>
      <article class="mvp-stat">
        <span>已绑定</span>
        <strong>{{ view?.boundCount ?? 0 }}</strong>
      </article>
    </section>

    <el-table v-loading="loading" :data="view?.rows ?? []" class="master-table" empty-text="暂无交付挂接">
      <el-table-column prop="deliverableDefinitionName" label="交付定义" min-width="170" />
      <el-table-column prop="deliverableTypeName" label="交付类型" min-width="150" />
      <el-table-column prop="fileName" label="文件" min-width="220" />
      <el-table-column prop="sectionNodeName" label="部位" min-width="140" />
      <el-table-column prop="managedObjectName" label="对象" min-width="140" />
      <el-table-column prop="versionNo" label="版本" width="90" />
      <el-table-column prop="reviewStatus" label="审核" width="100" />
      <el-table-column prop="bindingStatus" label="状态" width="100" />
    </el-table>

    <el-dialog v-model="dialogVisible" :title="`新增${title}挂接`" width="620px">
      <el-form label-position="top" class="master-form">
        <el-form-item label="交付物类型">
          <el-select v-model="form.deliverableTypeId" filterable>
            <el-option v-for="item in deliverableTypes" :key="item.id" :label="`${item.name} | ${item.code}`" :value="item.id" />
          </el-select>
        </el-form-item>
        <el-form-item label="文件资源">
          <el-select v-model="form.fileResourceId" filterable>
            <el-option v-for="file in files" :key="file.id" :label="`${file.originalName} | ${file.processStatus}`" :value="file.id" />
          </el-select>
        </el-form-item>
        <el-form-item label="挂接目标">
          <el-segmented v-model="form.targetMode" :options="targetModeOptions" />
        </el-form-item>
        <el-form-item v-if="form.targetMode === 'SECTION'" label="工程部位">
          <el-select v-model="form.sectionNodeId" filterable>
            <el-option v-for="node in sectionOptions" :key="node.id" :label="node.label" :value="node.id" />
          </el-select>
        </el-form-item>
        <el-form-item v-if="form.targetMode === 'OBJECT'" label="管理对象">
          <el-select v-model="form.managedObjectId" filterable>
            <el-option v-for="object in objects" :key="object.id" :label="`${object.name} | ${object.code}`" :value="object.id" />
          </el-select>
        </el-form-item>
        <el-form-item label="备注">
          <el-input v-model="form.remark" maxlength="512" />
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
import { ElMessage } from 'element-plus';
import { Link, Refresh } from '@element-plus/icons-vue';

import { fetchFileResources, fetchManagedObjects, type FileResource, type ManagedObject } from '@/modules/data-steward/api/dataSteward';
import { fetchDeliverableTypes, fetchSectionTree, type DeliverableType, type SectionNode } from '@/modules/master-data/api/masterData';
import { createDeliveryBinding, fetchDeliveryView, type DeliveryView } from '@/modules/work-center/api/delivery';
import { useAuthStore } from '@/stores/auth';

const props = defineProps<{
  viewType: 'DOCUMENT' | 'DRAWING';
  title: string;
}>();

const authStore = useAuthStore();
const loading = ref(false);
const saving = ref(false);
const dialogVisible = ref(false);
const view = ref<DeliveryView | null>(null);
const files = ref<FileResource[]>([]);
const objects = ref<ManagedObject[]>([]);
const allTypes = ref<DeliverableType[]>([]);
const sections = ref<SectionNode[]>([]);

const form = reactive({
  deliverableTypeId: null as number | null,
  fileResourceId: null as number | null,
  targetMode: 'SECTION',
  sectionNodeId: null as number | null,
  managedObjectId: null as number | null,
  remark: ''
});

const targetModeOptions = [
  { label: '部位', value: 'SECTION' },
  { label: '对象', value: 'OBJECT' }
];

const projectId = computed(() => authStore.currentProjectId);
const projectLabel = computed(() => authStore.currentUser?.currentProject.name ?? '等待项目上下文');
const deliverableTypes = computed(() => allTypes.value.filter((type) => type.fileKind === props.viewType));
const sectionOptions = computed(() => flattenSections(sections.value));

watch(projectId, () => loadPage(), { immediate: true });

async function loadPage() {
  if (!projectId.value) return;
  loading.value = true;
  try {
    const [nextView, nextFiles, nextObjects, nextTypes, nextSections] = await Promise.all([
      fetchDeliveryView(projectId.value, props.viewType),
      fetchFileResources(projectId.value, props.viewType),
      fetchManagedObjects(projectId.value),
      fetchDeliverableTypes(projectId.value),
      fetchSectionTree(projectId.value)
    ]);
    view.value = nextView;
    files.value = nextFiles.filter((file) => file.processStatus === 'PROCESSED');
    objects.value = nextObjects;
    allTypes.value = nextTypes;
    sections.value = nextSections;
  } catch (error) {
    ElMessage.error(error instanceof Error ? error.message : '交付视图加载失败');
  } finally {
    loading.value = false;
  }
}

function openCreateDialog() {
  Object.assign(form, {
    deliverableTypeId: deliverableTypes.value[0]?.id ?? null,
    fileResourceId: files.value[0]?.id ?? null,
    targetMode: 'SECTION',
    sectionNodeId: sectionOptions.value[0]?.id ?? null,
    managedObjectId: objects.value[0]?.id ?? null,
    remark: ''
  });
  dialogVisible.value = true;
}

async function handleSave() {
  if (!projectId.value || !form.deliverableTypeId || !form.fileResourceId) return;
  saving.value = true;
  try {
    await createDeliveryBinding(projectId.value, {
      viewType: props.viewType,
      deliverableTypeId: form.deliverableTypeId,
      fileResourceId: form.fileResourceId,
      sectionNodeId: form.targetMode === 'SECTION' ? form.sectionNodeId : null,
      managedObjectId: form.targetMode === 'OBJECT' ? form.managedObjectId : null,
      bindingStatus: 'BOUND',
      reviewStatus: 'PENDING',
      remark: form.remark
    });
    ElMessage.success('交付挂接已保存');
    dialogVisible.value = false;
    await loadPage();
  } catch (error) {
    ElMessage.error(error instanceof Error ? error.message : '保存失败');
  } finally {
    saving.value = false;
  }
}

function flattenSections(nodes: SectionNode[], prefix = ''): Array<{ id: number; label: string }> {
  return nodes.flatMap((node) => {
    const label = `${prefix}${node.name}`;
    return [{ id: node.id, label }, ...flattenSections(node.children ?? [], `${prefix} / `)];
  });
}
</script>
