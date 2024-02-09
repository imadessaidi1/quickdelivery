<template>
    <div class="input_container">
      <div class="input_only">
        <label for="firstName">{{$t('packageAddressFirstName')}}:</label>
        <Field id="firstName" v-model="address.firstName" name="address.firstName" :rules="validateString"/>
        <ErrorMessage class="errorMessage" name="address.firstName" />
      </div>
      <div class="input_only">
        <label for="lastName">{{$t('packageAddressLastName')}}:</label>
        <Field id="lastName" type="text" v-model="address.lastName" name="address.lastName" :rules="validateString"/>
          <ErrorMessage class="errorMessage" name="address.lastName" />
      </div>
    </div>
    <div class="input_container">
      <div class="input_only">
        <label for="email">{{$t('packageAddressEmail')}}:</label>
        <Field id="email" type="email" v-model="address.email" name="address.email" :rules="validateEmail"/>
          <ErrorMessage class="errorMessage" name="address.email" />
      </div>
      <div class="input_only">
        <label for="phone">{{$t('packageAddressPhone')}}:</label>
        <Field id="phone" type="text" v-model="address.phone" name="address.phone" :rules="validatePhone"/>
          <ErrorMessage class="errorMessage" name="address.phone" />
      </div>
    </div>
    <div>
      <label for="address">{{$t('packageAddressAddress')}}:</label>
        <AddressAutocomplete id="address" ref="addressAutoComplete"/>
        <span v-if="isAddressError" class="errorMessage">{{errorAddressMessage}}</span>
    </div>
    <div class="input_container">
        <div class="input_only">
            <label for="floor">{{$t('packageAddressFloor',{ state: $t(addressType === 'DEPARTURE' ? 'packageAddressFloorStatePickup' : 'packageAddressFloorStateDelivery') })}}:</label>
            <Field id="floor" type="number" v-model="address.floor" name="address.floor" :rules="validateNumericFieldAcceptZero"/>
            <ErrorMessage class="errorMessage" name="address.floor" />
        </div>
    </div>
    <div class="input_container">
        <div class="input_only">
            <label for="dateTime">{{$t('packageAddressDepartureTime',{ state: $t(addressType === 'DEPARTURE' ? 'packageAddressFloorStatePickup' : 'packageAddressFloorStateDelivery') })}}:</label>
            <VueDatePicker id="dateTime" v-model="address.dateTime" time-picker-inline :min-date="minDate" :max-date="maxDate"/>
        </div>
    </div>
</template>

<script>
import AddressAutocomplete from './AddressAutocomplete.vue';
import { Field, ErrorMessage } from 'vee-validate';
import { validatePhone, validateEmail, validateString, validateNumericFieldAcceptZero } from '@/config/comonFunction';
import VueDatePicker from '@vuepic/vue-datepicker';
import '@vuepic/vue-datepicker/dist/main.css';

export default {
  components: {
    AddressAutocomplete,
    Field,
    ErrorMessage,
    VueDatePicker,
  },
  props: {
    addressType: null,
  },
  data() {
      return {
        isAddressError: false,
        errorAddressMessage: null,
      };
    },
  computed: {
    address() {
          if(this.addressType === "ARRIVAL"){
            return this.$store.state.package_.addresses[1];
          }else{
            return this.$store.state.package_.addresses[0];
          }
    },
    minDate() {
        const d = new Date()
        d.setDate(d.getDate() + 1)
        return d
    },
    maxDate() {
        const d = new Date()
        d.setDate(d.getDate() + 7)
        return d
    },
  },
  methods: {
    validatePhone,
    validateEmail,
    validateString,
    validateNumericFieldAcceptZero,
  },
};
</script>

<style scoped>
.package-address {
  display: flex;
  flex-wrap: wrap;
}
.address-item {
  border: 1px solid #ddd;
  padding: 10px;
  margin-bottom: 10px;
}
.column-half {
  width: 50%;
  box-sizing: border-box;
  float: left;
}
.column-half:nth-child(even) {
  clear: both;
}

label {
  display: block;
  margin-bottom: 5px;
}
.invalid-field {
  border: 1px solid red;
}
</style>