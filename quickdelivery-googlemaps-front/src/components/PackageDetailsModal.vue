<!-- Modal.vue -->
<template>
  <div v-if="isOpen" class="modal-overlay" @click.self="closeModal">
    <div class="modal-premium glass-pane">
      <!-- Modal Header -->
      <div class="modal-header">
        <div class="header-content">
          <span class="material-symbols-outlined header-icon">package_2</span>
          <div class="header-titles">
            <h2>{{ $t('packagesArroundMArkerDetailActionsDetails') }}</h2>
            <p class="package-ref">{{ package_.reference }}</p>
          </div>
        </div>
        <div class="header-actions">
          <div :class="['status-badge', package_.status.toLowerCase()]">
            {{ $t(`packageStatus_${package_.status}`) || package_.status }}
          </div>
          <button class="icon-close-btn" @click="closeModal">
            <span class="material-symbols-outlined">close</span>
          </button>
        </div>
      </div>

      <!-- Modal Body -->
      <div class="modal-body-scroll">
        <PackageSummary />
      </div>

      <!-- Modal Footer (Actions) -->
      <div class="modal-footer">
        <!-- New Package Actions -->
        <button 
          v-if="canOperateDelivery && package_.status === 'NEW'"
          class="qd-btn-primary" 
          :disabled="isReserveDisabled"
          @click="reserve"
          style="width: 100%;"
        >
          <span class="material-symbols-outlined">add_task</span>
          {{ $t('packagesArroundMArkerDetailActionsReserve') }}
        </button>
        
        <div v-if="canOperateDelivery && package_.status === 'NEW' && isReserveDisabled" class="reservation-alert">
          <span class="material-symbols-outlined">info</span>
          {{ reservationDisabledReason }}
        </div>

        <!-- Reserved Actions -->
        <div v-if="canOperateDelivery && package_.status === 'RESERVED'" class="auth-action-block">
          <div class="otp-input-group">
            <label>{{$t('packagePickupPassword')}}</label>
            <div class="input-with-icon">
              <span class="material-symbols-outlined">key</span>
              <Field 
                id="otp" 
                type="tel" 
                inputmode="numeric"
                v-model="otp" 
                name="otp" 
                :rules="validateNumericField" 
                placeholder="0000"
                @keypress="$event.key >= '0' && $event.key <= '9' ? true : $event.preventDefault()"
                @input="otp = (otp || '').toString().replace(/\D/g, '')"
              />
            </div>
            <ErrorMessage class="errorMessage" name="otp" />
          </div>
          
          <div class="action-row">
            <button v-if="showCancelReservation" class="qd-btn-secondary" @click="cancelReservation" style="flex: 1;">
              {{ $t('actionCancelReservation') }}
            </button>
            <button class="qd-btn-secondary danger" @click="reportSenderAbsent" style="flex: 1;">
              {{ $t('actionReportSenderAbsent') }}
            </button>
            <button class="qd-btn-primary" @click="pickup" style="flex: 1;">
              <span class="material-symbols-outlined">local_shipping</span>
              {{ $t('packagesArroundMArkerDetailActionsPickUp') }}
            </button>
          </div>
        </div>

        <!-- PickedUp Actions -->
        <div v-if="canOperateDelivery && package_.status === 'PICKEDUP'" class="auth-action-block">
          <div class="otp-input-group">
            <label>{{$t('packageDeliveryPassword')}}</label>
            <div class="input-with-icon">
              <span class="material-symbols-outlined">verified_user</span>
              <Field 
                id="deliveryOtp" 
                type="tel" 
                inputmode="numeric"
                v-model="deliveryOtp" 
                name="deliveryOtp" 
                :rules="validateNumericField" 
                placeholder="0000"
                @keypress="$event.key >= '0' && $event.key <= '9' ? true : $event.preventDefault()"
                @input="deliveryOtp = (deliveryOtp || '').toString().replace(/\D/g, '')"
              />
            </div>
            <ErrorMessage class="errorMessage" name="deliveryOtp" />
          </div>
          <div class="action-row">
            <button class="qd-btn-secondary danger" @click="reportRecipientAbsent" style="flex: 1;">
              {{ $t('actionReportRecipientAbsent') }}
            </button>
            <button class="qd-btn-primary" @click="deliver" style="flex: 1;">
              <span class="material-symbols-outlined">task_alt</span>
              {{ $t('packagesArroundMArkerDetailActionsDeliver') }}
            </button>
          </div>
        </div>
      </div>
    </div>
  </div>
</template>

<script>
import PackageSummary from '../components/PackageDetails.vue';
import http from '@/config/httpInterceptor';
import { Field, ErrorMessage } from 'vee-validate';
import { validateNumericField } from '@/config/comonFunction';
import { getCurrentUserRoles } from '@/config/auth';
import { isPackageReservedByDeliveryPerson } from '@/config/packageReservations';

export default {
  components: {
      PackageSummary,
      Field,
      ErrorMessage,
    },
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
  },
  data() {
    return {
      deliveryOtp: '',
      otp: '',
      isOpen: false,
      emptyPackage: {
        id: null,
        version: null,
        creationDate: null,
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
        latitude: null,
        longitude: null,
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
        latitude: null,
        longitude: null,
      }],
      lastPositionLatitude: null,
      lastPositionLongitude: null
    }
    };
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
    openModal() {
      this.isOpen = true;
      this.loadPackageDetailsForAuthorization();
    },
    closeModal() {
      this.$store.commit('updatePackage', this.emptyPackage);
      this.isOpen = false;
    },
    async loadPackageDetailsForAuthorization() {
      const reference = this.package_?.reference;
      if (!reference) {
        return;
      }
      try {
        const response = await http.get(`${this.$i18n.t('rootURL')}${this.$i18n.t('getPackage')}${encodeURIComponent(reference)}`);
        if (response?.data) {
          this.$store.commit('updatePackage', response.data);
        }
      } catch (error) {
        console.error('Unable to load package details for reservation authorization.', error);
      }
    },
    reserve() {
      if (this.isReserveDisabled) {
        return Promise.resolve();
      }
      const userLanguage = navigator.languages && navigator.languages.length ? navigator.languages[0] : navigator.language || 'fr-FR';
      const url = this.$i18n.t('rootURL') + this.$i18n.t('reservePackageUrl') + "packageID=" + this.package_.id + "&deliveryPersonID=" + this.$store.state.connectedUser.id+"&locale="+userLanguage;
      return http.put(url)
        .then(response => {
          if(response.status == '200'){
            window.top.postMessage("RefreshPackagesList "+this.package_.id, "*");
            this.isOpen = false;
          }
          window.dispatchEvent(new CustomEvent('qd-refresh-reservation-availability'));
          this.$store.commit('updatePackage', this.emptyPackage);
          this.$store.commit('updateDocuments', []);
          return response.data;
        }).catch(() => {
          console.error("unable to process your request this time. please try again latter.");
        });
    },
    pickup(){
        return this.getCurrentLocationForStopValidation()
        .then((position) => {
        const userLanguage = navigator.languages && navigator.languages.length ? navigator.languages[0] : navigator.language || 'fr-FR';
        const url = this.$i18n.t('rootURL') + this.$i18n.t('pickup') + "packageID=" + this.package_.id + "&deliveryPersonID=" + this.$store.state.connectedUser.id + "&pickUpOTP=" + this.otp + "&currentLatitude=" + encodeURIComponent(position.latitude) + "&currentLongitude=" + encodeURIComponent(position.longitude) + "&locale=" + userLanguage;
        return http.put(url)
        .then(response => {
          if(response.status == '200'){
            this.$store.commit('updatePackage', this.emptyPackage);
            this.$store.commit('updateDocuments', []);
            window.dispatchEvent(new CustomEvent('qd-refresh-reservation-availability'));
            this.$router.push('/');
          }
          return response.data;
        }).catch(() => {
          console.error("unable to process your request this time. please try again latter.");
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
        this.$store.commit('updatePackage', response.data || this.emptyPackage);
        window.dispatchEvent(new CustomEvent('qd-refresh-reservation-availability'));
        this.isOpen = false;
      } catch (error) {
        console.error('Unable to cancel reservation.', error);
      }
    },
    reportSenderAbsent() {
      if (!window.confirm(this.$t('confirmSenderAbsent'))) {
        return Promise.resolve();
      }
      return this.reportStopIssue(this.$i18n.t('senderAbsentPickup'), 'Unable to report sender absence.');
    },
    reportRecipientAbsent() {
      if (!window.confirm(this.$t('confirmRecipientAbsent'))) {
        return Promise.resolve();
      }
      return this.reportStopIssue(this.$i18n.t('recipientAbsentDelivery'), 'Unable to report recipient absence.');
    },
    reportStopIssue(endpoint, errorMessage) {
      return this.getCurrentLocationForStopValidation()
        .then((position) => {
          const url = this.$i18n.t('rootURL') + endpoint
            + "packageID=" + encodeURIComponent(this.package_.id)
            + "&deliveryPersonID=" + encodeURIComponent(this.$store.state.connectedUser.id)
            + "&currentLatitude=" + encodeURIComponent(position.latitude)
            + "&currentLongitude=" + encodeURIComponent(position.longitude);
          return http.put(url)
            .then(response => {
              if (response.status == '200') {
                this.$store.commit('updatePackage', response.data || this.emptyPackage);
                this.$store.commit('updateDocuments', []);
                window.dispatchEvent(new CustomEvent('qd-refresh-reservation-availability'));
                this.isOpen = false;
                this.$router.push('/');
              }
              return response.data;
            }).catch((error) => {
              console.error(errorMessage, error);
            });
        }).catch((error) => {
          console.error('Unable to validate stop location.', error);
        });
    },
    deliver(){
        return this.getCurrentLocationForStopValidation()
        .then((position) => {
        const userLanguage = navigator.languages && navigator.languages.length ? navigator.languages[0] : navigator.language || 'fr-FR';
        const url = this.$i18n.t('rootURL') + this.$i18n.t('deliver') + "packageID=" + this.package_.id + "&deliveryPersonID=" + this.$store.state.connectedUser.id + "&deliveryOTP=" + this.deliveryOtp + "&currentLatitude=" + encodeURIComponent(position.latitude) + "&currentLongitude=" + encodeURIComponent(position.longitude) + "&locale=" + userLanguage;
        return http.put(url)
        .then(response => {
          if(response.status == '200'){
            this.$store.commit('updatePackage', this.emptyPackage);
            this.$store.commit('updateDocuments', []);
            window.dispatchEvent(new CustomEvent('qd-refresh-reservation-availability'));
            this.$router.push('/');
          }
          return response.data;
        }).catch(() => {
          console.error("unable to process your request this time. please try again latter.");
        });
      }).catch((error) => {
        console.error('Unable to validate delivery location.', error);
      });
    },
  },
};
</script>

<style scoped>
.modal-overlay {
  position: fixed;
  inset: 0;
  background: rgba(15, 23, 42, 0.4);
  backdrop-filter: blur(8px);
  -webkit-backdrop-filter: blur(8px);
  display: flex;
  justify-content: center;
  align-items: center;
  z-index: 2000;
  padding: 20px;
}

.modal-premium {
  width: 100%;
  max-width: 900px;
  max-height: 90vh;
  background: rgba(255, 255, 255, 0.9);
  border-radius: 24px;
  display: flex;
  flex-direction: column;
  box-shadow: 0 25px 50px -12px rgba(0, 0, 0, 0.25);
  border: 1px solid rgba(255, 255, 255, 0.5);
}

.modal-header {
  padding: 24px;
  display: flex;
  justify-content: space-between;
  align-items: flex-start;
  border-bottom: 1px solid #f1f5f9;
}

.header-content {
  display: flex;
  gap: 16px;
  align-items: center;
}

.header-icon {
  font-size: 2.5rem;
  color: #4f46e5;
  background: #f1f5f9;
  padding: 12px;
  border-radius: 16px;
}

.header-titles h2 {
  margin: 0;
  font-size: 1.25rem;
  font-weight: 800;
  color: #0f172a;
}

.package-ref {
  margin: 4px 0 0;
  font-size: 0.9rem;
  font-weight: 600;
  color: #64748b;
  font-family: monospace;
}

.header-actions {
  display: flex;
  align-items: center;
  gap: 16px;
}

.status-badge {
  padding: 6px 14px;
  border-radius: 99px;
  font-size: 0.75rem;
  font-weight: 800;
  text-transform: uppercase;
  letter-spacing: 0.05em;
}

.status-badge.new { background: #dcfce7; color: #166534; }
.status-badge.reserved { background: #fef9c3; color: #854d0e; }
.status-badge.pickedup { background: #dbeafe; color: #1e40af; }

.icon-close-btn {
  background: #f1f5f9;
  border: none;
  width: 40px;
  height: 40px;
  border-radius: 50%;
  display: flex;
  align-items: center;
  justify-content: center;
  cursor: pointer;
  color: #64748b;
  transition: all 0.2s;
}

.icon-close-btn:hover {
  background: #e2e8f0;
  color: #0f172a;
  transform: rotate(90deg);
}

.modal-body-scroll {
  flex: 1;
  overflow-y: auto;
  padding: 24px;
  background: #f8fafc;
}

.modal-footer {
  padding: 24px;
  background: #ffffff;
  border-top: 1px solid #f1f5f9;
  border-radius: 0 0 24px 24px;
}

/* .action-btn handled by design-system.css qd-btn-* */

.reservation-alert {
  display: flex;
  align-items: center;
  gap: 8px;
  padding: 12px;
  background: #fffbeb;
  border-radius: 12px;
  font-size: 0.85rem;
  color: #92400e;
  font-weight: 600;
}

.auth-action-block {
  display: flex;
  flex-direction: column;
  gap: 20px;
}

.otp-input-group {
  display: flex;
  flex-direction: column;
  gap: 8px;
}

.otp-input-group label {
  font-size: 0.85rem;
  font-weight: 700;
  color: #475569;
}

.input-with-icon {
  position: relative;
  display: flex;
  align-items: center;
}

.input-with-icon .material-symbols-outlined {
  position: absolute;
  left: 16px;
  color: #94a3b8;
}

.input-with-icon input {
  padding: 0 16px 0 48px;
  height: 52px;
  width: 100%;
  border: 2px solid #e2e8f0;
  border-radius: 14px;
  font-size: 1.1rem;
  font-weight: 800;
  letter-spacing: 0.1em;
  transition: border-color 0.2s;
}

.input-with-icon input:focus {
  border-color: #4f46e5;
  outline: none;
}

.action-row {
  display: flex;
  gap: 12px;
  flex-wrap: wrap;
}

.action-row .action-btn {
  flex: 1;
}

.action-row button {
  min-width: 160px;
}

.qd-btn-secondary.danger {
  border-color: #ef4444;
  color: #b91c1c;
}

@media screen and (max-width: 768px) {
  .modal-overlay {
    padding: 0;
    align-items: flex-end;
  }
  
  .modal-premium {
    max-height: 95vh;
    border-radius: 32px 32px 0 0;
  }
  
  .modal-header {
    padding: 20px;
  }
  
  .header-icon {
    display: none;
  }
}
</style>
