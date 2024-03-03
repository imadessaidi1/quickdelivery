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
    };
  },
  methods: {
    validatePasswordConfirmation,
    validateAddress,
    nextStep() {
        if (this.currentStep < 4) {
            if(this.currentStep === 1){
                const userInfo = this.$refs.userInfo;
                const addressAuto = userInfo.$refs.addressAutoComplete;
                console.log(addressAuto.address);
                userInfo.user.addressAuto = addressAuto.address;
                const validAddress = validateAddress(userInfo.user.addressAuto);
                const validPasswordConfirm = validatePasswordConfirmation(userInfo.user.password, userInfo.user.passwordConfirmation);
                if(!validPasswordConfirm){
                    userInfo.isPasswordConfirmationError = true;
                    userInfo.passwordConfirmationErrorMessage = this.$i18n.t('mandatoryField')+this.$i18n.t('PasswordConfirmation');
                }else{
                    userInfo.isPasswordConfirmationError = false;
                }
                if(!validAddress){
                    userInfo.isAddressError = true;
                    userInfo.errorAddressMessage=this.$i18n.t('mandatoryField')+this.$i18n.t('invalidAddress');
                }else{
                    userInfo.isAddressError = false;
                }
                if(validPasswordConfirm && validAddress){
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
        console.log('previousStep '+this.currentStep);
        this.currentStep--;
    },
    async submitFormUser() {
     if(this.currentStep === 4){
         const formData = new FormData();
         formData.append('user', JSON.stringify(this.$store.state.user));
         formData.append('files', this.$store.state.userDocuments);
         formData.append('vehicle', JSON.stringify(this.$store.state.vehicle));
         formData.append('files', this.$store.state.vehicleDocuments);
         const userLanguage = navigator.languages && navigator.languages.length ? navigator.languages[0] : navigator.language || 'fr-FR';
         formData.append('locale', userLanguage);
         return http.post(this.$i18n.t('userRootURL') + this.$i18n.t('createUser'), formData, { headers: { acept: 'application/json','Content-type': 'multipart/form-data' } })
            .then(response => {
                console.log('success'+response);
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