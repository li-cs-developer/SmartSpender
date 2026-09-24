<script setup lang="ts">
import { computed, reactive, ref, watch } from 'vue'
import Dialog from 'primevue/dialog'
import InputText from 'primevue/inputtext'
import InputNumber from 'primevue/inputnumber'
import SelectButton from 'primevue/selectbutton'
import Select from 'primevue/select'
import DatePicker from 'primevue/datepicker'
import Button from 'primevue/button'
import Message from 'primevue/message'
import CategoryQuickCreate from '@/components/categories/CategoryQuickCreate.vue'
import { useTransactionsStore } from '@/stores/transactions'
import { useCategoriesStore } from '@/stores/categories'
import { useDashboardStore } from '@/stores/dashboard'
import { toUserMessage } from '@/utils/errorMessages'
import type { TransactionType } from '@/types/transaction'
import type { Category } from '@/types/category'

const visible = defineModel<boolean>('visible', { required: true })

const transactions = useTransactionsStore()
const categories = useCategoriesStore()
const dashboard = useDashboardStore()

const typeOptions: { label: string; value: TransactionType }[] = [
  { label: 'Expense', value: 'EXPENSE' },
  { label: 'Income', value: 'INCOME' },
]

const form = reactive({
  type: 'EXPENSE' as TransactionType,
  amount: null as number | null,
  name: '',
  categoryId: null as number | null,
  date: new Date() as Date,
  notes: '',
})

const submitting = ref(false)
const errorMsg = ref<string | null>(null)
const showCreateForm = ref(false)

watch(visible, async (open) => {
  if (!open) return
  errorMsg.value = null
  showCreateForm.value = false
  form.type = 'EXPENSE'
  form.amount = null
  form.name = ''
  form.categoryId = null
  form.date = new Date()
  form.notes = ''
  await categories.load()
})

const categoryOptions = computed(() =>
  categories.items.map((c) => ({ label: c.name, value: c.id }))
)

const canSubmit = computed(() =>
  form.amount !== null && form.amount > 0 &&
  form.name.trim().length > 0 && form.categoryId !== null
)

function isoDate(d: Date): string {
  const y = d.getFullYear()
  const m = String(d.getMonth() + 1).padStart(2, '0')
  const day = String(d.getDate()).padStart(2, '0')
  return `${y}-${m}-${day}`
}

function onCategoryCreated(cat: Category) {
  form.categoryId = cat.id
  showCreateForm.value = false
}

async function submit() {
  if (!canSubmit.value) return
  submitting.value = true
  errorMsg.value = null
  try {
    await transactions.create({
      amount: form.amount!,
      type: form.type,
      name: form.name.trim(),
      notes: form.notes.trim() || null,
      date: isoDate(form.date),
      categoryId: form.categoryId!,
    })
    await dashboard.load()
    visible.value = false
  } catch (e) {
    errorMsg.value = toUserMessage(e)
  } finally {
    submitting.value = false
  }
}
</script>

<template>
  <Dialog v-model:visible="visible" header="Quick Add" modal :style="{ width: '480px' }" :draggable="false">
    <form @submit.prevent="submit" class="form">
      <div class="field">
        <label>Type</label>
        <SelectButton v-model="form.type" :options="typeOptions" option-label="label" option-value="value" :allow-empty="false" />
      </div>
      <div class="field">
        <label for="qa-amount">Amount</label>
        <InputNumber id="qa-amount" v-model="form.amount" mode="currency" currency="USD" locale="en-US" :min="0.01" :min-fraction-digits="2" :max-fraction-digits="2" autofocus fluid />
      </div>
      <div class="field">
        <label for="qa-name">Name</label>
        <InputText id="qa-name" v-model="form.name" maxlength="150" fluid />
      </div>
      <div class="field">
        <div class="label-row">
          <label for="qa-category">Category <span class="req">*</span></label>
          <button v-if="!showCreateForm" type="button" class="link-button" @click="showCreateForm = true">
            + New category
          </button>
        </div>
        <Select v-if="!showCreateForm" id="qa-category" v-model="form.categoryId" :options="categoryOptions" option-label="label" option-value="value" placeholder="Choose a category" filter fluid />
        <CategoryQuickCreate v-else @created="onCategoryCreated" @cancel="showCreateForm = false" />
      </div>
      <div class="field">
        <label for="qa-date">Date</label>
        <DatePicker id="qa-date" v-model="form.date" date-format="yy-mm-dd" show-icon fluid />
      </div>
      <div class="field">
        <label for="qa-notes">Notes (optional)</label>
        <InputText id="qa-notes" v-model="form.notes" maxlength="500" fluid />
      </div>
      <Message v-if="errorMsg" severity="error" :closable="false">{{ errorMsg }}</Message>
    </form>
    <template #footer>
      <Button label="Cancel" severity="secondary" text @click="visible = false" :disabled="submitting" />
      <Button label="Save" icon="pi pi-check" :loading="submitting" :disabled="!canSubmit" @click="submit" />
    </template>
  </Dialog>
</template>

<style scoped>
.form { display: flex; flex-direction: column; gap: var(--space-4); }
.field { display: flex; flex-direction: column; gap: var(--space-1); }
.field label { font-size: var(--text-sm); font-weight: 500; color: var(--color-text-muted); }
.label-row { display: flex; align-items: center; justify-content: space-between; }
.link-button { background: none; border: none; padding: 0; font-size: var(--text-xs); color: var(--color-primary); cursor: pointer; font-weight: 500; }
.link-button:hover { text-decoration: underline; }
.req { color: var(--color-danger); }
</style>