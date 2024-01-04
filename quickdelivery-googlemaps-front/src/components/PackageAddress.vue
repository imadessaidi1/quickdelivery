<template>
      <div>
        <label for="firstName">{{$t('packageAddressFirstName')}}:</label>
        <input id="firstName" v-model="address.firstName" :required="true" />
      </div>
      <div>
        <label for="lastName">{{$t('packageAddressLastName')}}:</label>
        <input id="lastName" type="text" v-model="address.lastName" :required="true" />
      </div>
        <div>
          <label for="email">{{$t('packageAddressEmail')}}:</label>
          <input id="email" type="email" v-model="address.email" :required="true" />
        </div>
        <div>
          <label for="phone">{{$t('packageAddressPhone')}}:</label>
          <input id="phone" type="text" v-model="address.phone" :required="true" />
        </div>
      <div>
        <label for="address">{{$t('packageAddressAddress')}}:</label>
          <AddressAutocomplete id="address" ref="addressAutoComplete"/>
      </div>
</template>

<script>
import AddressAutocomplete from './AddressAutocomplete.vue';
import useValidate from '@vuelidate/core';
import { required } from '@vuelidate/validators';

export default {
  components: {
    AddressAutocomplete,
  },
  props: {
    addressType: null,
  },
  computed: {
    address() {
    if(this.addressType === "ARRIVAL"){
        return this.$store.state.package_.addresses[1];
      }else{
        return this.$store.state.package_.addresses[0];
      }
    },
  },
  data() {
    return {
      v$: useValidate(),
    };
  },
  validations() {
      return {
        address: {
          firstName: { required },
          lastName: { required },
          type: { required },
        },
      }
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