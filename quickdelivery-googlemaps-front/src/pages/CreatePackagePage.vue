<template>
    <div class="packege_creation_main">
      <div class="package-form">
        <Form @submit="submitFormPackage" ref="packageCreationForm">
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
          <!--<button class="btn primary_btn" @click="callNotification">notification</button>&nbsp;-->
        <button class="btn primary_btn" type="button" @click="previousStep" v-show="currentStep > 1">{{$t('packagePreviousAction')}}</button>
        <button class="btn primary_btn" type="submit" v-show="currentStep < 3">{{$t('packageNextAction')}}</button>
        <button class="btn primary_btn" type="submit" v-show="currentStep === 3">{{$t('packageSummaryAction')}}</button>
        <button class="btn primary_btn" type="submit" v-show="currentStep === 4">{{$t('packageCreateAction')}}</button>
        </Form>
      </div>
    </div>
</template>
<script>
import PackageCreation from '../components/PackageCreation.vue';
import PackageAddress from '../components/PackageAddress.vue';
import PackageSummary from '../components/PackageDetails.vue';
import http from '@/config/httpInterceptor';
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
      if (this.currentStep < 5) {
        if(this.currentStep === 1){
           this.$refs.packageInfo.package_.status='PAYMENTPENDING';
           this.$store.commit('updatePackage', this.$refs.packageInfo.package_);
           this.$store.commit('updateDocuments', this.$refs.packageInfo.documentS);
           this.currentStep++;
        }else if(this.currentStep === 2){
           this.$refs.departureAddress.address.type = 'DEPARTURE';
           const addressAuto=this.$refs.departureAddress.$refs.addressAutoComplete;
           if(addressAuto.address){
                this.$refs.departureAddress.address.addressAuto = addressAuto.address;
           }
           if(!validateAddress(this.$refs.departureAddress.address.addressAuto)){
                this.$refs.departureAddress.isAddressError = true;
                this.$refs.departureAddress.errorAddressMessage=this.$i18n.t('mandatoryField')+this.$i18n.t('invalidAddress');
           }else{
               this.$refs.departureAddress.isAddressError = false;
               const address = this.$refs.departureAddress.address.addressAuto.split(',');
               this.$refs.departureAddress.address.line1 = address[0].trim();
               this.$refs.departureAddress.address.zipCode = address[1].trim().split(' ')[0];
               let index = address[1].trim().indexOf(' ');
                if (index !== -1) {
                    this.$refs.departureAddress.address.town = address[1].substring(index + 1); // Extrait la partie après le premier espace
                }
               this.$refs.departureAddress.address.country = address[2].trim();
               this.$store.commit('updatePackageDepartureAddress', this.$refs.departureAddress.address);
               this.currentStep++;
           }
        }else if(this.currentStep === 3){
           this.$refs.arrivalAddress.address.type = 'ARRIVAL';
           const addressAuto_=this.$refs.arrivalAddress.$refs.addressAutoComplete;
           if(addressAuto_.address){
                this.$refs.arrivalAddress.address.addressAuto = addressAuto_.address;
           }
           if(!validateAddress(this.$refs.arrivalAddress.address.addressAuto)){
                this.$refs.arrivalAddress.isAddressError = true;
                this.$refs.arrivalAddress.errorAddressMessage=this.$i18n.t('mandatoryField')+this.$i18n.t('invalidAddress');
           }
           if(!validateDeliveryDateTime(this.$store.state.package_.addresses[0].dateTime,this.$store.state.package_.addresses[1].dateTime)){
                this.$refs.arrivalAddress.isDateTimeError = true;
                this.$refs.arrivalAddress.errorDeliveryDateTimeMessage=this.$i18n.t('packageDeliveryInvalidDateTime');
           }
           if(validateAddress(this.$refs.arrivalAddress.address.addressAuto) && validateDeliveryDateTime(this.$store.state.package_.addresses[0].dateTime,this.$store.state.package_.addresses[1].dateTime)){
               this.$refs.arrivalAddress.isAddressError = false;
               this.$refs.arrivalAddress.isDateTimeError = false;
               const address_ = this.$refs.arrivalAddress.address.addressAuto.split(',');
               this.$refs.arrivalAddress.address.line1 = address_[0].trim();
               this.$refs.arrivalAddress.address.zipCode = address_[1].trim().split(' ')[0];
               let index = address_[1].trim().indexOf(' ');
                if (index !== -1) {
                    this.$refs.arrivalAddress.address.town = address_[1].substring(index + 1);
                }
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
    /*callNotification(){
        http.get(this.$i18n.t('rootURL') + this.$i18n.t('notify'))
            .then(response => {
                console.log(response);
            }).catch(() => {
                console.log("unable to process your request this time. please try again latter.");
            });
    },*/
    async submitFormPackage() {
     if(this.currentStep === 4){
         const formData = new FormData();
         this.package_.senderID = this.$store.state.connectedUser.id
         formData.append('packageDTO', JSON.stringify(this.package_));
         formData.append('files', this.documentS[0]);
         formData.append('files', this.documentS[1]);
         const userLanguage = navigator.languages && navigator.languages.length ? navigator.languages[0] : navigator.language || 'fr-FR';
         formData.append('locale', userLanguage);
         return http.post(this.$i18n.t('rootURL') + this.$i18n.t('createPackageUrl'), formData, { headers: { acept: 'application/json','Content-type': 'multipart/form-data' } })
            .then(response => {
                this.$store.commit('updatePackage', response.data);
                if(response.status == '200'){
                    this.$router.push('/paymentPage');
                }
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
  height: 87%;
  display: flex;
  justify-content: center;
  align-items: center;
  overflow: scroll;
}
.packege_creation_main .package-form {
  width: 75%;
  min-height: 70%;
  padding: 0 20px;
  margin: 0 auto;
  border-radius: 5px;
  background: linear-gradient(0.25turn, #ffffff, #ffffff79, #ffffff0c), no-repeat url('../assets/box 2.png') right -160px bottom 50%;
  box-shadow: rgba(0, 0, 0, 0.24) 0px 3px 8px;
}
.packege_creation_main .package-form button{
  margin-bottom: 20px;
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
  .packege_creation_main {
    align-items: baseline;
  }
  .input_container input{
    width: 83%;
  }
  .packege_creation_main .package-form {
    margin: 50px auto;
  }
}
@media screen and (max-width: 600px) {
  
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