<script setup lang="ts">
import { computed, reactive, ref, watch } from 'vue'
import Dialog from 'primevue/dialog'
import InputNumber from 'primevue/inputnumber'
import Select from 'primevue/select'
import Button from 'primevue/button'
import Message from 'primevue/message'
import { useCategoriesStore } from '@/stores/categories'
import { useBudgetsStore } from '@/stores/budgets'
import { toUserMessage } from '@/utils/errorMessages'
import { formatPeriod } from '@/utils/periodMath'
import type { EnvelopeStatus } from '@/types/dashboard'

const visible = defineModel<boolean>('visible', { required: true })

const props = defineProps<{
  /** When set, the dialog edits this envelope. Otherwise, creates a new one. */
  existing?: EnvelopeStatus | null
}>()

const emit = defineEmits<{ saved: [] }>()

const categoriesStore = useCategoriesStore()
const budgetsStore = useBudgetsStore()

const form = reactive({
  categoryId: null as number | null,
  limitAmount: null as number | null,
})

const submitting = ref(false)
const errorMsg = ref<string | null>(null)

const isEdit = computed(() => !!props.existing)
const title = computed(() => (isEdit.value ? 'Adjust Budget' : 'New Budget'))
const periodLabel = computed(() => formatPeriod(budgetsStore.year, budgetsStore.month))

watch(visible, async (open) => {
  if (!open) return
  errorMsg.value = null

  if (props.existing) {
    form.categoryId = props.existing.categoryId
    form.limitAmount = props.existing.limitAmount
  } else {
    form.categoryId = null
    form.limitAmount = null
  }

  await categoriesStore.load()
})

/**
 * In create mode: only categories without an existing budget for this period.
 * In edit mode: all categories (the current one is disabled anyway).
 */
const categoryOptions = computed(() => {
  const used = new Set(budgetsStore.items.map((e) => e.categoryId))
  return categoriesStore.items
    .filter((c) => isEdit.value || !used.has(c.id))
    .map((c) => ({ label: c.name, value: c.id }))
})

async function save() {
  if (form.categoryId === null || form.limitAmount === null) {
    errorMsg.value = 'Category and limit are required.'
    return
  }
  if (form.limitAmount <= 0) {
    errorMsg.value = 'Limit must be greater than zero.'
    return
  }

  submitting.value = true
  errorMsg.value = null
  try {
    await budgetsStore.upsert({
      categoryId: form.categoryId,
      limitAmount: form.limitAmount,
      month: budgetsStore.month,
      year: budgetsStore.year,
    })
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
  <Dialog v-model:visible="visible" :header="title" modal :style="{ width: '440px' }" :draggable="false">
    <form @submit.prevent="save" class="form">
      <div class="field">
        <label for="bf-category">Category</label>
        <Select
          id="bf-category"
          v-model="form.categoryId"
          :options="categoryOptions"
          option-label="label"
          option-value="value"
          placeholder="Choose a category"
          filter
          :disabled="isEdit"
          fluid
        />
        <small v-if="isEdit" class="sp-text-muted">
          Category can't be changed. Delete and recreate to switch.
        </small>
      </div>

      <div class="field">
        <label for="bf-limit">Monthly limit</label>
        <InputNumber
          id="bf-limit"
          v-model="form.limitAmount"
          mode="currency"
          currency="USD"
          locale="en-US"
          :min="0.01"
          :min-fraction-digits="2"
          :max-fraction-digits="2"
          autofocus
          fluid
        />
      </div>

      <div class="period-info sp-text-muted">
        <i class="pi pi-calendar" />
        For <strong>{{ periodLabel }}</strong>
      </div>

      <Message v-if="errorMsg" severity="error" :closable="false">{{ errorMsg }}</Message>
    </form>

    <template #footer>
      <Button label="Cancel" severity="secondary" text @click="visible = false" :disabled="submitting" />
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

.period-info {
  display: flex;
  align-items: center;
  gap: var(--space-2);
  font-size: var(--text-sm);
  padding: var(--space-3);
  background: var(--color-surface-2);
  border-radius: var(--radius-sm);
}
</style>