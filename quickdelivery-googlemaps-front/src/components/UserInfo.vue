<template>
    <div>
        <h2>User Info.</h2>

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
        </div>
        <div class="input_container">
            <div class="input_only">
                <label for="email">{{$t('packageAddressEmail')}}:</label>
                <Field id="email" type="email" v-model="user.emailAddress" name="email" :rules="validateEmail"/>
                <ErrorMessage class="errorMessage" name="email" />
                <span v-if="isExistingEmail" class="errorMessage">{{existingEmailErrorMessage}}</span>
            </div>
            <div class="input_only">
                <label for="phone">{{$t('packageAddressPhone')}}:</label>
                <Field id="phone" type="text" v-model="user.phone" name="phone" :rules="validatePhone"/>
                <ErrorMessage class="errorMessage" name="phone" />
            </div>
        </div>
        <div>
            <label for="address">{{$t('packageAddressAddress')}}:</label>
            <AddressAutocomplete id="address" ref="addressAutoComplete" :existingAddress="user.addressAuto"/>
            <br/><span><strong class="file_name">{{ user.addressAuto }}</strong></span>
            <span v-if="isAddressError" class="errorMessage" >{{errorAddressMessage}}</span>
        </div>
        <div class="input_container">
            <div class="input_only">
                <label for="password">{{$t('userPassword')}}:</label>
                <Field id="password" type="password" v-model="user.password" name="password" :rules="validatePassword"/>
                <ErrorMessage class="errorMessage" name="password" />
            </div>
        </div>
        <div class="input_container">
            <div class="input_only">
                <label for="passwordConfirmation">{{$t('userPasswordConfirmation')}}:</label>
                <input id="passwordConfirmation" type="password" v-model="user.passwordConfirmation" name="passwordConfirmation"/>
                <span v-if="isPasswordConfirmationError" class="errorMessage">{{passwordConfirmationErrorMessage}}</span>
            </div>
        </div>
    </div>
</template>
<script>
import { Field, ErrorMessage } from 'vee-validate';
import { validatePhone, validateEmail, validateString, validatePassword } from '@/config/comonFunction';
import AddressAutocomplete from './AddressAutocomplete.vue';
export default {
  components: {
    AddressAutocomplete,
    Field,
    ErrorMessage,
  },
  computed: {
    user() {
      return this.$store.state.user;
    },
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
    };
  },
  methods: {
    validatePhone,
    validateEmail,
    validateString,
    validatePassword,
  },
}
</script>
<style>
.payment-method {
  margin-bottom: 20px;
  display: flex;
  align-items: center;
}
</style>