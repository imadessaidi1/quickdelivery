<template>
  <div class="documents-step">
    <section class="section-head">
      <span class="eyebrow">{{ $t('wizardUserStepDocs') }}</span>
      <h3>{{ $t('userRegistrationDocumentsTitle') }}</h3>
      <p>{{ $t('userRegistrationDocumentsSubtitle') }}</p>
    </section>

    <div class="upload-grid">
      <label v-for="document in visibleDocuments" :key="document.key" class="upload-card">
        <span>{{ $t(document.label) }}</span>
        <input
          :ref="document.ref"
          type="file"
          :accept="document.accept"
          @change="handleUserFileChange(document.ref, document.key)"
        >
        <strong>{{ fileName(document.key) }}</strong>
        <small v-if="documentStatus(document.key)">{{ documentStatus(document.key) }}</small>
        <div v-if="documentReview(document.key)" class="review-note">
          <span class="review-label">{{ $t('userDocumentReviewCommentLabel') }}</span>
          <p>{{ documentReview(document.key) }}</p>
        </div>
        <span v-if="filesErrorMessages[document.key]" class="errorMessage">{{ filesErrorMessages[document.key] }}</span>
      </label>
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

export default {
  emits: ['update:selectedPaymentType'],
  components: {
    CreditCard,
    IBAN,
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
      return this.isForUpdate && this.user?.activeAccount !== true;
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
    handleUserFileChange(refName, type) {
      const file = this.$refs[refName]?.[0]?.files?.[0] || this.$refs[refName]?.files?.[0];
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
    documentStatus(type) {
      const status = this.userDocuments?.[type]?.documentStatus;
      if (!status || status === 'UPDATED') {
        return '';
      }
      return this.$t(status);
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
  gap: 22px;
  min-width: 0;
}

.documents-step,
.documents-step * {
  box-sizing: border-box;
}

.section-head h3 {
  margin: 6px 0 8px;
  font-size: 1.4rem;
  color: #14213d;
}

.section-head p {
  margin: 0;
  color: #617086;
}

.section-head.compact {
  margin-bottom: 16px;
}

.eyebrow {
  display: inline-flex;
  align-items: center;
  padding: 4px 10px;
  border-radius: 999px;
  background: #edf4ff;
  color: #27548a;
  font-size: 0.78rem;
  font-weight: 700;
  text-transform: uppercase;
}

.upload-grid {
  display: grid;
  grid-template-columns: repeat(3, minmax(0, 1fr));
  gap: 14px;
}

.upload-card,
.payment-card {
  padding: 18px;
  border: 1px solid #dde5f0;
  border-radius: 20px;
  background: #fff;
  box-shadow: 0 18px 38px rgba(24, 39, 75, 0.07);
}

.upload-card {
  display: grid;
  gap: 10px;
  min-width: 0;
}

.upload-card > span {
  color: #14213d;
  font-weight: 700;
}

.upload-card strong {
  color: #2b5a96;
  word-break: break-word;
}

.upload-card small {
  color: #617086;
}

.review-note {
  padding: 12px;
  border-radius: 14px;
  background: #fff1f2;
  border: 1px solid #fecdd3;
}

.review-label {
  display: block;
  margin-bottom: 4px;
  color: #9f1239;
  font-size: 0.75rem;
  font-weight: 700;
  text-transform: uppercase;
}

.review-note p {
  margin: 0;
  color: #7f1d1d;
  font-size: 0.88rem;
  line-height: 1.45;
}

.payment-toggle {
  display: flex;
  gap: 12px;
  flex-wrap: wrap;
  margin-bottom: 16px;
}

.toggle-option {
  display: inline-flex;
  align-items: center;
  gap: 8px;
  padding: 10px 14px;
  border: 1px solid #d7dfeb;
  border-radius: 999px;
  background: #f8fafc;
  color: #24364f;
}

.toggle-option.active {
  border-color: #1f4f89;
  background: #edf4ff;
}

.toggle-option input {
  margin: 0;
  flex: 0 0 auto;
}
.upload-card .errorMessage,
.payment-card .errorMessage,
.errorMessage {
  display: block;
  font-size: 0.78rem;
  color: #b42318;
  word-break: break-word;
}

@media screen and (max-width: 1180px) {
  .upload-grid {
    grid-template-columns: repeat(2, minmax(0, 1fr));
  }
}

@media screen and (max-width: 860px) {
  .upload-grid {
    grid-template-columns: 1fr;
  }
}
</style>
