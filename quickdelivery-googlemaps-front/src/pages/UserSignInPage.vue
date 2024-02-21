<template>
    <div class="packege_creation_main">
        <div class="package-form">
            <Form @submit="submitForm" ref="userCreationForm">
                <div class="components" >
                    <UserInfo ref="userInfo" v-if="currentStep === 1"/>
                </div>
                <div class="components" >
                    <UserVehicleInfo ref="vehicleInfo" v-if="currentStep === 2"/>
                </div>

                <br/>

            </Form>
        <button class="primary_btn" @click="previousStep">{{$t('packagePreviousAction')}}</button>&nbsp;
    <button class="primary_btn" @click="nextStep">{{$t('packageNextAction')}}</button>
        </div>
    </div>
</template>

<script>
import UserInfo from '../components/UserInfo.vue';
import UserVehicleInfo from '../components/UserVehicleInfo.vue';
import { Form } from 'vee-validate';

export default {
  components: {
    UserInfo,
    UserVehicleInfo,
    Form,
  },
  data() {
    return {
      currentStep: 1,
    };
  },
  methods: {
    nextStep() {
        console.log('next');
        if (this.currentStep < 3) {
            this.currentStep++;
        }
    },
    previousStep() {
      console.log('previousS');
      if (this.currentStep > 1) {
        this.currentStep--;
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