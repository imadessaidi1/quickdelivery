<template>
    <div class="user_creation_main">
        <div class="user-form">
            <Form @submit="submitFormUser" ref="userCreationForm">
                <div class="components" v-if="currentStep === 1">
                    <UserInfo ref="userInfo"/>
                </div>
                <div class="components" v-if="currentStep === 2">
                    <UserDocuments ref="userDocuments"/>
                </div>
                <div class="components" v-if="currentStep === 3">
                    <UserVehicleInfo ref="vehicleInfo" />
                </div>
                <div class="components" v-if="currentStep === 4">
                  <h2>{{$t('packageSummaryAction')}}</h2>
                    <UserSummary ref="userSummary" />
                </div>
                <br/>
                <button class="btn primary_btn" @click="previousStep" v-if="currentStep > 1">{{$t('packagePreviousAction')}}</button>&nbsp;
                <button class="btn primary_btn" type="submit"><span v-if="currentStep < 3">{{$t('packageNextAction')}}</span><span v-if="currentStep === 3">{{$t('packageSummaryAction')}}</span><span v-if="currentStep === 4">{{$t('userCreateAction')}}</span></button>
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
import { validatePasswordConfirmation, validateAddress } from '@/config/comonFunction';

export default {
  components: {
    UserInfo,
    UserVehicleInfo,
    Form,
    UserDocuments,
    UserSummary,
  },
  data() {
    return {
      currentStep: 1,
      emptyUser: {
                id: null,
                version: null,
                type: 'DELIVERY_PERSON',
                firstName: '',
                lastName: '',
                emailAddress: '',
                emailAddressValidation: false,
                phone: '',
                phoneValidation: false,
                activeAccount: false,
                password: '',
                passwordConfirmation: '',
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
  methods: {
    validatePasswordConfirmation,
    validateAddress,
    async nextStep() {
        if (this.currentStep < 4) {
            if(this.currentStep === 1){
                const userInfo = this.$refs.userInfo;
                const addressAuto = userInfo.$refs.addressAutoComplete;
                userInfo.user.addressAuto = addressAuto.address;
                const validAddress = validateAddress(userInfo.user.addressAuto);
                const validPasswordConfirm = validatePasswordConfirmation(userInfo.user.password, userInfo.user.passwordConfirmation);
                const existingEmail = await this.existingEmail(userInfo.user.emailAddress);
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
                if(!validAddress){
                    userInfo.isAddressError = true;
                    userInfo.errorAddressMessage=this.$i18n.t('mandatoryField')+this.$i18n.t('invalidAddress');
                }else{
                    userInfo.isAddressError = false;
                }
                if(validPasswordConfirm && validAddress && !existingEmail){
                    userInfo.isAddressError = false;
                    userInfo.isPasswordConfirmationError = false;
                    const address = addressAuto.address.split(',');
                    userInfo.user.personalAddress[0].line1 = address[0].trim();
                    userInfo.user.personalAddress[0].zipCode = address[1].trim().split(' ')[0];
                    let index = address[1].trim().indexOf(' ');
                    if (index !== -1) {
                        userInfo.user.personalAddress[0].town = address[1].substring(index + 1);
                    }
                    userInfo.user.personalAddress[0].country = address[2].trim();
                    this.$store.commit('updateUser', userInfo.user);
                    this.currentStep++;
                }
            }else if(this.currentStep === 2){
                this.currentStep++;
            }else{
                this.currentStep++;
            }
        }
    },
    previousStep() {
        this.currentStep--;
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
         entries.forEach(([key, value]) => {
            formData.append(key, value);
         });
         formData.append('vehicle', JSON.stringify(this.$store.state.vehicle));
         const entriesV = Object.entries(this.$store.state.vehicleDocuments);
         entriesV.forEach(([key, value]) => {
            formData.append(key, value);
         });
         const userLanguage = navigator.languages && navigator.languages.length ? navigator.languages[0] : navigator.language || 'fr-FR';
         formData.append('locale', userLanguage);
         return http.post(this.$i18n.t('userRootURL') + this.$i18n.t('createUser'), formData, { headers: { acept: 'application/json','Content-type': 'multipart/form-data' } })
            .then(response => {
                if(response.status == '200'){
                   this.$store.commit('updateUser', this.emptyUser);
                   this.$store.commit('updateVehicle', this.emptyVehicle);
                   this.$store.commit('updateUserDocuments', []);
                   this.$store.commit('updateVehicleDocuments', []);
                   this.$router.push('/');
                }
            }).catch(() => {
                console.log("unable to process your request this time. please try again latter.");
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
  height: 87%;
  display: flex;
  justify-content: center;
  align-items: center;
  overflow: scroll;
}
.user_creation_main .user-form {
  width: 75%;
  min-height: 70%;
  padding: 0 20px;
  margin: 0 auto;
  border-radius: 5px;
  background: linear-gradient(0.25turn, #ffffff, #ffffff79, #ffffff0c), no-repeat url('../assets/avatar.png') right -160px bottom 50%;
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
.user_creation_main .package-form button{
  margin-bottom: 20px;
}
.user_creation_main .package-form form{
  width: 100%;
}
.user_creation_main .package-form form .components,
.user_creation_main .package-form form .summary_component{
  width: 75%;
}
.user_creation_main .package-form form .components h2{
  border-left: solid 5px #ff7b00;
  padding-left: 8px;
  margin-left: 20px;
}
.user_creation_main .package-form .package-address {
  display: flex;
  flex-wrap: wrap;
}
.small_width{
  display: none;
}
.user-form .primary_btn{
  margin-bottom: 20px;
}
@media screen and (max-width: 1500px){
  .user_creation_main .package-form form .summary_component{
    width: 100%;
  }
  .user_creation_main .package-form form .components{
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