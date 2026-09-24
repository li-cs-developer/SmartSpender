<script setup lang="ts">
import { computed, reactive, ref, watch } from 'vue'
import Dialog from 'primevue/dialog'
import InputText from 'primevue/inputtext'
import Button from 'primevue/button'
import Message from 'primevue/message'
import { useCategoriesStore } from '@/stores/categories'
import { toUserMessage } from '@/utils/errorMessages'
import type { Category, CategoryRequest } from '@/types/category'

const visible = defineModel<boolean>('visible', { required: true })

const props = defineProps<{
  /** When set, the dialog edits this category. Otherwise, creates a new one. */
  existing?: Category | null
}>()

const emit = defineEmits<{ saved: [] }>()

const store = useCategoriesStore()

const form = reactive<CategoryRequest>({
  name: '',
  icon: 'pi pi-tag',
  color: '#6366f1',
})

const submitting = ref(false)
const errorMsg = ref<string | null>(null)

const isEdit = computed(() => !!props.existing)
const title = computed(() => (isEdit.value ? 'Edit Category' : 'New Category'))

// Seed the form when the dialog opens
watch(visible, (open) => {
  if (!open) return
  errorMsg.value = null
  if (props.existing) {
    form.name = props.existing.name
    form.icon = props.existing.icon
    form.color = props.existing.color
  } else {
    form.name = ''
    form.icon = 'pi pi-tag'
    form.color = '#6366f1'
  }
})

async function save() {
  const name = form.name.trim()
  if (!name) {
    errorMsg.value = 'Name is required.'
    return
  }
  if (name.length > 80) {
    errorMsg.value = 'Name must be 80 characters or fewer.'
    return
  }
  if (!/^#[0-9A-Fa-f]{6}$/.test(form.color)) {
    errorMsg.value = 'Color must be a hex string like #6366f1.'
    return
  }

  submitting.value = true
  errorMsg.value = null
  try {
    const payload: CategoryRequest = {
      name,
      icon: form.icon.trim() || 'pi pi-tag',
      color: form.color,
    }
    if (isEdit.value) {
      await store.update(props.existing!.id, payload)
    } else {
      await store.create(payload)
    }
    emit('saved')
    visible.value = false
  } catch (e) {
    errorMsg.value = toUserMessage(e)
  } finally {
    submitting.value = false
  }
}
</script>

<template>
  <Dialog
    v-model:visible="visible"
    :header="title"
    modal
    :style="{ width: '440px' }"
    :draggable="false"
  >
    <form @submit.prevent="save" class="form">
      <div class="field">
        <label for="cf-name">Name</label>
        <InputText id="cf-name" v-model="form.name" maxlength="80" autofocus fluid />
      </div>

      <div class="field">
        <label for="cf-icon">Icon (PrimeIcons class)</label>
        <InputText id="cf-icon" v-model="form.icon" maxlength="50" fluid />
        <small class="sp-text-muted icon-preview">
          Preview:
          <span class="icon-chip">
            <i :class="form.icon" />
          </span>
        </small>
      </div>

      <div class="field">
        <label for="cf-color">Color</label>
        <div class="color-row">
          <input id="cf-color" type="color" v-model="form.color" class="color-input" />
          <span class="color-value">{{ form.color }}</span>
          <span class="color-swatch" :style="{ background: form.color }" />
        </div>
      </div>

      <Message v-if="errorMsg" severity="error" :closable="false">{{ errorMsg }}</Message>
    </form>

    <template #footer>
      <Button
        label="Cancel"
        severity="secondary"
        text
        @click="visible = false"
        :disabled="submitting"
      />
      <Button
        :label="isEdit ? 'Update' : 'Create'"
        icon="pi pi-check"
        :loading="submitting"
        @click="save"
      />
    </template>
  </Dialog>
</template>

<style scoped>
.form {
  display: flex;
  flex-direction: column;
  gap: var(--space-4);
}

.field {
  display: flex;
  flex-direction: column;
  gap: var(--space-1);
}

.field label {
  font-size: var(--text-sm);
  font-weight: 500;
  color: var(--color-text-muted);
}

.icon-preview {
  display: inline-flex;
  align-items: center;
  gap: var(--space-2);
  margin-top: var(--space-1);
}

.icon-chip {
  display: inline-flex;
  align-items: center;
  justify-content: center;
  width: 26px;
  height: 26px;
  border-radius: var(--radius-sm);
  background: var(--color-surface-2);
}

.color-row {
  display: flex;
  align-items: center;
  gap: var(--space-3);
}

.color-input {
  width: 40px;
  height: 32px;
  padding: 2px;
  border: 1px solid var(--color-border);
  border-radius: var(--radius-sm);
  cursor: pointer;
  background: transparent;
}

.color-value {
  font-family: var(--font-mono);
  font-size: var(--text-sm);
  color: var(--color-text-muted);
}

.color-swatch {
  width: 24px;
  height: 24px;
  border-radius: 50%;
  border: 1px solid rgba(0, 0, 0, 0.08);
}
</style>