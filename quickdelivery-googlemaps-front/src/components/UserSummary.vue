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
          <div v-if="!Object.keys(uploadedUserDocuments).length">
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
            <strong>{{ user.paymentModes.CREDIT_CARD.cardNumber }}</strong>
          </div>
          <div v-if="selectedPaymentType === 'IBAN' && user.paymentModes?.IBAN?.iban">
            <small>{{ $t('IBAN') }}</small>
            <strong>{{ user.paymentModes.IBAN.iban }}</strong>
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
          <div v-if="!Object.keys(uploadedVehicleDocuments).length">
            <small>{{ $t('userDocumentsVehicleSection') }}</small>
            <strong>--</strong>
          </div>
        </div>
      </section>
    </div>

    <div class="conditionCheckbox">
      <Field name="userCondition" type="checkbox" value="consent" />
      <span>{{ $t('packageCreationAgreement') }}</span>
      <ErrorMessage name="userCondition" />
    </div>
  </div>
</template>

<script>
import { ErrorMessage, Field } from 'vee-validate';

export default {
  components: {
    Field,
    ErrorMessage,
  },
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
      return Object.fromEntries(Object.entries(this.userDocuments).filter(([, value]) => value?.name));
    },
    uploadedVehicleDocuments() {
      return Object.fromEntries(Object.entries(this.vehicleDocuments).filter(([, value]) => value?.name));
    },
    paymentMethodLabel() {
      if (this.selectedPaymentType === 'IBAN') {
        return this.$t('userIBAN');
      }
      if (this.selectedPaymentType === 'PAYPAL') {
        return this.$t('userPayPal');
      }
      return this.$t('userPaymentCreditCard');
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

.conditionCheckbox {
  display: flex;
  align-items: center;
  gap: 10px;
  padding: 0 4px;
}

.conditionCheckbox span {
  color: #617086;
  font-size: 0.9rem;
}

@media screen and (max-width: 820px) {
  .summary-grid {
    grid-template-columns: 1fr;
  }
}
</style>
