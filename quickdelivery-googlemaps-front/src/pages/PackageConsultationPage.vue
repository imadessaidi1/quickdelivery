<template>
    <div class="package-details-page-v3 qd-page">
        <!-- Dashboard Header -->
        <header class="qd-page-header">
            <div class="header-main">
                <button class="back-link qd-btn-secondary" type="button" @click="goBack" style="margin-bottom: 12px;">
                    <span class="icon">←</span> {{ $t('actionBack') }}
                </button>
                <div class="header-titles">
                    <h1>{{ $t('packagesArroundMArkerDetailActionsDetails') }}</h1>
                    <div class="ref-badge" style="display: inline-flex; align-items: center; gap: 6px; background: var(--qd-primary-soft); color: var(--qd-primary-dark); padding: 4px 12px; border-radius: 10px; font-weight: 700; font-size: 0.9rem; margin-top: 8px;">
                        <span class="material-symbols-outlined" style="font-size: 1.1rem;">tag</span>
                        <span>{{ package_.reference || id }}</span>
                    </div>
                </div>
            </div>
            
            <div class="qd-page-header-actions">
                <div v-if="package_.status" :class="['status-badge-lg', package_.status.toLowerCase()]">
                    <span class="pulse-dot-small" v-if="package_.status === 'NEW'"></span>
                    {{ $t(`packageStatus_${package_.status}`) || package_.status }}
                </div>
                <button
                    class="qd-btn-danger btn-cancel-header"
                    type="button"
                    v-show="showCancelReservation"
                    @click="cancelReservation"
                >
                    <span class="material-symbols-outlined">cancel</span>
                    {{ $t('actionCancelReservation') }}
                </button>
            </div>
        </header>

        <div v-if="isLoadingPage" class="page-loader">
            <div class="spinner-large"></div>
            <p>{{ $t('stateLoading') }}</p>
        </div>
        
        <div v-else-if="loadError" class="page-error-state">
            <span class="material-symbols-outlined large-icon">error_outline</span>
            <h2>{{ $t('stateLoadError') }}</h2>
            <button class="qd-btn-primary" @click="mounted">{{ $t('actionRetry') }}</button>
        </div>

        <div v-else class="dashboard-content dashboard-cards-grid">
            <!-- Information Col -->
            <div class="info-column">
                <div class="content-card main-details-card animate-card">
                    <PackageSummary/>
                </div>
            </div>

            <!-- Page Actions Floating Card -->
            <div class="action-dock">
                <div class="action-card-premium glass-pane">
                    <div class="actions-container">
                        <!-- New Package: Reserve -->
                        <div v-if="canOperateDelivery && package_.status === 'NEW'" class="reserve-action-block">
                            <button 
                                class="qd-btn-primary"
                                :disabled="isReserveDisabled"
                                @click="reserve"
                                style="width: 100%; height: 54px;"
                            >
                                <span class="material-symbols-outlined">add_circle</span>
                                {{ $t('packagesArroundMArkerDetailActionsReserve') }}
                            </button>
                            <div v-if="isReserveDisabled" class="inline-alert-mini">
                                <span class="material-symbols-outlined">info</span>
                                <span>{{ reservationDisabledReason }}</span>
                            </div>
                        </div>

                        <!-- Reserved: Pickup -->
                        <div v-if="canOperateDelivery && package_.status === 'RESERVED'" class="auth-action-column">
                            <div class="auth-action-row">
                                <div class="otp-input-premium">
                                    <span class="material-symbols-outlined icon">lock</span>
                                    <Field 
                                        id="otp" 
                                        type="tel" 
                                        inputmode="numeric"
                                        v-model="otp" 
                                        name="otp" 
                                        :rules="validateNumericField" 
                                        :placeholder="$t('packagePickupPassword')"
                                        @keypress="$event.key >= '0' && $event.key <= '9' ? true : $event.preventDefault()"
                                        @input="otp = (otp || '').toString().replace(/\D/g, '')"
                                    />
                                </div>
                                <button class="qd-btn-primary" @click="pickup" style="height: 54px; min-width: 160px;">
                                    <span class="material-symbols-outlined">local_shipping</span>
                                    {{ $t('packagesArroundMArkerDetailActionsPickUp') }}
                                </button>
                            </div>
                            <div class="secondary-actions-row" v-if="departureAddress.phone">
                                <a :href="'tel:' + departureAddress.phone" class="qd-btn-secondary">
                                    <span class="material-symbols-outlined">call</span>
                                    {{ $t('actionCallSender') }}
                                </a>
                            </div>
                            <ErrorMessage class="errorMessage-dock" name="otp" />
                        </div>

                        <!-- Picked Up: Deliver -->
                        <div v-if="canOperateDelivery && package_.status === 'PICKEDUP'" class="auth-action-column">
                            <div class="auth-action-row">
                                <div class="otp-input-premium">
                                    <span class="material-symbols-outlined icon">verified_user</span>
                                    <Field 
                                        id="deliveryOtp" 
                                        type="tel" 
                                        inputmode="numeric"
                                        v-model="deliveryOtp" 
                                        name="deliveryOtp" 
                                        :rules="validateNumericField" 
                                        :placeholder="$t('packageDeliveryPassword')"
                                        @keypress="$event.key >= '0' && $event.key <= '9' ? true : $event.preventDefault()"
                                        @input="deliveryOtp = (deliveryOtp || '').toString().replace(/\D/g, '')"
                                    />
                                </div>
                                <button class="qd-btn-primary" @click="deliver" style="height: 54px; min-width: 160px;">
                                    <span class="material-symbols-outlined">task_alt</span>
                                    {{ $t('packagesArroundMArkerDetailActionsDeliver') }}
                                </button>
                            </div>
                            <div class="secondary-actions-row" v-if="arrivalAddress.phone">
                                <a :href="'tel:' + arrivalAddress.phone" class="qd-btn-secondary">
                                    <span class="material-symbols-outlined">call</span>
                                    {{ $t('actionCallRecipient') }}
                                </a>
                            </div>
                            <ErrorMessage class="errorMessage-dock" name="deliveryOtp" />
                        </div>
                    </div>
                </div>
            </div>
        </div>
    </div>
</template>
<script>
import PackageSummary from '../components/PackageDetails.vue';
import { validateNumericField } from '@/config/comonFunction';
import http from '@/config/httpInterceptor';
import { Field, ErrorMessage } from 'vee-validate';
import { getCurrentUserRoles } from '@/config/auth';
import { isPackageReservedByDeliveryPerson } from '@/config/packageReservations';

export default{
  computed: {
        package_() {
          return this.$store.state.package_;
        },
        canOperateDelivery() {
          const roles = getCurrentUserRoles();
          return roles.includes('ROLE_LIVREUR') || roles.includes('ROLE_ADMIN');
        },
        reservationAvailability() {
          return this.$store.state.reservationAvailability || {};
        },
        isReserveDisabled() {
          return !this.reservationAvailability.canReserve
            || this.reservationAvailability.activeRouteBlocking
            || this.$store.state.isUserWithOngoingDelivery
            || Boolean(this.$store.state.activeDeliveryRoute);
        },
        reservationDisabledReason() {
          if (this.reservationAvailability.activeRouteBlocking || this.$store.state.isUserWithOngoingDelivery) {
            return this.$t('reservationBlockedActiveRoute');
          }
          if (this.reservationAvailability.capacityReached) {
            return this.$t('reservationBlockedCapacityReached');
          }
          if (!this.reservationAvailability.canReserve) {
            return this.$t('reservationBlockedGeneric');
          }
          return '';
        },
        showCancelReservation() {
          return this.canOperateDelivery
            && isPackageReservedByDeliveryPerson(this.package_, this.$store.state.connectedUser?.id);
        },
        canCancelReservation() {
          return this.showCancelReservation;
        },
        departureAddress() {
          return this.package_?.addresses?.find(a => a.type === 'DEPARTURE') || {};
        },
        arrivalAddress() {
          return this.package_?.addresses?.find(a => a.type === 'ARRIVAL') || {};
        },
  },
  components: {
        PackageSummary,
        Field,
        ErrorMessage,
    },
  props: {
    id: String,
    returnTo: String,
  },
  data() {
    return {
      otp: '',
      deliveryOtp: '',
      isLoadingPage: false,
      loadError: false,
      package: {
        id: null,
        version: null,
        creationDate: null,
        reference: "",
        height: 0,
        width: 0,
        depth: 0,
        weight: 0,
        pictureURL: "",
        status: "",
        deliveryPrice: null,
        senderID: null,
        packageReservations: [],
        addresses: [{
        firstName: "",
        lastName: "",
        line1: "",
        line2: "",
        town: "",
        zipCode: "",
        country: "",
        floor:0,
        dateTime: null,
        email: "",
        phone: "",
        type: "DEPARTURE",
        latitude: 0,
        longitude: 0,
      },
      {
        firstName: "",
        lastName: "",
        line1: "",
        line2: "",
        town: "",
        zipCode: "",
        country: "",
        floor:0,
        dateTime: null,
        email: "",
        phone: "",
        type: "ARRIVAL",
        latitude: 0,
        longitude: 0,
      }],
      lastPositionLatitude: null,
      lastPositionLongitude: null
    }
    };
  },
  mounted() {
    if (!this.id) {
      console.warn('Missing package reference in route query parameter "id".');
      return;
    }
    this.isLoadingPage = true;
    this.loadError = false;
    http.get(this.$i18n.t('rootURL') + this.$i18n.t('getPackage')+this.id)
      .then(response => {
        this.$store.commit('updatePackage', response.data);
    }).catch(() => {
      this.loadError = true;
      console.error("Unable to process your request this time. Please try again later.");
    }).finally(() => {
      this.isLoadingPage = false;
    });
  },
  methods: {
    validateNumericField,
    async getCurrentLocationForStopValidation() {
      return new Promise((resolve, reject) => {
        if (!navigator.geolocation) {
          reject(new Error('Geolocation not supported'));
          return;
        }
        navigator.geolocation.getCurrentPosition(
          (position) => resolve({
            latitude: position.coords.latitude,
            longitude: position.coords.longitude,
          }),
          (error) => reject(error),
          {
            enableHighAccuracy: true,
            maximumAge: 10000,
            timeout: 10000,
          },
        );
      });
    },
    goBack() {
      if (window.history.length > 1) {
        this.$router.back();
        return;
      }
      if (this.returnTo) {
        this.$router.push(this.returnTo);
        return;
      }
      this.$router.push('/');
    },
    reserve() {
      if (this.isReserveDisabled) {
        return Promise.resolve();
      }
      if (!this.$store.state.connectedUser?.id) {
        console.error('Missing connected user identifier for reservation.');
        return Promise.resolve();
      }
      const userLanguage = navigator.languages && navigator.languages.length ? navigator.languages[0] : navigator.language || 'fr-FR';
      const url = this.$i18n.t('rootURL') + this.$i18n.t('reservePackageUrl') + "packageID=" + this.package_.id + "&deliveryPersonID=" + this.$store.state.connectedUser.id+"&locale="+userLanguage;
      return http.put(url)
        .then(response => {
          if(response.status == '200'){
            window.dispatchEvent(new CustomEvent('qd-refresh-reservation-availability'));
            this.$router.push('/');
          }
          return response.data;
        }).catch(() => {
          console.error("Unable to process your request this time. Please try again later.");
        });
    },
    pickup(){
        return this.getCurrentLocationForStopValidation()
        .then((position) => {
        if (!this.$store.state.connectedUser?.id) {
          console.error('Missing connected user identifier for pickup.');
          return Promise.resolve();
        }
        const userLanguage = navigator.languages && navigator.languages.length ? navigator.languages[0] : navigator.language || 'fr-FR';
        const url = this.$i18n.t('rootURL') + this.$i18n.t('pickup') + "packageID=" + this.package_.id + "&deliveryPersonID=" + this.$store.state.connectedUser.id + "&pickUpOTP=" + this.otp + "&currentLatitude=" + encodeURIComponent(position.latitude) + "&currentLongitude=" + encodeURIComponent(position.longitude) + "&locale=" + userLanguage;
        return http.put(url)
        .then(response => {
          if(response.status == '200'){
            window.dispatchEvent(new CustomEvent('qd-refresh-reservation-availability'));
            this.$router.push('/');
          }
          return response.data;
        }).catch(() => {
          console.error("Unable to process your request this time. Please try again later.");
        });
      }).catch((error) => {
        console.error('Unable to validate pickup location.', error);
      });
    },
    async cancelReservation() {
      if (!this.canCancelReservation || !window.confirm(this.$t('reservationCancelConfirm'))) {
        return;
      }
      const url = `${this.$i18n.t('rootURL')}${this.$i18n.t('cancelReservationUrl')}packageID=${encodeURIComponent(this.package_.id)}&deliveryPersonID=${encodeURIComponent(this.$store.state.connectedUser.id)}`;
      try {
        const response = await http.put(url);
        this.$store.commit('updatePackage', response.data || this.package_);
        window.dispatchEvent(new CustomEvent('qd-refresh-reservation-availability'));
      } catch (error) {
        console.error('Unable to cancel reservation.', error);
      }
    },
    deliver(){
        return this.getCurrentLocationForStopValidation()
        .then((position) => {
        if (!this.$store.state.connectedUser?.id) {
          console.error('Missing connected user identifier for delivery.');
          return Promise.resolve();
        }
        const userLanguage = navigator.languages && navigator.languages.length ? navigator.languages[0] : navigator.language || 'fr-FR';
        const url = this.$i18n.t('rootURL') + this.$i18n.t('deliver') + "packageID=" + this.package_.id + "&deliveryPersonID=" + this.$store.state.connectedUser.id + "&deliveryOTP=" + this.deliveryOtp + "&currentLatitude=" + encodeURIComponent(position.latitude) + "&currentLongitude=" + encodeURIComponent(position.longitude) + "&locale=" + userLanguage;
        return http.put(url)
        .then(response => {
          if(response.status == '200'){
            window.dispatchEvent(new CustomEvent('qd-refresh-reservation-availability'));
            this.$router.push('/');
          }
          return response.data;
        }).catch(() => {
          console.error("Unable to process your request this time. Please try again later.");
        });
      }).catch((error) => {
        console.error('Unable to validate delivery location.', error);
      });
    },
  },
}
</script>
<style>
.package-details-page-v3 {
  padding-bottom: 120px !important;
}

.back-link {
  display: inline-flex;
  align-items: center;
  gap: 8px;
  height: 36px;
  padding: 0 16px;
  border-radius: 10px;
  font-size: 0.85rem;
  font-weight: 700;
}

.btn-cancel-header {
  height: 44px;
  padding: 0 18px;
  border-radius: 12px;
  display: inline-flex;
  align-items: center;
  gap: 8px;
  font-size: 0.9rem;
}

.header-titles h1 {
  font-size: 1.75rem;
  font-weight: 900;
  color: #0f172a;
  letter-spacing: -0.02em;
}

.ref-badge {
  display: flex;
  align-items: center;
  gap: 6px;
  margin-top: 4px;
  color: #64748b;
  font-weight: 600;
  font-size: 0.95rem;
}

.header-right {
  display: flex;
  align-items: center;
  gap: 16px;
}

.status-badge-lg {
  padding: 8px 18px;
  border-radius: 99px;
  font-size: 0.85rem;
  font-weight: 800;
  text-transform: uppercase;
  letter-spacing: 0.05em;
  display: flex;
  align-items: center;
  gap: 8px;
}

.status-badge-lg.new { background: #dcfce7; color: #166534; }
.status-badge-lg.reserved { background: #fef9c3; color: #854d0e; }
.status-badge-lg.pickedup { background: #dbeafe; color: #1e40af; }

.pulse-dot-small {
  width: 8px;
  height: 8px;
  background: currentColor;
  border-radius: 50%;
  animation: qd-pulse-mini 1.5s infinite;
}

@keyframes qd-pulse-mini {
  0% { opacity: 1; transform: scale(1); }
  50% { opacity: 0.4; transform: scale(0.8); }
  100% { opacity: 1; transform: scale(1); }
}

.btn-cancel-header {
  height: 44px;
  padding: 0 20px;
  border-radius: 12px;
  background: #fee2e2;
  color: #991b1b;
  border: none;
  font-weight: 700;
  display: flex;
  align-items: center;
  gap: 8px;
  cursor: pointer;
  transition: all 0.2s;
}

.btn-cancel-header:hover {
  background: #fecaca;
  transform: translateY(-2px);
}

.dashboard-content {
  display: flex;
  flex-direction: column;
  gap: 32px;
  max-width: 1400px;
  margin: 0 auto;
}

.content-card.main-details-card {
  background: #ffffff;
  border-radius: 32px;
  padding: 8px; /* Extra padding inside the card */
  box-shadow: 0 10px 40px rgba(0, 0, 0, 0.03);
  border: 1px solid #f1f5f9;
}

.animate-card {
  animation: qd-fade-up 0.5s ease-out;
}

@keyframes qd-fade-up {
  from { opacity: 0; transform: translateY(20px); }
  to { opacity: 1; transform: translateY(0); }
}

/* Action Dock Styling */
.action-dock {
  position: fixed;
  bottom: 24px;
  left: 50%;
  transform: translateX(-50%);
  width: calc(100% - 48px);
  max-width: 600px;
  z-index: 1000;
}

.action-card-premium {
  padding: 16px 20px;
  border-radius: 24px;
  background: rgba(255, 255, 255, 0.8) !important;
  backdrop-filter: blur(20px);
  -webkit-backdrop-filter: blur(20px);
  border: 1px solid rgba(255, 255, 255, 0.6);
  box-shadow: 0 20px 50px rgba(15, 23, 42, 0.15);
}

.auth-action-row {
  display: flex;
  align-items: center;
  gap: 16px;
  width: 100%;
}

.otp-input-premium {
  position: relative;
  flex: 1;
  display: flex;
  align-items: center;
}

.otp-input-premium .icon {
  position: absolute;
  left: 16px;
  color: #6366f1;
  font-size: 1.25rem;
  z-index: 10;
  pointer-events: none;
}

.otp-input-premium input {
  width: 100%;
  height: 54px;
  padding: 0 16px 0 48px;
  box-sizing: border-box;
  border-radius: 16px;
  border: 2px solid #e2e8f0;
  background: #ffffff;
  font-size: 1.1rem;
  font-weight: 800;
  color: #0f172a;
  transition: all 0.2s;
}

.otp-input-premium input:focus {
  border-color: #6366f1;
  box-shadow: 0 0 0 4px rgba(99, 102, 241, 0.1);
  outline: none;
}

/* Standardized qd-btn-* handled by design-system.css */

.auth-action-column {
  display: flex;
  flex-direction: column;
  gap: 8px;
  width: 100%;
}

.errorMessage-dock {
  color: #ef4444;
  font-size: 0.8rem;
  font-weight: 700;
  text-align: center;
}

.inline-alert-mini {
  display: flex;
  align-items: center;
  gap: 8px;
  margin-top: 12px;
  font-size: 0.85rem;
  font-weight: 700;
  color: #92400e;
  justify-content: center;
}

.secondary-actions-row {
  display: flex;
  justify-content: center;
  width: 100%;
  margin-top: 8px;
}

/* .btn-call-secondary handled by design-system.css qd-btn-secondary */

@media screen and (max-width: 768px) {
  .package-details-page-v3 {
    padding: 12px;
    padding-bottom: 12px;
    gap: 14px;
  }

  .dashboard-content {
    gap: 12px;
  }
  
  .dashboard-header {
    flex-direction: column;
    align-items: flex-start;
    gap: 12px;
  }

  .header-left {
    flex-direction: column;
    align-items: flex-start;
    gap: 12px;
    width: 100%;
  }

  .header-titles h1 {
    font-size: 1.4rem;
  }

  .header-right {
    width: 100%;
    justify-content: space-between;
    padding-top: 8px;
    border-top: 1px solid #e2e8f0;
  }
  
  .action-dock {
    position: static;
    transform: none;
    width: 100%;
    max-width: none;
    z-index: auto;
  }

  .action-card-premium {
    padding: 12px;
    border-radius: 16px;
  }
  
  .auth-action-row {
    display: grid;
    grid-template-columns: minmax(0, 1fr) minmax(118px, 0.7fr);
    gap: 8px;
  }

  .otp-input-premium input {
    height: 42px;
    padding: 0 10px 0 48px !important;
    box-sizing: border-box;
    border-radius: 12px;
    font-size: 0.95rem;
  }

  .otp-input-premium .icon {
    left: 12px;
    width: 24px;
    font-size: 1.05rem;
    text-align: center;
  }

  .auth-action-row .qd-btn-primary {
    width: 100%;
    min-width: 0 !important;
    height: 42px !important;
    padding: 0 10px !important;
    white-space: nowrap;
  }

  .secondary-actions-row {
    margin-top: 6px;
  }

  .secondary-actions-row .qd-btn-secondary {
    width: 100%;
    min-height: 38px;
  }
  
  .prime-action-btn {
    width: 100%;
  }
}
</style>
