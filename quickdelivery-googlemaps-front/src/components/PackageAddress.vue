<template>
  <div class="address-panel">
    <aside class="preview-card">
      <span class="preview-chip">
        {{ addressType === 'DEPARTURE' ? $t('wizardCreateStepPickup') : $t('wizardCreateStepDelivery') }}
      </span>
      <h3>{{ $t('packageAddressPreviewTitle') }}</h3>
      <p>{{ $t('packageAddressPreviewHint') }}</p>

      <div class="preview-box">
        <span class="material-symbols-outlined">location_on</span>
        <strong>{{ address.addressAuto || $t('packageAddressPreviewEmpty') }}</strong>
      </div>

      <div class="preview-box">
        <span class="material-symbols-outlined">schedule</span>
        <strong>{{ formattedDate || '--' }}</strong>
      </div>
    </aside>

    <div class="form-card">
      <div class="address-grid">
        <div class="field-wrap">
          <label for="firstName">{{ $t('packageAddressFirstNameLabel') }}</label>
          <Field id="firstName" v-model="address.firstName" name="address.firstName" :rules="validateString" />
          <ErrorMessage class="errorMessage" name="address.firstName" />
        </div>

        <div class="field-wrap">
          <label for="lastName">{{ $t('packageAddressLastName') }}</label>
          <Field id="lastName" v-model="address.lastName" name="address.lastName" :rules="validateString" />
          <ErrorMessage class="errorMessage" name="address.lastName" />
        </div>

        <div class="field-wrap">
          <label for="email">{{ $t('packageAddressEmail') }}</label>
          <Field id="email" v-model="address.email" type="email" name="address.email" :rules="validateEmail" />
          <ErrorMessage class="errorMessage" name="address.email" />
        </div>

        <div class="field-wrap">
          <label for="phone">{{ $t('packageAddressPhoneLabel') }}</label>
          <PhoneNumberField :input-id="`${addressType}-phone`" v-model="address.phone" name="address.phone" />
        </div>

        <div class="field-wrap field-wide">
          <label for="address">{{ $t('packageAddressAddress') }}</label>
          <div class="autocomplete-wrap">
            <AddressAutocomplete ref="addressAutoComplete" v-model="address.addressAuto" :existingAddress="address.addressAuto" />
          </div>
          <span v-if="isAddressError" class="errorMessage">{{ errorAddressMessage }}</span>
        </div>

        <div class="field-wrap">
          <label for="floor">
            {{ $t('packageAddressFloorLabel', { state: $t(addressType === 'DEPARTURE' ? 'packageAddressFloorStatePickup' : 'packageAddressFloorStateDelivery') }) }}
          </label>
          <Field id="floor" v-model="address.floor" type="number" name="address.floor" :rules="validateNumericFieldAcceptZero" />
          <ErrorMessage class="errorMessage" name="address.floor" />
        </div>

        <div class="field-wrap">
          <label for="dateTime">
            {{ $t('packageAddressDateTimeLabel', { state: $t(addressType === 'DEPARTURE' ? 'packageAddressFloorStatePickup' : 'packageAddressFloorStateDelivery') }) }}
          </label>
          <VueDatePicker
            id="dateTime"
            v-model="address.dateTime"
            time-picker-inline
            :min-date="minDate"
            :max-date="maxDate"
            :min-time="{ hours: 8, minutes: 0 }"
            :max-time="{ hours: 22, minutes: 59 }"
          />
          <span v-if="isDateTimeError" class="errorMessage">{{ errorDeliveryDateTimeMessage }}</span>
        </div>
      </div>
    </div>
  </div>
</template>

<script>
import { ErrorMessage, Field } from 'vee-validate';
import VueDatePicker from '@vuepic/vue-datepicker';
import '@vuepic/vue-datepicker/dist/main.css';
import AddressAutocomplete from './AddressAutocomplete.vue';
import PhoneNumberField from './PhoneNumberField.vue';
import { validateEmail, validateNumericFieldAcceptZero, validateString } from '@/config/comonFunction';

export default {
  components: {
    AddressAutocomplete,
    Field,
    ErrorMessage,
    VueDatePicker,
    PhoneNumberField,
  },
  props: {
    addressType: {
      type: String,
      default: 'DEPARTURE',
    },
  },
  data() {
    return {
      isAddressError: false,
      isDateTimeError: false,
      errorAddressMessage: null,
      errorDeliveryDateTimeMessage: null,
    };
  },
  computed: {
    address() {
      return this.addressType === 'ARRIVAL' ? this.$store.state.package_.addresses[1] : this.$store.state.package_.addresses[0];
    },
    minDate() {
      const date = new Date();
      date.setDate(date.getDate() + 1);
      return date;
    },
    maxDate() {
      const date = new Date();
      date.setDate(date.getDate() + 7);
      return date;
    },
    formattedDate() {
      if (!this.address.dateTime) {
        return null;
      }
      const locale = navigator.languages && navigator.languages.length ? navigator.languages[0] : navigator.language || 'fr-FR';
      return new Date(this.address.dateTime).toLocaleString(locale, {
        day: '2-digit',
        month: '2-digit',
        year: 'numeric',
        hour: '2-digit',
        minute: '2-digit',
      });
    },
  },
  methods: {
    validateEmail,
    validateString,
    validateNumericFieldAcceptZero,
  },
};
</script>

<style scoped>
.address-panel {
  display: grid;
  grid-template-columns: 300px minmax(0, 1fr);
  gap: 24px;
}

.preview-card,
.form-card {
  border: 1px solid #dde5f0;
  border-radius: 22px;
  background: #fff;
  box-shadow: 0 18px 38px rgba(24, 39, 75, 0.07);
}

.preview-card {
  display: grid;
  align-content: start;
  gap: 14px;
  padding: 22px;
  background: linear-gradient(180deg, #edf4ff 0%, #ffffff 78%);
}

.preview-chip {
  display: inline-flex;
  width: fit-content;
  padding: 5px 10px;
  border-radius: 999px;
  background: #dceaff;
  color: #214f88;
  font-size: 0.76rem;
  font-weight: 700;
  text-transform: uppercase;
}

.preview-card h3 {
  margin: 0;
  color: #14213d;
  font-size: 1.2rem;
}

.preview-card p {
  margin: 0;
  color: #617086;
}

.preview-box {
  display: grid;
  grid-template-columns: 22px minmax(0, 1fr);
  gap: 10px;
  align-items: start;
  padding: 14px;
  border-radius: 16px;
  background: rgba(255, 255, 255, 0.82);
  color: #24364f;
}

.preview-box .material-symbols-outlined {
  color: #2a6fcf;
  font-size: 1.1rem;
}

.form-card {
  padding: 24px;
}

.address-grid {
  display: grid;
  grid-template-columns: repeat(2, minmax(0, 1fr));
  gap: 18px;
}

.field-wrap {
  min-width: 0;
  display: flex;
  flex-direction: column;
}

.field-wide {
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
  min-height: 48px;
  padding: 0 14px;
  border: 1px solid #ced7e4;
  border-radius: 14px;
  background: #fff;
  box-sizing: border-box;
}

.autocomplete-wrap {
  width: 100%;
  min-width: 0;
}

.field-wrap :deep(.google-address-autocomplete-input),
.field-wrap :deep(.address-autocomplete-input) {
  width: 100%;
  min-height: 48px;
  padding: 0 14px;
  border: 1px solid #ced7e4;
  border-radius: 14px;
  box-sizing: border-box;
}

.field-wrap :deep(.dp__main) {
  width: 100%;
}

.field-wrap :deep(.dp__input) {
  width: 100%;
  min-height: 48px;
  border-radius: 14px;
  border-color: #ced7e4;
  box-sizing: border-box;
}

.errorMessage {
  display: block;
  margin-top: 6px;
  font-size: 0.78rem;
  color: #b42318;
}

@media screen and (max-width: 1080px) {
  .address-panel {
    grid-template-columns: 1fr;
  }
}

@media screen and (max-width: 900px) {
  .address-grid {
    grid-template-columns: 1fr;
  }
}
</style>
