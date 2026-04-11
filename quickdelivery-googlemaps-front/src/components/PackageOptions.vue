<template>
  <div class="options-layout">
    <section class="options-card">
      <div class="section-head">
        <span class="eyebrow">{{ $t('packageOptionsDeliverySpeed') }}</span>
        <h3>{{ $t('createPackageOptionsTitle') }}</h3>
        <p>{{ $t('createPackageOptionsSubtitle') }}</p>
      </div>

      <div class="choice-grid">
        <button
          v-for="speed in speedChoices"
          :key="speed.key"
          class="choice-card"
          :class="{ active: localValue.deliverySpeed === speed.key }"
          type="button"
          @click="localValue.deliverySpeed = speed.key"
        >
          <strong>{{ $t(speed.label) }}</strong>
          <span>{{ $t(speed.hint) }}</span>
        </button>
      </div>
    </section>

    <section class="options-card">
      <div class="section-head compact">
        <span class="eyebrow">{{ $t('packageOptionsProtection') }}</span>
        <h3>{{ $t('packageOptionInsurance') }}</h3>
        <p>{{ $t('packageOptionInsuranceHint') }}</p>
      </div>

      <label class="insurance-toggle">
        <input v-model="localValue.insurance" type="checkbox">
        <span>{{ $t('packageOptionInsurance') }}</span>
      </label>

      <div v-if="localValue.insurance" class="declared-value-field">
        <label for="declaredValue">{{ $t('packageDeclaredValueLabel') }}</label>
        <input
          id="declaredValue"
          v-model.number="localValue.declaredValue"
          type="number"
          :min="declaredValueMin"
          :max="declaredValueMax"
          step="0.01"
          @change="clampDeclaredValue"
        >
        <span v-if="declaredValueError" class="errorMessage">{{ declaredValueError }}</span>
      </div>
    </section>

    <section class="options-card">
      <div class="section-head compact">
        <span class="eyebrow">{{ $t('packageOptionsDocuments') }}</span>
        <h3>{{ $t('packageOptionsSelectedDocuments') }}</h3>
      </div>

      <div class="upload-grid">
        <label class="upload-card">
          <span>{{ $t('packagePicture') }}</span>
          <small>{{ $t('packagePictureInfo') }}</small>
          <input ref="fileInput0" type="file" accept="image/*" @change="handleFileChange(0)">
          <strong>{{ documentLabel(0) }}</strong>
        </label>

        <label class="upload-card">
          <span>{{ $t('packageInvoice') }}</span>
          <small>{{ $t('packageInvoiceInfoLabel') }}</small>
          <input ref="fileInput1" type="file" accept="image/*, application/pdf" @change="handleFileChange(1)">
          <strong>{{ documentLabel(1) }}</strong>
        </label>
      </div>
    </section>
  </div>
</template>

<script>
const DECLARED_VALUE_MIN = 1;
const DECLARED_VALUE_MAX = 10000;

export default {
  props: {
    modelValue: {
      type: Object,
      required: true,
    },
  },
  emits: ['update:modelValue'],
  data() {
    return {
      localValue: {
        deliverySpeed: this.modelValue.deliverySpeed || 'STANDARD',
        insurance: this.modelValue.insurance || false,
        declaredValue: this.modelValue.declaredValue ?? null,
      },
      declaredValueError: null,
      speedChoices: [
        { key: 'STANDARD', label: 'packageOptionStandard', hint: 'packageOptionStandardHint' },
        { key: 'EXPRESS', label: 'packageOptionExpress', hint: 'packageOptionExpressHint' },
        { key: 'SAMEDAY', label: 'packageOptionSameDay', hint: 'packageOptionSameDayHint' },
      ],
    };
  },
  computed: {
    documentS() {
      return this.$store.state.documentS;
    },
    declaredValueMin() {
      return DECLARED_VALUE_MIN;
    },
    declaredValueMax() {
      return DECLARED_VALUE_MAX;
    },
  },
  watch: {
    'localValue.insurance'(value) {
      if (!value) {
        this.localValue.declaredValue = null;
        this.declaredValueError = null;
      }
    },
    'localValue.declaredValue'() {
      if (this.declaredValueError && Number(this.localValue.declaredValue) >= this.declaredValueMin) {
        this.declaredValueError = null;
      }
    },
    localValue: {
      deep: true,
      handler(value) {
        this.$emit('update:modelValue', { ...value });
      },
    },
  },
  methods: {
    handleFileChange(index) {
      const file = this.$refs[`fileInput${index}`]?.files?.[0];
      if (file) {
        const nextDocuments = [...(this.documentS || [])];
        nextDocuments[index] = file;
        this.$store.commit('updateDocuments', nextDocuments);
      }
    },
    documentLabel(index) {
      return this.documentS?.[index]?.name || this.$t('packageDocumentMissing');
    },
    validateSelection() {
      const declaredValue = Number(this.localValue.declaredValue);
      if (this.localValue.insurance && !(declaredValue >= this.declaredValueMin && declaredValue <= this.declaredValueMax)) {
        this.declaredValueError = this.$t('packageDeclaredValueRequired');
        return false;
      }
      this.declaredValueError = null;
      return true;
    },
    clampDeclaredValue() {
      const parsedValue = Number(this.localValue.declaredValue);
      if (!Number.isFinite(parsedValue)) {
        return;
      }
      this.localValue.declaredValue = Math.min(this.declaredValueMax, Math.max(this.declaredValueMin, parsedValue));
    },
  },
};
</script>

<style scoped>
.options-layout {
  display: grid;
  gap: 20px;
  min-width: 0;
}

.options-layout,
.options-layout * {
  box-sizing: border-box;
}

.options-card {
  padding: 24px;
  border: 1px solid #dde5f0;
  border-radius: 22px;
  background: #fff;
  box-shadow: 0 18px 38px rgba(24, 39, 75, 0.07);
}

.section-head {
  margin-bottom: 18px;
}

.section-head.compact {
  margin-bottom: 14px;
}

.section-head h3 {
  margin: 6px 0 8px;
  font-size: 1.35rem;
  color: #14213d;
}

.section-head p {
  margin: 0;
  color: #617086;
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

.choice-grid,
.upload-grid {
  display: grid;
  grid-template-columns: repeat(3, minmax(0, 1fr));
  gap: 14px;
}

.choice-card,
.upload-card {
  display: grid;
  gap: 8px;
  padding: 16px;
  border: 1px solid #d7dfeb;
  border-radius: 18px;
  background: #fff;
}

.choice-card {
  text-align: left;
  cursor: pointer;
  transition: border-color 0.2s ease, box-shadow 0.2s ease, transform 0.2s ease;
}

.choice-card.active {
  border-color: #1f4f89;
  box-shadow: 0 14px 30px rgba(23, 48, 87, 0.12);
  transform: translateY(-2px);
}

.choice-card strong,
.upload-card span {
  color: #14213d;
  font-size: 1rem;
  font-weight: 700;
}

.choice-card span,
.choice-card small,
.upload-card small {
  color: #617086;
}

.upload-card input {
  width: 100%;
  max-width: 100%;
}

.upload-card strong {
  color: #2b5a96;
  word-break: break-word;
}

.insurance-toggle {
  display: flex;
  align-items: center;
  gap: 12px;
  padding: 16px 18px;
  border-radius: 18px;
  background: #f7f9fc;
  color: #24364f;
}

.insurance-toggle input {
  flex: 0 0 auto;
  width: 18px;
  height: 18px;
}

.insurance-toggle strong {
  margin-left: auto;
  color: #1f4f89;
}

.declared-value-field {
  display: grid;
  gap: 8px;
  margin-top: 14px;
}

.declared-value-field input {
  min-height: 46px;
  padding: 0 14px;
  border: 1px solid #ced7e4;
  border-radius: 14px;
}

.declared-value-field small {
  color: #617086;
}

.errorMessage {
  font-size: 0.78rem;
  color: #b42318;
}

@media screen and (max-width: 980px) {
  .choice-grid,
  .upload-grid {
    grid-template-columns: 1fr;
  }
}
</style>
