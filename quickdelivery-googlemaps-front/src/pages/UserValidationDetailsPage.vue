<template>
  <div class="validation-details-page">
    <template v-if="isLoadingPage">
      <div class="page-head">
        <button v-if="showBackButton" class="btn primary_btn back-btn" type="button" @click="goBack">
          {{ $t('actionBack') }}
        </button>
      </div>
      <div class="page-state">{{ $t('stateLoading') }}</div>
    </template>

    <template v-else-if="loadError || !selectedUser">
      <div class="page-head">
        <button v-if="showBackButton" class="btn primary_btn back-btn" type="button" @click="goBack">
          {{ $t('actionBack') }}
        </button>
      </div>
      <div class="page-state error">{{ $t('stateLoadError') }}</div>
    </template>

    <template v-else>
      <div class="page-head">
        <button v-if="showBackButton" class="btn primary_btn back-btn" type="button" @click="goBack">
          {{ $t('actionBack') }}
        </button>
        <div>
          <h1>{{ selectedUser.firstName }} {{ selectedUser.lastName }}</h1>
          <p>{{ selectedUser.emailAddress }}</p>
        </div>
      </div>

      <div class="page-grid">
        <div class="details-card">
          <UserDetails :user="selectedUser" :vehicle="selectedVehicle" :userDocuments="selectedUser.documents || {}" :show-update-button="false" />
        </div>
        <div class="documents-card">
          <DocumentViewer :documents="selectedUser.document || {}" />
        </div>
      </div>

      <div class="decision-panel">
        <label class="toggle-line" for="validationCheckboxPage">
          <input id="validationCheckboxPage" v-model="selectedUser.activeAccount" type="checkbox">
          <span>{{ $t('userValidationCheckboxLabel') }}</span>
        </label>
        <button class="btn primary_btn save-btn" type="button" @click="saveValidation">
          {{ $t('userValidationSave') }}
        </button>
      </div>
    </template>
  </div>
</template>

<script>
import http from '@/config/httpInterceptor';
import DocumentViewer from '../components/DocumentViwer.vue';
import UserDetails from '../components/UserDetails.vue';

export default {
  components: {
    UserDetails,
    DocumentViewer,
  },
  props: {
    returnTo: {
      type: String,
      default: '',
    },
  },
  data() {
    return {
      selectedUser: null,
      isLoadingPage: false,
      loadError: false,
    };
  },
  computed: {
    selectedVehicle() {
      return this.selectedUser?.vehicles?.[0] || {};
    },
    showBackButton() {
      return !!this.returnTo;
    },
  },
  mounted() {
    this.loadUser();
  },
  methods: {
    async loadUser() {
      const userId = this.$route.query.id;
      if (!userId) {
        this.goBack();
        return;
      }
      this.isLoadingPage = true;
      this.loadError = false;
      try {
        const response = await http.get(`${this.$i18n.t('userRootURL')}${this.$i18n.t('getUserById')}${encodeURIComponent(userId)}`);
        this.selectedUser = response.data;
      } catch (_error) {
        this.loadError = true;
      } finally {
        this.isLoadingPage = false;
      }
    },
    goBack() {
      if (this.returnTo) {
        this.$router.push(this.returnTo);
        return;
      }
      this.$router.push('/usersAccountValidation');
    },
    async saveValidation() {
      const formData = new FormData();
      const userLanguage = navigator.languages && navigator.languages.length ? navigator.languages[0] : navigator.language || 'fr-FR';
      const url = this.$i18n.t('userRootURL') + this.$i18n.t('validateUser');
      formData.append('locale', userLanguage);
      const userCopy = JSON.parse(JSON.stringify(this.selectedUser));
      if (userCopy.document) {
        for (const docType in userCopy.document) {
          userCopy.document[docType].data = '';
        }
      }
      formData.append('user', JSON.stringify(userCopy));
      try {
        await http.put(url, formData);
        this.$router.push(this.returnTo || '/usersAccountValidation');
      } catch (error) {
        console.error('Unable to process your request at this time. Please try again later.', error);
      }
    },
  },
};
</script>

<style scoped>
.validation-details-page {
  min-height: 100vh;
  padding: 24px;
  background: #f6f7f9;
  box-sizing: border-box;
  display: flex;
  flex-direction: column;
}

.page-head {
  display: flex;
  align-items: flex-start;
  gap: 16px;
  margin-bottom: 20px;
}

.page-head h1 {
  margin: 0;
  color: #0f172a;
}

.page-head p {
  margin: 6px 0 0;
  color: #64748b;
}

.back-btn {
  min-width: 110px;
  display: inline-flex;
  align-items: center;
  justify-content: center;
  height: 42px;
  padding: 0 18px;
  border: none;
  border-radius: 12px;
  background: #020617;
  color: #ffffff;
  font-weight: 600;
  box-shadow: 0 10px 22px rgba(15, 23, 42, 0.12);
}

.page-grid {
  display: grid;
  grid-template-columns: minmax(320px, 1fr) minmax(380px, 1.2fr);
  gap: 18px;
  align-items: stretch;
  flex: 1 1 auto;
  min-height: 0;
}

.details-card,
.documents-card,
.decision-panel {
  border: 1px solid #e5e7eb;
  border-radius: 18px;
  background: #ffffff;
  overflow: hidden;
  box-shadow: 0 12px 28px rgba(15, 23, 42, 0.06);
}

.details-card,
.documents-card {
  min-height: clamp(560px, calc(100vh - 250px), 860px);
}

.documents-card {
  min-width: 0;
}

.decision-panel {
  display: flex;
  justify-content: space-between;
  align-items: center;
  gap: 16px;
  margin-top: 18px;
  padding: 18px 20px;
}

.toggle-line {
  display: inline-flex;
  align-items: center;
  gap: 10px;
  color: #334155;
  font-weight: 600;
}

.save-btn {
  min-width: 156px;
  display: inline-flex;
  align-items: center;
  justify-content: center;
  height: 42px;
  padding: 0 18px;
  border: none;
  border-radius: 12px;
  background: #020617;
  color: #ffffff;
  font-weight: 600;
  box-shadow: 0 10px 22px rgba(15, 23, 42, 0.12);
}

.page-state {
  padding: 14px;
  border-radius: 12px;
  background: #fef2f2;
  color: #b91c1c;
  text-align: center;
}

@media screen and (max-width: 1200px) {
  .page-grid {
    grid-template-columns: 1fr;
  }

  .details-card,
  .documents-card {
    min-height: 520px;
  }
}

@media screen and (max-width: 767px) {
  .validation-details-page {
    padding: 16px;
  }

  .page-head,
  .decision-panel {
    flex-direction: column;
    align-items: stretch;
  }

  .save-btn {
    width: 100%;
  }

  .details-card,
  .documents-card {
    min-height: 460px;
  }
}
</style>
