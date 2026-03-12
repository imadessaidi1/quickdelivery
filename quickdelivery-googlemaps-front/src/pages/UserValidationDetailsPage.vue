<template>
  <div class="validation-details-page">
    <div class="page-head">
      <button class="btn primary_btn back-btn" type="button" @click="goBack">
        {{ $t('actionBack') }}
      </button>
      <div v-if="selectedUser">
        <h1>{{ selectedUser.firstName }} {{ selectedUser.lastName }}</h1>
        <p>{{ selectedUser.emailAddress }}</p>
      </div>
    </div>

    <div v-if="!selectedUser" class="page-state error">{{ $t('stateLoadError') }}</div>

    <template v-else>
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

const STORAGE_KEY = 'qd_validation_user';

export default {
  components: {
    UserDetails,
    DocumentViewer,
  },
  props: {
    returnTo: {
      type: String,
      default: '/usersAccountValidation',
    },
  },
  data() {
    return {
      selectedUser: null,
    };
  },
  computed: {
    selectedVehicle() {
      return this.selectedUser?.vehicles?.[0] || {};
    },
  },
  mounted() {
    const rawUser = sessionStorage.getItem(STORAGE_KEY);
    if (!rawUser) {
      this.goBack();
      return;
    }
    try {
      this.selectedUser = JSON.parse(rawUser);
    } catch (_error) {
      this.goBack();
    }
  },
  methods: {
    goBack() {
      if (window.history.length > 1) {
        this.$router.back();
        return;
      }
      this.$router.push(this.returnTo || '/usersAccountValidation');
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
        sessionStorage.removeItem(STORAGE_KEY);
        this.$router.push('/usersAccountValidation');
      } catch (error) {
        console.error('Unable to process your request at this time. Please try again later.', error);
      }
    },
  },
};
</script>

<style scoped>
.validation-details-page {
  min-height: 100%;
  padding: 24px;
  background: #f6f7f9;
  box-sizing: border-box;
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
}
</style>
