<template>
  <div class="user-validation-page">
    <div class="page-header">
      <div>
        <h1>{{ $t('validationPageTitle') }}</h1>
        <p>{{ $t('validationPageSubtitle') }}</p>
      </div>
      <div class="header-chip">{{ usersList.length }}</div>
    </div>

    <div class="summary-panel" v-if="!isLoadingPage && !loadError">
      <div class="summary-card">
        <strong>{{ usersList.length }}</strong>
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

    <div class="page-state" v-if="isLoadingPage">{{ $t('stateLoading') }}</div>
    <div v-else-if="loadError" class="page-state error">{{ $t('stateLoadError') }}</div>
    <div v-else-if="usersList.length === 0" class="page-state">{{ $t('stateEmptyUsersValidation') }}</div>
    <UsersAccountList v-else :users="usersList"/>
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
    };
  },
  computed: {
    totalVehicles() {
      return this.usersList.reduce((count, user) => count + (user.vehicles?.length || 0), 0);
    },
    totalDocuments() {
      return this.usersList.reduce((count, user) => {
        return count + Object.keys(user.documents || user.document || {}).length;
      }, 0);
    },
  },
  mounted() {
    this.loadUsers();
  },
  methods: {
    async loadUsers() {
      this.isLoadingPage = true;
      this.loadError = false;
      try {
        const response = await http.get(this.$i18n.t('userRootURL') + this.$i18n.t('getUsersForValidation'));
        this.usersList = Array.isArray(response.data) ? response.data : [];
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
