<template>
    <div class="package-form">
        <form @submit.prevent="submitForm" ref="packageCreationForm">
                <div v-if="currentStep === 1">
                    <h2>{{$t('createNewPackage')}}</h2>
                    <PackageCreation ref="packageInfo"/>
                    <br/>
                    <button class="primary_btn" @click="nextStep">{{$t('packageNextAction')}}</button>
                </div>
                    <div v-if="currentStep === 2">
                        <h2>{{$t('packageAddressDepartureAddresses')}}</h2>
                        <PackageAddress ref="departureAddress" :addressType="departure"/>
                        <br/>
                        <button class="primary_btn" @click="previousStep">{{$t('packagePreviousAction')}}</button>&nbsp;<button class="primary_btn" @click="nextStep">{{$t('packageNextAction')}}</button>
                    </div>
                    <div v-if="currentStep === 3">
                        <h2>{{$t('packageAddressArrivalAddresses')}}</h2>
                        <PackageAddress ref="arrivalAddress" :addressType="arrival"/>
                        <br/>
                        <button class="primary_btn" @click="previousStep">{{$t('packagePreviousAction')}}</button>&nbsp;<button class="primary_btn" @click="nextStep">{{$t('packageSummaryAction')}}</button>
                    </div>
                    <div v-if="currentStep === 4">
                        <h2>{{$t('packageSummaryAction')}}</h2>
                        <PackageSummary ref="packageSummary"/>
                    </div>
        <br/>
        <button class="primary_btn" type="submit" v-if="currentStep === 4">{{$t('packageCreateAction')}}</button>
        </form>
    </div>
</template>
<script>
import PackageCreation from '../components/PackageCreation.vue';
import PackageAddress from '../components/PackageAddress.vue';
import PackageSummary from '../components/PackageDetails.vue';
import axios from 'axios';
//import { useVuelidate } from '@vuelidate/core';
//import { validations } from '@/vuelidate/packageValidation';

export default{
  components: {
    PackageCreation,
    PackageAddress,
    PackageSummary,
  },
  /*setup() {
    const $v = useVuelidate(validations);
  },*/
  data() {
    return {
      currentStep: 1,
      departure: 'DEPARTURE',
      arrival: 'ARRIVAL',
    };
  },
  computed: {
    package_() {
      return this.$store.state.package_;
    },
    documentS() {
      return this.$store.state.documentS;
    },
  },
  methods: {
    nextStep() {
      if (this.currentStep < 4) {
        if(this.currentStep === 1){
           this.$refs.packageInfo.package_.status='PAYMENTPENDING';
           this.$store.commit('updatePackage', this.$refs.packageInfo.package_);
           this.$store.commit('updateDocuments', this.$refs.packageInfo.documentS);
        }else if(this.currentStep === 2){
           this.$refs.departureAddress.address.type = 'DEPARTURE';
           const addressAuto=this.$refs.departureAddress.$refs.addressAutoComplete;
           const address = addressAuto.address.split(',');
           this.$refs.departureAddress.address.line1 = address[0].trim();
           this.$refs.departureAddress.address.zipCode = address[1].trim().split(' ')[0];
           this.$refs.departureAddress.address.town = address[1].trim().split(' ')[1];
           this.$refs.departureAddress.address.country = address[2].trim();
           this.$store.commit('updatePackageDepartureAddress', this.$refs.departureAddress.address);
        }else if(this.currentStep === 3){
           this.$refs.arrivalAddress.address.type = 'ARRIVAL';
           const addressAuto_=this.$refs.arrivalAddress.$refs.addressAutoComplete;
           const address_ = addressAuto_.address.split(',');
           this.$refs.arrivalAddress.address.line1 = address_[0].trim();
           this.$refs.arrivalAddress.address.zipCode = address_[1].trim().split(' ')[0];
           this.$refs.arrivalAddress.address.town = address_[1].trim().split(' ')[1];
           this.$refs.arrivalAddress.address.country = address_[2].trim();
           this.$store.commit('updatePackageArrivalAddress', this.$refs.arrivalAddress.address);
        }
        this.currentStep++;
      }
    },
    previousStep() {
      if (this.currentStep > 1) {
        this.currentStep--;
      }
    },
    async submitForm() {
     const formData = new FormData();
     console.log(this.package_);
     this.package_.senderID = this.$store.state.connectedUser.id
     formData.append('packageDTO', JSON.stringify(this.package_));
     formData.append('files', this.documentS[0]);
     formData.append('files', this.documentS[1]);
     return axios.post(this.$i18n.t('rootURL') + this.$i18n.t('createPackageUrl'), formData, { headers: { acept: 'application/json','Content-type': 'multipart/form-data' } })
        .then(response => {
            this.$store.commit('updatePackage', response.data);
            return response.data;
        }).catch(() => {
            console.log("unable to process your request this time. please try again latter.");
        });
    },
  },
}
</script>
<style>
.package-form {
  padding: 20px;
  border: 1px solid #ccc;
  border-radius: 5px;
  margin: 10px;
  height: 81%;
}
.package-address {
  display: flex;
  flex-wrap: wrap;
}
</style>