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
  data() {
    return {
      v$: useValidate(),
      address: {
        fullAddress:"",
        firstName: "",
        lastName: "",
        line1: "",
        line2: "",
        town: "",
        zipCode: "",
        country: "",
        email: "",
        phone: "",
        type: "ARRIVAL",
        latitude: 0,
        longitude: 0,
      },
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
  methods: {
    addAddress() {
    console.log(this.v$);
    if (!this.v$.address.$invalid){
        if(this.addresses.length < 2){
        const addressAuto=this.$refs.addressAutoComplete;
        const address = addressAuto.address.split(',');
        this.address.fullAddress = addressAuto;
        this.address.line1 = address[0].trim();
        this.address.zipCode = address[1].trim().split(' ')[0];
        this.address.town = address[1].trim().split(' ')[1];
        this.address.country = address[2].trim();
        const typeExists = this.addresses.some(address => address.type === this.address.type);
        const addressExists = this.addresses.some(address => address.fullAddress === this.address.fullAddress);
        const i18n = this.$i18n;
        if (typeExists) {
          alert(i18n.t('messageExistingAddressType'));
        }else if(addressExists){
          alert(i18n.t('messageExistingAddress'));
        }else{
          this.addresses.push({ ...this.address});
        }
        }else{
        alert('Vous ne pouvez pas ajouter plus de 2 adresses');
        }
      }
    },
    removeAddress(index) {
      this.addresses.splice(index, 1);
    },
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