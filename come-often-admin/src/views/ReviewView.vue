<script setup lang="ts">
import { onMounted, ref } from 'vue';
import { ElMessage } from 'element-plus';
import { reviewApi } from '@/api/review';
import type { ReviewItem } from '@/api/review';
import { buildImageUrl } from '@/utils/image';

const STATUS_TABS = [
  { label: '全部', value: '' },
  { label: '待审核', value: 'PENDING' },
  { label: '已通过', value: 'APPROVED' },
  { label: '已拒绝', value: 'REJECTED' },
];

const loading = ref(false);
const rows = ref<ReviewItem[]>([]);
const total = ref(0);
const current = ref(1);
const size = ref(10);
const activeStatus = ref('');
const detailVisible = ref(false);
const detail = ref<ReviewItem | null>(null);

const errorMessage = (error: unknown, fallback: string): string =>
  error instanceof Error ? error.message : fallback;

const loadReviews = async (): Promise<void> => {
  loading.value = true;
  try {
    const page = await reviewApi.page({
      current: current.value,
      size: size.value,
      status: activeStatus.value,
    });
    rows.value = page.records;
    total.value = page.total;
  } catch (error) {
    ElMessage.error(errorMessage(error, '评价列表加载失败'));
  } finally {
    loading.value = false;
  }
};

const onQuery = async (): Promise<void> => {
  current.value = 1;
  await loadReviews();
};

const openDetail = async (row: ReviewItem): Promise<void> => {
  try {
    detail.value = await reviewApi.detail(row.id);
    detailVisible.value = true;
  } catch (error) {
    ElMessage.error(errorMessage(error, '评价详情加载失败'));
  }
};

/** 先发后审：新建评价默认 APPROVED，拒绝后从评分聚合与前台评价列表剔除 */
const onAudit = async (row: ReviewItem, status: string): Promise<void> => {
  try {
    await reviewApi.audit(row.id, status);
    ElMessage.success(status === 'APPROVED' ? '已通过' : status === 'REJECTED' ? '已拒绝' : '已转待复核');
    await loadReviews();
  } catch (error) {
    ElMessage.error(errorMessage(error, '审核失败'));
  }
};

onMounted(async () => {
  await loadReviews();
});
</script>

<template>
  <div v-loading="loading">
    <h1 class="title">评价审核</h1>
    <p class="subtitle">
      先发后审：新建评价默认通过，历史评价兼容；只有通过的评价计入菜品评分与前台评价列表。
      被拒绝的评价仍占「一单一评」，不开放重复提交。
    </p>
    <el-card class="page-card" shadow="never">
      <div class="toolbar">
        <el-radio-group v-model="activeStatus" @change="onQuery">
          <el-radio-button v-for="tab in STATUS_TABS" :key="tab.value" :label="tab.value">
            {{ tab.label }}
          </el-radio-button>
        </el-radio-group>
        <span class="muted">共 {{ total }} 条评价</span>
      </div>

      <el-table :data="rows" border empty-text="暂无评价">
        <el-table-column prop="orderNo" label="订单号" width="210" />
        <el-table-column prop="dishName" label="关联菜品" min-width="130" />
        <el-table-column prop="nickname" label="会员" width="110" />
        <el-table-column prop="rating" label="评分" width="80" />
        <el-table-column prop="content" label="内容" min-width="220" show-overflow-tooltip />
        <el-table-column prop="statusText" label="状态" width="100" />
        <el-table-column prop="createTime" label="评价时间" width="170" />
        <el-table-column label="操作" width="260" fixed="right">
          <template #default="{ row }">
            <el-button link type="primary" @click="openDetail(row)">详情</el-button>
            <el-button
              v-if="row.status !== 'APPROVED'"
              link
              type="success"
              @click="onAudit(row, 'APPROVED')"
            >通过</el-button>
            <el-button
              v-if="row.status !== 'REJECTED'"
              link
              type="danger"
              @click="onAudit(row, 'REJECTED')"
            >拒绝</el-button>
            <el-button
              v-if="row.status !== 'PENDING'"
              link
              type="warning"
              @click="onAudit(row, 'PENDING')"
            >待复核</el-button>
          </template>
        </el-table-column>
      </el-table>
      <el-pagination
        class="pager"
        layout="prev, pager, next"
        :total="total"
        :current-page="current"
        :page-size="size"
        @current-change="
          (page: number) => {
            current = page;
            loadReviews();
          }
        "
      />
    </el-card>

    <el-dialog v-model="detailVisible" title="评价详情" width="600px">
      <template v-if="detail">
        <el-descriptions :column="1" border>
          <el-descriptions-item label="订单号">{{ detail.orderNo }}</el-descriptions-item>
          <el-descriptions-item label="菜品">{{ detail.dishName || '未指定菜品' }}</el-descriptions-item>
          <el-descriptions-item label="会员">{{ detail.nickname }}</el-descriptions-item>
          <el-descriptions-item label="评分">{{ detail.rating }} 分</el-descriptions-item>
          <el-descriptions-item label="状态">{{ detail.statusText }}</el-descriptions-item>
          <el-descriptions-item label="内容">{{ detail.content }}</el-descriptions-item>
          <el-descriptions-item label="审核时间">
            {{ detail.auditTime || '未审核' }}
          </el-descriptions-item>
        </el-descriptions>
        <div v-if="detail.imageObjectKeys.length > 0" class="image-row">
          <el-image
            v-for="key in detail.imageObjectKeys"
            :key="key"
            class="review-image"
            :src="buildImageUrl(key)"
            fit="cover"
          />
        </div>
      </template>
    </el-dialog>
  </div>
</template>

<style scoped lang="scss">
.title {
  margin: 0;
  color: var(--co-text-primary);
  font-size: 28px;
  font-weight: 600;
}

.subtitle {
  margin: 8px 0 16px;
  color: var(--co-text-secondary);
  font-size: 14px;
}

.page-card {
  border: 1px solid var(--co-divider);
  border-radius: var(--co-radius-card);
}

.toolbar {
  display: flex;
  align-items: center;
  gap: 12px;
  margin-bottom: 12px;
}

.muted {
  color: var(--co-text-tertiary);
  font-size: 12px;
}

.image-row {
  display: flex;
  gap: 12px;
  margin-top: 16px;
}

.review-image {
  width: 120px;
  height: 120px;
  border-radius: var(--co-radius-card);
}

.pager {
  display: flex;
  justify-content: flex-end;
  margin-top: 12px;
}
</style>
