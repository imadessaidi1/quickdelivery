<template>
  <div class="documents-step">
    <section class="section-head">
      <span class="eyebrow">{{ $t('wizardUserStepDocs') }}</span>
      <h3>{{ $t('userRegistrationDocumentsTitle') }}</h3>
      <p>{{ $t('userRegistrationDocumentsSubtitle') }}</p>
    </section>

    <div class="upload-grid">
      <DocumentUploadCard
        v-for="document in visibleDocuments"
        :key="document.key"
        :label="$t(document.label)"
        :status="userDocuments?.[document.key]?.documentStatus"
        :file-name="fileName(document.key)"
        :review-comment="documentReview(document.key)"
        :error-message="filesErrorMessages[document.key]"
        :accept="document.accept"
        @change="handleUserFileChange($event, document.key)"
      />
    </div>

    <section class="payment-card">
      <div class="section-head compact">
        <span class="eyebrow">{{ $t('userPaymentModes') }}</span>
        <h3>{{ $t('userRegistrationPaymentTitle') }}</h3>
      </div>

      <div class="payment-toggle">
        <label v-for="option in paymentOptions" :key="option.value" class="toggle-option" :class="{ active: internalSelectedPaymentType === option.value }">
          <input v-model="internalSelectedPaymentType" type="radio" name="payment-type" :value="option.value">
          <span>{{ $t(option.label) }}</span>
        </label>
      </div>

      <div class="payment-pane">
        <CreditCard v-if="internalSelectedPaymentType === 'CARD'" />
        <IBAN v-if="internalSelectedPaymentType === 'IBAN'" :files-error-messages="filesErrorMessages" :is-for-update="isForUpdate" />
      </div>
    </section>
  </div>
</template>

<script>
import CreditCard from './CreditCard.vue';
import IBAN from './IbanBank.vue';
import { getRequiredUserDocuments } from '@/config/deliveryMode';
import DocumentUploadCard from './DocumentUploadCard.vue';

export default {
  emits: ['update:selectedPaymentType'],
  components: {
    CreditCard,
    IBAN,
    DocumentUploadCard,
  },
  props: {
    isForUpdate: {
      type: Boolean,
      default: false,
    },
    selectedPaymentType: {
      type: String,
      default: 'CARD',
    },
  },
  computed: {
    user() {
      return this.$store.state.user;
    },
    userDocuments() {
      return this.$store.state.userDocuments;
    },
    canUpdateRejectedOnly() {
      return this.isForUpdate && this.user?.activeAccount !== true && !this.isResumeOnboarding;
    },
    isResumeOnboarding() {
      const status = this.user?.onboarding?.status;
      return ['ACCOUNT_CREATED', 'PROFILE_COMPLETED', 'DOCUMENTS_UPLOADED'].includes(status);
    },
    visibleDocuments() {
      const definitionByKey = {
        ID: { key: 'ID', label: 'userDocumentID', ref: 'fileInputID', accept: 'image/*, application/pdf' },
        PICTURE: { key: 'PICTURE', label: 'PICTURE', ref: 'fileInputPICTURE', accept: 'image/*' },
        DRIVER_LICENCE: { key: 'DRIVER_LICENCE', label: 'userDocumentDriverLicence', ref: 'fileInputDRIVER_LICENCE', accept: 'image/*, application/pdf' },
        USER_COMPANY_EXTRACT: { key: 'USER_COMPANY_EXTRACT', label: 'userDocumentCompanyExtract', ref: 'fileInputUSER_COMPANY_EXTRACT', accept: 'image/*, application/pdf' },
        USER_COMPANY_INSURANCE: { key: 'USER_COMPANY_INSURANCE', label: 'userDocumentCompanyInsurance', ref: 'fileInputUSER_COMPANY_INSURANCE', accept: 'image/*, application/pdf' },
      };
      const documentKeys = this.user.type === 'DELIVERY_PERSON'
        ? getRequiredUserDocuments(this.user)
        : ['ID', 'PICTURE'];
      const documents = documentKeys.map((key) => definitionByKey[key]).filter(Boolean);
      if (!this.canUpdateRejectedOnly) {
        return documents;
      }
      return documents.filter((document) => ['REJECTED', 'UPDATED'].includes(this.userDocuments?.[document.key]?.documentStatus));
    },
  },
  data() {
    return {
      filesErrorMessages: [],
      internalSelectedPaymentType: 'CARD',
      paymentOptions: [
        { value: 'CARD', label: 'userPaymentCreditCard' },
        { value: 'IBAN', label: 'userIBAN' },
      ],
    };
  },
  mounted() {
    if (this.user.paymentModes?.IBAN?.iban) {
      this.internalSelectedPaymentType = 'IBAN';
    } else if (this.user.paymentModes?.CREDIT_CARD?.cardNumber) {
      this.internalSelectedPaymentType = 'CARD';
    } else {
      this.internalSelectedPaymentType = this.selectedPaymentType;
    }
    this.$emit('update:selectedPaymentType', this.internalSelectedPaymentType);
  },
  watch: {
    selectedPaymentType(value) {
      this.internalSelectedPaymentType = value || 'CARD';
    },
    internalSelectedPaymentType(value) {
      this.$emit('update:selectedPaymentType', value);
    },
    userDocuments: {
      deep: true,
      handler(documents) {
        Object.keys(this.filesErrorMessages).forEach((key) => {
          if (documents?.[key]?.file) {
            delete this.filesErrorMessages[key];
          }
        });
      },
    },
  },
  methods: {
    handleUserFileChange(event, type) {
      const file = event?.target?.files?.[0];
      if (!file) {
        return;
      }
      this.$store.commit('updateUserDocuments', {
        ...this.userDocuments,
        [type]: {
          ...this.userDocuments?.[type],
          file,
          name: file.name,
          documentStatus: this.isForUpdate ? 'UPDATED' : 'ACCEPTED',
        },
      });
      delete this.filesErrorMessages[type];
    },
    fileName(type) {
      const storedName = this.userDocuments?.[type]?.name;
      if (storedName && ![type, 'BANK ID'].includes(storedName)) {
        return storedName;
      }
      if (this.userDocuments?.[type]) {
        return this.$t(type);
      }
      return this.$t('packageDocumentMissing');
    },
    documentReview(type) {
      const document = this.userDocuments?.[type];
      if (!document?.reviewComment) {
        return '';
      }
      return document.reviewComment;
    },
  },
};
</script>

<style scoped>
.documents-step {
  display: grid;
  gap: 24px;
  min-width: 0;
}

.section-head h3 {
  margin: 6px 0 8px;
  font-size: 1.6rem;
  font-weight: 800;
  color: var(--qd-text);
}

.section-head p {
  color: var(--qd-muted);
}

.section-head.compact {
  margin-bottom: 16px;
}

.upload-grid {
  display: grid;
  grid-template-columns: repeat(3, 1fr);
  gap: 20px;
}

.payment-card {
  padding: 24px;
  border: 1px solid var(--qd-border);
  border-radius: 20px;
  background: #fff;
  box-shadow: 0 10px 40px rgba(15, 23, 42, 0.06);
}

.payment-toggle {
  display: flex;
  gap: 12px;
  margin-bottom: 24px;
}

.toggle-option {
  display: flex;
  align-items: center;
  gap: 8px;
  padding: 12px 20px;
  border: 1px solid var(--qd-border);
  border-radius: 999px;
  background: var(--qd-surface-strong);
  transition: var(--qd-transition);
  cursor: pointer;
}

.toggle-option:hover {
  background: #fff;
  border-color: var(--qd-primary-soft);
}

.toggle-option.active {
  border-color: var(--qd-primary);
  background: var(--qd-primary-soft);
  color: var(--qd-primary);
}

.errorMessage {
  display: block;
  font-size: 0.8rem;
  color: var(--qd-danger);
  font-weight: 600;
  margin-top: 4px;
}

@media (max-width: 1024px) {
  .upload-grid { grid-template-columns: repeat(2, 1fr); }
}

@media (max-width: 600px) {
  .upload-grid { grid-template-columns: 1fr; }
}
</style>
