<template>
  <div class="marker-details" :class="{ selected: isSelected, compact: compact, dense: dense, locked: package_.isSoftLockedBy }">
    <template v-if="compact">
      <div class="compact-head">
        <div class="compact-ref">#{{ package_.reference }}</div>
        <div class="compact-price">{{ displayedPrice() }}</div>
      </div>
      <div v-if="hasRouteSequence()" class="tour-sequence compact-sequence">
        <span v-if="package_.routePickupOrder" class="tour-chip pickup">C{{ package_.routePickupOrder }}</span>
        <span v-if="package_.routeDropoffOrder" class="tour-chip dropoff">L{{ package_.routeDropoffOrder }}</span>
      </div>
      <div class="compact-route">{{ departureAddress() }} -> {{ destinationAddress() }}</div>
      <div v-if="package_.isSoftLockedBy" class="lock-banner">
        <span class="material-symbols-outlined">lock</span> {{ $t('lockedByOther') }}
      </div>
      <div class="actions-row">
        <button class="qd-btn-primary" ref="reserveButtons" @click.stop="details">
          {{ $t('packagesArroundMArkerDetailActionsDetails') }}
        </button>
        <button
          v-if="canOperateDelivery() && package_.status === 'NEW' && !package_.isSoftLockedBy"
          class="qd-btn-secondary"
          :disabled="isReserveDisabled"
          :title="reservationDisabledReason"
          ref="detailsButtons"
          @click.stop="reserve"
        >
          {{ $t('packagesArroundMArkerDetailActionsReserve') }}
        </button>
        <button
          v-if="showCancelReservation"
          class="qd-btn-danger"
          type="button"
          @click.stop="cancelReservation"
        >
          {{ $t('actionCancelReservation') }}
        </button>
      </div>
      <div v-if="showReservationBlockedHint" class="reservation-hint">{{ reservationDisabledReason }}</div>
    </template>
    <template v-else>
    <div class="card-header">
      <div class="ref-block">
        <div class="ref-wrap">
          <span class="material-symbols-outlined ref-icon">deployed_code</span>
          <span class="ref-text">#{{ package_.reference }}</span>
        </div>
        <span v-if="package_.isSoftLockedBy" class="status-chip locked">{{ $t('statusLocked') }}</span>
        <span v-else class="status-chip">{{ $t('statusAvailable') }}</span>
      </div>
      <div class="price-wrap">{{ displayedPrice() }}</div>
    </div>

    <div v-if="package_.packageSizeCategory" class="smart-badges-row">
      <span v-if="package_.packageSizeCategory" class="smart-flag neutral">{{ package_.packageSizeCategory }}</span>
      <span v-if="package_.routePickupOrder" class="smart-flag pickup">Collecte {{ package_.routePickupOrder }}</span>
      <span v-if="package_.routeDropoffOrder" class="smart-flag dropoff">Livraison {{ package_.routeDropoffOrder }}</span>
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
      <div v-if="package_.detourMeters" class="metric detour">
        <div class="metric-label">{{ $t('packageDetour') }}</div>
        <div class="metric-value highlight">{{ formatDetour(package_.detourMeters) }}</div>
      </div>
      <div v-else class="metric">
        <div class="metric-label">{{ $t('packageDistanceToDestination') }}</div>
        <div class="metric-value">{{ package_.distanceToDestination || package_.fromYou || '-' }}</div>
      </div>
      <div class="metric">
        <div class="metric-label">{{ $t('packageWeight') }}</div>
        <div class="metric-value">{{ package_.weight || '-' }} kg</div>
      </div>
      <div class="metric">
        <div class="metric-label">{{ $t('packageDimensions') }}</div>
        <div class="metric-value">{{ packageDimensions() }}</div>
      </div>
    </div>

    <div class="actions-row">
      <button class="qd-btn-primary" ref="reserveButtons" @click.stop="details">
        {{ $t('packagesArroundMArkerDetailActionsDetails') }}
      </button>
      <button
        v-if="canOperateDelivery() && package_.status === 'NEW' && !package_.isSoftLockedBy"
        class="qd-btn-secondary"
        :disabled="isReserveDisabled"
        :title="reservationDisabledReason"
        ref="detailsButtons"
        @click.stop="reserve"
      >
        {{ $t('packagesArroundMArkerDetailActionsReserve') }}
      </button>
      <button
        v-if="showCancelReservation"
        class="qd-btn-danger"
        type="button"
        @click.stop="cancelReservation"
      >
        {{ $t('actionCancelReservation') }}
      </button>
    </div>
    <div v-if="showReservationBlockedHint" class="reservation-hint">{{ reservationDisabledReason }}</div>
    </template>
  </div>
</template>

<script>
import http from '@/config/httpInterceptor';
import { getCurrentUserRoles } from '@/config/auth';
import { formatDisplayedPackageAmount } from '@/config/packagePricing';
import { isPackageReservedByDeliveryPerson } from '@/config/packageReservations';

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
    dense: {
      type: Boolean,
      default: false,
    },
  },
  computed: {
    reservationAvailability() {
      return this.$store.state.reservationAvailability || {};
    },
    isReserveDisabled() {
      return !this.reservationAvailability.canReserve
        || this.reservationAvailability.activeRouteBlocking
        || this.$store.state.isUserWithOngoingDelivery
        || Boolean(this.$store.state.activeDeliveryRoute);
    },
    showReservationBlockedHint() {
      return this.canOperateDelivery() && !this.package_?.isSoftLockedBy && this.isReserveDisabled;
    },
    showCancelReservation() {
      return this.canOperateDelivery()
        && isPackageReservedByDeliveryPerson(this.package_, this.$store.state.connectedUser?.id);
    },
    canCancelReservation() {
      return this.showCancelReservation;
    },
    reservationDisabledReason() {
      if (this.reservationAvailability.activeRouteBlocking || this.$store.state.isUserWithOngoingDelivery) {
        return this.$t('reservationBlockedActiveRoute');
      }
      if (this.reservationAvailability.capacityReached) {
        const maxReservations = Number(this.reservationAvailability.maxReservations || 0);
        const activeReservations = Number(this.reservationAvailability.activeReservations || 0);
        const capacityLabel = maxReservations > 0
          ? ` (${this.$t('reservationCapacityStatus', { active: activeReservations, max: maxReservations })})`
          : '';
        return `${this.$t('reservationBlockedCapacityReached')}${capacityLabel}`;
      }
      if (!this.reservationAvailability.canReserve) {
        return this.$t('reservationBlockedGeneric');
      }
      return '';
    },
  },
  methods: {
    displayedPrice() {
      return formatDisplayedPackageAmount(this.$i18n, this.package_);
    },
    hasRouteSequence() {
      return Number.isFinite(this.package_?.routePickupOrder) || Number.isFinite(this.package_?.routeDropoffOrder);
    },
    canOperateDelivery() {
      const roles = getCurrentUserRoles();
      return roles.includes('ROLE_LIVREUR') || roles.includes('ROLE_ADMIN');
    },
    formatDetour(meters) {
      if (!meters) return '-';
      if (meters < 1000) return Math.round(meters) + ' m';
      return (meters / 1000).toFixed(1) + ' km';
    },
    async reserve() {
      if (this.isReserveDisabled) {
        return Promise.reject(new Error('Active delivery route blocks new reservations'));
      }
      const userLanguage = navigator.languages && navigator.languages.length ? navigator.languages[0] : navigator.language || 'fr-FR';
      const url = this.$i18n.t('rootURL') + this.$i18n.t('reservePackageUrl') + 'packageID=' + this.package_.id + '&deliveryPersonID=' + this.$store.state.connectedUser.id + '&locale=' + userLanguage;
      window.top.postMessage('RefreshPackagesList ' + this.package_.id, '*');
      return new Promise((resolve, reject) => {
        http.put(url)
          .then((response) => {
            window.dispatchEvent(new CustomEvent('qd-refresh-reservation-availability'));
            resolve(response.data);
          })
          .catch((error) => {
            console.error('Unable to process your request at this time. Please try again later.', error);
            reject(error);
          });
      });
    },
    async cancelReservation() {
      if (!this.canCancelReservation || !window.confirm(this.$t('reservationCancelConfirm'))) {
        return;
      }
      const url = `${this.$i18n.t('rootURL')}${this.$i18n.t('cancelReservationUrl')}packageID=${encodeURIComponent(this.package_.id)}&deliveryPersonID=${encodeURIComponent(this.$store.state.connectedUser.id)}`;
      try {
        await http.put(url);
        window.dispatchEvent(new CustomEvent('qd-refresh-reservation-availability'));
        window.top.postMessage(`RefreshPackagesList ${this.package_.id}`, '*');
      } catch (error) {
        console.error('Unable to cancel reservation.', error);
      }
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
  width: 100%;
  max-width: 100%;
  min-width: 0;
  box-sizing: border-box;
  overflow: hidden;
  position: relative;
}

.marker-details.locked {
  opacity: 0.85;
  background: #fffafa;
}

.smart-badges-row {
  display: flex;
  flex-wrap: wrap;
  gap: 6px;
  margin: 0 0 10px;
}

.smart-flag {
  display: inline-flex;
  align-items: center;
  min-height: 24px;
  padding: 0 8px;
  border-radius: 999px;
  font-size: 11px;
  font-weight: 700;
}

.smart-flag.neutral {
  background: #e2e8f0;
  color: #334155;
}

.smart-flag.pickup {
  background: #dbeafe;
  color: #1d4ed8;
}

.smart-flag.dropoff {
  background: #dcfce7;
  color: #15803d;
}

.tour-sequence {
  display: flex;
  gap: 6px;
  margin-bottom: 8px;
}

.compact-sequence {
  margin-bottom: 6px;
}

.tour-chip {
  display: inline-flex;
  align-items: center;
  justify-content: center;
  min-width: 34px;
  height: 22px;
  padding: 0 8px;
  border-radius: 999px;
  font-size: 11px;
  font-weight: 700;
}

.tour-chip.pickup {
  background: #dbeafe;
  color: #1d4ed8;
}

.tour-chip.dropoff {
  background: #dcfce7;
  color: #15803d;
}

.lock-banner {
  display: flex;
  align-items: center;
  gap: 5px;
  background: #fee2e2;
  color: #991b1b;
  padding: 4px 8px;
  border-radius: 6px;
  font-size: 11px;
  font-weight: 600;
  margin-bottom: 8px;
}

.marker-details.selected {
  border-color: #93a7cf;
  box-shadow: 0 0 0 1px #93a7cf, 0 10px 24px rgba(15, 23, 42, 0.08);
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

.status-chip.locked {
  color: #991b1b;
  background: #fee2e2;
  border-color: #fecaca;
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
  overflow-wrap: anywhere;
}

.metrics-row {
  display: grid;
  grid-template-columns: repeat(2, 1fr);
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

.metric-value.highlight {
  color: #059669;
}

.actions-row {
  display: grid;
  grid-template-columns: repeat(2, minmax(0, 1fr));
  gap: 8px;
  margin-top: 4px;
}

.actions-row > button {
  width: 100%;
  min-width: 0;
}

.actions-row > button:only-child,
.actions-row > .qd-btn-danger {
  grid-column: 1 / -1;
}

.reservation-hint {
  margin-top: 8px;
  font-size: 11px;
  line-height: 1.4;
  color: #b45309;
}

/* .details-btn, .reserve-btn, .cancel-reserve-btn handled by design-system.css qd-btn-* classes */
.qd-btn-primary, .qd-btn-secondary, .qd-btn-danger {
  height: 40px !important; /* Slightly more compact for carousel cards */
  font-size: 0.85rem !important;
}
.reserve-btn:disabled {
  background: #e5e7eb;
  color: #94a3b8;
  cursor: not-allowed;
}

@media screen and (max-width: 767px) {
  .metrics-row {
    grid-template-columns: repeat(2, 1fr);
  }
}
</style>
