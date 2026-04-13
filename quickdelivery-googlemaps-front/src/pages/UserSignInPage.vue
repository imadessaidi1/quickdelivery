<template>
  <div class="user-registration-page qd-page">
    <header class="qd-page-header">
      <div class="header-main">
        <span class="page-chip">{{ $t('menuUserSignin') }}</span>
        <h1>{{ pageTitle }}</h1>
        <p>{{ pageSubtitle }}</p>
      </div>
    </header>

    <div v-if="isLoadingPage" class="page-state">{{ $t('stateLoading') }}</div>
    <div v-else-if="loadError" class="page-state error">{{ $t('stateLoadError') }}</div>

    <Form v-else class="wizard-shell" @submit="handleStepSubmit">
      <aside class="wizard-sidebar">
        <button
          v-for="(step, index) in visibleSteps"
          :key="step.id"
          class="wizard-step"
          :class="stepState(step.id)"
          type="button"
        >
          <span class="step-index">{{ index + 1 }}</span>
          <span class="step-copy">
            <strong>{{ $t(step.label) }}</strong>
            <small>{{ $t(step.subtitle) }}</small>
          </span>
        </button>
      </aside>

      <section class="wizard-card">
        <div class="card-header">
          <div>
            <span class="eyebrow">{{ currentStepPosition }}/{{ visibleSteps.length }}</span>
            <h2>{{ $t(currentStepMeta.title) }}</h2>
            <p>{{ $t(currentStepMeta.subtitle) }}</p>
          </div>
        </div>

        <div class="card-readiness" v-if="draftCourierReadiness">
          <CourierReadinessCard :readiness="draftCourierReadiness" variant="compact" />
        </div>

        <section v-if="isPendingAccountValidation && rejectedDocumentsSummary.length" class="review-banner">
          <span class="eyebrow">{{ $t('userDocumentCorrectionsChip') }}</span>
          <h3>{{ $t('userDocumentCorrectionsTitle') }}</h3>
          <p>{{ $t('userDocumentCorrectionsSubtitle') }}</p>
          <ul>
            <li v-for="item in rejectedDocumentsSummary" :key="item.key">
              <strong>{{ $t(item.label) }}</strong>
              <span>{{ item.comment || $t('userDocumentCorrectionsFallback') }}</span>
            </li>
          </ul>
        </section>

        <div class="card-content">
          <UserInfo
            v-if="currentStep === 1"
            ref="userInfo"
            :is-for-update="isForUpdate"
            :restrict-to-contact-fields="isCustomerContactOnlyUpdate"
          />
          <UserAddressStep v-else-if="currentStep === 2" ref="userAddress" />
          <UserDocuments
            v-else-if="currentStep === 3"
            ref="userDocuments"
            v-model:selected-payment-type="selectedPaymentType"
            :is-for-update="isForUpdate"
          />
          <UserVehicleInfo v-else-if="currentStep === 4" ref="vehicleInfo" :is-for-update="isForUpdate" />
          <UserSummary v-else ref="userSummary" :selected-payment-type="selectedPaymentType" />

          <LegalConsentCard
            v-if="shouldShowLegalConsent"
            v-model="legalConsentAccepted"
            :flow="legalFlow"
            :return-to="$route.fullPath"
            :show-error="showLegalConsentError"
            @open-terms="saveDraftBeforeLegalConsultation"
          />
          <TurnstileCaptcha
            v-if="shouldRenderCaptcha"
            ref="accountCaptcha"
            action="account-create"
            @verified="handleCaptchaVerified"
            @expired="handleCaptchaExpired"
            @error="handleCaptchaExpired"
          />
          <p v-if="captchaErrorMessage" class="captcha-error">{{ captchaErrorMessage }}</p>
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
import http from '@/config/httpInterceptor';
import { Form } from 'vee-validate';
import { LEGAL_FLOW_ACCOUNT_CREATION, clearLegalDraft, hasLegalPageBeenConsulted, loadLegalDraft, saveLegalDraft } from '@/config/legal';
import { validateAddress, validateEmailConfirmation, validateFileInput, validatePasswordConfirmation, validatePhoneConfirmation } from '@/config/comonFunction';
import { isMobileCapacitorRuntime } from '@/config/network';
import TurnstileCaptcha from '../components/TurnstileCaptcha.vue';
import LegalConsentCard from '../components/LegalConsentCard.vue';
import CourierReadinessCard from '../components/CourierReadinessCard.vue';
import UserAddressStep from '../components/UserAddressStep.vue';
import UserDocuments from '../components/UserDocuments.vue';
import UserInfo from '../components/UserInfo.vue';
import UserSummary from '../components/UserSummary.vue';
import UserVehicleInfo from '../components/UserVehicleInfo.vue';
import { buildCourierReadiness } from '../config/courierReadiness';
import { DEFAULT_DELIVERY_MODE, getRequiredUserDocuments, getRequiredVehicleDocuments, requiresVehicleDetails } from '../config/deliveryMode';

const EMPTY_USER = {
  id: null,
  version: null,
  type: 'DELIVERY_PERSON',
  firstName: '',
  lastName: '',
  age: null,
  birthDate: null,
  sex: 'MAL',
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
  deliveryMode: DEFAULT_DELIVERY_MODE,
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
  type: 'CAR',
  energyType: 'ELECTRIC',
  vehicleDocuments: {},
};

function createEmptyResidenceAddress() {
  return JSON.parse(JSON.stringify(EMPTY_USER.personalAddress[0]));
}

function ensureResidenceAddress(user) {
  const normalizedUser = user ? { ...user } : {};
  const personalAddress = Array.isArray(normalizedUser.personalAddress)
    ? [...normalizedUser.personalAddress]
    : [];

  if (!personalAddress.length || !personalAddress[0]) {
    personalAddress[0] = createEmptyResidenceAddress();
  } else {
    personalAddress[0] = {
      ...createEmptyResidenceAddress(),
      ...personalAddress[0],
    };
  }

  normalizedUser.personalAddress = personalAddress;
  return normalizedUser;
}

function normalizeUserWizardState(user) {
  const normalizedUser = ensureResidenceAddress(user);
  normalizedUser.paymentModes = {
    CREDIT_CARD: {
      ...EMPTY_USER.paymentModes.CREDIT_CARD,
      ...(normalizedUser.paymentModes?.CREDIT_CARD || {}),
    },
    IBAN: {
      ...EMPTY_USER.paymentModes.IBAN,
      ...(normalizedUser.paymentModes?.IBAN || {}),
    },
  };
  return normalizedUser;
}

function normalizeUserForSubmission(user) {
  const normalizedUser = ensureResidenceAddress(user);
  if (!normalizedUser.sex) {
    normalizedUser.sex = EMPTY_USER.sex;
  }
  if (normalizedUser.type === 'DELIVERY_PERSON' && !normalizedUser.deliveryMode) {
    normalizedUser.deliveryMode = DEFAULT_DELIVERY_MODE;
  }
  return normalizedUser;
}

function normalizeVehicleForSubmission(vehicle) {
  return {
    ...JSON.parse(JSON.stringify(EMPTY_VEHICLE)),
    ...(vehicle || {}),
    energyType: vehicle?.energyType || EMPTY_VEHICLE.energyType,
  };
}

function onboardingAddressStorageKey(emailAddress) {
  return emailAddress ? `quickdelivery.onboarding.address.${emailAddress.toLowerCase()}` : null;
}

function createResidenceSnapshot(user) {
  const normalizedUser = normalizeUserWizardState(user);
  return {
    addressAuto: normalizedUser.addressAuto || '',
    personalAddress: normalizedUser.personalAddress?.length
      ? JSON.parse(JSON.stringify(normalizedUser.personalAddress))
      : [createEmptyResidenceAddress()],
  };
}

function hasValidResidenceData(user) {
  const residence = user?.personalAddress?.[0];
  return !!(
    user?.addressAuto
    && residence?.line1
    && residence?.town
    && residence?.zipCode
    && residence?.country
  );
}

function isAdultBirthDate(value) {
  if (!value) {
    return false;
  }

  const birthDate = value instanceof Date ? value : new Date(value);
  if (Number.isNaN(birthDate.getTime())) {
    return false;
  }

  const today = new Date();
  let age = today.getFullYear() - birthDate.getFullYear();
  const monthDelta = today.getMonth() - birthDate.getMonth();
  if (monthDelta < 0 || (monthDelta === 0 && today.getDate() < birthDate.getDate())) {
    age -= 1;
  }
  return age >= 18;
}

export default {
  components: {
    Form,
    TurnstileCaptcha,
    LegalConsentCard,
    CourierReadinessCard,
    UserInfo,
    UserAddressStep,
    UserDocuments,
    UserVehicleInfo,
    UserSummary,
  },
  props: {
    id: String,
    updateToken: String,
  },
  data() {
    return {
      currentStep: 1,
      isForUpdate: false,
      accountBootstrapCompleted: false,
      accountBootstrapPending: false,
      selectedPaymentType: 'CARD',
      steps: [
        { id: 1, label: 'wizardUserStepInfo', title: 'userRegistrationProfileTitle', subtitle: 'userRegistrationProfileSubtitle' },
        { id: 2, label: 'wizardUserStepAddress', title: 'userRegistrationAddressTitle', subtitle: 'userRegistrationAddressSubtitle' },
        { id: 3, label: 'wizardUserStepDocs', title: 'userRegistrationDocumentsTitle', subtitle: 'userRegistrationDocumentsSubtitle' },
        { id: 4, label: 'wizardUserStepVehicle', title: 'userRegistrationVehicleTitle', subtitle: 'userRegistrationVehicleSubtitle' },
        { id: 5, label: 'wizardUserStepSummary', title: 'userRegistrationConfirmTitle', subtitle: 'userRegistrationConfirmSubtitle' },
      ],
      legalConsentAccepted: false,
      showLegalConsentError: false,
      isLoadingPage: false,
      loadError: false,
      captchaVisible: false,
      captchaToken: '',
      captchaErrorMessage: '',
    };
  },
  computed: {
    legalFlow() {
      return LEGAL_FLOW_ACCOUNT_CREATION;
    },
    isPendingAccountValidation() {
      return this.isForUpdate && this.user?.activeAccount !== true && !this.isResumeOnboardingFlow;
    },
    isResumeOnboardingFlow() {
      const status = this.user?.onboarding?.status;
      return this.isForUpdate
        && this.user?.activeAccount !== true
        && ['ACCOUNT_CREATED', 'PROFILE_COMPLETED', 'DOCUMENTS_UPLOADED'].includes(status);
    },
    isPublicTokenUpdate() {
      return !!this.updateToken;
    },
    isClientRegistrationFlow() {
      return this.user.type !== 'DELIVERY_PERSON';
    },
    isAccountContactUpdateFlow() {
      return this.isForUpdate && !this.isResumeOnboardingFlow;
    },
    isCustomerContactOnlyUpdate() {
      return this.isAccountContactUpdateFlow;
    },
    editableUserDocumentKeys() {
      if (!this.isPendingAccountValidation) {
        return [];
      }
      const documentKeys = this.user.type === 'DELIVERY_PERSON'
        ? [...getRequiredUserDocuments(this.user)]
        : ['ID', 'PICTURE'];
      if (this.$store.state.userDocuments?.RIB?.documentStatus === 'REJECTED') {
        documentKeys.push('RIB');
      }
      return documentKeys.filter((key) => ['REJECTED', 'UPDATED'].includes(this.$store.state.userDocuments?.[key]?.documentStatus));
    },
    editableVehicleDocumentKeys() {
      if (!this.isPendingAccountValidation || this.user.type !== 'DELIVERY_PERSON' || !this.requiresVehicleSection) {
        return [];
      }
      return getRequiredVehicleDocuments(this.user).filter((key) => ['REJECTED', 'UPDATED'].includes(this.$store.state.vehicleDocuments?.[key]?.documentStatus));
    },
    hasEditableRejectedUserDocuments() {
      return this.editableUserDocumentKeys.length > 0;
    },
    hasEditableRejectedVehicleDocuments() {
      return this.editableVehicleDocumentKeys.length > 0;
    },
    requiresVehicleSection() {
      return requiresVehicleDetails(this.user);
    },
    rejectedDocumentsSummary() {
      const userDocuments = this.editableUserDocumentKeys.map((key) => ({
        key,
        label: this.documentLabel(key),
        comment: this.$store.state.userDocuments?.[key]?.reviewComment || '',
      }));
      const vehicleDocuments = this.editableVehicleDocumentKeys.map((key) => ({
        key,
        label: this.documentLabel(key),
        comment: this.$store.state.vehicleDocuments?.[key]?.reviewComment || '',
      }));
      return [...userDocuments, ...vehicleDocuments];
    },
    visibleSteps() {
      if (this.isAccountContactUpdateFlow) {
        return this.steps.filter((step) => step.id === 1 || step.id === 2);
      }
      if (this.isResumeOnboardingFlow) {
        if (!this.requiresVehicleSection) {
          return this.steps.filter((step) => step.id !== 4);
        }
        return this.steps;
      }
      if (this.isClientRegistrationFlow) {
        if (this.isForUpdate) {
          return this.steps.filter((step) => {
            if (step.id === 3) {
              return this.hasEditableRejectedUserDocuments;
            }
            return step.id === 1 || step.id === 2;
          });
        }
        if (this.hasEditableRejectedUserDocuments) {
          return [this.steps[0], this.steps[2]];
        }
        return [this.steps[0]];
      }
      if (this.isPendingAccountValidation) {
        return this.steps.filter((step) => {
          if (step.id === 3) {
            return this.hasEditableRejectedUserDocuments;
          }
          if (step.id === 4) {
            return this.requiresVehicleSection && this.hasEditableRejectedVehicleDocuments;
          }
          return step.id === 1 || step.id === 2;
        });
      }
      if (!this.requiresVehicleSection) {
        return this.steps.filter((step) => step.id !== 4);
      }
      return this.steps;
    },
    isLastStep() {
      return this.currentStep === this.visibleSteps[this.visibleSteps.length - 1].id;
    },
    currentStepMeta() {
      return this.steps.find((step) => step.id === this.currentStep) || this.visibleSteps[0];
    },
    currentStepPosition() {
      const visibleIndex = this.visibleSteps.findIndex((step) => step.id === this.currentStep);
      return visibleIndex === -1 ? 1 : visibleIndex + 1;
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
    shouldShowLegalConsent() {
      if (this.isForUpdate || this.isResumeOnboardingFlow) {
        return false;
      }
      return this.currentStep === 1;
    },
    shouldShowCaptcha() {
      return !this.isForUpdate
        && !this.accountBootstrapCompleted
        && !isMobileCapacitorRuntime()
        && !!process.env.VUE_APP_TURNSTILE_SITE_KEY;
    },
    shouldRenderCaptcha() {
      return this.shouldShowCaptcha && this.captchaVisible;
    },
    user() {
      return this.$store.state.user;
    },
    draftCourierReadiness() {
      return buildCourierReadiness(
        this.$store.state.user,
        this.$store.state.vehicle,
        this.$store.state.userDocuments,
        this.$store.state.vehicleDocuments
      );
    },
  },
  mounted() {
    if (!this.restoreDraftIfAvailable() && !this.hasLiveRegistrationDraft()) {
      this.initializeStore();
    }

    if (this.id) {
      this.isForUpdate = true;
      this.loadUserForUpdate();
      return;
    }

    if (this.updateToken) {
      this.isForUpdate = true;
      this.loadUserForUpdate();
    }
  },
  watch: {
    requiresVehicleSection(value) {
      if (!value && this.currentStep === 4) {
        this.currentStep = 5;
      }
    },
  },
  methods: {
    hasLiveRegistrationDraft() {
      if (this.id || this.updateToken) {
        return false;
      }

      const user = this.$store.state.user || {};
      const vehicle = this.$store.state.vehicle || {};
      const userDocuments = this.$store.state.userDocuments || {};
      const vehicleDocuments = this.$store.state.vehicleDocuments || {};

      return !!(
        user.firstName
        || user.lastName
        || user.emailAddress
        || user.phone
        || user.addressAuto
        || Object.keys(userDocuments).length
        || Object.keys(vehicleDocuments).length
        || vehicle.registrationNumber
        || vehicle.brand
        || vehicle.model
      );
    },
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
      this.$store.commit('updateUserDocuments', {});
      this.$store.commit('updateVehicleDocuments', {});
      this.selectedPaymentType = 'CARD';
      this.accountBootstrapCompleted = false;
      this.accountBootstrapPending = false;
      this.legalConsentAccepted = false;
      this.showLegalConsentError = false;
      this.captchaVisible = false;
      this.captchaToken = '';
      this.captchaErrorMessage = '';
    },
    handleCaptchaVerified(token) {
      this.captchaToken = token;
      this.captchaErrorMessage = '';
    },
    handleCaptchaExpired() {
      this.captchaToken = '';
    },
    requestCaptchaChallenge() {
      this.captchaVisible = true;
      this.captchaToken = '';
      this.captchaErrorMessage = this.$i18n.t('captchaRequiredMessage');
    },
    persistResidenceSnapshot(userCandidate = this.$store.state.user) {
      const storageKey = onboardingAddressStorageKey(userCandidate?.emailAddress);
      if (!storageKey || typeof window === 'undefined') {
        return;
      }
      try {
        const snapshot = createResidenceSnapshot(userCandidate);
        if (hasValidResidenceData(snapshot)) {
          window.localStorage.setItem(storageKey, JSON.stringify(snapshot));
        }
      } catch (error) {
        console.warn('Unable to persist onboarding residence snapshot:', error);
      }
    },
    restoreResidenceSnapshot() {
      const currentUser = normalizeUserWizardState(this.$store.state.user);
      if (hasValidResidenceData(currentUser)) {
        return currentUser;
      }

      const storageKey = onboardingAddressStorageKey(currentUser?.emailAddress);
      if (!storageKey || typeof window === 'undefined') {
        return currentUser;
      }

      try {
        const rawSnapshot = window.localStorage.getItem(storageKey);
        if (!rawSnapshot) {
          return currentUser;
        }
        const snapshot = JSON.parse(rawSnapshot);
        const restoredUser = normalizeUserWizardState({
          ...currentUser,
          addressAuto: currentUser.addressAuto || snapshot.addressAuto || '',
          personalAddress: hasValidResidenceData(currentUser)
            ? currentUser.personalAddress
            : (snapshot.personalAddress?.length ? snapshot.personalAddress : currentUser.personalAddress),
        });
        if (hasValidResidenceData(restoredUser)) {
          this.$store.commit('updateUser', restoredUser);
        }
        return restoredUser;
      } catch (error) {
        console.warn('Unable to restore onboarding residence snapshot:', error);
        return currentUser;
      }
    },
    restoreDraftIfAvailable() {
      if (this.id) {
        return false;
      }

      const draft = loadLegalDraft(this.legalFlow);
      if (!draft) {
        return false;
      }

      if (this.hasLiveRegistrationDraft()) {
        this.selectedPaymentType = draft.selectedPaymentType || this.selectedPaymentType || 'CARD';
        this.currentStep = draft.currentStep || this.currentStep || 1;
        this.legalConsentAccepted = !!draft.legalConsentAccepted;
        this.showLegalConsentError = false;
        clearLegalDraft(this.legalFlow);
        return true;
      }

      const restoredUser = draft.user || JSON.parse(JSON.stringify(EMPTY_USER));
      const restoredVehicle = draft.vehicle || JSON.parse(JSON.stringify(EMPTY_VEHICLE));

      this.$store.commit('updateUser', restoredUser);
      this.$store.commit('updateVehicle', restoredVehicle);
      this.$store.commit('updateUserDocuments', draft.userDocuments || {});
      this.$store.commit('updateVehicleDocuments', draft.vehicleDocuments || {});
      this.selectedPaymentType = draft.selectedPaymentType || 'CARD';
      this.currentStep = draft.currentStep || 1;
      this.legalConsentAccepted = !!draft.legalConsentAccepted;
      this.showLegalConsentError = false;
      clearLegalDraft(this.legalFlow);
      return true;
    },
    saveDraftBeforeLegalConsultation() {
      if (this.isForUpdate) {
        return;
      }

      saveLegalDraft(this.legalFlow, {
        currentStep: this.currentStep,
        user: this.$store.state.user,
        vehicle: this.$store.state.vehicle,
        userDocuments: {},
        vehicleDocuments: {},
        selectedPaymentType: this.selectedPaymentType,
        legalConsentAccepted: this.legalConsentAccepted,
      });
    },
    loadUserForUpdate() {
      const url = this.isPublicTokenUpdate
        ? `${this.$i18n.t('userRootURL')}public-update-profile?updateToken=${encodeURIComponent(this.updateToken)}`
        : this.$i18n.t('userRootURL') + this.$i18n.t('getUserByEmail') + encodeURIComponent(this.id);
      this.isLoadingPage = true;
      this.loadError = false;
      http.get(url)
        .then((response) => {
          const residence = response.data.personalAddress?.[0] || {};
          const normalizedAddressAuto = response.data.addressAuto
            || [residence.line1, residence.line2, `${residence.zipCode || ''} ${residence.town || ''}`.trim(), residence.country]
              .filter((chunk) => !!chunk && `${chunk}`.trim().length)
              .join(', ');
          const normalizedUser = {
            ...JSON.parse(JSON.stringify(EMPTY_USER)),
            ...response.data,
            onboarding: this.updateToken
              ? {
                  ...(response.data.onboarding || {}),
                  resumeToken: this.updateToken,
                }
              : (response.data.onboarding || null),
            deliveryMode: response.data.deliveryMode || DEFAULT_DELIVERY_MODE,
            addressAuto: normalizedAddressAuto,
            personalAddress: response.data.personalAddress?.length ? response.data.personalAddress : JSON.parse(JSON.stringify(EMPTY_USER.personalAddress)),
            paymentModes: {
              CREDIT_CARD: {
                ...EMPTY_USER.paymentModes.CREDIT_CARD,
                ...(response.data.paymentModes?.CREDIT_CARD || {}),
              },
              IBAN: {
                ...EMPTY_USER.paymentModes.IBAN,
                ...(response.data.paymentModes?.IBAN || {}),
              },
            },
          };
          const normalizedVehicle = {
            ...JSON.parse(JSON.stringify(EMPTY_VEHICLE)),
            ...(response.data.vehicles?.[0] || {}),
          };

          this.$store.commit('updateUser', normalizedUser);
          this.$store.commit('updateVehicle', normalizedVehicle);
          this.persistResidenceSnapshot(normalizedUser);

          const userDocument = {};
          ['ID', 'PICTURE', 'DRIVER_LICENCE', 'USER_COMPANY_EXTRACT', 'USER_COMPANY_INSURANCE', 'RIB'].forEach((key) => {
            if (response.data.document?.[key]) {
              userDocument[key] = {
                name: key === 'RIB' ? this.$t('bankIdLabel') : key,
                documentStatus: response.data.document[key].documentStatus,
                reviewComment: response.data.document[key].reviewComment || '',
                reviewedAt: response.data.document[key].reviewedAt || null,
                reviewedBy: response.data.document[key].reviewedBy || '',
              };
            }
          });
          this.$store.commit('updateUserDocuments', userDocument);

          const vehicleDocuments = {};
          ['GRAY_CARD', 'INSURANCE'].forEach((key) => {
            if (response.data.document?.[key]) {
              vehicleDocuments[key] = {
                name: key,
                documentStatus: response.data.document[key].documentStatus,
                reviewComment: response.data.document[key].reviewComment || '',
                reviewedAt: response.data.document[key].reviewedAt || null,
                reviewedBy: response.data.document[key].reviewedBy || '',
              };
            }
          });
          this.$store.commit('updateVehicleDocuments', vehicleDocuments);
          this.selectedPaymentType = normalizedUser.paymentModes?.IBAN?.iban
            || normalizedUser.paymentModes?.IBAN?.bic
            || userDocument.RIB
            ? 'IBAN'
            : 'CARD';
          this.accountBootstrapCompleted = true;
          this.currentStep = this.isAccountContactUpdateFlow
            ? 1
            : this.resolveInitialUpdateStep(userDocument, vehicleDocuments);
        })
        .catch((error) => {
          this.loadError = true;
          console.error(this.$t('requestErrorGeneric'), {
            status: error?.response?.status,
            data: error?.response?.data,
            message: error?.message,
          });
        })
        .finally(() => {
          this.isLoadingPage = false;
        });
    },
    previousStep() {
      const currentIndex = this.visibleSteps.findIndex((step) => step.id === this.currentStep);
      if (currentIndex > 0) {
        this.currentStep = this.visibleSteps[currentIndex - 1].id;
      }
    },
    goToNextVisibleStep() {
      const currentIndex = this.visibleSteps.findIndex((step) => step.id === this.currentStep);
      if (currentIndex !== -1 && currentIndex < this.visibleSteps.length - 1) {
        this.currentStep = this.visibleSteps[currentIndex + 1].id;
      }
    },
    documentLabel(key) {
      const labels = {
        ID: 'userDocumentID',
        PICTURE: 'PICTURE',
        DRIVER_LICENCE: 'userDocumentDriverLicence',
        USER_COMPANY_EXTRACT: 'userDocumentCompanyExtract',
        USER_COMPANY_INSURANCE: 'userDocumentCompanyInsurance',
        RIB: 'userIBAN',
        GRAY_CARD: 'userVehicleGryCard',
        INSURANCE: 'userVehicleInsurance',
      };
      return labels[key] || key;
    },
    resolveInitialUpdateStep(userDocuments, vehicleDocuments) {
      if (this.user?.onboarding?.currentStep) {
        return this.user.onboarding.currentStep;
      }
      const hasRejectedUserDocs = Object.values(userDocuments || {}).some((document) => document?.documentStatus === 'REJECTED');
      if (hasRejectedUserDocs) {
        return 3;
      }
      const hasRejectedVehicleDocs = Object.values(vehicleDocuments || {}).some((document) => document?.documentStatus === 'REJECTED');
      if (hasRejectedVehicleDocs) {
        return 4;
      }
      return 1;
    },
    async handleStepSubmit() {
      if (this.currentStep === 1) {
        await this.validateProfileStep();
        return;
      }
      if (this.currentStep === 2) {
        await this.validateAddressStep();
        return;
      }
      if (this.currentStep === 3) {
        await this.validateDocumentsStep();
        return;
      }
      if (this.currentStep === 4) {
        await this.validateVehicleStep();
        return;
      }
      await this.submitFormUser();
    },
    async validateProfileStep() {
      const userInfo = this.$refs.userInfo;
      const validPasswordConfirm = this.isForUpdate || validatePasswordConfirmation(this.user.password, this.user.passwordConfirmation);
      const validEmailConfirmation = validateEmailConfirmation(this.user.emailAddress, this.user.emailAddressConfirmation);
      const validPhoneConfirmation = validatePhoneConfirmation(this.user.phone, this.user.phoneConfirmation);
      const validBirthDate = this.user.type !== 'DELIVERY_PERSON' || isAdultBirthDate(this.user.birthDate);
      const shouldCheckExistingEmail = !this.isForUpdate && !!this.user.emailAddress && validEmailConfirmation;
      const existingEmail = shouldCheckExistingEmail ? await this.existingEmail(this.user.emailAddress) : false;

      userInfo.isPasswordConfirmationError = !validPasswordConfirm;
      userInfo.passwordConfirmationErrorMessage = this.$i18n.t('mandatoryField') + this.$i18n.t('PasswordConfirmation');
      userInfo.isExistingEmail = existingEmail;
      userInfo.existingEmailErrorMessage = this.$i18n.t('ExistingEmail');
      userInfo.isBirthDateError = !validBirthDate;
      userInfo.birthDateErrorMessage = this.$t('userRegistrationAgeError');
      userInfo.isEmailConfirmationError = !validEmailConfirmation;
      userInfo.emailConfirmationErrorMessage = this.$i18n.t('mandatoryField') + this.$i18n.t('emailConfirmation');
      userInfo.isPhoneConfirmationError = !validPhoneConfirmation;
      userInfo.phoneConfirmationErrorMessage = this.$i18n.t('mandatoryField') + this.$i18n.t('phoneConfirmation');
      this.showLegalConsentError = false;

      if (validPasswordConfirm && validEmailConfirmation && validPhoneConfirmation && validBirthDate && !existingEmail) {
        if (this.isClientRegistrationFlow) {
          if (this.isForUpdate && this.visibleSteps.length > 1) {
            this.goToNextVisibleStep();
            return;
          }
          await this.submitFormUser();
          return;
        }
        if (!this.isForUpdate && !this.accountBootstrapCompleted) {
          if (!this.validateLegalConsent()) {
            return;
          }
          const bootstrapSucceeded = await this.createAccountBootstrap();
          if (!bootstrapSucceeded) {
            return;
          }
        }
        const nextStep = this.user?.onboarding?.currentStep;
        if (nextStep && nextStep > this.currentStep) {
          this.currentStep = nextStep;
          return;
        }
        this.goToNextVisibleStep();
      }
    },
    async validateAddressStep() {
      const addressStep = this.$refs.userAddress;
      const addressAuto = addressStep.$refs.addressAutoComplete.address || this.user.addressAuto;
      this.user.addressAuto = addressAuto;

      if (!validateAddress(addressAuto)) {
        addressStep.isAddressError = true;
        addressStep.errorAddressMessage = this.$i18n.t('mandatoryField') + this.$i18n.t('invalidAddress');
        return;
      }

      addressStep.isAddressError = false;
      const chunks = addressAuto.split(',').map((chunk) => chunk.trim()).filter(Boolean);
      const normalizedUser = normalizeUserWizardState(this.user);
      const residence = normalizedUser.personalAddress[0];
      const lineChunks = chunks.slice(0, Math.max(chunks.length - 2, 1));
      const cityChunk = chunks[chunks.length - 2] || '';
      const cityTokens = cityChunk.split(' ').filter(Boolean);
      residence.line1 = lineChunks.join(', ');
      residence.zipCode = cityTokens[0] || '';
      residence.town = cityTokens.slice(1).join(' ');
      residence.country = chunks[chunks.length - 1] || '';

      const persistedUser = { ...normalizedUser, addressAuto };
      this.$store.commit('updateUser', persistedUser);
      this.persistResidenceSnapshot(persistedUser);
      if (this.shouldPersistDraftOnStepSubmit()) {
        await this.saveOnboardingDraftStep(2);
        return;
      }
      if (this.isLastStep) {
        await this.submitFormUser();
        return;
      }
      this.goToNextVisibleStep();
    },
    async validateDocumentsStep() {
      const restoredUser = this.restoreResidenceSnapshot();
      if (!hasValidResidenceData(restoredUser)) {
        this.currentStep = 2;
        return;
      }
      const selectedFilesKeys = this.isForUpdate
        ? this.editableUserDocumentKeys
        : (this.user.type === 'DELIVERY_PERSON'
            ? getRequiredUserDocuments(this.user)
            : ['ID', 'PICTURE']);

      const userDocs = this.$refs.userDocuments;
      if (!selectedFilesKeys.length) {
        this.goToNextVisibleStep();
        return;
      }
      const fileValidation = validateFileInput(selectedFilesKeys, this.$store.state.userDocuments);
      userDocs.filesErrorMessages = [];
      if (fileValidation?.length) {
        fileValidation.forEach((result) => {
          userDocs.filesErrorMessages[result.missingKey] = this.$i18n.t('fileRequired');
        });
        return;
      }

      if (!this.isForUpdate && (this.selectedPaymentType || 'CARD') === 'IBAN' && !this.$store.state.userDocuments?.RIB?.file) {
        userDocs.filesErrorMessages.RIB = this.$i18n.t('fileRequired');
        return;
      }

      if (this.shouldPersistDraftOnStepSubmit()) {
        await this.saveOnboardingDraftStep(3);
        return;
      }
      if (this.isLastStep) {
        await this.submitFormUser();
        return;
      }
      this.goToNextVisibleStep();
    },
    async validateVehicleStep() {
      const restoredUser = this.restoreResidenceSnapshot();
      if (!hasValidResidenceData(restoredUser)) {
        this.currentStep = 2;
        return;
      }
      const userDocs = this.$refs.userDocuments;
      const selectedPaymentType = this.selectedPaymentType || 'CARD';

      if (!this.isForUpdate && selectedPaymentType === 'IBAN' && !this.$store.state.userDocuments?.RIB) {
        userDocs.filesErrorMessages.RIB = this.$i18n.t('fileRequired');
        this.currentStep = 3;
        return;
      }

      if (this.user.type !== 'DELIVERY_PERSON') {
        this.goToNextVisibleStep();
        return;
      }

      if (!this.requiresVehicleSection) {
        this.goToNextVisibleStep();
        return;
      }

      const selectedFilesKeys = this.isForUpdate ? this.editableVehicleDocumentKeys : getRequiredVehicleDocuments(this.user);
      const vehicleDocs = this.$refs.vehicleInfo;
      if (!selectedFilesKeys.length) {
        this.goToNextVisibleStep();
        return;
      }
      const fileValidation = validateFileInput(selectedFilesKeys, this.$store.state.vehicleDocuments);
      vehicleDocs.filesErrorMessages = [];
      if (fileValidation?.length) {
        fileValidation.forEach((result) => {
          vehicleDocs.filesErrorMessages[result.missingKey] = this.$i18n.t('fileRequired');
        });
        return;
      }

      if (this.shouldPersistDraftOnStepSubmit()) {
        await this.saveOnboardingDraftStep(4);
        return;
      }
      if (this.isLastStep) {
        await this.submitFormUser();
        return;
      }
      this.goToNextVisibleStep();
    },
    existingEmail(email) {
      return new Promise((resolve) => {
        http.get(`${this.$i18n.t('userRootURL')}${this.$i18n.t('getPublicRegistrationStatus')}${encodeURIComponent(email)}`)
          .then((response) => {
            const lookup = response.data || {};
            resolve(response.status === 200 && lookup.emailInUse === true && lookup.resumableOnboarding !== true);
          })
          .catch(() => {
            resolve(false);
          });
      });
    },
    validateLegalConsent() {
      if (this.isForUpdate) {
        return true;
      }

      const hasConsulted = hasLegalPageBeenConsulted(this.legalFlow);
      const isAccepted = this.legalConsentAccepted;
      this.showLegalConsentError = !hasConsulted || !isAccepted;
      return hasConsulted && isAccepted;
    },
    buildAccountCreatePayload() {
      const userState = normalizeUserForSubmission(this.$store.state.user);
      delete userState.documents;
      return {
        ...userState,
        document: {},
      };
    },
    shouldPersistDraftOnStepSubmit() {
      return this.user.type === 'DELIVERY_PERSON'
        && !this.isPendingAccountValidation
        && (!this.isForUpdate || this.isResumeOnboardingFlow);
    },
    buildOnboardingFormData(includeFiles = false) {
      const formData = new FormData();
      const restoredUser = this.restoreResidenceSnapshot();
      const userState = normalizeUserForSubmission(restoredUser);
      delete userState.documents;
      const payloadUser = {
        ...userState,
        document: {},
      };
      const payloadVehicle = {
        ...normalizeVehicleForSubmission(this.$store.state.vehicle),
        vehicleDocuments: {},
      };
      formData.append('user', JSON.stringify(payloadUser));
      formData.append('vehicle', JSON.stringify(payloadVehicle));

      if (includeFiles) {
        Object.entries(this.$store.state.userDocuments || {}).forEach(([key, value]) => {
          if (value?.file) {
            formData.append(key, value.file);
          }
        });
        Object.entries(this.$store.state.vehicleDocuments || {}).forEach(([key, value]) => {
          if (value?.file) {
            formData.append(key, value.file);
          }
        });
      }

      const userLanguage = navigator.languages && navigator.languages.length ? navigator.languages[0] : navigator.language || 'fr-FR';
      formData.append('locale', userLanguage);
      return formData;
    },
    async saveOnboardingDraftStep(step) {
      const executeDraftSave = async () => {
        const formData = this.buildOnboardingFormData(true);
        formData.append('step', `${step}`);
        return http.post(
          this.$i18n.t('userRootURL') + this.$i18n.t('saveUserOnboardingDraft'),
          formData,
          { headers: { Accept: 'application/json' } }
        );
      };

      try {
        let response;
        try {
          response = await executeDraftSave();
        } catch (error) {
          const responseStatus = error?.response?.status;
          const staleResumeToken = this.$store.state.user?.onboarding?.resumeToken;
          if (responseStatus === 404 && staleResumeToken) {
            const currentUser = normalizeUserWizardState(this.$store.state.user);
            this.$store.commit('updateUser', {
              ...currentUser,
              onboarding: currentUser.onboarding
                ? {
                    ...currentUser.onboarding,
                    resumeToken: null,
                    resumeLink: null,
                  }
                : null,
            });
            response = await executeDraftSave();
          } else {
            throw error;
          }
        }
        if (`${response.status}` === '200') {
          const currentUser = normalizeUserWizardState(this.$store.state.user);
          const mergedUser = normalizeUserWizardState({
            ...currentUser,
            ...response.data,
            addressAuto: response.data?.addressAuto || currentUser.addressAuto,
            personalAddress: response.data?.personalAddress?.length
              ? response.data.personalAddress
              : currentUser.personalAddress,
            paymentModes: {
              CREDIT_CARD: {
                ...EMPTY_USER.paymentModes.CREDIT_CARD,
                ...(currentUser.paymentModes?.CREDIT_CARD || {}),
                ...(response.data?.paymentModes?.CREDIT_CARD || {}),
              },
              IBAN: {
                ...EMPTY_USER.paymentModes.IBAN,
                ...(currentUser.paymentModes?.IBAN || {}),
                ...(response.data?.paymentModes?.IBAN || {}),
              },
            },
            onboarding: response.data?.onboarding || this.$store.state.user?.onboarding || null,
          });
          this.$store.commit('updateUser', mergedUser);
          this.persistResidenceSnapshot(mergedUser);
          if (response.data?.vehicles?.[0]) {
            this.$store.commit('updateVehicle', {
              ...this.$store.state.vehicle,
              ...response.data.vehicles[0],
            });
          }
          const nextStep = mergedUser?.onboarding?.currentStep;
          if (nextStep) {
            this.currentStep = nextStep;
          } else {
            this.goToNextVisibleStep();
          }
          return true;
        }
      } catch (error) {
        console.error(this.$t('requestErrorGeneric'), {
          status: error?.response?.status,
          data: error?.response?.data,
          message: error?.message,
        });
      }
      return false;
    },
    async createAccountBootstrap() {
      if (this.isForUpdate || this.accountBootstrapCompleted || this.accountBootstrapPending) {
        return true;
      }

      this.accountBootstrapPending = true;
      const userLanguage = navigator.languages && navigator.languages.length ? navigator.languages[0] : navigator.language || 'fr-FR';
      const payload = {
        user: this.buildAccountCreatePayload(),
        locale: userLanguage,
      };
      if (this.shouldShowCaptcha && !this.captchaToken) {
        this.requestCaptchaChallenge();
        this.accountBootstrapPending = false;
        return false;
      }

      try {
        const response = await http.post(
          this.$i18n.t('userRootURL') + this.$i18n.t('createUserAccount'),
          payload,
          {
            headers: {
              'Content-Type': 'application/json',
              Accept: 'application/json',
              ...(this.captchaToken ? { 'X-Captcha-Token': this.captchaToken } : {}),
            },
          }
        );
        if (`${response.status}` === '200') {
          const mergedUser = normalizeUserWizardState({
            ...this.$store.state.user,
            ...response.data,
            onboarding: response.data?.onboarding || null,
          });
          this.$store.commit('updateUser', mergedUser);
          this.persistResidenceSnapshot(mergedUser);
          this.accountBootstrapCompleted = true;
          this.captchaVisible = false;
          this.captchaToken = '';
          this.captchaErrorMessage = '';
          return true;
        }
      } catch (error) {
        this.captchaToken = '';
        this.$refs.accountCaptcha?.resetCaptcha?.();
        if (error?.response?.status === 400) {
          this.captchaErrorMessage = this.$i18n.t('captchaRetryMessage');
        }
        console.error(this.$t('requestErrorGeneric'), {
          status: error?.response?.status,
          data: error?.response?.data,
          message: error?.message,
        });
      } finally {
        this.accountBootstrapPending = false;
      }
      return false;
    },
    async submitFormUser() {
      if (!this.isForUpdate && !this.isResumeOnboardingFlow && !this.validateLegalConsent()) {
        return;
      }

      if (!this.isForUpdate && this.isClientRegistrationFlow) {
        const bootstrapSucceeded = await this.createAccountBootstrap();
        if (!bootstrapSucceeded) {
          return;
        }
        clearLegalDraft(this.legalFlow);
        this.initializeStore();
        this.$router.push('/');
        return;
      }

      if (!this.isForUpdate && !this.accountBootstrapCompleted) {
        const bootstrapSucceeded = await this.createAccountBootstrap();
        if (!bootstrapSucceeded) {
          return;
        }
      }

      const formData = this.buildOnboardingFormData(false);

      if (!this.isForUpdate) {
        Object.entries(this.$store.state.userDocuments || {}).forEach(([key, value]) => {
          if (value?.file) {
            if (!formData.getAll(key).length) {
              formData.append(key, value.file);
            }
          }
        });
      } else if (this.isPendingAccountValidation) {
        this.editableUserDocumentKeys.forEach((key) => {
          const value = this.$store.state.userDocuments?.[key];
          if (value?.documentStatus === 'UPDATED' && value?.file) {
            if (!formData.getAll(key).length) {
              formData.append(key, value.file);
            }
          }
        });
      }
      if (!this.isForUpdate) {
        Object.entries(this.$store.state.vehicleDocuments || {}).forEach(([key, value]) => {
          if (value?.file) {
            if (!formData.getAll(key).length) {
              formData.append(key, value.file);
            }
          }
        });
      } else if (this.isPendingAccountValidation) {
        this.editableVehicleDocumentKeys.forEach((key) => {
          const value = this.$store.state.vehicleDocuments?.[key];
          if (value?.documentStatus === 'UPDATED' && value?.file) {
            if (!formData.getAll(key).length) {
              formData.append(key, value.file);
            }
          }
        });
      }

      const url = this.isForUpdate && !this.isResumeOnboardingFlow
        ? (this.isPublicTokenUpdate
            ? `${this.$i18n.t('userRootURL')}public-update?updateToken=${encodeURIComponent(this.updateToken)}`
            : this.$i18n.t('userRootURL') + this.$i18n.t('updateUser'))
        : this.$i18n.t('userRootURL') + this.$i18n.t('completeUserOnboarding');

      return http.post(url, formData, { headers: { Accept: 'application/json' } })
        .then((response) => {
          if (`${response.status}` === '200') {
            clearLegalDraft(this.legalFlow);
            this.initializeStore();
            this.$router.push('/');
          }
        })
        .catch((error) => {
          console.error(this.$t('requestErrorGeneric'), {
            status: error?.response?.status,
            data: error?.response?.data,
            message: error?.message,
          });
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
  padding: 24px 28px;
  border: 1px solid #dde5f0;
  border-radius: 26px;
  background: rgba(255, 255, 255, 0.92);
  box-shadow: 0 18px 44px rgba(24, 39, 75, 0.08);
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

.card-readiness {
  padding: 0 30px;
}

.review-banner {
  margin: 0 30px 18px;
  padding: 18px 20px;
  border: 1px solid #fecdd3;
  border-radius: 22px;
  background: linear-gradient(135deg, #fff7ed 0%, #fff1f2 100%);
}

.review-banner h3 {
  margin: 10px 0 8px;
  color: #7c2d12;
}

.review-banner p {
  margin: 0 0 14px;
  color: #9a3412;
}

.review-banner ul {
  margin: 0;
  padding-left: 18px;
  display: grid;
  gap: 10px;
}

.review-banner li {
  color: #7f1d1d;
}

.review-banner li strong {
  display: block;
  margin-bottom: 2px;
}

.review-banner li span {
  display: block;
  color: #9f1239;
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
  align-items: center;
  gap: 12px;
  padding: 18px 30px 28px;
}

.card-actions .wizard-action-btn {
  display: inline-flex;
  align-items: center;
  justify-content: center;
  width: calc(50% - 6px);
  min-width: 132px;
  max-width: 220px;
  height: 44px;
  padding: 0 18px;
  border-radius: 999px;
}

.card-actions .wizard-action-btn[type="submit"] {
  order: 1;
  margin-right: auto;
}

.card-actions .wizard-action-btn[type="button"] {
  order: 2;
  margin-left: auto;
}

.card-content :deep(.legal-consent-card) {
  margin-top: 18px;
}

.captcha-error {
  margin-top: 12px;
  color: #b42318;
  font-size: 0.9rem;
}

.card-actions .wizard-action-btn:hover {
  background: #0f172a;
}

@media screen and (max-width: 1180px) {
  .wizard-shell {
    grid-template-columns: 1fr;
  }

  .wizard-sidebar {
    display: flex;
    overflow-x: auto;
    overflow-y: hidden;
    padding-bottom: 8px;
    scroll-snap-type: x proximity;
  }

  .wizard-step {
    min-width: 200px;
    flex: 0 0 auto;
    scroll-snap-align: start;
  }
}

@media screen and (max-width: 720px) {
  .user-registration-page {
    padding: 12px;
  }

  .page-header {
    padding: 14px 12px;
  }

  .wizard-card {
    min-height: auto;
  }

  .wizard-sidebar {
    gap: 8px;
    padding: 8px;
  }

  .wizard-step {
    min-width: 72px;
    grid-template-columns: 1fr;
    justify-items: center;
    gap: 4px;
    padding: 5px;
    border-radius: 12px;
  }

  .step-index {
    width: 24px;
    height: 24px;
    font-size: 0.72rem;
  }

  .step-copy {
    justify-items: center;
    text-align: center;
  }

  .step-copy strong {
    font-size: 0.62rem;
    line-height: 1.1;
    max-width: 100%;
    overflow: hidden;
    text-overflow: ellipsis;
    white-space: nowrap;
  }

  .step-copy small {
    display: none;
  }

  .card-header,
  .card-readiness,
  .review-banner,
  .card-content,
  .card-actions {
    padding-left: 12px;
    padding-right: 12px;
  }

  .review-banner {
    margin-left: 12px;
    margin-right: 12px;
  }

  .card-actions {
    flex-direction: row;
    justify-content: space-between;
    gap: 8px;
  }

  .card-actions .wizard-action-btn {
    width: calc(50% - 4px);
    min-width: 0;
    max-width: none;
  }
}

@media screen and (max-width: 480px) {
  .wizard-sidebar {
    display: grid;
    grid-template-columns: repeat(auto-fit, minmax(58px, 1fr));
    overflow: hidden;
  }

  .wizard-step {
    min-width: 0;
    width: 100%;
    padding: 4px;
  }

  .step-copy strong {
    font-size: 0.58rem;
    line-height: 1;
    max-width: 42px;
  }

  .review-banner {
    padding: 12px;
  }
}
</style>
