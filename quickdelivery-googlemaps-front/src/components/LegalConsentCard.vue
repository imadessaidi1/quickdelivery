<template>
  <section class="legal-consent-card">
    <div class="legal-consent-copy">
      <span class="legal-chip">{{ $t('legalConsentChip') }}</span>
      <h3>{{ $t('legalConsentTitle') }}</h3>
      <p>{{ $t('legalConsentSubtitle') }}</p>
    </div>

    <div class="legal-consult-row">
      <router-link class="legal-link" :to="termsRoute" @click="openTerms">
        {{ hasConsulted ? $t('legalConsentReviewAction') : $t('legalConsentReadAction') }}
      </router-link>
      <span class="legal-status" :class="{ ready: hasConsulted }">
        {{ hasConsulted ? $t('legalConsentConsultedState') : $t('legalConsentPendingState') }}
      </span>
    </div>

    <label class="legal-checkbox-row">
      <input :checked="modelValue" type="checkbox" @change="updateValue($event)" />
      <span>{{ $t('legalConsentCheckbox') }}</span>
    </label>

    <p v-if="showError && !hasConsulted" class="legal-error">
      {{ $t('legalConsentConsultFirstError') }}
    </p>
    <p v-else-if="showError && !modelValue" class="legal-error">
      {{ $t('legalConsentAcceptanceError') }}
    </p>
  </section>
</template>

<script>
import { hasLegalPageBeenConsulted } from '@/config/legal';

export default {
  props: {
    flow: {
      type: String,
      required: true,
    },
    modelValue: {
      type: Boolean,
      default: false,
    },
    returnTo: {
      type: String,
      default: '',
    },
    showError: {
      type: Boolean,
      default: false,
    },
  },
  emits: ['open-terms', 'update:modelValue'],
  data() {
    return {
      hasConsulted: false,
    };
  },
  computed: {
    termsRoute() {
      return {
        path: '/terms-of-use',
        query: {
          flow: this.flow,
          returnTo: this.returnTo || this.$route.fullPath,
        },
      };
    },
  },
  mounted() {
    this.refreshConsultationState();
    window.addEventListener('focus', this.refreshConsultationState);
  },
  beforeUnmount() {
    window.removeEventListener('focus', this.refreshConsultationState);
  },
  methods: {
    openTerms() {
      this.$emit('open-terms');
      this.refreshConsultationState();
    },
    refreshConsultationState() {
      this.hasConsulted = hasLegalPageBeenConsulted(this.flow);
    },
    updateValue(event) {
      this.$emit('update:modelValue', event.target.checked);
    },
  },
};
</script>

<style scoped>
.legal-consent-card {
  display: grid;
  gap: 14px;
  padding: 18px;
  border: 1px solid #dbe4ef;
  border-radius: 18px;
  background: #f8fbff;
}

.legal-consent-copy h3 {
  margin: 8px 0 6px;
  color: #14213d;
}

.legal-consent-copy p {
  margin: 0;
  color: #617086;
}

.legal-chip {
  display: inline-flex;
  align-items: center;
  padding: 4px 10px;
  border-radius: 999px;
  background: #e7eefb;
  color: #28558c;
  font-size: 0.74rem;
  font-weight: 700;
  text-transform: uppercase;
}

.legal-consult-row {
  display: flex;
  justify-content: space-between;
  gap: 12px;
  flex-wrap: wrap;
  align-items: center;
}

.legal-link {
  color: #0f4c81;
  font-weight: 600;
  text-decoration: underline;
}

.legal-status {
  color: #8b5e00;
  font-size: 0.9rem;
  font-weight: 600;
}

.legal-status.ready {
  color: #1c7c54;
}

.legal-checkbox-row {
  display: flex;
  align-items: flex-start;
  gap: 10px;
  color: #14213d;
  font-weight: 500;
}

.legal-checkbox-row input {
  margin-top: 4px;
}

.legal-error {
  margin: 0;
  color: #b42318;
  font-size: 0.92rem;
  font-weight: 600;
}
</style>
