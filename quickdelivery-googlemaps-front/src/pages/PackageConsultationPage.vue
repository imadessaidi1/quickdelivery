<template>
    <div class="package-details-page">
        <div class="page-head">
            <button class="btn primary_btn back-btn" type="button" @click="goBack">{{ $t('actionBack') }}</button>
            <div>
                <h1>{{ $t('packagesArroundMArkerDetailActionsDetails') }}</h1>
                <p>{{ package_.reference || id }}</p>
            </div>
        </div>
        <div v-if="isLoadingPage" class="page-state">{{ $t('stateLoading') }}</div>
        <div v-else-if="loadError" class="page-state error">{{ $t('stateLoadError') }}</div>
        <div v-else class="package-form">
            <div class="summary_component">
                <PackageSummary/>
            </div>
            <br/>
            <button class="btn primary_btn" ref="detailsButtons" v-show="canOperateDelivery && package_.status === 'NEW'"
            @click="reserve">{{ $t('packagesArroundMArkerDetailActionsReserve') }}</button>
            <div v-show="canOperateDelivery && package_.status === 'RESERVED'">
                <div class="input_only">
                    <label for="otp">{{$t('packagePickupPassword')}}:</label>
                    <Field id="otp" type="number" v-model="otp" name="otp" :rules="validateNumericField"/>
                    <ErrorMessage class="errorMessage" name="otp" />
                </div>
                <button class="btn primary_btn" ref="detailsButtons"
                    @click="pickup">{{ $t('packagesArroundMArkerDetailActionsPickUp') }}</button>
            </div>
            <div v-show="canOperateDelivery && package_.status === 'PICKEDUP'">
                <div class="input_only">
                    <label for="deliveryOtp">{{$t('packageDeliveryPassword')}}:</label>
                    <Field id="deliveryOtp" type="number" v-model="deliveryOtp" name="deliveryOtp" :rules="validateNumericField"/>
                    <ErrorMessage class="errorMessage" name="deliveryOtp" />
                </div>
                <button class="btn primary_btn" ref="detailsButtons"
                @click="deliver">{{ $t('packagesArroundMArkerDetailActionsDeliver') }}</button>
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

export default{
  computed: {
        package_() {
          return this.$store.state.package_;
        },
        canOperateDelivery() {
          const roles = getCurrentUserRoles();
          return roles.includes('ROLE_LIVREUR') || roles.includes('ROLE_ADMIN');
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
      if (!this.$store.state.connectedUser?.id) {
        console.error('Missing connected user identifier for reservation.');
        return Promise.resolve();
      }
      const userLanguage = navigator.languages && navigator.languages.length ? navigator.languages[0] : navigator.language || 'fr-FR';
      const url = this.$i18n.t('rootURL') + this.$i18n.t('reservePackageUrl') + "packageID=" + this.package_.id + "&deliveryPersonID=" + this.$store.state.connectedUser.id+"&locale="+userLanguage;
      return http.put(url)
        .then(response => {
          if(response.status == '200'){
            this.$router.push('/');
          }
          return response.data;
        }).catch(() => {
          console.error("Unable to process your request this time. Please try again later.");
        });
    },
    pickup(){
        if (!this.$store.state.connectedUser?.id) {
          console.error('Missing connected user identifier for pickup.');
          return Promise.resolve();
        }
        const userLanguage = navigator.languages && navigator.languages.length ? navigator.languages[0] : navigator.language || 'fr-FR';
        const url = this.$i18n.t('rootURL') + this.$i18n.t('pickup') + "packageID=" + this.package_.id + "&deliveryPersonID=" + this.$store.state.connectedUser.id + "&pickUpOTP=" + this.otp + "&locale=" + userLanguage;
        return http.put(url)
        .then(response => {
          if(response.status == '200'){
            this.$router.push('/');
          }
          return response.data;
        }).catch(() => {
          console.error("Unable to process your request this time. Please try again later.");
        });
    },
    deliver(){
        if (!this.$store.state.connectedUser?.id) {
          console.error('Missing connected user identifier for delivery.');
          return Promise.resolve();
        }
        const userLanguage = navigator.languages && navigator.languages.length ? navigator.languages[0] : navigator.language || 'fr-FR';
        const url = this.$i18n.t('rootURL') + this.$i18n.t('deliver') + "packageID=" + this.package_.id + "&deliveryPersonID=" + this.$store.state.connectedUser.id + "&deliveryOTP=" + this.deliveryOtp + "&locale=" + userLanguage;
        return http.put(url)
        .then(response => {
          if(response.status == '200'){
            this.$router.push('/');
          }
          return response.data;
        }).catch(() => {
          console.error("Unable to process your request this time. Please try again later.");
        });
    },
  },
}
</script>
<style>
  .package-details-page {
    min-height: 100%;
    padding: 24px;
    background: #f6f7f9;
    box-sizing: border-box;
  }
  .page-head {
    display: flex;
    align-items: flex-start;
    gap: 16px;
    margin-bottom: 18px;
  }
  .page-head h1 {
    margin: 0;
    color: #0f172a;
  }
  .page-head p {
    margin: 6px 0 0;
    color: #64748b;
  }
  .back-btn {
    min-width: 110px;
    display: inline-flex;
    align-items: center;
    justify-content: center;
    height: 42px;
    padding: 0 18px;
    border: none;
    border-radius: 12px;
    background: #020617;
    color: #ffffff;
    font-weight: 600;
    box-shadow: 0 10px 22px rgba(15, 23, 42, 0.12);
  }
  .package-form {
    border: 1px solid #e5e7eb;
    border-radius: 18px;
    background: #ffffff;
    padding: 18px;
    box-shadow: 0 12px 28px rgba(15, 23, 42, 0.06);
  }
  .input_only input {
    margin: 10px 0;
    width: 200px;
  }
  @media screen and (max-width: 767px) {
    .package-details-page {
      padding: 16px;
    }
    .page-head {
      flex-direction: column;
    }
  }
</style>
