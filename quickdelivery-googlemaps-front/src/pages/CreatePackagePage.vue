<template>
  <div class="create-package-page qd-page">
    <header class="qd-page-header">
      <div class="header-main">
        <span class="page-chip">{{ $t('menuNewPackage') }}</span>
        <h1>{{ $t('createPackagePageTitle') }}</h1>
        <p>{{ $t('createPackagePageSubtitle') }}</p>
      </div>
    </header>

    <section v-if="showGuestChoice" class="guest-entry-card glass-pane">
      <span class="page-chip">{{ $t('guestCreatePackageChip') }}</span>
      <h2>{{ $t('guestCreatePackageTitle') }}</h2>
      <p>{{ $t('guestCreatePackageSubtitle') }}</p>
      <div class="guest-entry-actions">
        <router-link class="qd-btn-primary guest-entry-btn" to="/register">
          {{ $t('guestCreatePackageRegisterAction') }}
        </router-link>
        <button class="qd-btn-secondary guest-entry-btn" type="button" @click="continueAsGuest">
          {{ $t('guestCreatePackageAnonymousAction') }}
        </button>
      </div>
    </section>

    <Form v-if="canAccessWizard" class="wizard-shell" @submit="handleStepSubmit">
      <aside class="wizard-sidebar">
        <button
          v-for="step in steps"
          :key="step.id"
          class="wizard-step"
          :class="stepState(step.id)"
          type="button"
        >
          <span class="step-index">{{ step.id }}</span>
          <span class="step-copy">
            <strong>{{ $t(step.label) }}</strong>
            <small>{{ stepHint(step.id) }}</small>
          </span>
        </button>
      </aside>

      <section class="wizard-card">
        <div class="card-header">
          <div>
            <span class="eyebrow">{{ currentStepMeta.id }}/{{ steps.length }}</span>
            <h2>{{ $t(currentStepMeta.title) }}</h2>
            <p>{{ $t(currentStepMeta.subtitle) }}</p>
          </div>
        </div>

        <div class="card-content">
          <PackageAddress
            v-if="currentStep === 1"
            ref="departureAddress"
            addressType="DEPARTURE"
          />

          <PackageAddress
            v-else-if="currentStep === 2"
            ref="arrivalAddress"
            addressType="ARRIVAL"
          />

          <PackageCreation
            v-else-if="currentStep === 3"
            v-model:selectedPreset="selectedPreset"
          />

          <PackageOptions
            v-else-if="currentStep === 4"
            ref="packageOptions"
            v-model="wizardOptions"
          />

          <PackageConfirmationSummary
            v-else
            :options="wizardOptions"
            :selected-preset="selectedPreset"
          />

          <LegalConsentCard
            v-if="currentStep === steps.length"
            v-model="legalConsentAccepted"
            :flow="legalFlow"
            :return-to="$route.fullPath"
            :show-error="showLegalConsentError"
            @open-terms="saveDraftBeforeLegalConsultation"
          />
          <TurnstileCaptcha
            v-if="requiresGuestCaptcha"
            ref="packageCaptcha"
            action="package-create"
            @verified="captchaToken = $event"
            @expired="captchaToken = ''"
            @error="captchaToken = ''"
          />
          <p v-if="captchaErrorMessage" class="captcha-error">{{ captchaErrorMessage }}</p>
        </div>

        <footer class="card-actions">
          <button
            v-if="currentStep > 1"
            class="qd-btn-secondary wizard-action-btn"
            type="button"
            @click="previousStep"
          >
            {{ $t('createPackageBackAction') }}
          </button>

          <button class="qd-btn-primary wizard-action-btn" type="submit">
            {{ currentStep === steps.length ? $t('createPackageSubmitAction') : $t('packageNextAction') }}
          </button>
        </footer>
      </section>
    </Form>
  </div>
</template>

<script>
import { Form } from 'vee-validate';
import http from '@/config/httpInterceptor';
import { validateAddress, validateDeliveryDateTime } from '@/config/comonFunction';
import { getCurrentUserIdentity, hasValidAccessToken } from '@/config/auth';
import { hydrateConnectedUser } from '@/config/session';
import { LEGAL_FLOW_PACKAGE_CREATION, clearLegalDraft, hasLegalPageBeenConsulted, loadLegalDraft, saveLegalDraft } from '@/config/legal';
import { isMobileCapacitorRuntime } from '@/config/network';
import TurnstileCaptcha from '../components/TurnstileCaptcha.vue';
import LegalConsentCard from '../components/LegalConsentCard.vue';
import PackageAddress from '../components/PackageAddress.vue';
import PackageConfirmationSummary from '../components/PackageConfirmationSummary.vue';
import PackageCreation from '../components/PackageCreation.vue';
import PackageOptions from '../components/PackageOptions.vue';

const EMPTY_PACKAGE = {
  id: null,
  version: null,
  creationDate: null,
  reference: '',
  height: 0,
  width: 0,
  depth: 0,
  weight: 0,
  packageSizeCategory: 'MEDIUM',
  pictureURL: '',
  status: '',
  deliveryPrice: null,
  deliverySpeed: 'STANDARD',
  insuranceSelected: false,
  declaredValue: null,
  senderID: null,
  packageReservations: [],
  addresses: [
    {
      firstName: '',
      lastName: '',
      line1: '',
      line2: '',
      town: '',
      zipCode: '',
      country: '',
      floor: 0,
      hasElevator: null,
      dateTime: null,
      email: '',
      phone: '',
      type: 'DEPARTURE',
      addressAuto: '',
      latitude: null,
      longitude: null,
    },
    {
      firstName: '',
      lastName: '',
      line1: '',
      line2: '',
      town: '',
      zipCode: '',
      country: '',
      floor: 0,
      hasElevator: null,
      dateTime: null,
      email: '',
      phone: '',
      type: 'ARRIVAL',
      addressAuto: '',
      latitude: null,
      longitude: null,
    },
  ],
  lastPositionLatitude: null,
  lastPositionLongitude: null,
};

function onboardingAddressStorageKey(emailAddress) {
  return emailAddress ? `quickdelivery.onboarding.address.${emailAddress.toLowerCase()}` : null;
}

export default {
  components: {
    Form,
    TurnstileCaptcha,
    LegalConsentCard,
    PackageAddress,
    PackageCreation,
    PackageOptions,
    PackageConfirmationSummary,
  },
  data() {
    return {
      currentStep: 1,
      selectedPreset: 'MEDIUM',
      wizardOptions: {
        deliverySpeed: 'STANDARD',
        insurance: false,
        declaredValue: null,
      },
      steps: [
        { id: 1, label: 'wizardCreateStepPickup', title: 'createPackagePickupTitle', subtitle: 'createPackagePickupSubtitle' },
        { id: 2, label: 'wizardCreateStepDelivery', title: 'createPackageDeliveryTitle', subtitle: 'createPackageDeliverySubtitle' },
        { id: 3, label: 'wizardCreateStepPackage', title: 'createPackageInfoTitle', subtitle: 'createPackageInfoSubtitle' },
        { id: 4, label: 'wizardCreateStepOptions', title: 'createPackageOptionsTitle', subtitle: 'createPackageOptionsSubtitle' },
        { id: 5, label: 'wizardCreateStepConfirm', title: 'createPackageConfirmTitle', subtitle: 'createPackageConfirmSubtitle' },
      ],
      allowAnonymousCreation: false,
      legalConsentAccepted: false,
      showLegalConsentError: false,
      captchaToken: '',
      captchaErrorMessage: '',
    };
  },
  computed: {
    legalFlow() {
      return LEGAL_FLOW_PACKAGE_CREATION;
    },
    package_() {
      return this.$store.state.package_;
    },
    documentS() {
      return this.$store.state.documentS || [];
    },
    currentStepMeta() {
      return this.steps.find((step) => step.id === this.currentStep) || this.steps[0];
    },
    isAuthenticated() {
      return hasValidAccessToken();
    },
    showGuestChoice() {
      return !this.isAuthenticated && !this.allowAnonymousCreation;
    },
    canAccessWizard() {
      return this.isAuthenticated || this.allowAnonymousCreation;
    },
    requiresGuestCaptcha() {
      return this.currentStep === 1
        && !this.isAuthenticated
        && !isMobileCapacitorRuntime()
        && !!process.env.VUE_APP_TURNSTILE_SITE_KEY;
    },
    connectedUser() {
      return this.$store.state.connectedUser || {};
    },
  },
  watch: {
    'connectedUser.loaded': {
      immediate: true,
      handler(loaded) {
        if (loaded) {
          this.prefillDepartureAddressFromConnectedUser();
        }
      },
    },
    'connectedUser.personalAddress': {
      deep: true,
      handler(addresses) {
        if (this.connectedUser?.loaded && Array.isArray(addresses) && addresses.length > 0) {
          this.prefillDepartureAddressFromConnectedUser();
        }
      },
    },
  },
  mounted() {
    if (!this.restoreDraftIfAvailable()) {
      this.$store.commit('updateDocuments', []);
      this.$store.commit('updatePackage', JSON.parse(JSON.stringify(EMPTY_PACKAGE)));
      this.prefillDepartureAddressFromConnectedUser();
    }
  },
  methods: {
    restoreDraftIfAvailable() {
      const draft = loadLegalDraft(this.legalFlow);
      if (!draft) {
        return false;
      }

      this.currentStep = draft.currentStep || 1;
      this.selectedPreset = draft.selectedPreset || 'MEDIUM';
      this.wizardOptions = draft.wizardOptions || {
        deliverySpeed: 'STANDARD',
        insurance: false,
        declaredValue: null,
      };
      this.allowAnonymousCreation = !!draft.allowAnonymousCreation;
      this.legalConsentAccepted = !!draft.legalConsentAccepted;
      this.showLegalConsentError = false;
      this.$store.commit('updateDocuments', draft.documentS || []);
      this.$store.commit('updatePackage', draft.package_ || JSON.parse(JSON.stringify(EMPTY_PACKAGE)));
      clearLegalDraft(this.legalFlow);
      return true;
    },
    saveDraftBeforeLegalConsultation() {
      saveLegalDraft(this.legalFlow, {
        currentStep: this.currentStep,
        selectedPreset: this.selectedPreset,
        wizardOptions: this.wizardOptions,
        allowAnonymousCreation: this.allowAnonymousCreation,
        legalConsentAccepted: this.legalConsentAccepted,
        package_: this.$store.state.package_,
        documentS: this.$store.state.documentS,
      });
    },
    async prefillDepartureAddressFromConnectedUser() {
      if (!this.isAuthenticated) {
        return;
      }

      const departureAddress = this.$store.state.package_?.addresses?.[0];
      if (!departureAddress || this.hasLockedDepartureAddressData(departureAddress)) {
        return;
      }

      try {
        const user = await hydrateConnectedUser();
        const fallbackResidenceSnapshot = this.loadConnectedUserResidenceSnapshot(user);
        const residence = Array.isArray(user?.personalAddress) && user.personalAddress.length
          ? user.personalAddress[0]
          : fallbackResidenceSnapshot?.personalAddress?.[0] || null;
        console.info('QuickDelivery connected user for package prefill:', user);
        console.info('QuickDelivery residence used for package prefill:', residence);
        if (!residence && !user?.firstName && !user?.lastName && !user?.email) {
          return;
        }
        const resolvedAddressAuto = user?.addressAuto || fallbackResidenceSnapshot?.addressAuto || '';
        const formattedAddress = residence
          ? [residence.line1, residence.zipCode ? `${residence.zipCode} ${residence.town || ''}`.trim() : residence.town, residence.country]
              .filter((value) => !!value)
              .join(', ')
          : '';

        this.$store.commit('updatePackageDepartureAddress', {
          ...departureAddress,
          firstName: user?.firstName || this.$store.state.connectedUser?.firstName || '',
          lastName: user?.lastName || this.$store.state.connectedUser?.lastName || '',
          email: user?.email || this.$store.state.connectedUser?.email || getCurrentUserIdentity()?.email || '',
          phone: user?.phone || '',
          floor: residence?.floor ?? departureAddress.floor,
          addressAuto: resolvedAddressAuto || formattedAddress || departureAddress.addressAuto,
          line1: residence?.line1 || departureAddress.line1,
          line2: residence?.line2 || departureAddress.line2,
          town: residence?.town || departureAddress.town,
          zipCode: residence?.zipCode || departureAddress.zipCode,
          country: residence?.country || departureAddress.country,
          latitude: residence?.latitude ?? departureAddress.latitude,
          longitude: residence?.longitude ?? departureAddress.longitude,
        });
        await this.$nextTick();
        const departureAddressRef = Array.isArray(this.$refs.departureAddress)
          ? this.$refs.departureAddress[0]
          : this.$refs.departureAddress;
        departureAddressRef?.syncAddressAutocomplete?.();
      } catch (error) {
        console.error('QuickDelivery failed to hydrate connected user for package prefill:', error);
        // Keep the wizard usable even if profile hydration fails.
      }
    },
    loadConnectedUserResidenceSnapshot(user) {
      const storageKey = onboardingAddressStorageKey(user?.email || user?.emailAddress || getCurrentUserIdentity()?.email || '');
      if (!storageKey || typeof window === 'undefined') {
        return null;
      }

      try {
        const rawSnapshot = window.localStorage.getItem(storageKey);
        if (!rawSnapshot) {
          return null;
        }
        const snapshot = JSON.parse(rawSnapshot);
        if (!snapshot || typeof snapshot !== 'object') {
          return null;
        }
        return snapshot;
      } catch (error) {
        console.warn('Unable to load onboarding residence snapshot for package prefill:', error);
        return null;
      }
    },
    hasLockedDepartureAddressData(address) {
      return Boolean(
        address.addressAuto ||
        address.line1 ||
        address.town ||
        address.zipCode ||
        address.country
      );
    },
    continueAsGuest() {
      this.allowAnonymousCreation = true;
      this.showLegalConsentError = false;
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
    stepHint(stepId) {
      const map = {
        1: 'createPackagePickupSubtitle',
        2: 'createPackageDeliverySubtitle',
        3: 'createPackageInfoSubtitle',
        4: 'createPackageOptionsSubtitle',
        5: 'createPackageConfirmSubtitle',
      };
      return this.$t(map[stepId]);
    },
    previousStep() {
      if (this.currentStep > 1) {
        this.currentStep -= 1;
      }
    },
    async handleStepSubmit() {
      if (this.currentStep === 1) {
        await this.validateAndStoreAddress(this.$refs.departureAddress, 'DEPARTURE');
        return;
      }

      if (this.currentStep === 2) {
        await this.validateAndStoreAddress(this.$refs.arrivalAddress, 'ARRIVAL', true);
        return;
      }

      if (this.currentStep === 3) {
        this.package_.status = 'PAYMENTPENDING';
        this.currentStep += 1;
        return;
      }

      if (this.currentStep === 4) {
        const optionsComponent = Array.isArray(this.$refs.packageOptions)
          ? this.$refs.packageOptions[0]
          : this.$refs.packageOptions;
        if (optionsComponent && !optionsComponent.validateSelection()) {
          return;
        }
        this.package_.deliverySpeed = this.wizardOptions.deliverySpeed || 'STANDARD';
        this.package_.insuranceSelected = !!this.wizardOptions.insurance;
        this.package_.declaredValue = this.wizardOptions.insurance ? Number(this.wizardOptions.declaredValue || 0) : null;
        this.package_.deliveryPrice = null;
        this.currentStep += 1;
        return;
      }

      await this.submitPackage();
    },
    async validateAndStoreAddress(componentRef, type, validateDeliveryWindow = false) {
      const addressComponent = Array.isArray(componentRef) ? componentRef[0] : componentRef;
      const address = addressComponent.address;
      const addressAuto = addressComponent.$refs.addressAutoComplete.address || address.addressAuto;

      address.type = type;
      address.addressAuto = addressAuto;

      if (!validateAddress(address.addressAuto)) {
        addressComponent.isAddressError = true;
        addressComponent.errorAddressMessage = this.$i18n.t('mandatoryField') + this.$i18n.t('invalidAddress');
        return;
      }

      if (validateDeliveryWindow && !validateDeliveryDateTime(this.package_.addresses[0].dateTime, this.package_.addresses[1].dateTime)) {
        addressComponent.isDateTimeError = true;
        addressComponent.errorDeliveryDateTimeMessage = this.$i18n.t('createPackageDateTimeError');
        return;
      }

      if (Number(address.floor || 0) > 0 && typeof address.hasElevator !== 'boolean') {
        addressComponent.isElevatorError = true;
        addressComponent.errorElevatorMessage = this.$i18n.t('packageAddressElevatorRequired');
        return;
      }

      if (type === 'ARRIVAL' && this.areAddressesEquivalent(this.resolveDepartureAddressForValidation(), address)) {
        addressComponent.isAddressError = true;
        addressComponent.errorAddressMessage = this.$i18n.t('packageAddressMustDiffer');
        return;
      }

      addressComponent.isAddressError = false;
      addressComponent.isDateTimeError = false;
      addressComponent.isElevatorError = false;
      addressComponent.errorElevatorMessage = null;

      try {
        await this.ensureAddressCoordinates(address);
      } catch (_error) {
        addressComponent.isAddressError = true;
        addressComponent.errorAddressMessage = this.$i18n.t('mandatoryField') + this.$i18n.t('invalidAddress');
        return;
      }

      if (type === 'DEPARTURE') {
        this.$store.commit('updatePackageDepartureAddress', { ...address });
      } else {
        this.$store.commit('updatePackageArrivalAddress', { ...address });
      }

      this.currentStep += 1;
    },
    resolveDepartureAddressForValidation() {
      const departureComponent = Array.isArray(this.$refs.departureAddress)
        ? this.$refs.departureAddress[0]
        : this.$refs.departureAddress;
      return departureComponent?.address || this.package_.addresses?.[0] || null;
    },
    areAddressesEquivalent(departureAddress, arrivalAddress) {
      const normalize = (value) => `${value || ''}`.trim().toLowerCase();
      const buildSignature = (address) => [
        normalize(address?.line1),
        normalize(address?.zipCode),
        normalize(address?.town),
        normalize(address?.country),
        normalize(address?.addressAuto),
      ].join('|');

      const departureSignature = buildSignature(departureAddress);
      const arrivalSignature = buildSignature(arrivalAddress);
      return departureSignature !== '||||' && departureSignature === arrivalSignature;
    },
    hasUsableCoordinates(address) {
      if (!Number.isFinite(Number(address?.latitude)) || !Number.isFinite(Number(address?.longitude))) {
        return false;
      }
      const latitude = Number(address.latitude);
      const longitude = Number(address.longitude);
      return !(Math.abs(latitude) < 1e-9 && Math.abs(longitude) < 1e-9);
    },
    async ensureAddressCoordinates(address) {
      if (this.hasUsableCoordinates(address)) {
        address.latitude = Number(address.latitude);
        address.longitude = Number(address.longitude);
        this.populateAddressPartsFromFormattedAddress(address);
        return;
      }

      const geocoder = await this.getGoogleGeocoder();
      const result = await geocoder.geocode({ address: address.addressAuto });
      const resolved = result?.results?.[0];
      const location = resolved?.geometry?.location;
      const latitude = location?.lat?.();
      const longitude = location?.lng?.();

      if (!resolved || !Number.isFinite(latitude) || !Number.isFinite(longitude)) {
        throw new Error('Unable to geocode address');
      }

      address.addressAuto = resolved.formatted_address || address.addressAuto;
      address.latitude = latitude;
      address.longitude = longitude;
      this.populateAddressPartsFromGeocoder(address, resolved.address_components || []);
    },
    async getGoogleGeocoder() {
      if (!window.google?.maps?.Geocoder) {
        throw new Error('Google Maps Geocoder unavailable');
      }
      return new window.google.maps.Geocoder();
    },
    populateAddressPartsFromFormattedAddress(address) {
      if (!address?.addressAuto) {
        return;
      }
      const chunks = `${address.addressAuto}`.split(',').map((chunk) => chunk.trim()).filter(Boolean);
      if (!address.line1) {
        address.line1 = chunks[0] || '';
      }
      if (!address.country) {
        address.country = chunks[chunks.length - 1] || '';
      }
      const zipTownChunk = chunks.find((chunk) => /\b\d{4,5}\b/.test(chunk)) || '';
      const zipMatch = zipTownChunk.match(/\b\d{4,5}\b/);
      if (!address.zipCode && zipMatch) {
        address.zipCode = zipMatch[0];
      }
      if (!address.town && zipTownChunk) {
        address.town = zipTownChunk.replace(/\b\d{4,5}\b/, '').trim();
      }
    },
    populateAddressPartsFromGeocoder(address, components) {
      const byType = (type) => components.find((component) => component.types?.includes(type))?.long_name || '';
      const streetNumber = byType('street_number');
      const route = byType('route');
      address.line1 = [streetNumber, route].filter(Boolean).join(' ').trim() || address.line1 || address.addressAuto || '';
      address.zipCode = byType('postal_code') || address.zipCode || '';
      address.town = byType('locality') || byType('postal_town') || byType('administrative_area_level_2') || address.town || '';
      address.country = byType('country') || address.country || '';
    },
    async submitPackage() {
      const hasConsulted = hasLegalPageBeenConsulted(this.legalFlow);
      if (!hasConsulted || !this.legalConsentAccepted) {
        this.showLegalConsentError = true;
        return;
      }

      this.showLegalConsentError = false;
      const formData = new FormData();
      if (this.isAuthenticated && !this.$store.state.connectedUser?.id) {
        await hydrateConnectedUser();
      }
      if (this.isAuthenticated && !this.$store.state.connectedUser?.id) {
        console.error('Unable to resolve the connected user before package creation.');
        return;
      }
      this.package_.senderID = this.isAuthenticated ? this.$store.state.connectedUser.id : null;
      if (this.requiresGuestCaptcha && !this.captchaToken) {
        this.captchaErrorMessage = this.$i18n.t('captchaRequiredMessage');
        return;
      }
      this.package_.status = 'PAYMENTPENDING';
      this.package_.deliverySpeed = this.wizardOptions.deliverySpeed || 'STANDARD';
      this.package_.insuranceSelected = !!this.wizardOptions.insurance;
      this.package_.declaredValue = this.wizardOptions.insurance ? Number(this.wizardOptions.declaredValue || 0) : null;
      formData.append('packageDTO', JSON.stringify(this.package_));

      this.documentS.forEach((file) => {
        if (file) {
          formData.append('files', file);
        }
      });

      const locale = navigator.languages && navigator.languages.length ? navigator.languages[0] : navigator.language || 'fr-FR';
      formData.append('locale', locale);

      return http
        .post(this.$i18n.t('rootURL') + this.$i18n.t('createPackageUrl'), formData, {
          headers: {
            Accept: 'application/json',
            'Content-Type': 'multipart/form-data',
            ...(this.captchaToken ? { 'X-Captcha-Token': this.captchaToken } : {}),
          },
        })
        .then((response) => {
          this.captchaToken = '';
          this.captchaErrorMessage = '';
          this.$store.commit('updatePackage', response.data);
          if (`${response.status}` === '200') {
            clearLegalDraft(this.legalFlow);
            this.$router.push({
              path: '/paymentPage',
              query: {
                packageId: response.data?.id ?? '',
                reference: response.data?.reference ?? '',
                guestMode: response.data?.guestMode ? 'true' : 'false',
                guestAccessToken: response.data?.guestAccessToken ?? '',
              },
            });
          }
        })
        .catch((error) => {
          this.captchaToken = '';
          this.$refs.packageCaptcha?.resetCaptcha?.();
          if (error?.response?.status === 400) {
            this.captchaErrorMessage = this.$i18n.t('captchaRetryMessage');
          }
          console.error('Unable to process your request this time. Please try again later.');
        });
    },
  },
};
</script>

<style scoped>
.create-package-page {
  padding-bottom: 40px;
}

.guest-entry-card {
  margin-bottom: 32px;
  padding: 32px;
  border-radius: 24px;
}

.guest-entry-actions {
  display: flex;
  align-items: stretch;
  gap: 12px;
}

.guest-entry-btn {
  flex: 1 1 0;
  width: 50%;
  min-width: 0;
  border-radius: 999px !important;
}

.wizard-shell {
  display: grid;
  grid-template-columns: 280px minmax(0, 1fr);
  gap: 24px;
  align-items: start;
}

.wizard-sidebar,
.wizard-card {
  border: 1px solid var(--qd-border);
  border-radius: 24px;
  background: #fff;
  box-shadow: 0 10px 40px rgba(15, 23, 42, 0.06);
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
  min-height: 720px;
  min-width: 0;
  overflow: hidden;
}

.card-header {
  padding: 28px 30px 12px;
}

.card-content {
  flex: 1;
  padding: 14px 30px 30px;
  min-width: 0;
  overflow: hidden;
}

.card-actions {
  display: flex;
  justify-content: space-between;
  align-items: center;
  gap: 12px;
  padding: 18px 30px 28px;
  border-top: 1px solid #e6edf6;
}

.card-actions .btn {
  min-width: 150px;
}

.card-actions .wizard-action-btn {
  display: inline-flex;
  align-items: center;
  justify-content: center;
  width: calc(50% - 6px);
  min-width: 132px;
  max-width: 220px;
  height: 42px;
  padding: 0 18px;
  font-weight: 600;
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
  .create-package-page {
    padding: 12px;
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

  .guest-entry-card {
    padding: 16px 12px;
  }

  .page-header {
    padding: 14px 12px;
  }

  .card-header,
  .card-content,
  .card-actions {
    padding-left: 12px;
    padding-right: 12px;
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

  .guest-entry-actions {
    display: flex;
    flex-direction: row;
    gap: 8px;
  }

  .guest-entry-btn {
    flex: 1 1 0;
    width: 50%;
    min-width: 0;
    border-radius: 999px !important;
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
  }

  .step-copy strong {
    font-size: 0.68rem;
    max-width: 46px;
  }
}
</style>
