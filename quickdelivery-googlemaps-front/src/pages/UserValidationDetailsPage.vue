<template>
  <div class="validation-details-page qd-page">
    <template v-if="isLoadingPage">
      <header class="qd-page-header">
        <div class="header-main">
          <button v-if="showBackButton" class="back-link qd-btn-secondary" type="button" @click="goBack" style="margin-bottom: 12px;">
            <span class="icon">←</span> {{ $t('actionBack') }}
          </button>
          <h1>{{ $t('stateLoading') }}</h1>
        </div>
      </header>
      <div class="page-state loading-state">
        <div class="spinner"></div>
        {{ $t('stateLoading') }}
      </div>
    </template>

    <template v-else-if="loadError || !selectedUser">
      <header class="qd-page-header">
        <div class="header-main">
          <button v-if="showBackButton" class="back-link qd-btn-secondary" type="button" @click="goBack" style="margin-bottom: 12px;">
            <span class="icon">←</span> {{ $t('actionBack') }}
          </button>
          <h1>{{ $t('stateLoadError') }}</h1>
        </div>
      </header>
      <div class="page-state error-state">{{ $t('stateLoadError') }}</div>
    </template>

    <template v-else>
      <header class="qd-page-header">
        <div class="header-main">
          <button v-if="showBackButton" class="back-link qd-btn-secondary" type="button" @click="goBack" style="margin-bottom: 12px;">
            <span class="icon">←</span> {{ $t('actionBack') }}
          </button>
          <div class="header-profile">
            <div class="avatar-placeholder">
              {{ selectedUser.firstName.charAt(0) }}{{ selectedUser.lastName.charAt(0) }}
            </div>
            <div class="profile-text">
              <h1>{{ selectedUser.firstName }} {{ selectedUser.lastName }}</h1>
              <p class="email">{{ selectedUser.emailAddress }}</p>
            </div>
          </div>
        </div>
      </header>

      <div class="content-split">
        <section class="details-section">
          <div class="section-card">
            <div class="card-header">
              <h3>{{ $t('userInfo') }}</h3>
            </div>
            <div class="card-body scrollable">
              <UserDetails :user="selectedUser" :vehicle="selectedVehicle" :userDocuments="normalizedDocuments" :show-update-button="false" />
            </div>
          </div>
        </section>

        <section class="documents-section">
          <div class="section-card fill-height">
            <div class="card-header">
              <h3>{{ $t('userDocuments') }}</h3>
            </div>
            <div class="card-body">
              <DocumentViewer :documents="normalizedDocuments" />
            </div>
          </div>
        </section>
      </div>

      <footer class="action-footer">
        <div class="footer-content">
          <div class="status-control">
            <label class="premium-toggle" for="validationCheckboxPage">
              <input id="validationCheckboxPage" v-model="selectedUser.activeAccount" type="checkbox">
              <span class="toggle-track"></span>
              <span class="label-text">{{ $t('userValidationCheckboxLabel') }}</span>
            </label>
          </div>
          <div class="actions">
            <button class="qd-btn-primary save-action" type="button" @click="saveValidation">
              {{ $t('userValidationSave') }}
            </button>
          </div>
        </div>
      </footer>
    </template>
  </div>
</template>

<script>
import http from '@/config/httpInterceptor';
import DocumentViewer from '../components/DocumentViwer.vue';
import UserDetails from '../components/UserDetails.vue';
import { normalizeDocumentCollection } from '@/config/documents';

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
    normalizedDocuments() {
      return normalizeDocumentCollection(this.selectedUser?.document || this.selectedUser?.documents || {});
    },
    showBackButton() {
      return !!this.returnTo || true; // Force show back button
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
        console.error(this.$t('requestErrorGeneric'), error);
      }
    },
  },
};
</script>

<style scoped>
.validation-details-page {
  padding-bottom: 120px !important;
}

.back-link {
  display: inline-flex;
  align-items: center;
  gap: 8px;
  height: 36px;
  padding: 0 16px;
  border-radius: 10px;
  font-size: 0.85rem;
  font-weight: 700;
}

.page-head {
  display: flex;
  flex-direction: column;
  gap: 24px;
}

.back-link {
  display: flex;
  align-items: center;
  gap: 8px;
  background: none;
  border: none;
  color: #64748b;
  font-weight: 700;
  font-size: 0.9375rem;
  cursor: pointer;
  padding: 0;
  transition: all 0.2s cubic-bezier(0.4, 0, 0.2, 1);
}

.back-link:hover {
  color: #0f172a;
  transform: translateX(-4px);
}

.back-link .icon {
  font-size: 1.1rem;
}

.header-profile {
  display: flex;
  align-items: center;
  gap: 20px;
}

.avatar-placeholder {
  width: 64px;
  height: 64px;
  background: #0f172a;
  color: #fff;
  border-radius: 20px;
  display: flex;
  align-items: center;
  justify-content: center;
  font-size: 1.5rem;
  font-weight: 700;
  box-shadow: 0 8px 20px rgba(15, 23, 42, 0.15);
}

.profile-text h1 {
  margin: 0;
  font-size: 2rem;
  font-weight: 800;
  color: #0f172a;
  letter-spacing: -0.025em;
}

.profile-text .email {
  margin: 4px 0 0;
  color: #64748b;
  font-size: 1rem;
}

/* Content Split */
.content-split {
  display: grid;
  grid-template-columns: 420px 1fr;
  gap: 24px;
  flex: 1;
  min-height: 0;
}

.section-card {
  background: #fff;
  border-radius: 24px;
  border: 1px solid #f1f5f9;
  box-shadow: 0 10px 30px rgba(0, 0, 0, 0.03);
  display: flex;
  flex-direction: column;
  overflow: hidden;
}

.fill-height {
  height: 100%;
}

.card-header {
  padding: 20px 24px;
  border-bottom: 1px solid #f1f5f9;
}

.card-header h3 {
  margin: 0;
  font-size: 1rem;
  font-weight: 700;
  color: #0f172a;
  text-transform: uppercase;
  letter-spacing: 0.05em;
}

.card-body {
  flex: 1;
  min-height: 0;
}

.card-body.scrollable {
  overflow-y: auto;
  padding: 8px;
}

/* Action Footer */
.action-footer {
  position: sticky;
  bottom: 0px;
  background: rgba(255, 255, 255, 0.8);
  backdrop-filter: blur(12px);
  -webkit-backdrop-filter: blur(12px);
  border: 1px solid rgba(255, 255, 255, 0.8);
  border-radius: 24px;
  margin-top: 24px;
  box-shadow: 0 -10px 40px rgba(0, 0, 0, 0.05);
}

.footer-content {
  display: flex;
  justify-content: space-between;
  align-items: center;
  padding: 20px 32px;
}

.premium-toggle {
  display: flex;
  align-items: center;
  gap: 16px;
  cursor: pointer;
  user-select: none;
}

.premium-toggle input {
  display: none;
}

.toggle-track {
  width: 52px;
  height: 28px;
  background: #e2e8f0;
  border-radius: 100px;
  position: relative;
  transition: background 0.3s;
}

.toggle-track::after {
  content: "";
  position: absolute;
  top: 4px;
  left: 4px;
  width: 20px;
  height: 20px;
  background: #fff;
  border-radius: 50%;
  box-shadow: 0 2px 4px rgba(0, 0, 0, 0.1);
  transition: transform 0.3s;
}

.premium-toggle input:checked + .toggle-track {
  background: #10b981;
}

.premium-toggle input:checked + .toggle-track::after {
  transform: translateX(24px);
}

.label-text {
  font-weight: 700;
  color: #0f172a;
  font-size: 1rem;
}

/* .btn-premium handled by design-system.css qd-btn-primary */

/* States */
.page-state {
  display: flex;
  align-items: center;
  justify-content: center;
  padding: 80px;
  font-weight: 600;
  color: #64748b;
  background: #fff;
  border-radius: 24px;
}

.spinner {
  width: 24px;
  height: 24px;
  border: 3px solid #f1f5f9;
  border-top: 3px solid #0f172a;
  border-radius: 50%;
  animation: spin 1s linear infinite;
  margin-right: 16px;
}

@keyframes spin { to { transform: rotate(360deg); } }

@media screen and (max-width: 1024px) {
  .content-split {
    grid-template-columns: 1fr;
  }
  .validation-details-page {
    padding: 12px;
  }
}

@media screen and (max-width: 768px) {
  .back-link,
  .save-action {
    width: 100%;
    justify-content: center;
    border-radius: 999px !important;
  }

  .actions {
    width: 100%;
  }

  .footer-content {
    flex-direction: column;
    gap: 10px;
    align-items: stretch;
  }
/* mobile overrides handled by design-system.css */
}
</style>
