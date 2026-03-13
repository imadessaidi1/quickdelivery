<template>
  <div class="phone-field">
    <div class="phone-row">
      <div ref="countryPicker" class="phone-country-wrap">
        <button :id="`${inputId}-country`" class="phone-country" type="button" @click="toggleCountryList">
          <FlagIcon :country-code="currentOption.countryCode" />
          <span>{{ currentOption.label }}</span>
          <span class="phone-country-arrow">v</span>
        </button>

        <div v-if="isCountryListOpen" class="phone-country-list">
          <button
            v-for="option in countryOptions"
            :key="option.dialCode"
            class="phone-country-option"
            type="button"
            @click="selectCountry(option)"
          >
            <FlagIcon :country-code="option.countryCode" />
            <span>{{ option.label }}</span>
          </button>
        </div>
      </div>

      <input
        :id="inputId"
        v-model="localNumber"
        class="phone-number"
        type="tel"
        inputmode="tel"
        autocomplete="tel-national"
        @input="handleLocalNumberInput"
      >
    </div>

    <Field v-show="false" :name="name" :model-value="normalizedPhone" :rules="validatePhone" />
    <ErrorMessage class="errorMessage" :name="name" />
  </div>
</template>

<script>
import { ErrorMessage, Field } from 'vee-validate';
import { validatePhone } from '@/config/comonFunction';
import FlagIcon from './FlagIcon.vue';

const COUNTRY_OPTIONS = [
  { countryCode: 'FR', label: '+33', dialCode: '+33' },
  { countryCode: 'BE', label: '+32', dialCode: '+32' },
  { countryCode: 'CH', label: '+41', dialCode: '+41' },
  { countryCode: 'DE', label: '+49', dialCode: '+49' },
  { countryCode: 'ES', label: '+34', dialCode: '+34' },
  { countryCode: 'GB', label: '+44', dialCode: '+44' },
  { countryCode: 'IT', label: '+39', dialCode: '+39' },
  { countryCode: 'MA', label: '+212', dialCode: '+212' },
  { countryCode: 'PT', label: '+351', dialCode: '+351' },
  { countryCode: 'US', label: '+1', dialCode: '+1' },
];

export default {
  components: {
    ErrorMessage,
    Field,
    FlagIcon,
  },
  props: {
    modelValue: {
      type: String,
      default: '',
    },
    name: {
      type: String,
      required: true,
    },
    inputId: {
      type: String,
      required: true,
    },
  },
  emits: ['update:modelValue'],
  data() {
    return {
      countryOptions: COUNTRY_OPTIONS,
      selectedDialCode: '+33',
      localNumber: '',
      isCountryListOpen: false,
    };
  },
  computed: {
    currentOption() {
      return this.countryOptions.find((option) => option.dialCode === this.selectedDialCode) || this.countryOptions[0];
    },
    normalizedPhone() {
      const digits = (this.localNumber || '').replace(/\D/g, '').replace(/^0+/, '');
      if (!digits) {
        return '';
      }
      return `${this.selectedDialCode}${digits}`;
    },
  },
  watch: {
    modelValue: {
      immediate: true,
      handler(value) {
        this.parsePhone(value);
      },
    },
  },
  mounted() {
    document.addEventListener('click', this.handleOutsideClick);
  },
  beforeUnmount() {
    document.removeEventListener('click', this.handleOutsideClick);
  },
  methods: {
    validatePhone,
    toggleCountryList() {
      this.isCountryListOpen = !this.isCountryListOpen;
    },
    selectCountry(option) {
      this.selectedDialCode = option.dialCode;
      this.isCountryListOpen = false;
      this.emitPhoneValue();
    },
    parsePhone(value) {
      const normalizedValue = (value || '').trim();
      if (!normalizedValue) {
        this.selectedDialCode = '+33';
        this.localNumber = '';
        return;
      }

      const matchingOption = this.countryOptions
        .slice()
        .sort((left, right) => right.dialCode.length - left.dialCode.length)
        .find((option) => normalizedValue.startsWith(option.dialCode));

      if (matchingOption) {
        this.selectedDialCode = matchingOption.dialCode;
        this.localNumber = normalizedValue.slice(matchingOption.dialCode.length).replace(/\D/g, '');
        return;
      }

      this.selectedDialCode = '+33';
      this.localNumber = normalizedValue.replace(/\D/g, '');
    },
    handleLocalNumberInput(event) {
      this.localNumber = event.target.value.replace(/\D/g, '');
      this.emitPhoneValue();
    },
    emitPhoneValue() {
      this.$emit('update:modelValue', this.normalizedPhone);
    },
    handleOutsideClick(event) {
      if (!this.$refs.countryPicker?.contains(event.target)) {
        this.isCountryListOpen = false;
      }
    },
  },
};
</script>

<style scoped>
.phone-field {
  min-width: 0;
}

.phone-row {
  display: grid;
  grid-template-columns: 140px minmax(0, 1fr);
  gap: 10px;
}

.phone-country-wrap {
  position: relative;
}

.phone-country,
.phone-number {
  width: 100%;
  min-height: 48px;
  padding: 0 14px;
  border: 1px solid #ced7e4;
  border-radius: 14px;
  background: #fff;
  box-sizing: border-box;
}

.phone-country {
  display: inline-flex;
  align-items: center;
  gap: 8px;
  cursor: pointer;
}

.phone-country-arrow {
  margin-left: auto;
  color: #617086;
}

.phone-country-list {
  position: absolute;
  top: calc(100% + 6px);
  left: 0;
  right: 0;
  z-index: 30;
  display: grid;
  gap: 4px;
  max-height: 240px;
  padding: 8px;
  overflow-y: auto;
  border: 1px solid #ced7e4;
  border-radius: 14px;
  background: #fff;
  box-shadow: 0 16px 30px rgba(15, 23, 42, 0.12);
}

.phone-country-option {
  display: inline-flex;
  align-items: center;
  gap: 10px;
  width: 100%;
  min-height: 42px;
  padding: 0 10px;
  border: none;
  border-radius: 10px;
  background: transparent;
  text-align: left;
  cursor: pointer;
}

.phone-country-option:hover {
  background: #edf4ff;
}

.errorMessage {
  display: block;
  margin-top: 6px;
  font-size: 0.78rem;
  color: #b42318;
  word-break: break-word;
}

@media screen and (max-width: 720px) {
  .phone-row {
    grid-template-columns: 120px minmax(0, 1fr);
  }

  .phone-country,
  .phone-number {
    min-height: 44px;
    font-size: 16px;
    border-radius: 12px;
  }
}
</style>
