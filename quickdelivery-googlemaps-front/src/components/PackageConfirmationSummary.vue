<template>
  <div class="confirmation-grid">
    <section class="summary-card">
      <h3>{{ $t('packageConfirmRouteTitle') }}</h3>
      <div class="summary-block">
        <div>
          <small>{{ $t('wizardCreateStepPickup') }}</small>
          <strong>{{ departureAddress?.addressAuto || '--' }}</strong>
        </div>
        <div>
          <small>{{ $t('wizardCreateStepDelivery') }}</small>
          <strong>{{ arrivalAddress?.addressAuto || '--' }}</strong>
        </div>
      </div>
    </section>

    <section class="summary-card">
      <h3>{{ $t('packageConfirmPackageTitle') }}</h3>
      <div class="summary-block compact">
        <div>
          <small>{{ $t('packageSizeSelectorTitle') }}</small>
          <strong>{{ presetLabel }}</strong>
        </div>
        <div>
          <small>{{ $t('packageDimensionsLabel') }}</small>
          <strong>{{ package_.height }} x {{ package_.width }} x {{ package_.depth }} cm</strong>
        </div>
        <div>
          <small>{{ $t('packageWeight') }}</small>
          <strong>{{ package_.weight }} kg</strong>
        </div>
      </div>
    </section>

    <section class="summary-card">
      <h3>{{ $t('packageConfirmOptionsTitle') }}</h3>
      <div class="summary-block compact">
        <div>
          <small>{{ $t('packageOptionsDeliverySpeed') }}</small>
          <strong>{{ speedLabel }}</strong>
        </div>
        <div>
          <small>{{ $t('packageOptionsProtection') }}</small>
          <strong>{{ options.insurance ? $t('packageOptionInsuranceEnabled') : $t('packageOptionInsuranceDisabled') }}</strong>
        </div>
        <div v-if="options.insurance">
          <small>{{ $t('packageDeclaredValueLabel') }}</small>
          <strong>{{ declaredValueLabel }}</strong>
        </div>
      </div>
    </section>

    <section class="summary-card">
      <h3>{{ $t('packageConfirmDocumentsTitle') }}</h3>
      <div class="summary-block compact">
        <div>
          <small>{{ $t('packagePicture') }}</small>
          <strong>{{ documentS?.[0]?.name || $t('packageDocumentMissing') }}</strong>
        </div>
        <div>
          <small>{{ $t('packageInvoice') }}</small>
          <strong>{{ documentS?.[1]?.name || $t('packageDocumentMissing') }}</strong>
        </div>
      </div>
    </section>

    <section class="estimate-card">
      <small>{{ $t('packageConfirmEstimateTitle') }}</small>
      <strong>{{ backendPriceLabel }}</strong>
      <span>{{ backendPriceHint }}</span>
    </section>
  </div>
</template>

<script>
import { getArrivalAddress, getDepartureAddress } from '@/config/comonFunction';

export default {
  props: {
    options: {
      type: Object,
      required: true,
    },
    selectedPreset: {
      type: String,
      default: 'MEDIUM',
    },
  },
  computed: {
    package_() {
      return this.$store.state.package_;
    },
    documentS() {
      return this.$store.state.documentS;
    },
    departureAddress() {
      return getDepartureAddress(this.package_.addresses || []);
    },
    arrivalAddress() {
      return getArrivalAddress(this.package_.addresses || []);
    },
    presetLabel() {
      const map = {
        SMALL: 'packageSizeSmall',
        MEDIUM: 'packageSizeMedium',
        LARGE: 'packageSizeLarge',
        EXTRA_LARGE: 'packageSizeExtraLarge',
      };
      return this.$t(map[this.selectedPreset] || map.MEDIUM);
    },
    speedLabel() {
      const map = {
        STANDARD: 'packageOptionStandard',
        EXPRESS: 'packageOptionExpress',
        SAMEDAY: 'packageOptionSameDay',
      };
      return this.$t(map[this.options.deliverySpeed] || map.STANDARD);
    },
    backendPriceLabel() {
      if (this.package_.deliveryPrice == null) {
        return '--';
      }
      return `${Number(this.package_.deliveryPrice).toFixed(2)} ${this.$t('currency')}`;
    },
    backendPriceHint() {
      return this.package_.deliveryPrice == null
        ? this.$t('packageEstimateCalculatedOnPayment')
        : this.$t('packageEstimateBackendConfirmed');
    },
    declaredValueLabel() {
      if (this.options.declaredValue == null || this.options.declaredValue === '') {
        return '--';
      }
      return `${Number(this.options.declaredValue).toFixed(2)} ${this.$t('currency')}`;
    },
  },
};
</script>

<style scoped>
.confirmation-grid {
  display: grid;
  grid-template-columns: repeat(2, minmax(0, 1fr));
  gap: 18px;
  min-width: 0;
}

.confirmation-grid,
.confirmation-grid * {
  box-sizing: border-box;
}

.summary-card,
.estimate-card {
  padding: 22px;
  border: 1px solid #dde5f0;
  border-radius: 22px;
  background: #fff;
  box-shadow: 0 18px 38px rgba(24, 39, 75, 0.07);
}

.summary-card h3 {
  margin: 0 0 14px;
  color: #14213d;
}

.summary-block {
  display: grid;
  gap: 14px;
}

.summary-block.compact {
  grid-template-columns: repeat(2, minmax(0, 1fr));
}

.summary-block small,
.estimate-card small,
.estimate-card span {
  color: #617086;
}

.summary-block strong,
.estimate-card strong {
  display: block;
  margin-top: 4px;
  color: #14213d;
  font-size: 1rem;
}

.estimate-card {
  display: grid;
  align-content: center;
  justify-items: start;
  background: linear-gradient(135deg, #153e75 0%, #27548a 100%);
}

.estimate-card small,
.estimate-card span,
.estimate-card strong {
  color: #fff;
}

.estimate-card strong {
  font-size: 2rem;
}

@media screen and (max-width: 820px) {
  .confirmation-grid,
  .summary-block.compact {
    grid-template-columns: 1fr;
  }
}
</style>
