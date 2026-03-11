<template>
  <div class="marker-details" :class="{ selected: isSelected, compact: compact }">
    <template v-if="compact">
      <div class="compact-head">
        <div class="compact-ref">#{{ package_.reference }}</div>
        <div class="compact-price">{{ package_.deliveryPrice }}{{ $t('currency') }}</div>
      </div>
      <div class="compact-route">{{ departureAddress() }} -> {{ destinationAddress() }}</div>
      <div class="actions-row">
        <button class="details-btn" ref="reserveButtons" @click.stop="details">
          {{ $t('packagesArroundMArkerDetailActionsDetails') }}
        </button>
        <button v-if="canReserve()" class="reserve-btn" ref="detailsButtons" @click.stop="reserve">
          {{ $t('packagesArroundMArkerDetailActionsReserve') }}
        </button>
      </div>
    </template>
    <template v-else>
    <div class="card-header">
      <div class="ref-block">
        <div class="ref-wrap">
          <span class="material-symbols-outlined ref-icon">deployed_code</span>
          <span class="ref-text">#{{ package_.reference }}</span>
        </div>
        <span class="status-chip">{{ $t('statusAvailable') }}</span>
      </div>
      <div class="price-wrap">{{ package_.deliveryPrice }}{{ $t('currency') }}</div>
    </div>

    <div class="address-line">
      <span class="material-symbols-outlined address-icon pickup">trip_origin</span>
      <div>
        <div class="label">{{ $t('packageDeparture') }}</div>
        <div class="value">{{ departureAddress() }}</div>
      </div>
    </div>

    <div class="address-line">
      <span class="material-symbols-outlined address-icon dropoff">location_on</span>
      <div>
        <div class="label">{{ $t('packageDestination') }}</div>
        <div class="value">{{ destinationAddress() }}</div>
      </div>
    </div>

    <div class="metrics-row">
      <div class="metric">
        <div class="metric-label">{{ $t('packageWeight') }}</div>
        <div class="metric-value">{{ package_.weight || '-' }}</div>
      </div>
      <div class="metric">
        <div class="metric-label">{{ $t('packageDimensions') }}</div>
        <div class="metric-value">{{ packageDimensions() }}</div>
      </div>
      <div class="metric">
        <div class="metric-label">{{ $t('packageDistanceToDestination') }}</div>
        <div class="metric-value">{{ package_.distanceToDestination || package_.fromYou || '-' }}</div>
      </div>
    </div>

    <div class="actions-row">
      <button class="details-btn" ref="reserveButtons" @click.stop="details">
        {{ $t('packagesArroundMArkerDetailActionsDetails') }}
      </button>
      <button v-if="canReserve()" class="reserve-btn" ref="detailsButtons" @click.stop="reserve">
        {{ $t('packagesArroundMArkerDetailActionsReserve') }}
      </button>
    </div>
    </template>
  </div>
</template>

<script>
import http from '@/config/httpInterceptor';
import { getCurrentUserRoles } from '@/config/auth';

export default {
  props: {
    package_: Object,
    mapVue: Object,
    modal: Object,
    isSelected: {
      type: Boolean,
      default: false,
    },
    compact: {
      type: Boolean,
      default: false,
    },
  },
  methods: {
    canReserve() {
      const roles = getCurrentUserRoles();
      return roles.includes('ROLE_LIVREUR') || roles.includes('ROLE_ADMIN');
    },
    async reserve() {
      const userLanguage = navigator.languages && navigator.languages.length ? navigator.languages[0] : navigator.language || 'fr-FR';
      const url = this.$i18n.t('rootURL') + this.$i18n.t('reservePackageUrl') + 'packageID=' + this.package_.id + '&deliveryPersonID=' + this.$store.state.connectedUser.id + '&locale=' + userLanguage;
      window.top.postMessage('RefreshPackagesList ' + this.package_.id, '*');
      return new Promise((resolve, reject) => {
        http.put(url)
          .then((response) => {
            resolve(response.data);
          })
          .catch((error) => {
            console.error('Unable to process your request at this time. Please try again later.', error);
            reject(error);
          });
      });
    },
    details() {
      this.$store.commit('updatePackage', this.package_);
      if (this.modal && this.modal.openModal) {
        this.modal.openModal();
        return;
      }
      this.$router.push({
        path: '/package',
        query: {
          id: this.package_.reference,
          returnTo: this.$route.fullPath,
        },
      });
    },
    destinationAddress() {
      let destinationAddressS = '';
      this.package_.addresses.forEach((address) => {
        if (address.type === 'ARRIVAL') {
          destinationAddressS = address.addressAuto;
        }
      });
      return destinationAddressS;
    },
    departureAddress() {
      let destinationAddressS = '';
      this.package_.addresses.forEach((address) => {
        if (address.type === 'DEPARTURE') {
          destinationAddressS = address.addressAuto;
        }
      });
      return destinationAddressS;
    },
    packageDimensions() {
      const width = this.package_.width || '-';
      const height = this.package_.height || '-';
      const depth = this.package_.dept || this.package_.depth || '-';
      return `${width}x${height}x${depth}`;
    },
    setFocusOnReserveButton() {
      const reserveButton = this.$refs.reserveButtons;
      if (reserveButton) {
        reserveButton.focus();
      }
    },
  },
};
</script>

<style>
.marker-details {
  padding: 12px;
  border: 1px solid #e6e9f0;
  border-radius: 14px;
  background: #ffffff;
  box-shadow: 0 8px 20px rgba(15, 23, 42, 0.06);
  cursor: pointer;
  transition: border-color 0.2s ease, box-shadow 0.2s ease;
}

.marker-details.selected {
  border-color: #93a7cf;
  box-shadow: 0 0 0 1px #93a7cf, 0 10px 24px rgba(15, 23, 42, 0.08);
}

.marker-details.compact {
  padding: 10px;
}

.compact-head {
  display: flex;
  justify-content: space-between;
  align-items: center;
  gap: 10px;
  margin-bottom: 6px;
}

.compact-ref {
  font-size: 14px;
  font-weight: 700;
  color: #0f172a;
  overflow: hidden;
  text-overflow: ellipsis;
  white-space: nowrap;
}

.compact-price {
  font-size: 18px;
  font-weight: 700;
  color: #0f172a;
  white-space: nowrap;
}

.compact-route {
  font-size: 12px;
  color: #475569;
  margin-bottom: 10px;
  line-height: 1.35;
  display: -webkit-box;
  -webkit-line-clamp: 2;
  -webkit-box-orient: vertical;
  overflow: hidden;
}

.card-header {
  display: flex;
  justify-content: space-between;
  align-items: flex-start;
  gap: 10px;
  margin-bottom: 8px;
}

.ref-block {
  flex: 1 1 auto;
  min-width: 0;
}

.ref-wrap {
  display: flex;
  align-items: center;
  gap: 8px;
  width: 100%;
  min-width: 0;
}

.ref-icon {
  font-size: 18px;
  color: #4b5563;
}

.ref-text {
  font-size: 16px;
  font-weight: 700;
  color: #0f172a;
  display: block;
  min-width: 0;
  overflow: hidden;
  text-overflow: ellipsis;
  white-space: nowrap;
}

.status-chip {
  display: inline-flex;
  align-items: center;
  padding: 2px 8px;
  border-radius: 999px;
  font-size: 11px;
  color: #166534;
  background: #dcfce7;
  border: 1px solid #bbf7d0;
  margin-top: 5px;
}

.price-wrap {
  font-size: 20px;
  font-weight: 700;
  color: #111827;
  white-space: nowrap;
  flex: 0 0 auto;
}

.address-line {
  display: flex;
  gap: 8px;
  align-items: flex-start;
  margin-bottom: 6px;
}

.address-icon {
  font-size: 16px;
  padding-top: 2px;
}

.address-icon.pickup {
  color: #65a30d;
}

.address-icon.dropoff {
  color: #3b82f6;
}

.label {
  font-size: 11px;
  color: #6b7280;
  margin-bottom: 1px;
}

.value {
  font-size: 13px;
  color: #1f2937;
  line-height: 1.3;
}

.metrics-row {
  display: grid;
  grid-template-columns: repeat(3, minmax(0, 1fr));
  gap: 8px;
  margin: 10px 0;
  padding-top: 8px;
  border-top: 1px solid #eef2f7;
}

.metric-label {
  font-size: 11px;
  color: #6b7280;
}

.metric-value {
  margin-top: 2px;
  font-size: 12px;
  font-weight: 700;
  color: #111827;
}

.actions-row {
  display: flex;
  gap: 8px;
}

.details-btn,
.reserve-btn {
  flex: 1;
  height: 36px;
  border-radius: 10px;
  border: none;
  font-size: 13px;
  font-weight: 600;
  cursor: pointer;
}

.details-btn {
  color: #ffffff;
  background: #020617;
}

.details-btn:hover {
  background: #0f172a;
}

.reserve-btn {
  color: #0f172a;
  background: #e2e8f0;
}

.reserve-btn:hover {
  background: #cfd8e5;
}

@media screen and (max-width: 767px) {
  .card-header {
    flex-wrap: wrap;
    gap: 6px;
  }
  .price-wrap {
    font-size: 18px;
  }

  .actions-row {
    flex-direction: column;
  }
}
</style>
