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
                <button class="primary_btn" @click="previousStep" v-if="currentStep > 1">{{$t('packagePreviousAction')}}</button>&nbsp;
                <button class="primary_btn" type="submit"><span v-if="currentStep < 3">{{$t('packageNextAction')}}</span><span v-if="currentStep === 3">{{$t('packageSummaryAction')}}</span><span v-if="currentStep === 4">{{$t('userCreateAction')}}</span></button>
            </Form>
        </div>
    </div>
</template>

<script>
import UserInfo from '../components/UserInfo.vue';
import UserVehicleInfo from '../components/UserVehicleInfo.vue';
import UserDocuments from '../components/UserDocuments.vue';
import UserSummary from '../components/UserSummary.vue';

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
         console.log('submiting ...');
     }else{
        this.nextStep();
     }
    },
  },
};
</script>
<style>
.user_creation_main{
  padding: 10px;
  height: 84%;
  display: flex;
  justify-content: center;
  overflow: scroll;
}
.user_creation_main .user-form {
  width: 60%;
  height: max-content;
  padding: 20px;
  margin: auto auto;
  border-radius: 5px;
  background: linear-gradient(0.25turn, #ffffff, #ffffff40), no-repeat url('../assets/avatar.png') right -180px bottom 50%;
  box-shadow: rgba(0, 0, 0, 0.24) 0px 3px 8px;
}
.user_creation_main .user-form form{
  width: 100%;
}
.user_creation_main .user-form form .components,
.user_creation_main .user-form form .summary_component{
  width: 75%;
}
.user_creation_main .user-form form .components h2{
  border-left: solid 5px #467fd0;
  padding-left: 8px;
  margin-left: 20px;
}
.user_creation_main .user-form .package-address {
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
  .user_creation_main .user-form form .summary_component{
    width: 100%;
  }
  .user_creation_main .user-form form .components{
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
  .user_creation_main{
    height: max-content;
  }
  .usercreation_main .user-form {
    margin: 25px auto;
  }
}
@media screen and (max-width: 600px) {
  .user_creation_main{
    height: max-content;
  }
  .user_creation_main .user-form {
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