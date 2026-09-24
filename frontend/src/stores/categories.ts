import { defineStore } from 'pinia'
import { ref } from 'vue'
import { categoriesApi } from '@/api/categories.api'
import type { Category, CategoryRequest } from '@/types/category'

export const useCategoriesStore = defineStore('categories', () => {
  const items = ref<Category[]>([])
  const loading = ref(false)
  const loaded = ref(false)

  async function load(force = false): Promise<void> {
    if (loaded.value && !force) return
    loading.value = true
    try {
      items.value = await categoriesApi.list()
      loaded.value = true
    } finally {
      loading.value = false
    }
  }
  async function create(payload: CategoryRequest): Promise<Category> {
    const created = await categoriesApi.create(payload)
    items.value = [...items.value, created].sort((a, b) => a.name.localeCompare(b.name))
    return created
  }
  async function update(id: number, payload: CategoryRequest): Promise<Category> {
    const updated = await categoriesApi.update(id, payload)
    items.value = items.value.map((c) => (c.id === id ? updated : c)).sort((a, b) => a.name.localeCompare(b.name))
    return updated
  }
  async function remove(id: number): Promise<void> {
    await categoriesApi.remove(id)
    items.value = items.value.filter((c) => c.id !== id)
  }

  return { items, loading, loaded, load, create, update, remove }
})