<script setup lang="ts">
import { computed, onMounted, ref, watch } from 'vue'
import { useRoute, useRouter } from 'vue-router'
import { useAuthStore } from '@/stores/auth'
import Button from 'primevue/button'
import Menu from 'primevue/menu'
import Drawer from 'primevue/drawer'
import QuickAddModal from '@/components/transactions/QuickAddModal.vue'

const auth = useAuthStore()
const route = useRoute()
const router = useRouter()

const quickAddVisible = ref(false)
const collapsed = ref(false)
const drawerOpen = ref(false)
const isNarrow = ref(false)

const userMenu = ref()
const userMenuItems = ref([
  { label: 'Settings', icon: 'pi pi-cog', command: () => router.push('/settings') },
  { separator: true },
  { label: 'Sign out', icon: 'pi pi-sign-out', command: () => signOut() },
])

const navItems = [
  { to: '/',             label: 'Dashboard',    icon: 'pi pi-home' },
  { to: '/transactions', label: 'Transactions', icon: 'pi pi-list' },
  { to: '/budgets',      label: 'Budgets',      icon: 'pi pi-wallet' },
  { to: '/categories',   label: 'Categories',   icon: 'pi pi-tags' },
  { to: '/settings',     label: 'Settings',     icon: 'pi pi-cog' },
]

const SIDEBAR_W = 240
const SIDEBAR_W_COLLAPSED = 68

const sidebarWidth = computed(() =>
  collapsed.value ? `${SIDEBAR_W_COLLAPSED}px` : `${SIDEBAR_W}px`
)

onMounted(() => {
  collapsed.value = localStorage.getItem('sp_sidebar_collapsed') === 'true'
  updateNarrow()
  window.addEventListener('resize', updateNarrow)
})

function updateNarrow() {
  isNarrow.value = window.innerWidth <= 900
}

watch(collapsed, (v) => localStorage.setItem('sp_sidebar_collapsed', String(v)))
watch(() => route.fullPath, () => { drawerOpen.value = false })

function toggleSidebar() {
  if (isNarrow.value) {
    drawerOpen.value = true
  } else {
    collapsed.value = !collapsed.value
  }
}

function toggleUserMenu(event: Event) {
  userMenu.value.toggle(event)
}

function signOut() {
  auth.logout()
  router.push('/login')
}
</script>

<template>
  <div class="layout">
    <!-- ============ TOP HEADER (full width, purple) ============ -->
    <header class="app-header">
      <button
        type="button"
        class="brand"
        aria-label="Toggle navigation"
        @click="toggleSidebar"
      >
        <i class="pi pi-chart-pie brand-icon" />
        <span class="brand-name">SmartSpender</span>
      </button>

      <div class="header-spacer" />

      <Button
        label="Quick Add"
        icon="pi pi-plus"
        class="quick-add-btn"
        size="small"
        @click="quickAddVisible = true"
      />

      <button
        class="avatar-button"
        aria-label="User menu"
        @click="toggleUserMenu"
      >
        <span class="avatar-icon">
          <i class="pi pi-user" />
        </span>
      </button>
      <Menu ref="userMenu" :model="userMenuItems" :popup="true" />
    </header>

    <!-- ============ MIDDLE ROW (sidebar + content) ============ -->
    <div class="app-body">
      <!-- Desktop sidebar -->
      <aside
        v-if="!isNarrow"
        class="sidebar"
        :style="{ width: sidebarWidth }"
      >
        <nav class="nav">
          <RouterLink
            v-for="item in navItems"
            :key="item.to"
            :to="item.to"
            class="nav-item"
            :class="{ 'nav-item-collapsed': collapsed }"
            active-class="nav-item-active"
            :title="collapsed ? item.label : undefined"
          >
            <i :class="item.icon" />
            <span v-if="!collapsed">{{ item.label }}</span>
          </RouterLink>
        </nav>
      </aside>

      <!-- Mobile drawer -->
      <Drawer v-model:visible="drawerOpen" position="left" :style="{ width: '260px' }">
        <template #container="{ closeCallback }">
          <div class="drawer-content">
            <nav class="nav">
              <RouterLink
                v-for="item in navItems"
                :key="item.to"
                :to="item.to"
                class="nav-item"
                active-class="nav-item-active"
                @click="closeCallback"
              >
                <i :class="item.icon" />
                <span>{{ item.label }}</span>
              </RouterLink>
            </nav>
          </div>
        </template>
      </Drawer>

      <main class="content">
        <RouterView />
      </main>
    </div>

    <!-- ============ FOOTER (full width, light purple) ============ -->
    <footer class="app-footer">
      <i class="pi pi-copyright footer-icon" />
      <span>SmartSpender</span>
      <i class="pi pi-chart-pie footer-icon-right" />
    </footer>

    <QuickAddModal v-model:visible="quickAddVisible" />
  </div>
</template>

<style scoped>
/* ============================================================
 * Layout shape: column with header / body / footer
 *
 * The `.layout` is a 100vh flex column with overflow hidden.
 * `.app-body` flexes to fill the middle.
 * Only `.content` inside `.app-body` scrolls.
 * No magic numbers — flexbox computes everything.
 * ============================================================ */
.layout {
  display: flex;
  flex-direction: column;
  height: 100vh;
  overflow: hidden;
  background: var(--color-bg);
}

/* ============================================================
 * HEADER
 * ============================================================ */
.app-header {
  display: flex;
  align-items: center;
  gap: var(--space-3);
  padding: 0 var(--space-6);
  height: var(--topbar-h);
  flex-shrink: 0;              /* do not shrink — takes its fixed height */
  background: var(--color-primary);
  color: white;
  box-shadow: var(--shadow-sm);
  z-index: 10;
}

.brand {
  display: inline-flex;
  align-items: center;
  gap: var(--space-2);
  padding: var(--space-2) var(--space-3);
  margin: 0;
  border: none;
  background: transparent;
  font-family: inherit;
  font-weight: 700;
  font-size: var(--text-lg);
  color: white;
  cursor: pointer;
  border-radius: var(--radius-sm);
  transition: background 120ms ease;
}

.brand:hover {
  background: rgba(255, 255, 255, 0.12);
}

.brand:focus-visible {
  outline: 2px solid white;
  outline-offset: 2px;
}

.brand-icon {
  color: white;
  font-size: 1.4rem;
  flex-shrink: 0;
}

.header-spacer {
  flex: 1;
}

.quick-add-btn {
  background: white !important;
  border-color: white !important;
  color: var(--color-primary) !important;
  font-weight: 600;
}

.quick-add-btn:hover {
  background: #f3f4ff !important;
  border-color: #f3f4ff !important;
}

.avatar-button {
  border: none;
  background: transparent;
  padding: 0;
  cursor: pointer;
  border-radius: 50%;
  display: inline-flex;
}

.avatar-icon {
  display: inline-flex;
  align-items: center;
  justify-content: center;
  width: 36px;
  height: 36px;
  border-radius: 50%;
  background: white;
  color: var(--color-primary);
  font-size: 1rem;
  transition: background 120ms ease;
}

.avatar-button:hover .avatar-icon {
  background: #f3f4ff;
}

.avatar-button:focus-visible {
  outline: 2px solid white;
  outline-offset: 2px;
}

/* ============================================================
 * BODY (sidebar + main content)
 *
 * flex: 1      → take all remaining vertical space
 * min-height:0 → allows children to shrink below their content size
 * overflow:    → keep the body itself from scrolling
 * ============================================================ */
.app-body {
  display: flex;
  flex: 1;
  min-height: 0;
  overflow: hidden;
}

/* ---------- Sidebar ---------- */
.sidebar {
  flex-shrink: 0;
  background: var(--color-surface);
  border-right: 1px solid var(--color-border);
  padding: var(--space-4) var(--space-3);
  display: flex;
  flex-direction: column;
  gap: var(--space-4);
  transition: width 200ms ease;
  overflow-y: auto;            /* sidebar scrolls internally if nav grows */
  overflow-x: hidden;
}

.nav {
  display: flex;
  flex-direction: column;
  gap: var(--space-1);
}

.nav-item {
  display: flex;
  align-items: center;
  gap: var(--space-3);
  padding: var(--space-3);
  border-radius: var(--radius-sm);
  color: var(--color-text-muted);
  text-decoration: none;
  font-size: var(--text-sm);
  font-weight: 500;
  transition: background 120ms ease, color 120ms ease;
  white-space: nowrap;
  overflow: hidden;
}

.nav-item-collapsed {
  justify-content: center;
  padding: var(--space-3) 0;
}

.nav-item:hover {
  background: var(--color-surface-2);
  color: var(--color-text);
}

.nav-item-active {
  background: var(--color-primary-soft);
  color: var(--color-primary);
}

.nav-item i {
  font-size: 1.05rem;
  flex-shrink: 0;
}

.drawer-content {
  display: flex;
  flex-direction: column;
  gap: var(--space-4);
  padding: var(--space-4) var(--space-3);
  background: var(--color-surface);
  height: 100%;
}

/* ---------- Main content: the ONLY scrollable region ---------- */
.content {
  flex: 1;
  padding: var(--space-8) var(--space-10);
  max-width: var(--content-max);
  width: 100%;
  margin: 0 auto;
  min-width: 0;
  overflow-y: auto;            /* this is the scroll */
}

@media (max-width: 900px) {
  .content { padding: var(--space-6) var(--space-5); }
}

/* ============================================================
 * FOOTER (light purple)
 * ============================================================ */
.app-footer {
  display: flex;
  align-items: center;
  justify-content: center;
  gap: var(--space-2);
  padding: var(--space-4) var(--space-6);
  flex-shrink: 0;              /* do not shrink — takes its natural height */
  background: var(--color-primary-soft);
  color: var(--color-primary);
  font-size: var(--text-sm);
  font-weight: 500;
  border-top: 1px solid rgba(99, 102, 241, 0.15);
}

.footer-icon {
  font-size: 0.875rem;
}

.footer-icon-right {
  margin-left: var(--space-2);
  font-size: 1rem;
}
</style>