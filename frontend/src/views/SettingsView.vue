<script setup lang="ts">
import { useAuthStore } from '@/stores/auth'
const auth = useAuthStore()
</script>

<template>
  <div class="sp-stack-lg">
    <h1>Settings</h1>
    <div class="sp-card">
      <div class="field">
        <span class="sp-text-muted">Name</span>
        <div>{{ auth.user?.displayName }}</div>
      </div>
      <div class="field" style="margin-top: 1rem;">
        <span class="sp-text-muted">Email</span>
        <div>{{ auth.user?.email }}</div>
      </div>
    </div>
  </div>
</template>

<style scoped>
h1 { font-size: var(--text-xl); }
.field { display: flex; flex-direction: column; gap: 0.25rem; }
</style>