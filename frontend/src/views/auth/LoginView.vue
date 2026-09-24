<script setup lang="ts">
import { reactive, ref } from 'vue'
import { useRouter } from 'vue-router'
import InputText from 'primevue/inputtext'
import Password from 'primevue/password'
import Button from 'primevue/button'
import Message from 'primevue/message'
import { useAuthStore } from '@/stores/auth'
import { toUserMessage } from '@/utils/errorMessages'

const router = useRouter()
const auth = useAuthStore()
const form = reactive({ email: '', password: '' })
const submitting = ref(false)
const errorMsg = ref<string | null>(null)

async function submit() {
  submitting.value = true
  errorMsg.value = null
  try {
    await auth.login({ email: form.email, password: form.password })
    router.push('/')
  } catch (e) {
    errorMsg.value = toUserMessage(e)
  } finally {
    submitting.value = false
  }
}
</script>

<template>
  <div class="auth-shell">
    <div class="auth-card sp-card">
      <h1 class="auth-title">SmartSpender</h1>
      <p class="sp-text-muted auth-sub">Sign in to your account</p>

      <form @submit.prevent="submit" class="auth-form">
        <div class="field">
          <label for="email">Email</label>
          <InputText id="email" v-model="form.email" type="email" required />
        </div>
        <div class="field">
          <label for="password">Password</label>
          <Password id="password" v-model="form.password" :feedback="false" toggleMask required fluid />
        </div>
        <Message v-if="errorMsg" severity="error" :closable="false">{{ errorMsg }}</Message>
        <Button type="submit" label="Sign in" :loading="submitting" class="w-full" />
      </form>

      <p class="auth-footer sp-text-muted">
        No account? <RouterLink to="/register">Create one</RouterLink>
      </p>
    </div>
  </div>
</template>

<style scoped>
.auth-shell {
  min-height: 100vh;
  display: flex;
  align-items: center;
  justify-content: center;
  background: linear-gradient(135deg, #eef2ff 0%, #f8fafc 100%);
  padding: var(--space-4);
}
.auth-card { width: 100%; max-width: 400px; }
.auth-title { font-size: var(--text-xl); margin-bottom: var(--space-1); }
.auth-sub { margin-bottom: var(--space-6); }
.auth-form { display: flex; flex-direction: column; gap: var(--space-4); }
.field { display: flex; flex-direction: column; gap: var(--space-1); }
.field label { font-size: var(--text-sm); font-weight: 500; }
.auth-footer { margin-top: var(--space-6); text-align: center; font-size: var(--text-sm); }
.auth-footer a { color: var(--color-primary); font-weight: 500; text-decoration: none; }
.w-full { width: 100%; }
</style>