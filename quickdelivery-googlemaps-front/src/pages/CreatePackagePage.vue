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
                        <PackageSummary ref="packageSummary" :package_="package_"/>
                    </div>
        <button type="submit" v-if="currentStep === 4">{{$t('packageCreateAction')}}</button>
        </form>
    </div>
</template>
<script>
import PackageCreation from '../components/PackageCreation.vue';
import PackageAddress from '../components/PackageAddress.vue';
import PackageSummary from '../components/PackageDetails.vue';

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
        height: 0,
        width: 0,
        weight: 0,
        depth: 0,
        pictureURL: '',
        senderID: 905,
        addresses: [{
        fullAddress:"",
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
        fullAddress:"",
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
        documentS: [],
      },
    };
  },
  methods: {
    nextStep() {
      if (this.currentStep < 4) {
        if(this.currentStep === 1){
           this.package_=this.$refs.packageInfo.package_;
        }else if(this.currentStep === 2){
           this.$refs.departureAddress.address.type = 'DEPARTURE';
           this.package_.addresses[0]=this.$refs.departureAddress.address;
           const addressAuto=this.$refs.departureAddress.$refs.addressAutoComplete;
           const address = addressAuto.address.split(',');
           this.package_.addresses[0].fullAddress = addressAuto;
           this.package_.addresses[0].line1 = address[0].trim();
           this.package_.addresses[0].zipCode = address[1].trim().split(' ')[0];
           this.package_.addresses[0].town = address[1].trim().split(' ')[1];
           this.package_.addresses[0].country = address[2].trim();
        }else if(this.currentStep === 3){
           this.$refs.arrivalAddress.address.type = 'ARRIVAL';
           this.package_.addresses[1]=this.$refs.arrivalAddress.address;
           const addressAuto_=this.$refs.arrivalAddress.$refs.addressAutoComplete;
           const address_ = addressAuto_.address.split(',');
           this.package_.addresses[1].fullAddress = address_;
           this.package_.addresses[1].line1 = address_[0].trim();
           this.package_.addresses[1].zipCode = address_[1].trim().split(' ')[0];
           this.package_.addresses[1].town = address_[1].trim().split(' ')[1];
           this.package_.addresses[1].country = address_[2].trim();
        }
        this.currentStep++;
      }
    },

    async submitForm() {
     /* const i18n = this.$i18n;
      const addressListComponent=this.$refs.addressList;
      this.package_.addresses = addressListComponent.addresses;
      const requestOptions = {
          method: "POST",
          headers: { "Content-Type": "application/json" },
          body: JSON.stringify(this.package_)
      };
      console.log(i18n.t('rootURL') + i18n.t('createPackageUrl'));
      const response = await fetch(i18n.t('rootURL') + i18n.t('createPackageUrl'), requestOptions);
      const data = await response.json();
      console.log(data);*/
      this.$refs.arrivalAddress.address.type='ARRIVAL';
      this.package_.addresses[1]=this.$refs.arrivalAddress.address;
      console.log(this.package_);
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