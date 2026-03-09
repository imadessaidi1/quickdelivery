<template>
    <div class="user_creation_main">
        <div class="user-form">
            <Form @submit="submitFormUser" ref="userCreationForm">
                <div class="wizard-steps">
                    <div class="step" :class="stepClass(1)">{{ $t('wizardUserStepInfo') }}</div>
                    <div class="step" :class="stepClass(2)">{{ $t('wizardUserStepDocs') }}</div>
                    <div class="step" :class="stepClass(3)">{{ $t('wizardUserStepVehicle') }}</div>
                    <div class="step" :class="stepClass(4)">{{ $t('wizardUserStepSummary') }}</div>
                </div>
                <div class="components" v-if="currentStep === 1">
                    <UserInfo ref="userInfo" :isForUpdate="id"/>
                </div>
                <div class="components" v-if="currentStep === 2">
                    <UserDocuments ref="userDocuments" :isForUpdate="id"/>
                </div>
                <div class="components" v-if="currentStep === 3">
                    <UserVehicleInfo ref="vehicleInfo" :isForUpdate="id"/>
                </div>
                <div class="components" v-if="currentStep === 4">
                  <h2>{{$t('packageSummaryAction')}}</h2>
                    <UserSummary ref="userSummary" />
                </div>
                <br/>
                <button class="btn primary_btn" type="button" @click="previousStep" v-show="currentStep > 1">{{$t('packagePreviousAction')}}</button>
                <button class="btn primary_btn" type="submit" v-show="currentStep < 3">{{$t('packageNextAction')}}</button>
                <button class="btn primary_btn" type="submit" v-show="currentStep === 3">{{$t('packageSummaryAction')}}</button>
                <button class="btn primary_btn" type="submit" v-show="currentStep === 4">{{$t('userCreateAction')}}</button>
            </Form>
        </div>
    </div>
</template>

<script>
import UserInfo from '../components/UserInfo.vue';
import UserVehicleInfo from '../components/UserVehicleInfo.vue';
import UserDocuments from '../components/UserDocuments.vue';
import UserSummary from '../components/UserSummary.vue';
import http from '@/config/httpInterceptor';
import { Form } from 'vee-validate';
import { validatePasswordConfirmation, validateAddress, validateEmailConfirmation, validatePhoneConfirmation, validateFileInput } from '@/config/comonFunction';

export default {
  components: {
    UserInfo,
    UserVehicleInfo,
    Form,
    UserDocuments,
    UserSummary,
  },
  props: {
    id: String
  },
  data() {
    return {
      currentStep: 1,
      isForUpdate: false,
      emptyUser: {
                id: null,
                version: null,
                type: 'DELIVERY_PERSON',
                firstName: '',
                lastName: '',
                age: null,
                birthDate: null,
                sex: '',
                emailAddress: '',
                emailAddressValidation: false,
                phone: '',
                phoneValidation: false,
                activeAccount: false,
                password: '',
                passwordConfirmation: '',
                emailAddressConfirmation: '',
                phoneConfirmation:'',
                addressAuto: '',
                personalAddress: [
                  {
                    id: null,
                    version: null,
                    firstName: '',
                    lastName: '',
                    line1: '',
                    line2: '',
                    town: '',
                    zipCode: '',
                    country: '',
                    floor: 0,
                    dateTime: null,
                    type: 'RESIDENCE',
                    latitude: 0,
                    longitude: 0,
                    email: '',
                    phone: ''
                  }
                ],
                documents: [],
                paymentModes: {
                    "CREDIT_CARD": {
                                     cardNumber: '',
                                     expiryDate: '',
                                     cvv: '',
                                    },
                    "IBAN": {
                             iban: '',
                             bic: '',
                            },
                },
      },
      emptyVehicle: {
                  registrationNumber: '',
                  brand: '',
                  model: '',
                  energyType: '',
                  vehicleDocuments: []
              },
    };
  },
   mounted() {
    if(this.id){
        this.isForUpdate = true;
        http.get(this.$i18n.t('userRootURL') + this.$i18n.t('getUserByEmail')+this.id)
          .then(response => {
            this.$store.commit('updateUser', response.data);
            this.$store.commit('updateVehicle', response.data.vehicles[0]);
            let userDocument = [];
            userDocument['ID'] = {name:'ID' , documentStatus: response.data.document['ID'].documentStatus};
            userDocument['PICTURE'] = {name:'PICTURE' , documentStatus: response.data.document['PICTURE'].documentStatus};
            userDocument['DRIVER_LICENCE'] = {name:'DRIVER_LICENCE' , documentStatus: response.data.document['DRIVER_LICENCE'].documentStatus};
            userDocument['USER_COMPANY_EXTRACT'] = {name:'USER_COMPANY_EXTRACT' , documentStatus: response.data.document['USER_COMPANY_EXTRACT'].documentStatus};
            userDocument['USER_COMPANY_INSURANCE'] = {name:'USER_COMPANY_INSURANCE' , documentStatus: response.data.document['USER_COMPANY_INSURANCE'].documentStatus};
            if(response.data.document['RIB']){
                userDocument['RIB'] = {name:'BANK ID' , documentStatus: response.data.document['RIB'].documentStatus};
            }
            this.$store.commit('updateUserDocuments', userDocument);
            let vehicleDocuments = [];
            vehicleDocuments['GRAY_CARD'] = {name:'GRAY_CARD' , documentStatus: response.data.document['GRAY_CARD'].documentStatus};
            vehicleDocuments['INSURANCE'] = {name:'INSURANCE' , documentStatus: response.data.document['INSURANCE'].documentStatus};
            this.$store.commit('updateVehicleDocuments', vehicleDocuments);
        }).catch(() => {
          console.error("Unable to process your request this time. Please try again later.");
        });
    }else{
        this.isForUpdate = false;
    }
  },
  methods: {
    stepClass(stepNumber) {
        if (this.currentStep === stepNumber) {
            return 'active';
        }
        if (this.currentStep > stepNumber) {
            return 'done';
        }
        return '';
    },
    validatePasswordConfirmation,
    validateEmailConfirmation,
    validateAddress,
    validatePhoneConfirmation,
    validateFileInput,
    async nextStep() {
        if (this.currentStep < 4) {
            if(this.currentStep === 1){
                const userInfo = this.$refs.userInfo;
                const addressAuto = userInfo.$refs.addressAutoComplete;
                if(!this.isForUpdate){
                    userInfo.user.addressAuto = addressAuto.address;
                }
                let validAddress = false;
                let existingEmail = false
                if(!this.isForUpdate){
                    validAddress = validateAddress(userInfo.user.addressAuto);
                    existingEmail = await this.existingEmail(userInfo.user.emailAddress);
                }else{
                    validAddress=true;
                    existingEmail = false;
                }
                const validPasswordConfirm = validatePasswordConfirmation(userInfo.user.password, userInfo.user.passwordConfirmation);
                const validEmailConfirmation = validateEmailConfirmation(userInfo.user.emailAddress, userInfo.user.emailAddressConfirmation);
                const validPhoneConfirmation = validatePhoneConfirmation(userInfo.user.phone, userInfo.user.phoneConfirmation);

                if(!validPasswordConfirm){
                    userInfo.isPasswordConfirmationError = true;
                    userInfo.passwordConfirmationErrorMessage = this.$i18n.t('mandatoryField')+this.$i18n.t('PasswordConfirmation');
                }else{
                    userInfo.isPasswordConfirmationError = false;
                }
                if(existingEmail){
                    userInfo.isExistingEmail = true;
                    userInfo.existingEmailErrorMessage = this.$i18n.t('ExistingEmail');
                }
                else{
                    userInfo.isExistingEmail = false;
                }
                if(!validEmailConfirmation){
                    userInfo.isEmailConfirmationError = true;
                    userInfo.emailConfirmationErrorMessage = this.$i18n.t('mandatoryField')+this.$i18n.t('emailConfirmation');
                }else{
                    userInfo.isEmailConfirmationError = false;
                }
                if(!validPhoneConfirmation){
                    userInfo.isPhoneConfirmationError = true;
                    userInfo.phoneConfirmationErrorMessage = this.$i18n.t('mandatoryField')+this.$i18n.t('phoneConfirmation');
                }else{
                    userInfo.isPhoneConfirmationError = false;
                }
                if(!validAddress){
                    userInfo.isAddressError = true;
                    userInfo.errorAddressMessage=this.$i18n.t('mandatoryField')+this.$i18n.t('invalidAddress');
                }else{
                    userInfo.isAddressError = false;
                }
                if(validPasswordConfirm && validAddress && !existingEmail){
                    userInfo.isAddressError = false;
                    userInfo.isPasswordConfirmationError = false;
                    if(addressAuto.address && addressAuto.address.includes(',')){
                        const address = addressAuto.address.split(',');
                        userInfo.user.personalAddress[0].line1 = address[0].trim();
                        userInfo.user.personalAddress[0].zipCode = address[1].trim().split(' ')[0];
                        let index = address[1].trim().indexOf(' ');
                        if (index !== -1) {
                            userInfo.user.personalAddress[0].town = address[1].substring(index + 1);
                        }
                        userInfo.user.personalAddress[0].country = address[2].trim();
                    }
                    this.$store.commit('updateUser', userInfo.user);
                    this.currentStep++;
                }
            }else if(this.currentStep === 2){
                const selectedFilesKeys = [
                  'ID',
                  'DRIVER_LICENCE',
                  'USER_COMPANY_EXTRACT',
                  'USER_COMPANY_INSURANCE',
                  'PICTURE',
                ];
                const userDocs = this.$refs.userDocuments;
                if(userDocs.selectedPaymentType === 'IBAN'){
                    selectedFilesKeys.push('RIB');
                }
                const fileValidation = validateFileInput(selectedFilesKeys,this.$store.state.userDocuments);
                userDocs.filesErrorMessages = [];
                if(fileValidation && fileValidation.length > 0){
                    fileValidation.forEach(result => {
                          userDocs.filesErrorMessages[result.missingKey] = this.$i18n.t('fileRequired');
                    });
                }else{
                    this.currentStep++;
                }
            }else if(this.currentStep === 3){
                const selectedFilesKeys = [
                  'GRAY_CARD',
                  'INSURANCE'
                ];
                const vehicleDocs = this.$refs.vehicleInfo;
                const fileValidation = validateFileInput(selectedFilesKeys,this.$store.state.vehicleDocuments);
                vehicleDocs.filesErrorMessages = [];
                if(fileValidation && fileValidation.length > 0){
                    fileValidation.forEach(result => {
                          vehicleDocs.filesErrorMessages[result.missingKey] = this.$i18n.t('fileRequired');
                    });
                }else{
                    this.currentStep++;
                }
            }else{
                this.currentStep++;
            }
        }
    },
    previousStep() {
        if (this.currentStep > 1) {
            this.currentStep--;
        }
    },
    existingEmail(email) {
        return new Promise((resolve) => {
            http.get(`${this.$i18n.t('userRootURL')}${this.$i18n.t('getUserByEmail')}${email}`)
                .then(response => {
                    if (response.status === 200 && response.data) {
                        resolve(true);
                    } else {
                        resolve(false);
                    }
                })
                .catch(error => {
                    console.error('Erreur lors de la requête API', error);
                    resolve(false);
                });
        });
    },

    async submitFormUser() {
     if(this.currentStep === 4){
         const formData = new FormData();
         formData.append('user', JSON.stringify(this.$store.state.user));
         const entries = Object.entries(this.$store.state.userDocuments);
         if(this.isForUpdate){
             entries.forEach(([key, value]) => {
                if(value.documentStatus === 'UPDATED'){
                    formData.append(key, value.file);
                }
             });
         }else{
             entries.forEach(([key, value]) => {
                formData.append(key, value.file);
             });
         }
         formData.append('vehicle', JSON.stringify(this.$store.state.vehicle));
         const entriesV = Object.entries(this.$store.state.vehicleDocuments);
         if(this.isForUpdate){
             entriesV.forEach(([key, value]) => {
                if(value.documentStatus === 'UPDATED'){
                    formData.append(key, value.file);
                }
             });
         }else{
             entriesV.forEach(([key, value]) => {
                formData.append(key, value.file);
             });
         }
         const userLanguage = navigator.languages && navigator.languages.length ? navigator.languages[0] : navigator.language || 'fr-FR';
         formData.append('locale', userLanguage);
         let url;
         if(this.isForUpdate){
            url = this.$i18n.t('userRootURL') + this.$i18n.t('updateUser');
         }else{
            url = this.$i18n.t('userRootURL') + this.$i18n.t('createUser');
         }
         return http.post(url, formData, { headers: { acept: 'application/json','Content-type': 'multipart/form-data' } })
            .then(response => {
                if(response.status == '200'){
                   this.$store.commit('updateUser', this.emptyUser);
                   this.$store.commit('updateVehicle', this.emptyVehicle);
                   this.$store.commit('updateUserDocuments', []);
                   this.$store.commit('updateVehicleDocuments', []);
                   this.$router.push('/');
                }
            }).catch(() => {
                console.error("Unable to process your request this time. Please try again later.");
            });
     }else{
        this.nextStep();
     }
    },
  },
};
</script>
<style>
.user_creation_main{
  height: 100%;
  display: flex;
  justify-content: center;
  align-items: center;
  overflow: scroll;
  background-color: #F9F7F7;
}
.user_creation_main .user-form {
  width: 75%;
  min-height: 70%;
  padding: 0 20px;
  margin: 20px auto;
  border-radius: 5px;
  background: no-repeat url('../assets/avatar.png') right -160px bottom 50%, #ffffffe1;
  box-shadow: rgba(0, 0, 0, 0.24) 0px 3px 8px;
}
.file_name{
  font-size: 13px;
  display: block;
  padding-left: 15px;
}
.file_name::before{
  width: 180px;
  height: 18px;
  background-image: url('../assets/check.png');
}
.input_only{
  margin-top: 15px;
  height: max-content;
}
.user_creation_main .user-form button{
  margin-bottom: 20px;
}
.user_creation_main .user-form form{
  width: 100%;
}
.wizard-steps {
  display: flex;
  gap: 8px;
  flex-wrap: wrap;
  margin: 16px 0 10px 20px;
}
.wizard-steps .step {
  padding: 6px 10px;
  border-radius: 18px;
  border: 1px solid #d0d7e2;
  background: #f4f7fb;
  color: #516074;
  font-size: 12px;
}
.wizard-steps .step.active {
  background: #e8f2ff;
  border-color: #70a6e8;
  color: #1f4f89;
  font-weight: 700;
}
.wizard-steps .step.done {
  background: #edf8f2;
  border-color: #75c79c;
  color: #1f7a4e;
}
.user_creation_main .user-form form .components,
.user_creation_main .user-form form .summary_component{
  width: 75%;
}
.user_creation_main .user-form form .components h2{
  border-left: solid 5px #004f87;
  padding-left: 8px;
  margin-left: 20px;
}
.user_creation_main .user-form .package-address {
  display: flex;
  flex-wrap: wrap;
}
.small_width{
  display: none;
}
.user-form .primary_btn{
  margin-bottom: 20px;
  margin-left: 10px;
}
@media screen and (max-width: 1500px){
  .user_creation_main .user-form form .summary_component{
    width: 100%;
  }
  .user_creation_main .user-form form .components{
    width: 100%;
  }
}
@media screen and (max-width: 1100px) {
  .user_creation_main {
    align-items: baseline;
  }
  .user_creation_main .user-form {
    margin: 50px auto;
  }
}
@media screen and (max-width: 600px) {
  
  .user_creation_main .user-form {
    width: 75%;
    padding: 30px;
  }
}
</style>
