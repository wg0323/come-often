<script setup lang="ts">
import { reactive, ref, watch } from 'vue';
import type { UploadRequestOptions } from 'element-plus';
import { ElMessage } from 'element-plus';
import {
  dishApi,
  uploadImage,
  type AddonPayload,
  type DishCategory,
  type DishSavePayload,
  type ImagePayload,
  type SpecGroupPayload,
} from '@/api/dish';
import { buildImageUrl } from '@/utils/image';

interface FormState {
  dishName: string;
  categoryId: string;
  price: number;
  originalPrice: number | null;
  subtitle: string;
  description: string;
  allergen: string;
  craftInfo: string;
  monthlySales: number;
  preorder: boolean;
  status: number;
  sort: number;
  stockTotal: number;
  specGroups: SpecGroupPayload[];
  addons: AddonPayload[];
  images: ImagePayload[];
}

const props = defineProps<{
  modelValue: boolean;
  dishId: string | null;
  categories: DishCategory[];
}>();

const emit = defineEmits<{
  (event: 'update:modelValue', value: boolean): void;
  (event: 'saved'): void;
}>();

const loading = ref(false);
const submitting = ref(false);

const emptyForm = (): FormState => ({
  dishName: '',
  categoryId: props.categories[0]?.id ?? '',
  price: 0,
  originalPrice: null,
  subtitle: '',
  description: '',
  allergen: '',
  craftInfo: '',
  monthlySales: 0,
  preorder: false,
  status: 1,
  sort: 0,
  stockTotal: 50,
  specGroups: [],
  addons: [],
  images: [
    { objectKey: '', imageType: 'LIST', sort: 1 },
    { objectKey: '', imageType: 'DETAIL', sort: 2 },
    { objectKey: '', imageType: 'THUMB', sort: 3 },
  ],
});

const form = reactive<FormState>(emptyForm());

const reset = (): void => {
  Object.assign(form, emptyForm());
};

const loadDetail = async (dishId: string): Promise<void> => {
  loading.value = true;
  try {
    const detail = await dishApi.detail(dishId);
    Object.assign(form, {
      dishName: detail.dishName,
      categoryId: detail.categoryId,
      price: detail.price,
      originalPrice: detail.originalPrice ?? null,
      subtitle: detail.subtitle ?? '',
      description: detail.description ?? '',
      allergen: detail.allergen ?? '',
      craftInfo: detail.craftInfo ?? '',
      monthlySales: detail.monthlySales ?? 0,
      preorder: detail.preorder ?? false,
      status: detail.status ?? 1,
      sort: detail.sort ?? 0,
      specGroups: detail.specGroups ?? [],
      addons: detail.addons ?? [],
    });
    const images = emptyForm().images;
    (detail.images ?? []).forEach((image) => {
      const target = images.find((item) => item.imageType === image.imageType);
      if (target) {
        target.objectKey = image.objectKey;
      }
    });
    form.images = images;
  } finally {
    loading.value = false;
  }
};

watch(
  () => props.modelValue,
  async (open) => {
    if (!open) {
      return;
    }
    reset();
    if (props.dishId) {
      try {
        await loadDetail(props.dishId);
      } catch (error) {
        ElMessage.error(error instanceof Error ? error.message : '菜品详情加载失败');
      }
    }
  },
);

const close = (): void => {
  emit('update:modelValue', false);
};

const addSpecGroup = (): void => {
  form.specGroups.push({ specName: '', required: true, options: [] });
};

const addSpecOption = (group: SpecGroupPayload): void => {
  group.options.push({ optionName: '', priceDelta: 0 });
};

const addAddon = (): void => {
  form.addons.push({ addonName: '', priceDelta: 0 });
};

const uploadHandler = (index: number) => (options: UploadRequestOptions): Promise<void> =>
  handleUpload(index, options);

const handleUpload = async (index: number, options: UploadRequestOptions): Promise<void> => {
  try {
    const result = await uploadImage(options.file as File, 'dish');
    form.images[index].objectKey = result.objectKey;
    ElMessage.success('图片已上传并回填 objectKey');
  } catch (error) {
    ElMessage.error(error instanceof Error ? error.message : '图片上传失败');
  }
};

const submit = async (): Promise<void> => {
  if (!form.dishName.trim() || !form.categoryId || form.price < 0) {
    ElMessage.warning('请填写菜名、分类与合法售价');
    return;
  }
  if (form.specGroups.some((group) => !group.specName.trim() || group.options.length === 0)) {
    ElMessage.warning('每个规格组都要填名称并至少有一个选项');
    return;
  }
  submitting.value = true;
  try {
    const payload: DishSavePayload = {
      dishName: form.dishName.trim(),
      categoryId: form.categoryId,
      price: form.price,
      originalPrice: form.originalPrice,
      subtitle: form.subtitle,
      description: form.description,
      allergen: form.allergen,
      craftInfo: form.craftInfo,
      monthlySales: form.monthlySales,
      preorder: form.preorder,
      status: form.status,
      sort: form.sort,
      specGroups: form.specGroups,
      addons: form.addons,
      images: form.images.filter((image) => image.objectKey.trim() !== ''),
      tagIds: [],
    };
    if (!props.dishId) {
      payload.stockTotal = form.stockTotal;
    }
    if (props.dishId) {
      await dishApi.update(props.dishId, payload);
    } else {
      await dishApi.create(payload);
    }
    ElMessage.success(props.dishId ? '菜品已更新' : '菜品已上架');
    emit('saved');
    close();
  } catch (error) {
    ElMessage.error(error instanceof Error ? error.message : '保存失败');
  } finally {
    submitting.value = false;
  }
};
</script>

<template>
  <el-dialog
    :model-value="modelValue"
    :title="dishId ? '编辑菜品' : '新增菜品并上架'"
    width="820px"
    top="6vh"
    @update:model-value="close"
  >
    <div v-loading="loading">
      <el-form label-width="110px">
        <el-form-item label="菜品名称">
          <el-input v-model="form.dishName" placeholder="如 爽口青笋丝" />
        </el-form-item>
        <el-form-item label="所属分类">
          <el-select v-model="form.categoryId" placeholder="请选择分类" style="width: 100%">
            <el-option
              v-for="category in categories"
              :key="category.id"
              :label="category.storageName"
              :value="category.id"
            />
          </el-select>
        </el-form-item>
        <el-form-item label="售价（元）">
          <el-input-number v-model="form.price" :min="0" :precision="2" :step="1" />
        </el-form-item>
        <el-form-item label="划线价（元）">
          <el-input-number v-model="form.originalPrice" :min="0" :precision="2" :step="1" />
        </el-form-item>
        <el-form-item label="副标题">
          <el-input v-model="form.subtitle" />
        </el-form-item>
        <el-form-item label="菜品描述">
          <el-input v-model="form.description" type="textarea" :rows="2" />
        </el-form-item>
        <el-form-item label="食材与过敏原">
          <el-input v-model="form.allergen" />
        </el-form-item>
        <el-form-item label="技艺说明">
          <el-input v-model="form.craftInfo" placeholder="预定菜展示该项替代月售" />
        </el-form-item>
        <el-form-item label="月售 / 排序">
          <el-input-number v-model="form.monthlySales" :min="0" />
          <el-input-number v-model="form.sort" :min="0" />
        </el-form-item>
        <el-form-item label="状态">
          <el-switch v-model="form.status" :active-value="1" :inactive-value="0" active-text="上架" inactive-text="下架" />
        </el-form-item>
        <el-form-item label="需提前预定">
          <el-switch v-model="form.preorder" />
        </el-form-item>
        <el-form-item v-if="!dishId" label="当日备货">
          <el-input-number v-model="form.stockTotal" :min="0" />
        </el-form-item>

        <el-form-item label="规格组">
          <div class="editor-block">
            <div v-for="(group, groupIndex) in form.specGroups" :key="groupIndex" class="editor-row">
              <el-input v-model="group.specName" placeholder="规格名，如份量" class="editor-name" />
              <el-switch v-model="group.required" active-text="必选" inactive-text="可选" />
              <el-button link type="danger" @click="form.specGroups.splice(groupIndex, 1)">移除</el-button>
              <div v-for="(option, optionIndex) in group.options" :key="optionIndex" class="editor-option">
                <el-input v-model="option.optionName" placeholder="选项名，如大份" class="editor-name" />
                <el-input-number v-model="option.priceDelta" :precision="2" :step="1" />
                <span class="editor-hint">元加价</span>
                <el-button link type="danger" @click="group.options.splice(optionIndex, 1)">移除</el-button>
              </div>
              <el-button size="small" @click="addSpecOption(group)">加选项</el-button>
            </div>
            <el-button size="small" type="primary" plain @click="addSpecGroup">加规格组</el-button>
          </div>
        </el-form-item>

        <el-form-item label="加料">
          <div class="editor-block">
            <div v-for="(addon, index) in form.addons" :key="index" class="editor-row">
              <el-input v-model="addon.addonName" placeholder="加料名，如加香菜" class="editor-name" />
              <el-input-number v-model="addon.priceDelta" :precision="2" :step="1" />
              <span class="editor-hint">元加价</span>
              <el-button link type="danger" @click="form.addons.splice(index, 1)">移除</el-button>
            </div>
            <el-button size="small" type="primary" plain @click="addAddon">加加料</el-button>
          </div>
        </el-form-item>

        <el-form-item label="菜品图">
          <div class="editor-block">
            <div v-for="(image, index) in form.images" :key="image.imageType" class="editor-row">
              <span class="image-type">{{ image.imageType }}</span>
              <el-image
                v-if="image.objectKey"
                :src="buildImageUrl(image.objectKey)"
                class="image-preview"
                fit="cover"
              />
              <el-input v-model="image.objectKey" placeholder="dish/1001_list.webp" class="editor-key" />
              <el-upload :show-file-list="false" :http-request="uploadHandler(index)">
                <el-button size="small">上传</el-button>
              </el-upload>
            </div>
          </div>
        </el-form-item>
      </el-form>
    </div>
    <template #footer>
      <el-button @click="close">取消</el-button>
      <el-button type="primary" :loading="submitting" @click="submit">保存</el-button>
    </template>
  </el-dialog>
</template>

<style scoped lang="scss">
.editor-block {
  width: 100%;
}

.editor-row {
  display: flex;
  flex-wrap: wrap;
  align-items: center;
  gap: 8px;
  margin-bottom: 10px;
  padding: 10px;
  border: 1px solid var(--co-divider);
  border-radius: var(--co-radius-tag);
}

.editor-option {
  display: flex;
  align-items: center;
  gap: 8px;
  width: 100%;
  padding-left: 16px;
}

.editor-name {
  width: 180px;
}

.editor-key {
  width: 260px;
}

.editor-hint,
.image-type {
  width: 62px;
  color: var(--co-text-secondary);
  font-size: 12px;
}

.image-preview {
  width: 60px;
  height: 45px;
  border-radius: var(--co-radius-tag);
}
</style>
