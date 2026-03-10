<template>
  <div class="accounts-shell">
    <div class="accounts-grid">
      <article v-for="user in users" :key="user.id || user.emailAddress" class="account-card">
        <div class="card-head">
          <div>
            <h3>{{ user.firstName }} {{ user.lastName }}</h3>
            <p>{{ user.emailAddress }}</p>
          </div>
          <span class="status-chip">{{ $t('validationPendingBadge') }}</span>
        </div>

        <div class="account-meta">
          <div class="meta-row">
            <span class="material-symbols-outlined">call</span>
            <span>{{ user.phone || '-' }}</span>
          </div>
          <div class="meta-row">
            <span class="material-symbols-outlined">directions_car</span>
            <span>{{ vehicleSummary(user) }}</span>
          </div>
          <div class="meta-row">
            <span class="material-symbols-outlined">description</span>
            <span>{{ documentCount(user) }} {{ $t('validationDocumentsLabel') }}</span>
          </div>
        </div>

        <div class="card-actions">
          <button class="details-btn" @click="showDetails(user)">{{ $t('packagesArroundMArkerDetailActionsDetails') }}</button>
        </div>
      </article>
    </div>

    <div v-if="selectedUser" class="validation-modal">
      <div class="validation-modal__content">
        <button class="close-btn" @click="hideDetails">
          <span class="material-symbols-outlined">close</span>
        </button>

        <div class="modal-header">
          <div>
            <h2>{{ selectedUser.firstName }} {{ selectedUser.lastName }}</h2>
            <p>{{ selectedUser.emailAddress }}</p>
          </div>
          <span class="status-chip">{{ $t('validationPendingBadge') }}</span>
        </div>

        <div class="modal-grid">
          <div class="details-card">
            <UserDetails :user="selectedUser" :vehicle="selectedVehicle" :userDocuments="selectedUser.documents || {}"/>
          </div>
          <div class="documents-card">
            <DocumentViewer :documents="selectedUser.document"/>
          </div>
        </div>

        <div class="decision-panel">
          <label class="toggle-line" for="validationCheckbox">
            <input id="validationCheckbox" v-model="selectedUser.activeAccount" type="checkbox">
            <span>{{ $t('userValidationCheckboxLabel') }}</span>
          </label>
          <button class="details-btn save-btn" @click="saveValidation">{{ $t('userValidationSave') }}</button>
        </div>
      </div>
    </div>
  </div>
</template>

<script>
import UserDetails from '../components/UserDetails.vue';
import DocumentViewer from '../components/DocumentViwer.vue';
import http from '@/config/httpInterceptor';

export default {
  components: {
    UserDetails,
    DocumentViewer,
  },
  props: {
    users: {
      type: Array,
      required: true,
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
  methods: {
    showDetails(user) {
      this.selectedUser = JSON.parse(JSON.stringify(user));
    },
    hideDetails() {
      this.selectedUser = null;
    },
    vehicleSummary(user) {
      const vehicle = user?.vehicles?.[0];
      if (!vehicle) {
        return '-';
      }
      return [vehicle.brand, vehicle.model, vehicle.registrationNumber].filter(Boolean).join(' - ');
    },
    documentCount(user) {
      const directDocs = Object.keys(user?.document || {}).length;
      const userDocs = Object.keys(user?.documents || {}).length;
      return directDocs || userDocs || 0;
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
        this.hideDetails();
        this.$parent.loadUsers();
      } catch (error) {
        console.error('Unable to process your request at this time. Please try again later.', error);
      }
    },
  },
};
</script>

<style scoped>
.accounts-shell {
  width: 100%;
}

.accounts-grid {
  display: grid;
  grid-template-columns: repeat(3, minmax(0, 1fr));
  gap: 18px;
}

.account-card {
  padding: 20px;
  border: 1px solid #e5e7eb;
  border-radius: 18px;
  background: #ffffff;
  box-shadow: 0 12px 28px rgba(15, 23, 42, 0.06);
}

.card-head {
  display: flex;
  justify-content: space-between;
  align-items: flex-start;
  gap: 12px;
  margin-bottom: 18px;
}

.card-head h3 {
  margin: 0;
  font-size: 1.45rem;
  line-height: 1.1;
  color: #0f172a;
}

.card-head p {
  margin: 6px 0 0;
  color: #64748b;
  overflow-wrap: anywhere;
}

.status-chip {
  display: inline-flex;
  align-items: center;
  padding: 6px 10px;
  border-radius: 999px;
  background: #fef3c7;
  color: #b45309;
  font-size: 0.82rem;
  font-weight: 700;
  white-space: nowrap;
}

.account-meta {
  display: flex;
  flex-direction: column;
  gap: 10px;
  padding-bottom: 18px;
  margin-bottom: 18px;
  border-bottom: 1px solid #eef2f7;
}

.meta-row {
  display: flex;
  gap: 10px;
  align-items: flex-start;
  color: #334155;
}

.meta-row span:last-child {
  overflow-wrap: anywhere;
}

.details-btn {
  display: inline-flex;
  align-items: center;
  justify-content: center;
  min-width: 132px;
  height: 40px;
  padding: 0 16px;
  border: none;
  border-radius: 12px;
  background: #020617;
  color: #ffffff;
  font-weight: 600;
}

.card-actions {
  display: flex;
  justify-content: flex-start;
}

.validation-modal {
  position: fixed;
  inset: 0;
  z-index: 50;
  display: flex;
  justify-content: center;
  align-items: flex-start;
  padding: 24px;
  background: rgba(15, 23, 42, 0.42);
  overflow-y: auto;
}

.validation-modal__content {
  position: relative;
  width: min(1220px, 100%);
  padding: 24px;
  border-radius: 22px;
  background: #f8fafc;
  box-shadow: 0 24px 60px rgba(15, 23, 42, 0.18);
}

.modal-header {
  display: flex;
  justify-content: space-between;
  align-items: flex-start;
  gap: 16px;
  margin-bottom: 18px;
}

.modal-header h2 {
  margin: 0;
  color: #0f172a;
}

.modal-header p {
  margin: 6px 0 0;
  color: #64748b;
}

.modal-grid {
  display: grid;
  grid-template-columns: minmax(320px, 1fr) minmax(380px, 1.2fr);
  gap: 18px;
}

.details-card,
.documents-card {
  border: 1px solid #e5e7eb;
  border-radius: 18px;
  background: #ffffff;
  overflow: hidden;
}

.decision-panel {
  display: flex;
  justify-content: space-between;
  align-items: center;
  gap: 16px;
  margin-top: 18px;
  padding: 18px 20px;
  border: 1px solid #e5e7eb;
  border-radius: 18px;
  background: #ffffff;
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
}

.close-btn {
  position: absolute;
  top: 14px;
  right: 14px;
  display: inline-flex;
  align-items: center;
  justify-content: center;
  width: 36px;
  height: 36px;
  border: 1px solid #dbe1ea;
  border-radius: 999px;
  background: #ffffff;
  color: #0f172a;
}

@media screen and (max-width: 1200px) {
  .accounts-grid {
    grid-template-columns: repeat(2, minmax(0, 1fr));
  }

  .modal-grid {
    grid-template-columns: 1fr;
  }
}

@media screen and (max-width: 767px) {
  .accounts-grid {
    grid-template-columns: 1fr;
  }

  .validation-modal {
    padding: 12px;
  }

  .validation-modal__content {
    padding: 18px;
  }

  .decision-panel {
    flex-direction: column;
    align-items: stretch;
  }

  .save-btn {
    width: 100%;
  }
}
</style>
