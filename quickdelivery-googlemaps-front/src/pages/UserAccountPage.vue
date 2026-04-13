<template>
  <div class="user-account-page qd-page">
    <template v-if="isLoadingPage">
      <header class="qd-page-header">
        <div class="header-main">
          <h1>{{ $t('userAccountPageTitle') }}</h1>
          <p>{{ $t('userAccountPageSubtitle') }}</p>
        </div>
      </header>
      <div class="page-state loading-state">
        <div class="spinner"></div>
        {{ $t('stateLoading') }}
      </div>
    </template>
    <template v-else-if="loadError">
      <header class="qd-page-header">
        <div class="header-main">
          <h1>{{ $t('userAccountPageTitle') }}</h1>
          <p>{{ $t('userAccountPageSubtitle') }}</p>
        </div>
      </header>
      <div class="page-state error-state">
        <i class="error-icon">️⚠</i>
        {{ $t('stateLoadError') }}
      </div>
    </template>
    <template v-else-if="selectedUser">
      <header class="qd-page-header">
        <div class="header-main">
          <h1>{{ $t('userAccountPageTitle') }}</h1>
          <p class="subtitle">{{ $t('userAccountPageSubtitle') }}</p>
        </div>
        <div class="qd-page-header-actions">
          <span class="user-role-badge">{{ $t(selectedUser.type || 'userType') }}</span>
        </div>
      </header>

      <div class="account-layout">
        <aside class="sidebar">
          <CourierReadinessCard :readiness="courierReadiness" variant="default" />
        </aside>
        <main class="main-content">
          <UserAccount :selectedUser="selectedUser"/>
        </main>
      </div>
    </template>
  </div>
</template>

<script>
import UserAccount from '../components/UserAccount.vue';
import CourierReadinessCard from '../components/CourierReadinessCard.vue';
import { buildCourierReadiness } from '../config/courierReadiness';
import { hydrateConnectedUser } from '@/config/session';

export default {
  components: {
    UserAccount,
    CourierReadinessCard,
  },
  data() {
    return {
      selectedUser: null,
      isLoadingPage: false,
      loadError: false,
    };
  },
  async mounted() {
    this.isLoadingPage = true;
    this.loadError = false;
    try {
      this.selectedUser = await this.loadUser();
    } catch (error) {
      this.loadError = true;
      console.error("Error loading user:", error);
    } finally {
      this.isLoadingPage = false;
    }
  },
  computed: {
    courierReadiness() {
      return buildCourierReadiness(this.selectedUser);
    },
  },
  methods: {
    async loadUser() {
      try {
        const connectedUser = await hydrateConnectedUser();
        return connectedUser;
      } catch (error) {
        console.error("Unable to process your request at this time. Please try again later.", error);
        throw error;
      }
    },
  },
};
</script>

<style scoped>
.user-role-badge {
  background: var(--qd-primary-dark);
  color: #fff;
  padding: 8px 18px;
  border-radius: 99px;
  font-size: 0.82rem;
  font-weight: 800;
  text-transform: uppercase;
  letter-spacing: 0.04em;
  box-shadow: 0 4px 12px rgba(15, 23, 42, 0.1);
}

.subtitle {
  margin: 8px 0 0;
  color: var(--qd-muted);
  font-size: 1.1rem;
}

.account-layout {
  display: grid;
  grid-template-columns: 320px 1fr;
  gap: 32px;
  align-items: start;
}

.sidebar {
  position: sticky;
  top: 40px;
}

.main-content {
  min-width: 0;
}

/* States */
@keyframes spin { to { transform: rotate(360deg); } }

@media screen and (max-width: 1024px) {
  .account-layout {
    grid-template-columns: 1fr;
  }
  .sidebar {
    position: static;
  }
}

@media screen and (max-width: 768px) {
  .user-account-page {
    padding: 12px;
  }
  .title-wrap h1 {
    font-size: 1.6rem;
    line-height: 1.1;
  }
}
</style>
