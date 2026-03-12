<template>
  <div class="user-registration-page">
    <header class="page-header">
      <div>
        <span class="page-chip">{{ $t('menuUserSignin') }}</span>
        <h1>{{ pageTitle }}</h1>
        <p>{{ pageSubtitle }}</p>
      </div>
    </header>

    <Form class="wizard-shell" @submit="handleStepSubmit">
      <aside class="wizard-sidebar">
        <button
          v-for="step in visibleSteps"
          :key="step.id"
          class="wizard-step"
          :class="stepState(step.id)"
          type="button"
        >
          <span class="step-index">{{ step.id }}</span>
          <span class="step-copy">
            <strong>{{ $t(step.label) }}</strong>
            <small>{{ $t(step.subtitle) }}</small>
          </span>
        </button>
      </aside>

      <section class="wizard-card">
        <div class="card-header">
          <div>
            <span class="eyebrow">{{ currentStepMeta.id }}/{{ visibleSteps.length }}</span>
            <h2>{{ $t(currentStepMeta.title) }}</h2>
            <p>{{ $t(currentStepMeta.subtitle) }}</p>
          </div>
        </div>

        <div class="card-content">
          <UserInfo v-if="currentStep === 1" ref="userInfo" :is-for-update="isForUpdate" />
          <UserAddressStep v-else-if="currentStep === 2" ref="userAddress" />
          <UserDocuments
            v-else-if="currentStep === 3"
            ref="userDocuments"
            v-model:selected-payment-type="selectedPaymentType"
            :is-for-update="isForUpdate"
          />
          <UserVehicleInfo v-else-if="currentStep === 4" ref="vehicleInfo" :is-for-update="isForUpdate" />
          <UserSummary v-else ref="userSummary" :selected-payment-type="selectedPaymentType" />
        </div>

        <footer class="card-actions">
          <button v-if="currentStep > 1" class="btn primary_btn wizard-action-btn" type="button" @click="previousStep">
            {{ $t('createPackageBackAction') }}
          </button>
          <button class="btn primary_btn wizard-action-btn" type="submit">
            {{ isLastStep ? submitLabel : $t('packageNextAction') }}
          </button>
        </footer>
      </section>
    </Form>
  </div>
</template>

<script>
import { Form } from 'vee-validate';
import http from '@/config/httpInterceptor';
import { validateAddress, validateEmailConfirmation, validateFileInput, validatePasswordConfirmation, validatePhoneConfirmation } from '@/config/comonFunction';
import UserAddressStep from '../components/UserAddressStep.vue';
import UserDocuments from '../components/UserDocuments.vue';
import UserInfo from '../components/UserInfo.vue';
import UserSummary from '../components/UserSummary.vue';
import UserVehicleInfo from '../components/UserVehicleInfo.vue';

const EMPTY_USER = {
  id: null,
  version: null,
  type: 'DELIVERY_PERSON',
  firstName: '',
  lastName: '',
  age: null,
  birthDate: null,
  sex: '',
  emailAddress: '',
  emailAddressValidation: false,
  phone: '',
  phoneValidation: false,
  activeAccount: false,
  password: '',
  passwordConfirmation: '',
  emailAddressConfirmation: '',
  phoneConfirmation: '',
  addressAuto: '',
  personalAddress: [
    {
      id: null,
      version: null,
      firstName: '',
      lastName: '',
      line1: '',
      line2: '',
      town: '',
      zipCode: '',
      country: '',
      floor: 0,
      dateTime: null,
      type: 'RESIDENCE',
      latitude: 0,
      longitude: 0,
      email: '',
      phone: '',
    },
  ],
  documents: [],
  paymentModes: {
    CREDIT_CARD: {
      holderNam: '',
      cardNumber: '',
      expiryDate: '',
      cvv: '',
    },
    IBAN: {
      iban: '',
      bic: '',
    },
  },
};

const EMPTY_VEHICLE = {
  registrationNumber: '',
  brand: '',
  model: '',
  energyType: '',
  vehicleDocuments: {},
};

export default {
  components: {
    Form,
    UserInfo,
    UserAddressStep,
    UserDocuments,
    UserVehicleInfo,
    UserSummary,
  },
  props: {
    id: String,
  },
  data() {
    return {
      currentStep: 1,
      isForUpdate: false,
      selectedPaymentType: 'CARD',
      steps: [
        { id: 1, label: 'wizardUserStepInfo', title: 'userRegistrationProfileTitle', subtitle: 'userRegistrationProfileSubtitle' },
        { id: 2, label: 'wizardUserStepAddress', title: 'userRegistrationAddressTitle', subtitle: 'userRegistrationAddressSubtitle' },
        { id: 3, label: 'wizardUserStepDocs', title: 'userRegistrationDocumentsTitle', subtitle: 'userRegistrationDocumentsSubtitle' },
        { id: 4, label: 'wizardUserStepVehicle', title: 'userRegistrationVehicleTitle', subtitle: 'userRegistrationVehicleSubtitle' },
        { id: 5, label: 'wizardUserStepSummary', title: 'userRegistrationConfirmTitle', subtitle: 'userRegistrationConfirmSubtitle' },
      ],
    };
  },
  computed: {
    isClientRegistrationFlow() {
      return !this.isForUpdate && this.user.type !== 'DELIVERY_PERSON';
    },
    visibleSteps() {
      if (this.isClientRegistrationFlow) {
        return [this.steps[0]];
      }
      return this.steps;
    },
    isLastStep() {
      return this.currentStep === this.visibleSteps[this.visibleSteps.length - 1].id;
    },
    currentStepMeta() {
      return this.steps.find((step) => step.id === this.currentStep) || this.visibleSteps[0];
    },
    pageTitle() {
      return this.isForUpdate ? this.$t('userRegistrationUpdateTitle') : this.$t('userRegistrationPageTitle');
    },
    pageSubtitle() {
      return this.isForUpdate ? this.$t('userRegistrationUpdateSubtitle') : this.$t('userRegistrationPageSubtitle');
    },
    submitLabel() {
      return this.isForUpdate ? this.$t('userAccountUpdate') : this.$t('userCreateAction');
    },
    user() {
      return this.$store.state.user;
    },
  },
  mounted() {
    this.initializeStore();

    if (this.id) {
      this.isForUpdate = true;
      this.loadUserForUpdate();
    }
  },
  methods: {
    stepState(stepId) {
      if (this.currentStep === stepId) {
        return 'active';
      }
      if (this.currentStep > stepId) {
        return 'done';
      }
      return '';
    },
    initializeStore() {
      this.$store.commit('updateUser', JSON.parse(JSON.stringify(EMPTY_USER)));
      this.$store.commit('updateVehicle', JSON.parse(JSON.stringify(EMPTY_VEHICLE)));
      this.$store.commit('updateUserDocuments', []);
      this.$store.commit('updateVehicleDocuments', []);
      this.selectedPaymentType = 'CARD';
    },
    loadUserForUpdate() {
      http.get(this.$i18n.t('userRootURL') + this.$i18n.t('getUserByEmail') + this.id)
        .then((response) => {
          this.$store.commit('updateUser', response.data);
          this.$store.commit('updateVehicle', response.data.vehicles?.[0] || JSON.parse(JSON.stringify(EMPTY_VEHICLE)));

          const userDocument = [];
          ['ID', 'PICTURE', 'DRIVER_LICENCE', 'USER_COMPANY_EXTRACT', 'USER_COMPANY_INSURANCE', 'RIB'].forEach((key) => {
            if (response.data.document?.[key]) {
              userDocument[key] = { name: key === 'RIB' ? 'BANK ID' : key, documentStatus: response.data.document[key].documentStatus };
            }
          });
          this.$store.commit('updateUserDocuments', userDocument);

          const vehicleDocuments = [];
          ['GRAY_CARD', 'INSURANCE'].forEach((key) => {
            if (response.data.document?.[key]) {
              vehicleDocuments[key] = { name: key, documentStatus: response.data.document[key].documentStatus };
            }
          });
          this.$store.commit('updateVehicleDocuments', vehicleDocuments);
        })
        .catch(() => {
          console.error('Unable to process your request this time. Please try again later.');
        });
    },
    previousStep() {
      if (this.currentStep > 1) {
        this.currentStep -= 1;
      }
    },
    async handleStepSubmit() {
      if (this.currentStep === 1) {
        await this.validateProfileStep();
        return;
      }
      if (this.currentStep === 2) {
        this.validateAddressStep();
        return;
      }
      if (this.currentStep === 3) {
        this.validateDocumentsStep();
        return;
      }
      if (this.currentStep === 4) {
        this.validateVehicleStep();
        return;
      }
      await this.submitFormUser();
    },
    async validateProfileStep() {
      const userInfo = this.$refs.userInfo;
      const validPasswordConfirm = this.isForUpdate || validatePasswordConfirmation(this.user.password, this.user.passwordConfirmation);
      const validEmailConfirmation = validateEmailConfirmation(this.user.emailAddress, this.user.emailAddressConfirmation);
      const validPhoneConfirmation = validatePhoneConfirmation(this.user.phone, this.user.phoneConfirmation);
      const shouldCheckExistingEmail = !this.isForUpdate && !!this.user.emailAddress && validEmailConfirmation;
      const existingEmail = shouldCheckExistingEmail ? await this.existingEmail(this.user.emailAddress) : false;

      userInfo.isPasswordConfirmationError = !validPasswordConfirm;
      userInfo.passwordConfirmationErrorMessage = this.$i18n.t('mandatoryField') + this.$i18n.t('PasswordConfirmation');
      userInfo.isExistingEmail = existingEmail;
      userInfo.existingEmailErrorMessage = this.$i18n.t('ExistingEmail');
      userInfo.isEmailConfirmationError = !validEmailConfirmation;
      userInfo.emailConfirmationErrorMessage = this.$i18n.t('mandatoryField') + this.$i18n.t('emailConfirmation');
      userInfo.isPhoneConfirmationError = !validPhoneConfirmation;
      userInfo.phoneConfirmationErrorMessage = this.$i18n.t('mandatoryField') + this.$i18n.t('phoneConfirmation');

      if (validPasswordConfirm && validEmailConfirmation && validPhoneConfirmation && !existingEmail) {
        if (this.isClientRegistrationFlow) {
          await this.submitFormUser();
          return;
        }
        this.currentStep += 1;
      }
    },
    validateAddressStep() {
      const addressStep = this.$refs.userAddress;
      const addressAuto = addressStep.$refs.addressAutoComplete.address || this.user.addressAuto;
      this.user.addressAuto = addressAuto;

      if (!validateAddress(addressAuto)) {
        addressStep.isAddressError = true;
        addressStep.errorAddressMessage = this.$i18n.t('mandatoryField') + this.$i18n.t('invalidAddress');
        return;
      }

      addressStep.isAddressError = false;
      const chunks = addressAuto.split(',');
      const residence = this.user.personalAddress[0];
      residence.line1 = chunks[0]?.trim() || '';
      residence.zipCode = chunks[1]?.trim().split(' ')[0] || '';
      const index = chunks[1]?.trim().indexOf(' ');
      residence.town = index !== -1 ? chunks[1].trim().substring(index + 1) : '';
      residence.country = chunks[2]?.trim() || '';

      this.$store.commit('updateUser', { ...this.user });
      this.currentStep += 1;
    },
    validateDocumentsStep() {
      const selectedFilesKeys = ['ID', 'PICTURE'];
      if (this.user.type === 'DELIVERY_PERSON') {
        selectedFilesKeys.push('DRIVER_LICENCE', 'USER_COMPANY_EXTRACT', 'USER_COMPANY_INSURANCE');
      }

      const userDocs = this.$refs.userDocuments;
      const fileValidation = validateFileInput(selectedFilesKeys, this.$store.state.userDocuments);
      userDocs.filesErrorMessages = [];
      if (fileValidation?.length) {
        fileValidation.forEach((result) => {
          userDocs.filesErrorMessages[result.missingKey] = this.$i18n.t('fileRequired');
        });
        return;
      }

      this.currentStep += 1;
    },
    validateVehicleStep() {
      const userDocs = this.$refs.userDocuments;
      const selectedPaymentType = this.selectedPaymentType || 'CARD';

      if (selectedPaymentType === 'IBAN' && !this.$store.state.userDocuments?.RIB) {
        userDocs.filesErrorMessages.RIB = this.$i18n.t('fileRequired');
        this.currentStep = 3;
        return;
      }

      if (this.user.type !== 'DELIVERY_PERSON') {
        this.currentStep += 1;
        return;
      }

      const selectedFilesKeys = ['GRAY_CARD', 'INSURANCE'];
      const vehicleDocs = this.$refs.vehicleInfo;
      const fileValidation = validateFileInput(selectedFilesKeys, this.$store.state.vehicleDocuments);
      vehicleDocs.filesErrorMessages = [];
      if (fileValidation?.length) {
        fileValidation.forEach((result) => {
          vehicleDocs.filesErrorMessages[result.missingKey] = this.$i18n.t('fileRequired');
        });
        return;
      }

      this.currentStep += 1;
    },
    existingEmail(email) {
      return new Promise((resolve) => {
        http.get(`${this.$i18n.t('userRootURL')}${this.$i18n.t('getUserByEmail')}${email}`)
          .then((response) => {
            resolve(response.status === 200 && !!response.data);
          })
          .catch(() => {
            resolve(false);
          });
      });
    },
    async submitFormUser() {
      const formData = new FormData();
      const userState = { ...this.$store.state.user };
      delete userState.documents;
      const payloadUser = {
        ...userState,
        document: {},
      };
      const payloadVehicle = {
        ...this.$store.state.vehicle,
        vehicleDocuments: {},
      };
      formData.append('user', JSON.stringify(payloadUser));

      Object.entries(this.$store.state.userDocuments || {}).forEach(([key, value]) => {
        if (!value) {
          return;
        }
        if (this.isForUpdate) {
          if (value.documentStatus === 'UPDATED' && value.file) {
            formData.append(key, value.file);
          }
          return;
        }
        if (value.file) {
          formData.append(key, value.file);
        }
      });

      formData.append('vehicle', JSON.stringify(payloadVehicle));
      Object.entries(this.$store.state.vehicleDocuments || {}).forEach(([key, value]) => {
        if (!value) {
          return;
        }
        if (this.isForUpdate) {
          if (value.documentStatus === 'UPDATED' && value.file) {
            formData.append(key, value.file);
          }
          return;
        }
        if (value.file) {
          formData.append(key, value.file);
        }
      });

      const userLanguage = navigator.languages && navigator.languages.length ? navigator.languages[0] : navigator.language || 'fr-FR';
      formData.append('locale', userLanguage);

      const url = this.isForUpdate
        ? this.$i18n.t('userRootURL') + this.$i18n.t('updateUser')
        : this.$i18n.t('userRootURL') + this.$i18n.t('createUser');

      return http.post(url, formData, { headers: { acept: 'application/json', 'Content-type': 'multipart/form-data' } })
        .then((response) => {
          if (`${response.status}` === '200') {
            this.initializeStore();
            this.$router.push('/');
          }
        })
        .catch(() => {
          console.error('Unable to process your request this time. Please try again later.');
        });
    },
  },
};
</script>

<style scoped>
.user-registration-page {
  min-height: 100%;
  padding: 24px;
  background: linear-gradient(180deg, #f3f6fb 0%, #eef3f9 100%);
}

.page-header {
  margin-bottom: 18px;
}

.page-chip,
.eyebrow {
  display: inline-flex;
  align-items: center;
  padding: 5px 10px;
  border-radius: 999px;
  background: #e7eefb;
  color: #28558c;
  font-size: 0.78rem;
  font-weight: 700;
  text-transform: uppercase;
  letter-spacing: 0.04em;
}

.page-header h1,
.card-header h2 {
  margin: 8px 0;
  color: #14213d;
}

.page-header p,
.card-header p {
  margin: 0;
  color: #617086;
}

.wizard-shell {
  display: grid;
  grid-template-columns: 280px minmax(0, 1fr);
  gap: 24px;
  align-items: start;
}

.wizard-sidebar,
.wizard-card {
  border: 1px solid #dde5f0;
  border-radius: 26px;
  background: rgba(255, 255, 255, 0.92);
  box-shadow: 0 18px 44px rgba(24, 39, 75, 0.08);
}

.wizard-sidebar {
  padding: 18px;
  display: grid;
  gap: 10px;
}

.wizard-step {
  display: grid;
  grid-template-columns: 40px minmax(0, 1fr);
  gap: 12px;
  align-items: center;
  padding: 14px;
  border: 1px solid #d8e0ec;
  border-radius: 18px;
  background: #fff;
  text-align: left;
}

.step-index {
  display: inline-flex;
  align-items: center;
  justify-content: center;
  width: 40px;
  height: 40px;
  border-radius: 50%;
  background: #eef3f9;
  color: #27548a;
  font-weight: 700;
}

.step-copy {
  display: grid;
  gap: 2px;
}

.step-copy strong {
  color: #14213d;
}

.step-copy small {
  color: #617086;
}

.wizard-step.active {
  border-color: #28558c;
  background: #f5f9ff;
}

.wizard-step.active .step-index,
.wizard-step.done .step-index {
  background: #28558c;
  color: #fff;
}

.wizard-step.done {
  border-color: #b9d4c4;
  background: #f4fbf7;
}

.wizard-card {
  display: flex;
  flex-direction: column;
  min-height: 760px;
  min-width: 0;
  overflow: hidden;
}

.card-header {
  padding: 28px 30px 12px;
}

.card-content {
  flex: 1;
  min-width: 0;
  padding: 14px 30px 30px;
  overflow: hidden;
}

.card-actions {
  display: flex;
  justify-content: space-between;
  gap: 12px;
  padding: 18px 30px 28px;
  border-top: 1px solid #e6edf6;
}

.card-actions .wizard-action-btn {
  display: inline-flex;
  align-items: center;
  justify-content: center;
  min-width: 152px;
  height: 42px;
  padding: 0 18px;
  border: none;
  border-radius: 12px;
  background: #020617;
  color: #ffffff;
  font-weight: 600;
  box-shadow: 0 10px 22px rgba(15, 23, 42, 0.12);
}

.card-actions .wizard-action-btn:hover {
  background: #0f172a;
}

@media screen and (max-width: 1180px) {
  .wizard-shell {
    grid-template-columns: 1fr;
  }

  .wizard-sidebar {
    grid-template-columns: repeat(5, minmax(0, 1fr));
    overflow-x: auto;
  }

  .wizard-step {
    min-width: 220px;
  }
}

@media screen and (max-width: 720px) {
  .user-registration-page {
    padding: 16px;
  }

  .wizard-card {
    min-height: auto;
  }

  .card-header,
  .card-content,
  .card-actions {
    padding-left: 18px;
    padding-right: 18px;
  }

  .card-actions {
    flex-direction: column-reverse;
  }

  .card-actions .wizard-action-btn {
    width: 100%;
  }
}
</style>
