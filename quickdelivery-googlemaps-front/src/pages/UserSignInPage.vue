<template>
    <div class="packege_creation_main">
        <div class="package-form">
            <Form @submit="submitForm" ref="userCreationForm">
                <div class="components" >
                    <UserInfo ref="userInfo" v-if="currentStep === 1"/>
                </div>
                <div class="components" >
                    <UserDocuments v-if="currentStep === 2"/>
                </div>
                <div class="components" >
                    <UserVehicleInfo ref="vehicleInfo" v-if="currentStep === 3"/>
                </div>
                <div class="components" v-if="currentStep === 4">
                    <UserSummary />
                </div>
                <br/>
                <button class="primary_btn" v-if="currentStep > 1" @click="previousStep">{{$t('packagePreviousAction')}}</button>&nbsp;
                <button class="primary_btn" type="submit"><span v-if="currentStep < 3">{{$t('packageNextAction')}}</span><span v-if="currentStep === 3">{{$t('packageSummaryAction')}}</span><span v-if="currentStep === 4">{{$t('packageCreateAction')}}</span></button>
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
                const validAddress = validateAddress(addressAuto.address);
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
      if (this.currentStep > 1) {
        this.currentStep--;
      }
    },
    async submitForm() {
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
.packege_creation_main{
  padding: 10px;
  height: 700px;
  display: flex;
  justify-content: center;
  align-items: center;
}
.packege_creation_main .package-form {
  width: 75%;
  height: max-content;
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