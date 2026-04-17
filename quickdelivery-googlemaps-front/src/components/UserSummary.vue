<template>
  <div class="user-summary-step">
    <div class="summary-grid">
      <section class="summary-card">
        <h3>{{ $t('userInfo') }}</h3>
        <div class="summary-list">
          <div><small>{{ $t('userRegistrationAccountType') }}</small><strong>{{ $t(user.type) }}</strong></div>
          <div><small>{{ $t('packageAddressFirstName') }}</small><strong>{{ user.firstName || '--' }}</strong></div>
          <div><small>{{ $t('packageAddressLastName') }}</small><strong>{{ user.lastName || '--' }}</strong></div>
          <div><small>{{ $t('packageAddressEmail') }}</small><strong>{{ user.emailAddress || '--' }}</strong></div>
          <div><small>{{ $t('packageAddressPhone') }}</small><strong>{{ user.phone || '--' }}</strong></div>
        </div>
      </section>

      <section class="summary-card">
        <h3>{{ $t('wizardUserStepAddress') }}</h3>
        <div class="summary-list">
          <div><small>{{ $t('packageAddressAddress') }}</small><strong>{{ user.addressAuto || '--' }}</strong></div>
        </div>
      </section>

      <section class="summary-card">
        <h3>{{ $t('wizardUserStepDocs') }}</h3>
        <div class="summary-list">
          <div v-for="(value, key) in uploadedUserDocuments" :key="key">
            <small>{{ $t(key) }}</small>
            <strong>{{ value?.name || '--' }}</strong>
          </div>
          <div v-if="!hasUserDocuments">
            <small>{{ $t('wizardUserStepDocs') }}</small>
            <strong>--</strong>
          </div>
        </div>
      </section>

      <section class="summary-card">
        <h3>{{ $t('userPaymentModes') }}</h3>
        <div class="summary-list">
          <div>
            <small>{{ $t('userRegistrationPaymentTitle') }}</small>
            <strong>{{ paymentMethodLabel }}</strong>
          </div>
          <div v-if="selectedPaymentType === 'CARD' && user.paymentModes?.CREDIT_CARD?.cardNumber">
            <small>{{ $t('CREDIT_CARD') }}</small>
            <strong>{{ maskedCardNumber }}</strong>
          </div>
          <div v-if="selectedPaymentType === 'IBAN' && user.paymentModes?.IBAN?.iban">
            <small>{{ $t('IBAN') }}</small>
            <strong>{{ maskedIban }}</strong>
          </div>
          <div v-if="selectedPaymentType === 'IBAN' && user.paymentModes?.IBAN?.bic">
            <small>{{ $t('userIBANBIC') }}</small>
            <strong>{{ maskedBic }}</strong>
          </div>
        </div>
      </section>

      <section class="summary-card" v-if="user.type === 'DELIVERY_PERSON'">
        <h3>{{ $t('userVehicle') }}</h3>
        <div class="summary-list">
          <div><small>{{ $t('userVehicleRegistration') }}</small><strong>{{ vehicle.registrationNumber || '--' }}</strong></div>
          <div><small>{{ $t('userVehicleBrand') }}</small><strong>{{ vehicle.brand || '--' }}</strong></div>
          <div><small>{{ $t('userVehicleModel') }}</small><strong>{{ vehicle.model || '--' }}</strong></div>
          <div><small>{{ $t('userVehicleEnergy') }}</small><strong>{{ vehicle.energyType ? $t(vehicle.energyType) : '--' }}</strong></div>
        </div>
      </section>

      <section class="summary-card" v-if="user.type === 'DELIVERY_PERSON'">
        <h3>{{ $t('userDocumentsVehicleSection') }}</h3>
        <div class="summary-list">
          <div v-for="(value, key) in uploadedVehicleDocuments" :key="key">
            <small>{{ $t(key) }}</small>
            <strong>{{ value?.name || '--' }}</strong>
          </div>
          <div v-if="!hasVehicleDocuments">
            <small>{{ $t('userDocumentsVehicleSection') }}</small>
            <strong>--</strong>
          </div>
        </div>
      </section>
    </div>

  </div>
</template>

<script>
import { maskBic, maskCardNumber, maskIban } from '@/config/paymentMask';

export default {
  props: {
    selectedPaymentType: {
      type: String,
      default: 'CARD',
    },
  },
  computed: {
    user() {
      return this.$store.state.user;
    },
    vehicle() {
      return this.$store.state.vehicle;
    },
    userDocuments() {
      return this.$store.state.userDocuments || {};
    },
    vehicleDocuments() {
      return this.$store.state.vehicleDocuments || {};
    },
    uploadedUserDocuments() {
      return this.userDocuments;
    },
    uploadedVehicleDocuments() {
      return this.vehicleDocuments;
    },
    hasUserDocuments() {
      return Object.keys(this.uploadedUserDocuments).length > 0;
    },
    hasVehicleDocuments() {
      return Object.keys(this.uploadedVehicleDocuments).length > 0;
    },
    paymentMethodLabel() {
      if (this.selectedPaymentType === 'IBAN') {
        return this.$i18n.t('userIBAN');
      }
      return this.$i18n.t('userPaymentCreditCard');
    },
    maskedCardNumber() {
      return maskCardNumber(this.user.paymentModes?.CREDIT_CARD?.cardNumber);
    },
    maskedIban() {
      return maskIban(this.user.paymentModes?.IBAN?.iban);
    },
    maskedBic() {
      return maskBic(this.user.paymentModes?.IBAN?.bic);
    },
  },
};
</script>

<style scoped>
.user-summary-step {
  display: grid;
  gap: 18px;
}

.summary-grid {
  display: grid;
  grid-template-columns: repeat(2, minmax(0, 1fr));
  gap: 18px;
}

.summary-card {
  padding: 22px;
  border: 1px solid #dde5f0;
  border-radius: 22px;
  background: #fff;
  box-shadow: 0 18px 38px rgba(24, 39, 75, 0.07);
}

.summary-card h3 {
  margin: 0 0 12px;
  color: #14213d;
}

.summary-list {
  display: grid;
  gap: 12px;
}

.summary-list small {
  color: #617086;
}

.summary-list strong {
  display: block;
  margin-top: 3px;
  color: #14213d;
}

@media screen and (max-width: 820px) {
  .summary-grid {
    grid-template-columns: 1fr;
  }
}
</style>
