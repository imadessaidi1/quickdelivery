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
        <DocumentUploadCard
          :label="$t('packagePicture')"
          :file-name="documentLabel(0)"
          @change="handleFileChange($event, 0)"
        />

        <DocumentUploadCard
          :label="$t('packageInvoice')"
          :file-name="documentLabel(1)"
          accept="image/*, application/pdf"
          @change="handleFileChange($event, 1)"
        />
      </div>
    </section>
  </div>
</template>

<script>
import DocumentUploadCard from './DocumentUploadCard.vue';

const DECLARED_VALUE_MIN = 1;
const DECLARED_VALUE_MAX = 10000;

export default {
  components: {
    DocumentUploadCard,
  },
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
    handleFileChange(event, index) {
      const file = event?.target?.files?.[0];
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
  gap: 24px;
  min-width: 0;
}

.options-card {
  padding: 24px;
  border: 1px solid var(--qd-border);
  border-radius: 22px;
  background: #fff;
  box-shadow: 0 10px 40px rgba(15, 23, 42, 0.06);
}

.section-head {
  margin-bottom: 24px;
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

.eyebrow {
  display: inline-flex;
  align-items: center;
  padding: 4px 10px;
  border-radius: 999px;
  background: var(--qd-primary-soft);
  color: var(--qd-primary);
  font-size: 0.78rem;
  font-weight: 700;
  text-transform: uppercase;
}

.choice-grid,
.upload-grid {
  display: grid;
  grid-template-columns: repeat(3, 1fr);
  gap: 20px;
}

.choice-card {
  display: flex;
  flex-direction: column;
  gap: 8px;
  padding: 24px;
  border: 1px solid var(--qd-border);
  border-radius: 20px;
  background: var(--qd-surface-strong);
  text-align: left;
  cursor: pointer;
  transition: var(--qd-transition);
}

.choice-card:hover {
  background: #fff;
  border-color: var(--qd-primary-soft);
}

.choice-card.active {
  border-color: var(--qd-primary);
  background: var(--qd-primary-soft);
  color: var(--qd-primary);
  transform: translateY(-2px);
  box-shadow: 0 10px 20px rgba(39, 84, 138, 0.1);
}

.choice-card strong {
  font-size: 1.1rem;
  font-weight: 800;
}

.choice-card span {
  font-size: 0.9rem;
  opacity: 0.8;
}

.insurance-toggle {
  display: flex;
  align-items: center;
  gap: 12px;
  padding: 16px 20px;
  border-radius: 18px;
  background: var(--qd-surface-strong);
  cursor: pointer;
  transition: var(--qd-transition);
}

.insurance-toggle:hover {
  background: #fff;
  border: 1px solid var(--qd-border);
}

.insurance-toggle input {
  width: 20px;
  height: 20px;
  accent-color: var(--qd-primary);
}

.declared-value-field {
  display: grid;
  gap: 8px;
  margin-top: 16px;
}

.declared-value-field input {
  min-height: 52px;
  padding: 0 16px;
  border: 1px solid var(--qd-border);
  border-radius: 14px;
  background: #fff;
  font-size: 1rem;
  transition: var(--qd-transition);
}

.declared-value-field input:focus {
  border-color: var(--qd-primary);
  box-shadow: 0 0 0 4px var(--qd-primary-soft);
  outline: none;
}

.errorMessage {
  font-size: 0.8rem;
  color: var(--qd-danger);
  font-weight: 600;
}

@media (max-width: 1024px) {
  .choice-grid, .upload-grid { grid-template-columns: 1fr; }
}
</style>
