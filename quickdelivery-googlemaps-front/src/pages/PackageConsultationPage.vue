<template>
    <div class="packege_creation_main">
        <div class="package-form">
            <div class="summary_component">
                <PackageSummary/>
            </div>
            <br/>
            <button class="btn primary_btn" ref="detailsButtons" v-show="package_.status === 'NEW'"
            @click="reserve">{{ $t('packagesArroundMArkerDetailActionsReserve') }}</button>
            <div v-show="package_.status === 'RESERVED'">
                <div class="input_only">
                    <label for="otp">{{$t('packagePickupPassword')}}:</label>
                    <Field id="otp" type="number" v-model="otp" name="otp" :rules="validateNumericField"/>
                    <ErrorMessage class="errorMessage" name="otp" />
                </div>
                <button class="btn primary_btn" ref="detailsButtons"
                    @click="pickup">{{ $t('packagesArroundMArkerDetailActionsPickUp') }}</button>
            </div>
            <div v-show="package_.status === 'PICKEDUP'">
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

export default{
  computed: {
        package_() {
          return this.$store.state.package_;
        },
  },
  components: {
        PackageSummary,
        Field,
        ErrorMessage,
  },
  props: {
    id: String
  },
  data() {
    return {
      otp: '',
      deliveryOtp: '',
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
    http.get(this.$i18n.t('rootURL') + this.$i18n.t('getPackage')+this.id)
      .then(response => {
        this.$store.commit('updatePackage', response.data);
    }).catch(() => {
      console.log("unable to process your request this time. please try again latter.");
    });
  },
  methods: {
    validateNumericField,
    reserve() {
      const userLanguage = navigator.languages && navigator.languages.length ? navigator.languages[0] : navigator.language || 'fr-FR';
      const url = this.$i18n.t('rootURL') + this.$i18n.t('reservePackageUrl') + "packageID=" + this.package_.id + "&deliveryPersonID=" + this.$store.state.connectedUser.id+"&locale="+userLanguage;
      return http.put(url)
        .then(response => {
          if(response.status == '200'){
            this.$store.commit('updatePackage', this.package);
            this.$store.commit('updateDocuments', []);
            this.$router.push('/');
          }
          return response.data;
        }).catch(() => {
          console.log("unable to process your request this time. please try again latter.");
        });
    },
    pickup(){
        const userLanguage = navigator.languages && navigator.languages.length ? navigator.languages[0] : navigator.language || 'fr-FR';
        const url = this.$i18n.t('rootURL') + this.$i18n.t('pickup') + "packageID=" + this.package_.id + "&deliveryPersonID=" + this.$store.state.connectedUser.id + "&pickUpOTP=" + this.otp + "&locale=" + userLanguage;
        return http.put(url)
        .then(response => {
          if(response.status == '200'){
            this.$store.commit('updatePackage', this.package);
            this.$store.commit('updateDocuments', []);
            this.$router.push('/');
          }
          return response.data;
        }).catch(() => {
          console.log("unable to process your request this time. please try again latter.");
        });
    },
    deliver(){
        const userLanguage = navigator.languages && navigator.languages.length ? navigator.languages[0] : navigator.language || 'fr-FR';
        const url = this.$i18n.t('rootURL') + this.$i18n.t('deliver') + "packageID=" + this.package_.id + "&deliveryPersonID=" + this.$store.state.connectedUser.id + "&deliveryOTP=" + this.deliveryOtp + "&locale=" + userLanguage;
        return http.put(url)
        .then(response => {
          if(response.status == '200'){
            this.$store.commit('updatePackage', this.package);
            this.$store.commit('updateDocuments', []);
            this.$router.push('/');
          }
          return response.data;
        }).catch(() => {
          console.log("unable to process your request this time. please try again latter.");
        });
    },
  },
}
</script>
<style>
  .input_only input {
    margin: 10px 0;
    width: 200px;
  }
</style>
