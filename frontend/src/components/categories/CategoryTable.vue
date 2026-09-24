<script setup lang="ts">
import DataTable from 'primevue/datatable'
import Column from 'primevue/column'
import Button from 'primevue/button'
import type { Category } from '@/types/category'

defineProps<{
  categories: Category[]
  loading?: boolean
}>()

const emit = defineEmits<{
  edit: [category: Category]
  delete: [category: Category]
}>()
</script>

<template>
  <DataTable :value="categories" :loading="loading" striped-rows>
    <template #empty>
      <div class="empty">No categories yet. Create one to get started.</div>
    </template>

    <Column header="Color" style="width: 80px;">
      <template #body="{ data }: { data: Category }">
        <span class="color-dot" :style="{ background: data.color }" />
      </template>
    </Column>

    <Column field="name" header="Name" sortable style="min-width: 200px;">
      <template #body="{ data }: { data: Category }">
        <span class="text-truncate" :title="data.name">{{ data.name }}</span>
      </template>
    </Column>

    <Column field="icon" header="Icon" style="width: 200px;">
      <template #body="{ data }: { data: Category }">
        <span class="icon-cell">
          <i :class="data.icon" />
          <code class="icon-class">{{ data.icon }}</code>
        </span>
      </template>
    </Column>

    <Column field="color" header="Hex" style="width: 120px;">
      <template #body="{ data }: { data: Category }">
        <code>{{ data.color }}</code>
      </template>
    </Column>

    <Column header="Actions" style="width: 120px;">
      <template #body="{ data }: { data: Category }">
        <div class="actions">
          <Button
            icon="pi pi-pencil"
            text
            rounded
            size="small"
            aria-label="Edit"
            @click="emit('edit', data)"
          />
          <Button
            icon="pi pi-trash"
            text
            rounded
            size="small"
            severity="danger"
            aria-label="Delete"
            @click="emit('delete', data)"
          />
        </div>
      </template>
    </Column>
  </DataTable>
</template>

<style scoped>
.empty {
  padding: var(--space-8) var(--space-4);
  text-align: center;
  color: var(--color-text-muted);
}

.color-dot {
  display: inline-block;
  width: 20px;
  height: 20px;
  border-radius: 50%;
  border: 1px solid rgba(0, 0, 0, 0.08);
}

.icon-cell {
  display: inline-flex;
  align-items: center;
  gap: var(--space-3);
}

.icon-class {
  font-size: var(--text-xs);
  color: var(--color-text-muted);
  background: var(--color-surface-2);
  padding: 2px 6px;
  border-radius: var(--radius-sm);
}

.actions {
  display: inline-flex;
  gap: var(--space-1);
}

code {
  font-family: var(--font-mono);
  font-size: var(--text-sm);
}
</style>