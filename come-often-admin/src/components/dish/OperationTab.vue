<script setup lang="ts">
import { onMounted, reactive, ref } from 'vue';
import { ElMessage, ElMessageBox } from 'element-plus';
import {
  bannerApi,
  noticeApi,
  type BannerItem,
  type BannerSavePayload,
  type NoticeItem,
  type NoticeSavePayload,
} from '@/api/dish';

const banners = ref<BannerItem[]>([]);
const notices = ref<NoticeItem[]>([]);
const loading = ref(false);
const bannerDialog = ref(false);
const noticeDialog = ref(false);
const editingBannerId = ref<string | null>(null);
const editingNoticeId = ref<string | null>(null);
const bannerForm = reactive<BannerSavePayload>({ title: '', objectKey: '', jumpType: 'NONE', jumpValue: '', weight: 0 });
const noticeForm = reactive<NoticeSavePayload>({ content: '', noticeType: 'NORMAL' });

const errorMessage = (error: unknown, fallback: string): string =>
  error instanceof Error ? error.message : fallback;

const load = async (): Promise<void> => {
  loading.value = true;
  try {
    [banners.value, notices.value] = await Promise.all([bannerApi.list(), noticeApi.list()]);
  } catch (error) {
    ElMessage.error(errorMessage(error, '运营位加载失败'));
  } finally {
    loading.value = false;
  }
};

const openBanner = (row?: BannerItem): void => {
  editingBannerId.value = row?.id ?? null;
  Object.assign(bannerForm, row
    ? { title: row.title, objectKey: row.objectKey, jumpType: row.jumpType, jumpValue: row.jumpValue, weight: row.weight }
    : { title: '', objectKey: '', jumpType: 'NONE', jumpValue: '', weight: 0 });
  bannerDialog.value = true;
};

const submitBanner = async (): Promise<void> => {
  if (!bannerForm.title.trim() || !bannerForm.objectKey.trim()) {
    ElMessage.warning('请填写标题与图片 objectKey');
    return;
  }
  try {
    if (editingBannerId.value) {
      await bannerApi.update(editingBannerId.value, bannerForm);
    } else {
      await bannerApi.create(bannerForm);
    }
    bannerDialog.value = false;
    await load();
  } catch (error) {
    ElMessage.error(errorMessage(error, '保存失败'));
  }
};

const removeBanner = async (row: BannerItem): Promise<void> => {
  try {
    await ElMessageBox.confirm(`确认删除轮播「${row.title}」？`, '删除轮播', { type: 'warning' });
  } catch {
    return;
  }
  await bannerApi.remove(row.id);
  await load();
};

const openNotice = (row?: NoticeItem): void => {
  editingNoticeId.value = row?.id ?? null;
  Object.assign(noticeForm, row
    ? { content: row.content, noticeType: row.noticeType }
    : { content: '', noticeType: 'NORMAL' });
  noticeDialog.value = true;
};

const submitNotice = async (): Promise<void> => {
  if (!noticeForm.content.trim()) {
    ElMessage.warning('请填写公告内容');
    return;
  }
  try {
    if (editingNoticeId.value) {
      await noticeApi.update(editingNoticeId.value, noticeForm);
    } else {
      await noticeApi.create(noticeForm);
    }
    noticeDialog.value = false;
    await load();
  } catch (error) {
    ElMessage.error(errorMessage(error, '保存失败'));
  }
};

const removeNotice = async (row: NoticeItem): Promise<void> => {
  try {
    await ElMessageBox.confirm('确认删除该公告？', '删除公告', { type: 'warning' });
  } catch {
    return;
  }
  await noticeApi.remove(row.id);
  await load();
};

onMounted(load);
</script>

<template>
  <div v-loading="loading">
    <div class="toolbar">
      <span class="section-title">首页轮播（按权重降序，最多展示 3 张）</span>
      <el-button type="primary" size="small" @click="openBanner()">新增轮播</el-button>
    </div>
    <el-table :data="banners" border empty-text="暂无轮播" class="section-table">
      <el-table-column prop="title" label="标题" min-width="180" />
      <el-table-column prop="objectKey" label="图片 objectKey" min-width="220" />
      <el-table-column prop="jumpType" label="跳转类型" width="120" />
      <el-table-column prop="weight" label="权重" width="90" />
      <el-table-column label="操作" width="150">
        <template #default="{ row }">
          <el-button link type="primary" @click="openBanner(row)">编辑</el-button>
          <el-button link type="danger" @click="removeBanner(row)">删除</el-button>
        </template>
      </el-table-column>
    </el-table>

    <div class="toolbar">
      <span class="section-title">首页公告（单行滚动）</span>
      <el-button type="primary" size="small" @click="openNotice()">新增公告</el-button>
    </div>
    <el-table :data="notices" border empty-text="暂无公告">
      <el-table-column prop="content" label="公告内容" min-width="300" />
      <el-table-column prop="noticeType" label="类型" width="120" />
      <el-table-column label="操作" width="150">
        <template #default="{ row }">
          <el-button link type="primary" @click="openNotice(row)">编辑</el-button>
          <el-button link type="danger" @click="removeNotice(row)">删除</el-button>
        </template>
      </el-table-column>
    </el-table>

    <el-dialog v-model="bannerDialog" title="轮播配置" width="460px">
      <el-form label-width="100px">
        <el-form-item label="标题"><el-input v-model="bannerForm.title" /></el-form-item>
        <el-form-item label="图片 objectKey"><el-input v-model="bannerForm.objectKey" placeholder="dish/1003_detail.webp" /></el-form-item>
        <el-form-item label="跳转类型"><el-input v-model="bannerForm.jumpType" /></el-form-item>
        <el-form-item label="跳转目标"><el-input v-model="bannerForm.jumpValue" /></el-form-item>
        <el-form-item label="权重"><el-input-number v-model="bannerForm.weight" :min="0" /></el-form-item>
      </el-form>
      <template #footer>
        <el-button @click="bannerDialog = false">取消</el-button>
        <el-button type="primary" @click="submitBanner">保存</el-button>
      </template>
    </el-dialog>

    <el-dialog v-model="noticeDialog" title="公告配置" width="460px">
      <el-form label-width="100px">
        <el-form-item label="公告内容"><el-input v-model="noticeForm.content" type="textarea" :rows="2" /></el-form-item>
        <el-form-item label="类型"><el-input v-model="noticeForm.noticeType" /></el-form-item>
      </el-form>
      <template #footer>
        <el-button @click="noticeDialog = false">取消</el-button>
        <el-button type="primary" @click="submitNotice">保存</el-button>
      </template>
    </el-dialog>
  </div>
</template>

<style scoped lang="scss">
.toolbar {
  display: flex;
  align-items: center;
  justify-content: space-between;
  margin: 8px 0 12px;
}

.section-title {
  color: var(--co-text-primary);
  font-weight: 600;
}

.section-table {
  margin-bottom: 24px;
}
</style>
