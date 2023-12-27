<template>
    <div class="package-form">
        <form @submit.prevent="submitForm" ref="packageCreationForm">
                <div v-if="currentStep === 1">
                    <h2>{{$t('createNewPackage')}}</h2>
                    <PackageCreation ref="packageInfo"/>
                    <button @click="nextStep">{{$t('packageNextAction')}}</button>
                </div>
                    <div v-if="currentStep === 2">
                        <h2>{{$t('packageAddressDepartureAddresses')}}</h2>
                        <PackageAddress ref="departureAddress"/>
                        <button @click="nextStep">{{$t('packageNextAction')}}</button>
                    </div>
                    <div v-if="currentStep === 3">
                        <h2>{{$t('packageAddressArrivalAddresses')}}</h2>
                        <PackageAddress ref="arrivalAddress"/>
                        <button @click="nextStep">{{$t('packageSummaryAction')}}</button>
                    </div>
                    <div v-if="currentStep === 4">
                        <h2>{{$t('packageSummaryAction')}}</h2>
                        <PackageSummary ref="packageSummary" :package_="package_" :documentS="documentS"/>
                    </div>
        <button type="submit" v-if="currentStep === 4">{{$t('packageCreateAction')}}</button>
        </form>
    </div>
</template>
<script>
import PackageCreation from '../components/PackageCreation.vue';
import PackageAddress from '../components/PackageAddress.vue';
import PackageSummary from '../components/PackageDetails.vue';
import axios from 'axios';

export default{
  components: {
    PackageCreation,
    PackageAddress,
    PackageSummary,
  },
  data() {
    return {
      currentStep: 1,
      package_: {
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
        senderID: 905,
        packageReservations: [],
        addresses: [{
        firstName: "",
        lastName: "",
        line1: "",
        line2: "",
        town: "",
        zipCode: "",
        country: "",
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
        email: "",
        phone: "",
        type: "ARRIVAL",
        latitude: 0,
        longitude: 0,
      }],
      lastPositionLatitude: null,
      lastPositionLongitude: null
      },
      documentS: [],
    };
  },
  methods: {
    nextStep() {
      if (this.currentStep < 4) {
        if(this.currentStep === 1){
           this.package_=this.$refs.packageInfo.package_;
           this.documentS=this.$refs.packageInfo.documentS
           this.package_.status='PAYMENTPENDING';
        }else if(this.currentStep === 2){
           this.$refs.departureAddress.address.type = 'DEPARTURE';
           this.package_.addresses[0]=this.$refs.departureAddress.address;
           const addressAuto=this.$refs.departureAddress.$refs.addressAutoComplete;
           const address = addressAuto.address.split(',');
           this.package_.addresses[0].line1 = address[0].trim();
           this.package_.addresses[0].zipCode = address[1].trim().split(' ')[0];
           this.package_.addresses[0].town = address[1].trim().split(' ')[1];
           this.package_.addresses[0].country = address[2].trim();
        }else if(this.currentStep === 3){
           this.$refs.arrivalAddress.address.type = 'ARRIVAL';
           this.package_.addresses[1]=this.$refs.arrivalAddress.address;
           const addressAuto_=this.$refs.arrivalAddress.$refs.addressAutoComplete;
           const address_ = addressAuto_.address.split(',');
           this.package_.addresses[1].line1 = address_[0].trim();
           this.package_.addresses[1].zipCode = address_[1].trim().split(' ')[0];
           this.package_.addresses[1].town = address_[1].trim().split(' ')[1];
           this.package_.addresses[1].country = address_[2].trim();
        }
        this.currentStep++;
      }
    },

    async submitForm() {
     const formData = new FormData();
     console.log(JSON.stringify(this.package_));
     formData.append('packageDTO', JSON.stringify(this.package_));
     formData.append('files', this.documentS[0]);
     formData.append('files', this.documentS[1]);
     console.log(this.documentS[0]);
     console.log(this.documentS[1]);
     console.log(formData.get('files')[0]);
     const i18n = this.$i18n;
     return axios.post(i18n.t('rootURL') + i18n.t('createPackageUrl'), formData, { headers: { acept: 'application/json','Content-type': 'multipart/form-data' } })
        .then(response => {
            console.log(response.data);
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
}
.package-address {
  display: flex;
  flex-wrap: wrap;
}
</style>