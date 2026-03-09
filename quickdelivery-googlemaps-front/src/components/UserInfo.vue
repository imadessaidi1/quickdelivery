<template>
    <div>
        <h2>User Informations</h2>

        <div class="input_container">
            <div class="input_only">
                <label for="firstName">{{$t('packageAddressFirstName')}}:</label>
                <Field id="firstName" v-model="user.firstName" name="firstName" :rules="validateString"/>
                <ErrorMessage class="errorMessage" name="firstName" />
            </div>
            <div class="input_only">
                <label for="lastName">{{$t('packageAddressLastName')}}:</label>
                <Field id="lastName" type="text" v-model="user.lastName" name="lastName" :rules="validateString"/>
                <ErrorMessage class="errorMessage" name="lastName" />
            </div>
            <div class="input_only">
                <label for="sex">{{$t('userGender')}}:</label>
                <select id="sex" v-model="user.sex">
                    <option value="MAL">Mal</option>
                    <option value="FEMALE">Female</option>
                    <option value="OTHER">Other</option>
                </select>
                <ErrorMessage class="errorMessage" name="sex" />
            </div>
            <div class="input_only">
                <label for="age">{{$t('userBirthDate')}}:</label>
                <!--<Field id="age" type="number" v-model="user.age" name="age" :rules="validateNumericField"/>-->
                <VueDatePicker placeholder="Select Date" id="dateTime" v-model="user.birthDate" :flow="flow" :enable-time-picker="false"/>

                <ErrorMessage class="errorMessage" name="age" />
            </div>
            <div class="input_only">
                <label for="email">{{$t('packageAddressEmail')}}:</label>
                <Field id="email" type="email" v-model="user.emailAddress" name="email" :rules="validateEmail"/>
                <ErrorMessage class="errorMessage" name="email" />
                <span v-if="isExistingEmail" class="errorMessage">{{existingEmailErrorMessage}}</span>
            </div>

        </div>
        <div class="input_container">
            <div class="input_only">
                <label for="emailAddressConfirmation">{{$t('userEmailConfirmation')}}:</label>
                <Field id="emailAddressConfirmation" type="email" v-model="user.emailAddressConfirmation" name="emailAddressConfirmation" :rules="validateEmail"/>
                <ErrorMessage class="errorMessage" name="emailAddressConfirmation" />
                <span v-if="isEmailConfirmationError" class="errorMessage">{{emailConfirmationErrorMessage}}</span>
            </div>
            <div class="input_only">
                <label for="phone">{{$t('packageAddressPhone')}}:</label>
                <Field id="phone" type="text" v-model="user.phone" name="phone" :rules="validatePhone"/>
                <ErrorMessage class="errorMessage" name="phone" />
            </div>
            <div class="input_only">
                <label for="phoneConfirmation">{{$t('userPhoneConfirmation')}}:</label>
                <Field id="phoneConfirmation" type="text" v-model="user.phoneConfirmation" name="phoneConfirmation" :rules="validatePhone"/>
                <ErrorMessage class="errorMessage" name="phoneConfirmation" />
                <span v-if="isPhoneConfirmationError" class="errorMessage">{{phoneConfirmationErrorMessage}}</span>
            </div>
            <div class="input_only" v-show="!isForUpdate">
                <label for="password">{{$t('userPassword')}}:</label>
                <Field id="password" type="password" v-model="user.password" name="password" :rules="validatePassword"/>
                <ErrorMessage class="errorMessage" name="password" />
            </div>
            <div class="input_only" v-show="!isForUpdate">
                <label for="passwordConfirmation">{{$t('userPasswordConfirmation')}}:</label>
                <input id="passwordConfirmation" type="password" v-model="user.passwordConfirmation" name="passwordConfirmation"/>
                <span v-if="isPasswordConfirmationError" class="errorMessage">{{passwordConfirmationErrorMessage}}</span>
            </div>
        </div>
        <div class="input_container">

        </div>
        <div>
            <label for="address">{{$t('packageAddressAddress')}}:</label>
            <AddressAutocomplete id="address" ref="addressAutoComplete" :existingAddress="user.addressAuto"/>
            <br/><span><strong class="file_name">{{ user.addressAuto }}</strong></span>
            <span v-if="isAddressError" class="errorMessage" >{{errorAddressMessage}}</span>
        </div>

    </div>
</template>
<script>
import { Field, ErrorMessage } from 'vee-validate';
import { validatePhone, validateEmail, validateString, validatePassword, validateNumericField } from '@/config/comonFunction';
import AddressAutocomplete from './AddressAutocomplete.vue';
import VueDatePicker from '@vuepic/vue-datepicker';
import { ref } from 'vue';
export default {
  components: {
    AddressAutocomplete,
    Field,
    ErrorMessage,
    VueDatePicker,
  },
  computed: {
    user() {
      return this.$store.state.user;
    },
  },
  props: {
    isForUpdate: ref(false),
  },
  data() {
    return {
      selectedPaymentType: 'CARD',
      isAddressError: false,
      errorAddressMessage:'',
      isPasswordConfirmationError: false,
      passwordConfirmationErrorMessage: '',
      isExistingEmail: false,
      existingEmailErrorMessage: '',
      isEmailConfirmationError: false,
      emailConfirmationErrorMessage: '',
      isPhoneConfirmationError: false,
      phoneConfirmationErrorMessage: '',
      flow: ref(['month', 'year', 'calendar']),
    };
  },
  methods: {
    validatePhone,
    validateEmail,
    validateString,
    validatePassword,
    validateNumericField,
  },
}
</script>
<style>
.user-payment-method {
  margin-bottom: 20px;
  display: flex;
  align-items: center;
}
</style>
