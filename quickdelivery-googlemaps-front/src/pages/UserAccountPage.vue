<template>
  <div class="user-account-page">
    <div class="page-header">
      <div>
        <h1>{{ $t('userAccountPageTitle') }}</h1>
        <p>{{ $t('userAccountPageSubtitle') }}</p>
      </div>
      <div class="header-chip" v-if="selectedUser">{{ $t(selectedUser.type || 'userType') }}</div>
    </div>

    <div class="page-state" v-if="isLoadingPage">{{ $t('stateLoading') }}</div>
    <div v-else-if="loadError" class="page-state error">{{ $t('stateLoadError') }}</div>
    <UserAccount v-else-if="selectedUser" :selectedUser="selectedUser"/>
  </div>
</template>

<script>
import UserAccount from '../components/UserAccount.vue';
import http from '@/config/httpInterceptor';

export default {
  components: {
    UserAccount,
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
  methods: {
    async loadUser() {
      try {
        const response = await http.get(this.$i18n.t('userRootURL') + this.$i18n.t('getUserByEmail') + this.$store.state.connectedUser.email);
        return response.data;
      } catch (error) {
        console.error("Unable to process your request at this time. Please try again later.", error);
        throw error;
      }
    },
  },
};
</script>
<style>
.user-account-page {
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
  min-height: 44px;
  padding: 0 18px;
  border-radius: 999px;
  background: #020617;
  color: #ffffff;
  font-weight: 700;
  text-align: center;
  overflow-wrap: anywhere;
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

@media screen and (max-width: 767px) {
  .user-account-page {
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
