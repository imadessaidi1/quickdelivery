<template>
  <div class="user-profile-step">
    <section class="profile-intro">
      <span class="eyebrow">{{ $t('wizardUserStepInfo') }}</span>
      <h3>{{ $t('userRegistrationProfileTitle') }}</h3>
      <p>{{ $t('userRegistrationProfileSubtitle') }}</p>
    </section>

    <div class="profile-grid">
      <div class="field-wrap">
        <label for="accountType">{{ $t('userRegistrationAccountType') }}</label>
        <select id="accountType" v-model="user.type" :disabled="isFieldLocked('accountType')" @change="handleAccountTypeChange">
          <option value="DELIVERY_PERSON">{{ $t('DELIVERY_PERSON') }}</option>
          <option value="CUSTOMER">{{ $t('CUSTOMER') }}</option>
        </select>
      </div>

      <div v-if="user.type === 'DELIVERY_PERSON'" class="field-wrap">
        <label for="deliveryMode">{{ $t('userDeliveryMode') }}</label>
        <select id="deliveryMode" v-model="user.deliveryMode" :disabled="isFieldLocked('deliveryMode')">
          <option value="CAR">{{ $t('deliveryModeCar') }}</option>
          <option value="SCOOTER">{{ $t('deliveryModeScooter') }}</option>
          <option value="BIKE">{{ $t('deliveryModeBike') }}</option>
          <option value="ON_FOOT">{{ $t('deliveryModeOnFoot') }}</option>
        </select>
        <small class="field-hint">{{ $t('userDeliveryModeHint') }}</small>
      </div>

      <div class="field-wrap">
        <label for="sex">{{ $t('userGender') }}</label>
        <select id="sex" v-model="user.sex" :disabled="isFieldLocked('sex')">
          <option value="MAL">{{ $t('userRegistrationGenderMale') }}</option>
          <option value="FEMALE">{{ $t('userRegistrationGenderFemale') }}</option>
          <option value="OTHER">{{ $t('userRegistrationGenderOther') }}</option>
        </select>
      </div>

      <div class="field-wrap">
        <label for="firstName">{{ $t('packageAddressFirstName') }}</label>
        <Field id="firstName" v-model="user.firstName" name="firstName" autocomplete="given-name" :rules="validateString" :disabled="isFieldLocked('firstName')" />
        <ErrorMessage class="errorMessage" name="firstName" />
      </div>

      <div class="field-wrap">
        <label for="lastName">{{ $t('packageAddressLastName') }}</label>
        <Field id="lastName" v-model="user.lastName" name="lastName" autocomplete="family-name" :rules="validateString" :disabled="isFieldLocked('lastName')" />
        <ErrorMessage class="errorMessage" name="lastName" />
      </div>

      <div class="field-wrap">
        <label for="birthDate">{{ $t('userBirthDate') }}</label>
        <VueDatePicker
          id="birthDate"
          v-model="user.birthDate"
          :flow="flow"
          :enable-time-picker="false"
          :max-date="birthDateMaxDate"
          :disabled="isFieldLocked('birthDate')"
        />
        <span v-if="isBirthDateError" class="errorMessage">{{ birthDateErrorMessage }}</span>
      </div>

      <div class="field-wrap">
        <label for="email">{{ $t('packageAddressEmail') }}</label>
        <Field id="email" v-model="user.emailAddress" type="email" name="email" autocomplete="username email" :rules="validateEmail" />
        <ErrorMessage class="errorMessage" name="email" />
        <span v-if="isExistingEmail" class="errorMessage">{{ existingEmailErrorMessage }}</span>
      </div>

      <div class="field-wrap">
        <label for="emailAddressConfirmation">{{ $t('userEmailConfirmation') }}</label>
        <Field id="emailAddressConfirmation" v-model="user.emailAddressConfirmation" type="email" name="emailAddressConfirmation" autocomplete="off" :rules="validateEmail" />
        <ErrorMessage class="errorMessage" name="emailAddressConfirmation" />
        <span v-if="isEmailConfirmationError" class="errorMessage">{{ emailConfirmationErrorMessage }}</span>
      </div>

      <div class="field-wrap">
        <label for="phone">{{ $t('packageAddressPhone') }}</label>
        <PhoneNumberField input-id="phone" v-model="user.phone" name="phone" :disabled="isFieldLocked('phone')" />
      </div>

      <div class="field-wrap">
        <label for="phoneConfirmation">{{ $t('userPhoneConfirmation') }}</label>
        <PhoneNumberField input-id="phoneConfirmation" v-model="user.phoneConfirmation" name="phoneConfirmation" :disabled="isFieldLocked('phoneConfirmation')" />
        <span v-if="isPhoneConfirmationError" class="errorMessage">{{ phoneConfirmationErrorMessage }}</span>
      </div>

      <div v-if="!isForUpdate" class="field-wrap">
        <label for="password">{{ $t('userPassword') }}</label>
        <Field id="password" v-model="user.password" type="password" name="password" autocomplete="new-password" :rules="validatePassword" />
        <ErrorMessage class="errorMessage" name="password" />
      </div>

      <div v-if="!isForUpdate" class="field-wrap">
        <label for="passwordConfirmation">{{ $t('userPasswordConfirmation') }}</label>
        <input id="passwordConfirmation" v-model="user.passwordConfirmation" type="password" autocomplete="new-password">
        <span v-if="isPasswordConfirmationError" class="errorMessage">{{ passwordConfirmationErrorMessage }}</span>
      </div>
    </div>
  </div>
</template>

<script>
import { ref } from 'vue';
import { ErrorMessage, Field } from 'vee-validate';
import VueDatePicker from '@vuepic/vue-datepicker';
import { validateEmail, validatePassword, validateString } from '@/config/comonFunction';
import PhoneNumberField from './PhoneNumberField.vue';
import { DEFAULT_DELIVERY_MODE } from '@/config/deliveryMode';

export default {
  components: {
    Field,
    ErrorMessage,
    VueDatePicker,
    PhoneNumberField,
  },
  props: {
    isForUpdate: {
      type: Boolean,
      default: false,
    },
    restrictToContactFields: {
      type: Boolean,
      default: false,
    },
  },
  computed: {
    user() {
      return this.$store.state.user;
    },
    birthDateMaxDate() {
      if (this.user?.type !== 'DELIVERY_PERSON') {
        return null;
      }
      const maxDate = new Date();
      maxDate.setFullYear(maxDate.getFullYear() - 18);
      return maxDate;
    },
  },
  data() {
    return {
      isPasswordConfirmationError: false,
      passwordConfirmationErrorMessage: '',
      isExistingEmail: false,
      existingEmailErrorMessage: '',
      isBirthDateError: false,
      birthDateErrorMessage: '',
      isEmailConfirmationError: false,
      emailConfirmationErrorMessage: '',
      isPhoneConfirmationError: false,
      phoneConfirmationErrorMessage: '',
      flow: ref(['month', 'year', 'calendar']),
    };
  },
  methods: {
    validateEmail,
    validateString,
    validatePassword,
    isFieldLocked(fieldName) {
      if (!this.restrictToContactFields) {
        return false;
      }
      return !['email', 'emailAddressConfirmation', 'phone', 'phoneConfirmation'].includes(fieldName);
    },
    handleAccountTypeChange() {
      if (this.user.type === 'DELIVERY_PERSON' && !this.user.deliveryMode) {
        this.user.deliveryMode = DEFAULT_DELIVERY_MODE;
        return;
      }
      if (this.user.type !== 'DELIVERY_PERSON') {
        this.user.deliveryMode = '';
      }
    },
  },
};
</script>

<style scoped>
.user-profile-step {
  display: grid;
  gap: 22px;
  min-width: 0;
}

.user-profile-step,
.user-profile-step * {
  box-sizing: border-box;
}

.profile-intro h3 {
  margin: 6px 0 8px;
  font-size: 1.45rem;
  color: #14213d;
}

.profile-intro p {
  margin: 0;
  color: #617086;
  max-width: 42ch;
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

.profile-grid {
  display: grid;
  grid-template-columns: repeat(2, minmax(0, 1fr));
  gap: 18px;
}

.field-wrap {
  min-width: 0;
  display: flex;
  flex-direction: column;
}

.field-wrap label {
  display: block;
  margin-bottom: 8px;
  font-weight: 600;
  color: #24364f;
  line-height: 1.25;
}

.field-hint {
  margin-top: 6px;
  color: #617086;
  font-size: 0.82rem;
}

.field-wrap :deep(input),
.field-wrap select {
  width: 100%;
  max-width: 100%;
  min-height: 48px;
  padding: 0 14px;
  border: 1px solid #ced7e4;
  border-radius: 14px;
  background: #fff;
  box-sizing: border-box;
}

.field-wrap :deep(input:disabled),
.field-wrap select:disabled,
.field-wrap :deep(.dp__input:disabled) {
  cursor: not-allowed;
  background: #f3f6fb;
  color: #617086;
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
  word-break: break-word;
}

@media screen and (max-width: 1040px) {
  .profile-grid {
    grid-template-columns: 1fr;
  }
}

@media screen and (max-width: 720px) {
  .user-profile-step {
    gap: 18px;
  }

  .profile-intro h3 {
    font-size: 1.2rem;
    line-height: 1.15;
  }

  .profile-intro p {
    max-width: none;
    font-size: 0.98rem;
    line-height: 1.35;
  }

  .profile-grid {
    gap: 14px;
  }

  .field-wrap label {
    margin-bottom: 6px;
    font-size: 0.95rem;
  }

  .field-wrap :deep(input),
  .field-wrap select,
  .field-wrap :deep(.dp__input) {
    min-height: 44px;
    font-size: 16px;
    border-radius: 12px;
  }

  .field-wrap :deep(.dp__input_wrap) {
    width: 100%;
  }
}

@media screen and (max-width: 480px) {
  .user-profile-step {
    gap: 16px;
  }

  .profile-intro {
    display: grid;
    gap: 8px;
  }

  .profile-intro h3 {
    margin: 0;
    font-size: 1.08rem;
  }

  .profile-intro p {
    font-size: 0.92rem;
  }

  .eyebrow {
    font-size: 0.72rem;
    padding: 4px 8px;
  }

  .profile-grid {
    gap: 12px;
  }
}
</style>
