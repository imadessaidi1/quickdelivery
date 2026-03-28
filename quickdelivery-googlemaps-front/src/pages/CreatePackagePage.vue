<template>
  <div class="create-package-page">
    <header class="page-header">
      <div>
        <span class="page-chip">{{ $t('menuNewPackage') }}</span>
        <h1>{{ $t('createPackagePageTitle') }}</h1>
        <p>{{ $t('createPackagePageSubtitle') }}</p>
      </div>
    </header>

    <section v-if="showGuestChoice" class="guest-entry-card">
      <span class="page-chip">{{ $t('guestCreatePackageChip') }}</span>
      <h2>{{ $t('guestCreatePackageTitle') }}</h2>
      <p>{{ $t('guestCreatePackageSubtitle') }}</p>
      <div class="guest-entry-actions">
        <router-link class="btn primary_btn guest-entry-btn" to="/register">
          {{ $t('guestCreatePackageRegisterAction') }}
        </router-link>
        <button class="btn primary_btn guest-entry-btn" type="button" @click="continueAsGuest">
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
        </div>

        <footer class="card-actions">
          <button
            v-if="currentStep > 1"
            class="btn primary_btn wizard-action-btn"
            type="button"
            @click="previousStep"
          >
            {{ $t('createPackageBackAction') }}
          </button>

          <button class="btn primary_btn wizard-action-btn" type="submit">
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
      latitude: 0,
      longitude: 0,
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
      latitude: 0,
      longitude: 0,
    },
  ],
  lastPositionLatitude: null,
  lastPositionLongitude: null,
};

export default {
  components: {
    Form,
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
        const residence = Array.isArray(user?.personalAddress) ? user.personalAddress[0] : null;
        console.info('QuickDelivery connected user for package prefill:', user);
        console.info('QuickDelivery residence used for package prefill:', residence);
        if (!residence && !user?.firstName && !user?.lastName && !user?.email) {
          return;
        }
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
          addressAuto: formattedAddress || departureAddress.addressAuto,
          line1: residence?.line1 || departureAddress.line1,
          line2: residence?.line2 || departureAddress.line2,
          town: residence?.town || departureAddress.town,
          zipCode: residence?.zipCode || departureAddress.zipCode,
          country: residence?.country || departureAddress.country,
          latitude: residence?.latitude ?? departureAddress.latitude,
            longitude: residence?.longitude ?? departureAddress.longitude,
        });
      } catch (error) {
        console.error('QuickDelivery failed to hydrate connected user for package prefill:', error);
        // Keep the wizard usable even if profile hydration fails.
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
        this.validateAndStoreAddress(this.$refs.departureAddress, 'DEPARTURE');
        return;
      }

      if (this.currentStep === 2) {
        this.validateAndStoreAddress(this.$refs.arrivalAddress, 'ARRIVAL', true);
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
    validateAndStoreAddress(componentRef, type, validateDeliveryWindow = false) {
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

      const chunks = address.addressAuto.split(',');
      address.line1 = chunks[0]?.trim() || '';
      address.zipCode = chunks[1]?.trim().split(' ')[0] || '';
      const index = chunks[1]?.trim().indexOf(' ');
      address.town = index !== -1 ? chunks[1].trim().substring(index + 1) : '';
      address.country = chunks[2]?.trim() || '';

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
          headers: { acept: 'application/json', 'Content-type': 'multipart/form-data' },
        })
        .then((response) => {
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
        .catch(() => {
          console.error('Unable to process your request this time. Please try again later.');
        });
    },
  },
};
</script>

<style scoped>
.create-package-page {
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

.guest-entry-card {
  margin-bottom: 24px;
  padding: 28px 30px;
  border: 1px solid #dde5f0;
  border-radius: 26px;
  background: rgba(255, 255, 255, 0.92);
  box-shadow: 0 18px 44px rgba(24, 39, 75, 0.08);
}

.guest-entry-card h2 {
  margin: 10px 0 8px;
  color: #14213d;
}

.guest-entry-card p {
  margin: 0;
  color: #617086;
}

.guest-entry-actions {
  display: flex;
  gap: 12px;
  flex-wrap: wrap;
  margin-top: 20px;
}

.guest-entry-btn {
  display: inline-flex;
  align-items: center;
  justify-content: center;
  min-width: 210px;
  height: 42px;
  padding: 0 18px;
  border: none;
  border-radius: 12px;
  background: #020617;
  color: #ffffff;
  font-weight: 600;
  text-decoration: none;
  box-shadow: 0 10px 22px rgba(15, 23, 42, 0.12);
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

.card-content :deep(.legal-consent-card) {
  margin-top: 18px;
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
    padding: 16px;
  }

  .wizard-card {
    min-height: auto;
  }

  .wizard-sidebar {
    gap: 8px;
    padding: 12px;
  }

  .wizard-step {
    min-width: 72px;
    grid-template-columns: 1fr;
    justify-items: center;
    gap: 6px;
    padding: 10px 8px;
    border-radius: 16px;
  }

  .step-index {
    width: 36px;
    height: 36px;
    font-size: 0.95rem;
  }

  .step-copy {
    justify-items: center;
    text-align: center;
  }

  .step-copy strong {
    font-size: 0.72rem;
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
    padding: 22px 18px;
  }

  .page-header {
    padding: 20px 18px;
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

  .card-actions .btn {
    width: 100%;
  }

  .guest-entry-actions {
    flex-direction: column;
  }

  .guest-entry-btn {
    width: 100%;
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
