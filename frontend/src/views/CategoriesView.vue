<script setup lang="ts">
import { onMounted, ref } from 'vue'
import Button from 'primevue/button'
import ConfirmDialog from 'primevue/confirmdialog'
import { useConfirm } from 'primevue/useconfirm'
import CategoryTable from '@/components/categories/CategoryTable.vue'
import CategoryFormDialog from '@/components/categories/CategoryFormDialog.vue'
import { useCategoriesStore } from '@/stores/categories'
import { toUserMessage } from '@/utils/errorMessages'
import type { Category } from '@/types/category'

const store = useCategoriesStore()
const confirm = useConfirm()

const formVisible = ref(false)
const editing = ref<Category | null>(null)
const errorMsg = ref<string | null>(null)

onMounted(() => store.load(true))

function openCreate() {
  editing.value = null
  errorMsg.value = null
  formVisible.value = true
}

function openEdit(cat: Category) {
  editing.value = cat
  errorMsg.value = null
  formVisible.value = true
}

function onDelete(cat: Category) {
  confirm.require({
    message: `Delete the "${cat.name}" category?`,
    header: 'Delete Category',
    icon: 'pi pi-exclamation-triangle',
    rejectProps: { label: 'Cancel', severity: 'secondary', outlined: true },
    acceptProps: { label: 'Delete', severity: 'danger' },
    accept: async () => {
      errorMsg.value = null
      try {
        await store.remove(cat.id)
      } catch (e) {
        // Backend returns CATEGORY_IN_USE for categories with transactions.
        errorMsg.value = toUserMessage(e)
      }
    },
  })
}
</script>

<template>
  <div class="sp-stack-lg">
    <header class="page-header">
      <div>
        <h1>Categories</h1>
        <p class="sp-text-muted sub">
          Each category has a unique name and a unique color per user.
        </p>
      </div>
      <Button label="New Category" icon="pi pi-plus" @click="openCreate" />
    </header>

    <p v-if="errorMsg" class="error">{{ errorMsg }}</p>

    <div class="sp-card">
      <CategoryTable
        :categories="store.items"
        :loading="store.loading"
        @edit="openEdit"
        @delete="onDelete"
      />
    </div>

    <CategoryFormDialog
      v-model:visible="formVisible"
      :existing="editing"
    />

    <ConfirmDialog />
  </div>
</template>

<style scoped>
.page-header {
  display: flex;
  align-items: flex-start;
  justify-content: space-between;
  gap: var(--space-4);
  flex-wrap: wrap;
}

.page-header h1 {
  font-size: var(--text-xl);
  margin-bottom: var(--space-1);
}

.sub {
  margin: 0;
  font-size: var(--text-sm);
}

.error {
  color: var(--color-danger);
  font-size: var(--text-sm);
}
</style>