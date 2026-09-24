import { createRouter, createWebHistory } from 'vue-router'
import { useAuthStore } from '@/stores/auth'

const router = createRouter({
  history: createWebHistory(import.meta.env.BASE_URL),
  routes: [
    { path: '/login',    name: 'login',    component: () => import('@/views/auth/LoginView.vue'),    meta: { public: true } },
    { path: '/register', name: 'register', component: () => import('@/views/auth/RegisterView.vue'), meta: { public: true } },
    {
      path: '/',
      component: () => import('@/layouts/DefaultLayout.vue'),
      children: [
        { path: '',             name: 'home',         component: () => import('@/views/DashboardView.vue') },
        { path: 'transactions', name: 'transactions', component: () => import('@/views/TransactionsView.vue') },
        { path: 'budgets',      name: 'budgets',      component: () => import('@/views/BudgetsView.vue') },
        { path: 'categories',   name: 'categories',   component: () => import('@/views/CategoriesView.vue') },
        { path: 'settings',     name: 'settings',     component: () => import('@/views/SettingsView.vue') },
      ],
    },
    { path: '/:pathMatch(.*)*', name: 'not-found', component: () => import('@/views/NotFoundView.vue') },
  ],
})

router.beforeEach((to) => {
  const auth = useAuthStore()
  const isPublic = to.meta.public === true
  if (!isPublic && !auth.isAuthenticated) {
    return { name: 'login', query: { redirect: to.fullPath } }
  }
  if (isPublic && auth.isAuthenticated && (to.name === 'login' || to.name === 'register')) {
    return { name: 'home' }
  }
})

export default router