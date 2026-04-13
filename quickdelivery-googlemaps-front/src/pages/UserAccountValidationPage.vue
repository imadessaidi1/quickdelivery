<template>
  <div class="user-validation-page qd-page">
    <template v-if="isLoadingPage">
      <header class="qd-page-header">
        <div class="header-main">
          <h1>{{ $t('validationPageTitle') }}</h1>
          <p>{{ $t('validationPageSubtitle') }}</p>
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
          <h1>{{ $t('validationPageTitle') }}</h1>
          <p>{{ $t('validationPageSubtitle') }}</p>
        </div>
      </header>
      <div class="page-state error-state">
        <i class="error-icon">️⚠</i>
        {{ $t('stateLoadError') }}
      </div>
    </template>
    <template v-else-if="usersList.length === 0">
      <header class="qd-page-header">
        <div class="header-main">
          <h1>{{ $t('validationPageTitle') }}</h1>
          <p>{{ $t('validationPageSubtitle') }}</p>
        </div>
      </header>
      <div class="page-state empty-state">
        {{ $t('stateEmptyUsersValidation') }}
      </div>
    </template>
    <template v-else>
      <header class="qd-page-header">
        <div class="header-main">
          <div class="title-row" style="display: flex; align-items: center; gap: 16px;">
            <h1>{{ $t('validationPageTitle') }}</h1>
            <span class="page-chip">{{ totalUsers }}</span>
          </div>
          <p class="subtitle">{{ $t('validationPageSubtitle') }}</p>
        </div>
      </header>

      <div class="summary-grid">
        <div class="glass-card primary-accent">
          <div class="card-icon">👥</div>
          <div class="card-content">
            <strong>{{ totalUsers }}</strong>
            <span>{{ $t('validationPendingUsers') }}</span>
          </div>
        </div>
        <div class="glass-card neutral-accent">
          <div class="card-icon">🚛</div>
          <div class="card-content">
            <strong>{{ totalVehicles }}</strong>
            <span>{{ $t('validationVehiclesCount') }}</span>
          </div>
        </div>
        <div class="glass-card warn-accent">
          <div class="card-icon">🖇</div>
          <div class="card-content">
            <strong>{{ totalDocuments }}</strong>
            <span>{{ $t('validationDocumentsCount') }}</span>
          </div>
        </div>
      </div>

      <div class="list-container">
        <UsersAccountList :users="usersList"/>
      </div>

      <div class="pagination-container">
        <div class="pagination-bar">
          <button class="qd-btn-secondary" type="button" :disabled="currentPage === 0 || isLoadingPage" @click="loadUsers(currentPage - 1)">
            <span class="arrow">←</span> {{ $t('packagePreviousAction') }}
          </button>
          <div class="page-info">
            <span class="current">{{ currentPage + 1 }}</span>
            <span class="separator">/</span>
            <span class="total">{{ totalPages }}</span>
          </div>
          <button class="qd-btn-secondary" type="button" :disabled="currentPage >= totalPages - 1 || isLoadingPage" @click="loadUsers(currentPage + 1)">
            {{ $t('packageNextAction') }} <span class="arrow">→</span>
          </button>
        </div>
      </div>
    </template>
  </div>
</template>

<script>
import UsersAccountList from '../components/UsersAccountList.vue';
import http from '@/config/httpInterceptor';

export default {
  components: {
    UsersAccountList,
  },
  data() {
    return {
      usersList: [],
      isLoadingPage: false,
      loadError: false,
      currentPage: 0,
      pageSize: 12,
      totalPages: 1,
      totalUsers: 0,
      totalVehiclesCount: 0,
      totalDocumentsCount: 0,
    };
  },
  computed: {
    totalVehicles() {
      return this.totalVehiclesCount;
    },
    totalDocuments() {
      return this.totalDocumentsCount;
    },
  },
  mounted() {
    this.loadUsers();
  },
  methods: {
    async loadUsers(page = 0) {
      this.isLoadingPage = true;
      this.loadError = false;
      try {
        const response = await http.get(`${this.$i18n.t('userRootURL')}${this.$i18n.t('getUsersForValidation')}?page=${page}&size=${this.pageSize}`);
        const payload = response.data || {};
        this.usersList = Array.isArray(payload.items) ? payload.items : [];
        this.currentPage = payload.page || 0;
        this.totalPages = Math.max(payload.totalPages || 1, 1);
        this.totalUsers = payload.totalItems || 0;
        this.totalVehiclesCount = payload.totalVehicles || 0;
        this.totalDocumentsCount = payload.totalDocuments || 0;
      } catch (error) {
        this.loadError = true;
        console.error(this.$t('requestErrorGeneric'), error);
      } finally {
        this.isLoadingPage = false;
      }
    },
  },
};
</script>

<style scoped>
.subtitle {
  margin: 8px 0 0;
  font-size: 1.1rem;
  color: var(--qd-muted);
}

/* Summary Grid - Glassmorphism */
.summary-grid {
  display: grid;
  grid-template-columns: repeat(3, 1fr);
  gap: 24px;
  margin-bottom: 40px;
}

.glass-card {
  display: flex;
  align-items: center;
  gap: 20px;
  padding: 28px;
  background: rgba(255, 255, 255, 0.7);
  backdrop-filter: blur(12px);
  -webkit-backdrop-filter: blur(12px);
  border: 1px solid rgba(255, 255, 255, 0.8);
  border-radius: 24px;
  box-shadow: 0 10px 30px rgba(0, 0, 0, 0.03);
  transition: all 0.3s cubic-bezier(0.4, 0, 0.2, 1);
}

.glass-card:hover {
  transform: translateY(-4px);
  box-shadow: 0 20px 40px rgba(0, 0, 0, 0.06);
}

.card-icon {
  display: flex;
  align-items: center;
  justify-content: center;
  width: 56px;
  height: 56px;
  background: #fff;
  border-radius: 16px;
  font-size: 1.5rem;
  box-shadow: 0 8px 16px rgba(0, 0, 0, 0.05);
}

.card-content strong {
  display: block;
  font-size: 2rem;
  font-weight: 800;
  color: #0f172a;
  line-height: 1.1;
  margin-bottom: 4px;
}

.card-content span {
  font-size: 0.875rem;
  font-weight: 600;
  color: #64748b;
  text-transform: uppercase;
  letter-spacing: 0.05em;
}

.primary-accent { border-left: 6px solid #0f172a; }
.neutral-accent { border-left: 6px solid #64748b; }
.warn-accent { border-left: 6px solid #f59e0b; }

/* List Container */
.list-container {
  background: #fff;
  border-radius: 24px;
  padding: 8px;
  box-shadow: 0 4px 20px rgba(0, 0, 0, 0.02);
  border: 1px solid #f1f5f9;
}

/* Pagination */
.pagination-container {
  display: flex;
  justify-content: center;
  margin-top: 40px;
}

.pagination-bar {
  display: flex;
  align-items: center;
  gap: 24px;
  background: #fff;
  padding: 12px 24px;
  border-radius: 100px;
  box-shadow: 0 10px 25px rgba(0, 0, 0, 0.04);
}

/* .nav-btn handled by design-system.css qd-btn-secondary */

.page-info {
  display: flex;
  align-items: center;
  gap: 10px;
  padding: 0 16px;
  border-left: 1px solid #e2e8f0;
  border-right: 1px solid #e2e8f0;
}

.page-info .current {
  font-size: 1.125rem;
  font-weight: 800;
  color: #0f172a;
}

.page-info .separator {
  color: #cbd5e1;
}

.page-info .total {
  font-size: 1rem;
  font-weight: 600;
  color: #94a3b8;
}

/* Page States */
.page-state {
  display: flex;
  flex-direction: column;
  align-items: center;
  justify-content: center;
  padding: 80px 40px;
  background: #fff;
  border-radius: 32px;
  color: #64748b;
  font-size: 1.25rem;
  font-weight: 600;
  box-shadow: 0 10px 30px rgba(0, 0, 0, 0.03);
  text-align: center;
}

.loading-state .spinner {
  width: 48px;
  height: 48px;
  border: 4px solid #f1f5f9;
  border-top: 4px solid #0f172a;
  border-radius: 50%;
  animation: spin 1s linear infinite;
  margin-bottom: 24px;
}

@keyframes spin {
  to { transform: rotate(360deg); }
}

@media screen and (max-width: 1024px) {
  .summary-grid {
    grid-template-columns: repeat(2, minmax(0, 1fr));
    gap: 16px;
  }
}

@media screen and (max-width: 768px) {
  .user-validation-page {
    padding: 12px;
  }

  .summary-grid {
    grid-template-columns: repeat(2, minmax(0, 1fr));
    gap: 10px;
    margin-bottom: 14px;
  }

  .glass-card {
    gap: 10px;
    padding: 14px;
    border-radius: 14px;
  }

  .card-icon {
    width: 34px;
    height: 34px;
    border-radius: 10px;
    font-size: 1rem;
  }

  .card-content strong {
    font-size: 1.35rem;
    line-height: 1.05;
    margin-bottom: 3px;
  }

  .card-content span {
    font-size: 0.66rem;
    line-height: 1.2;
    letter-spacing: 0.02em;
  }

  .page-header h1 {
    font-size: 1.55rem;
    line-height: 1.1;
  }

  .nav-btn span {
    display: none;
  }
}

@media screen and (max-width: 420px) {
  .user-validation-page {
    padding: 12px;
  }

  .glass-card {
    align-items: flex-start;
    flex-direction: column;
  }
}
</style>
