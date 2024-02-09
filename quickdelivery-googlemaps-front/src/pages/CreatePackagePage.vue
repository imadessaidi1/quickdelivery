<template>
    <div class="packege_creation_main">
      <div class="package-form">
        <Form @submit="submitForm" ref="packageCreationForm">
          <div class="components" v-if="currentStep === 1">
              <h2>{{$t('createNewPackage')}}</h2>
              <PackageCreation ref="packageInfo"/>
          </div>
          <div class="components" v-if="currentStep === 2">
              <h2>{{$t('packageAddressDepartureAddresses')}}</h2>
              <PackageAddress ref="departureAddress" :addressType="departure"/>
              <br/>
          </div>
          <div class="components" v-if="currentStep === 3">
              <h2>{{$t('packageAddressArrivalAddresses')}}</h2>
              <PackageAddress ref="arrivalAddress" :addressType="arrival"/>
              <br/>
          </div>
          <div class="summary_component" v-show="currentStep === 4">
              <h2>{{$t('packageSummaryAction')}}</h2>
              <PackageSummary ref="packageSummary"/>
          </div>
          <br/>
          <button class="primary_btn" @click="previousStep" v-if="currentStep > 1">{{$t('packagePreviousAction')}}</button>&nbsp;
        <button class="primary_btn" type="submit"><span v-if="currentStep < 3">{{$t('packageNextAction')}}</span><span v-if="currentStep === 3">{{$t('packageSummaryAction')}}</span><span v-if="currentStep === 4">{{$t('packageCreateAction')}}</span></button>
        </Form>
      </div>
    </div>
</template>
<script>
import PackageCreation from '../components/PackageCreation.vue';
import PackageAddress from '../components/PackageAddress.vue';
import PackageSummary from '../components/PackageDetails.vue';
import axios from 'axios';
import { Form } from 'vee-validate';
import { validateAddress, validateDeliveryDateTime } from '@/config/comonFunction';

export default{
  components: {
    PackageCreation,
    PackageAddress,
    PackageSummary,
    Form,
  },
  data() {
    return {
      currentStep: 1,
      widthSize: window.innerWidth,
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
           this.currentStep++;
        }else if(this.currentStep === 2){
           this.$refs.departureAddress.address.type = 'DEPARTURE';
           const addressAuto=this.$refs.departureAddress.$refs.addressAutoComplete;
           if(!validateAddress(addressAuto.address)){
                this.$refs.departureAddress.isAddressError = true;
                this.$refs.departureAddress.errorAddressMessage=this.$i18n.t('mandatoryField')+this.$i18n.t('invalidAddress');
           }else{
               const address = addressAuto.address.split(',');
               this.$refs.departureAddress.address.line1 = address[0].trim();
               this.$refs.departureAddress.address.zipCode = address[1].trim().split(' ')[0];
               this.$refs.departureAddress.address.town = address[1].trim().split(' ')[1];
               this.$refs.departureAddress.address.country = address[2].trim();
               this.$store.commit('updatePackageDepartureAddress', this.$refs.departureAddress.address);
               this.currentStep++;
           }
        }else if(this.currentStep === 3){
           this.$refs.arrivalAddress.address.type = 'ARRIVAL';
           const addressAuto_=this.$refs.arrivalAddress.$refs.addressAutoComplete;
           if(!validateAddress(addressAuto_.address)){
                this.$refs.arrivalAddress.isAddressError = true;
                this.$refs.arrivalAddress.errorAddressMessage=this.$i18n.t('mandatoryField')+this.$i18n.t('invalidAddress');
           } if(!validateDeliveryDateTime(this.$store.state.package_.addresses[0].dateTime,this.$store.state.package_.addresses[1].dateTime)){
                this.$refs.arrivalAddress.isDateTimeError = true;
                this.$refs.arrivalAddress.errorDeliveryDateTimeMessage=this.$i18n.t('packageDeliveryInvalidDateTime');
           }else {
               this.$refs.arrivalAddress.isAddressError = false;
               this.$refs.arrivalAddress.isDateTimeError = false;
               const address_ = addressAuto_.address.split(',');
               this.$refs.arrivalAddress.address.line1 = address_[0].trim();
               this.$refs.arrivalAddress.address.zipCode = address_[1].trim().split(' ')[0];
               this.$refs.arrivalAddress.address.town = address_[1].trim().split(' ')[1];
               this.$refs.arrivalAddress.address.country = address_[2].trim();
               this.$store.commit('updatePackageArrivalAddress', this.$refs.arrivalAddress.address);
               this.currentStep++;
           }
        }
      }
    },
    previousStep() {
      if (this.currentStep > 1) {
        this.currentStep--;
      }
    },
    async submitForm() {
     if(this.currentStep === 4){
         const formData = new FormData();
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
     }else{
        this.nextStep();
     }
    },
  },
}
</script>
<style>
.packege_creation_main{
  padding: 10px;
  height: 815px;
  display: flex;
  justify-content: center;
  align-items: center;
}
.packege_creation_main .package-form {
  width: 60%;
  height: 70%;
  padding: 20px;
  margin: 0 auto;
  border-radius: 5px;
  background: linear-gradient(0.25turn, #ffffff, #ffffff79, #ffffff0c), no-repeat url('../assets/box 2.png') right -160px bottom 50%;
  box-shadow: rgba(0, 0, 0, 0.24) 0px 3px 8px;
}
.packege_creation_main .package-form form{
  width: 100%;
}
.packege_creation_main .package-form form .components,
.packege_creation_main .package-form form .summary_component{
  width: 75%;
}
.packege_creation_main .package-form form .components h2{
  border-left: solid 5px #ff7b00;
  padding-left: 8px;
  margin-left: 20px;
}
.packege_creation_main .package-form .package-address {
  display: flex;
  flex-wrap: wrap;
}
.input_container{
  display: inline-block;
  width: 50%;
}
.input_only{
  height: 85px;
}
.input_container input{
  width: 65%;
}
.small_width{
  display: none;
}
@media screen and (max-width: 1500px){
  .packege_creation_main .package-form form .summary_component{
    width: 100%;
  }
  .packege_creation_main .package-form form .components{
    width: 100%;
  }
}
@media screen and (max-width: 1100px) {
  .input_container{
    width: 100%;
  }
  .input_container input{
    width: 83%;
  }
  .packege_creation_main .package-form {
    height: 85%;
  }
  .packege_creation_main{
    height: max-content;
  }
  .packege_creation_main .package-form {
    margin: 50px auto;
  }
  /*.large_width{
    display: none;
  }
  .small_width{
    display: block;
  }*/
}
@media screen and (max-width: 600px) {
  .packege_creation_main{
    height: max-content;
  }
  .packege_creation_main .package-form {
    width: 75%;
    padding: 30px;
  }
  .input_container input,
  .picture_file_container .input_only input{
    width: 93%;
    height: 30px;
  }
  .input_container input{
    height: 35px;
  }
}
</style>  