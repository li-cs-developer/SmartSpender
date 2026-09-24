<script setup lang="ts">
import { reactive, ref } from 'vue'
import InputText from 'primevue/inputtext'
import Button from 'primevue/button'
import Message from 'primevue/message'
import { useCategoriesStore } from '@/stores/categories'
import { toUserMessage } from '@/utils/errorMessages'
import type { Category } from '@/types/category'

const emit = defineEmits<{ created: [Category]; cancel: [] }>()
const categories = useCategoriesStore()

const form = reactive({ name: '', color: '#6366f1' })
const submitting = ref(false)
const errorMsg = ref<string | null>(null)

async function save() {
  const name = form.name.trim()
  if (!name) { errorMsg.value = 'Name is required.'; return }
  submitting.value = true
  errorMsg.value = null
  try {
    const created = await categories.create({ name, icon: 'pi pi-tag', color: form.color })
    emit('created', created)
  } catch (e) {
    errorMsg.value = toUserMessage(e)
  } finally {
    submitting.value = false
  }
}
</script>

<template>
  <div class="quick-create">
    <div class="field">
      <label for="qc-name">Name</label>
      <InputText id="qc-name" v-model="form.name" placeholder="e.g. Groceries" maxlength="80" autofocus @keydown.enter.prevent="save" />
    </div>
    <div class="field">
      <label for="qc-color">Color</label>
      <div class="color-row">
        <input id="qc-color" type="color" v-model="form.color" class="color-input" />
        <span class="color-value">{{ form.color }}</span>
      </div>
    </div>
    <Message v-if="errorMsg" severity="error" :closable="false">{{ errorMsg }}</Message>
    <div class="actions">
      <Button label="Cancel" text severity="secondary" size="small" @click="emit('cancel')" :disabled="submitting" />
      <Button label="Save" icon="pi pi-check" size="small" :loading="submitting" @click="save" />
    </div>
  </div>
</template>

<style scoped>
.quick-create {
  display: flex;
  flex-direction: column;
  gap: var(--space-3);
  padding: var(--space-3) var(--space-4);
  background: var(--color-surface-2);
  border: 1px solid var(--color-border);
  border-radius: var(--radius-sm);
}
.field { display: flex; flex-direction: column; gap: var(--space-1); }
.field label { font-size: var(--text-xs); font-weight: 500; color: var(--color-text-muted); }
.color-row { display: flex; align-items: center; gap: var(--space-3); }
.color-input { width: 40px; height: 32px; padding: 2px; border: 1px solid var(--color-border); border-radius: var(--radius-sm); cursor: pointer; background: transparent; }
.color-value { font-family: var(--font-mono); font-size: var(--text-sm); color: var(--color-text-muted); }
.actions { display: flex; justify-content: flex-end; gap: var(--space-2); }
</style>