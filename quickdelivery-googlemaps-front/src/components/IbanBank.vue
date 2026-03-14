<template>
  <div class="iban-form">
    <div class="field-wrap wide">
      <label for="iban">{{ $t('userIBAN') }}</label>
      <Field id="iban" v-model="user.paymentModes['IBAN'].iban" type="text" name="iban" :rules="validateIBAN" />
      <ErrorMessage class="errorMessage" name="iban" />
    </div>

    <div class="field-wrap wide">
      <label for="bic">{{ $t('userIBANBIC') }}</label>
      <Field id="bic" v-model="user.paymentModes['IBAN'].bic" type="text" name="bic" :rules="validateBIC" />
      <ErrorMessage class="errorMessage" name="bic" />
    </div>

    <label v-if="showRibUpload" class="upload-card">
      <span>{{ $t('userRIB') }}</span>
      <input ref="fileInputRIB" type="file" accept="image/*, application/pdf" @change="handleRibFileChange('RIB')">
      <strong>{{ userDocuments['RIB']?.name || $t('packageDocumentMissing') }}</strong>
      <span v-if="filesErrorMessages['RIB']" class="errorMessage">{{ filesErrorMessages['RIB'] }}</span>
    </label>
  </div>
</template>

<script>
import { ErrorMessage, Field } from 'vee-validate';
import { validateBIC, validateIBAN } from '@/config/comonFunction';

export default {
  components: {
    Field,
    ErrorMessage,
  },
  props: {
    filesErrorMessages: {
      type: Object,
      default: () => ({}),
    },
    isForUpdate: {
      type: Boolean,
      default: false,
    },
  },
  computed: {
    user() {
      return this.$store.state.user;
    },
    userDocuments() {
      return this.$store.state.userDocuments;
    },
    showRibUpload() {
      if (!this.isForUpdate) {
        return true;
      }
      return this.user?.activeAccount !== true && ['REJECTED', 'UPDATED'].includes(this.userDocuments?.RIB?.documentStatus);
    },
  },
  methods: {
    validateIBAN,
    validateBIC,
    handleRibFileChange(type) {
      const file = this.$refs.fileInputRIB?.files?.[0];
      if (!file) {
        return;
      }
      this.userDocuments[type] = {
        file,
        name: file.name,
        documentStatus: this.isForUpdate ? 'UPDATED' : 'ACCEPTED',
      };
      if (this.filesErrorMessages[type]) {
        delete this.filesErrorMessages[type];
      }
    },
  },
};
</script>

<style scoped>
.iban-form {
  display: grid;
  grid-template-columns: repeat(2, minmax(0, 1fr));
  gap: 14px;
}

.field-wrap {
  min-width: 0;
  display: flex;
  flex-direction: column;
}

.field-wrap.wide {
  grid-column: 1 / -1;
}

.field-wrap label {
  display: block;
  margin-bottom: 8px;
  font-weight: 600;
  color: #24364f;
}

.field-wrap :deep(input) {
  width: 100%;
  max-width: 100%;
  min-height: 48px;
  padding: 0 14px;
  border: 1px solid #ced7e4;
  border-radius: 14px;
  background: #fff;
  box-sizing: border-box;
}

.upload-card {
  grid-column: 1 / -1;
  display: grid;
  gap: 10px;
  padding: 16px;
  border: 1px solid #d7dfeb;
  border-radius: 18px;
  background: #fff;
}

.upload-card > span {
  color: #14213d;
  font-weight: 700;
}

.upload-card strong {
  color: #2b5a96;
}

.upload-card .errorMessage,
.errorMessage {
  margin-top: 6px;
  font-size: 0.78rem;
  color: #b42318;
  word-break: break-word;
}

@media screen and (max-width: 980px) {
  .iban-form {
    grid-template-columns: 1fr;
  }
}
</style>
