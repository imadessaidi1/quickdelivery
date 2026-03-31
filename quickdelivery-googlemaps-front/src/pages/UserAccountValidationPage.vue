<template>
  <div class="user-validation-page">
    <template v-if="isLoadingPage">
      <div class="page-header">
        <div>
          <h1>{{ $t('validationPageTitle') }}</h1>
          <p>{{ $t('validationPageSubtitle') }}</p>
        </div>
      </div>
      <div class="page-state">{{ $t('stateLoading') }}</div>
    </template>
    <template v-else-if="loadError">
      <div class="page-header">
        <div>
          <h1>{{ $t('validationPageTitle') }}</h1>
          <p>{{ $t('validationPageSubtitle') }}</p>
        </div>
      </div>
      <div class="page-state error">{{ $t('stateLoadError') }}</div>
    </template>
    <template v-else-if="usersList.length === 0">
      <div class="page-header">
        <div>
          <h1>{{ $t('validationPageTitle') }}</h1>
          <p>{{ $t('validationPageSubtitle') }}</p>
        </div>
      </div>
      <div class="page-state">{{ $t('stateEmptyUsersValidation') }}</div>
    </template>
    <template v-else>
      <div class="page-header">
        <div>
          <h1>{{ $t('validationPageTitle') }}</h1>
          <p>{{ $t('validationPageSubtitle') }}</p>
        </div>
        <div class="header-chip">{{ totalUsers }}</div>
      </div>

      <div class="summary-panel">
        <div class="summary-card">
          <strong>{{ totalUsers }}</strong>
          <span>{{ $t('validationPendingUsers') }}</span>
        </div>
        <div class="summary-card accent-neutral">
          <strong>{{ totalVehicles }}</strong>
          <span>{{ $t('validationVehiclesCount') }}</span>
        </div>
        <div class="summary-card accent-warn">
          <strong>{{ totalDocuments }}</strong>
          <span>{{ $t('validationDocumentsCount') }}</span>
        </div>
      </div>
      <UsersAccountList :users="usersList"/>
      <div class="pagination-bar">
        <button class="pagination-btn" type="button" :disabled="currentPage === 0 || isLoadingPage" @click="loadUsers(currentPage - 1)">
          {{ $t('packagePreviousAction') }}
        </button>
        <span class="pagination-info">{{ currentPage + 1 }} / {{ totalPages }}</span>
        <button class="pagination-btn" type="button" :disabled="currentPage >= totalPages - 1 || isLoadingPage" @click="loadUsers(currentPage + 1)">
          {{ $t('packageNextAction') }}
        </button>
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
        console.error('Unable to process your request this time. Please try again later.', error);
      } finally {
        this.isLoadingPage = false;
      }
    },
  },
};
</script>

<style>
.user-validation-page {
  min-height: 100%;
  padding: 28px;
  background: #f6f7f9;
  box-sizing: border-box;
}

.page-header {
  display: flex;
  justify-content: space-between;
  align-items: flex-start;
  gap: 16px;
  margin-bottom: 24px;
}

.page-header h1 {
  margin: 0;
  font-size: 3rem;
  line-height: 1;
  color: #0f172a;
}

.page-header p {
  margin: 8px 0 0;
  color: #64748b;
  font-size: 1rem;
}

.header-chip {
  display: inline-flex;
  align-items: center;
  justify-content: center;
  min-width: 44px;
  height: 44px;
  padding: 0 14px;
  border-radius: 999px;
  background: #020617;
  color: #ffffff;
  font-weight: 700;
}

.summary-panel {
  display: grid;
  grid-template-columns: repeat(3, minmax(0, 1fr));
  gap: 16px;
  margin-bottom: 22px;
}

.summary-card {
  padding: 26px 20px;
  border: 1px solid #e5e7eb;
  border-radius: 18px;
  background: #ffffff;
  text-align: center;
  box-shadow: 0 10px 24px rgba(15, 23, 42, 0.04);
}

.summary-card strong {
  display: block;
  margin-bottom: 8px;
  font-size: 2rem;
  color: #0f172a;
}

.summary-card span {
  color: #64748b;
}

.accent-neutral strong {
  color: #475569;
}

.accent-warn strong {
  color: #d97706;
}

.page-state {
  padding: 14px;
  border-radius: 12px;
  background: #eef3f9;
  color: #334155;
  text-align: center;
}

.page-state.error {
  background: #fef2f2;
  color: #b91c1c;
}
.pagination-bar {
  display: flex;
  justify-content: center;
  align-items: center;
  gap: 12px;
  margin-top: 22px;
}
.pagination-btn {
  min-width: 110px;
  height: 40px;
  padding: 0 16px;
  border: 1px solid #dbe1ea;
  border-radius: 12px;
  background: #ffffff;
  color: #0f172a;
  font-weight: 600;
}
.pagination-btn:disabled {
  opacity: .45;
}
.pagination-info {
  color: #475569;
  font-weight: 600;
}

@media screen and (max-width: 900px) {
  .summary-panel {
    grid-template-columns: 1fr;
  }
}

@media screen and (max-width: 767px) {
  .user-validation-page {
    padding: 16px;
  }

  .page-header {
    flex-direction: column;
    align-items: stretch;
  }

  .page-header h1 {
    font-size: 2.2rem;
  }
}
</style>
