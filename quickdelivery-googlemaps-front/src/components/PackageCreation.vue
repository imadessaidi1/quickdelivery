<!-- PackageForm.vue -->

<template>
    <div>
        <label for="height">{{$t('packageHeight')}}:</label>
        <input id="height" type="number" v-model="package_.height" required="true" />

      </div>
      <div>
        <label for="width">{{$t('packageWidth')}}:</label>
        <input id="width" type="number" v-model="package_.width" required="true" />
      </div>
      <div>
        <label for="depth">{{$t('packageDepth')}}:</label>
        <input id="depth" type="number" v-model="package_.depth" required="true" />
      </div>
      <div>
        <label for="weight">{{$t('packageWeight')}}:</label>
        <input id="weight" type="number" v-model="package_.weight" required="true" />
      </div>
</template>
<script>

export default {
  data() {
    return {
      package_: {
        height: 0,
        width: 0,
        weight: 0,
        depth: 0,
        pictureURL: '',
        senderID: 905,
        addresses: [{
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
        type: "DEPARTURE",
        latitude: 0,
        longitude: 0,
      },
      {
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
      }],
        documentS: [],
      },
    };
  },
  methods: {
    async submitForm() {
      const i18n = this.$i18n;
      const addressListComponent=this.$refs.addressList;
      this.package_.addresses = addressListComponent.addresses;
      console.log(this.package_);
      const requestOptions = {
          method: "POST",
          headers: { "Content-Type": "application/json" },
          body: JSON.stringify(this.package_)
      };
      console.log(i18n.t('rootURL') + i18n.t('createPackageUrl'));
      const response = await fetch(i18n.t('rootURL') + i18n.t('createPackageUrl'), requestOptions);
      const data = await response.json();
      console.log(data);
    },
  },
};
</script>

<style>

label {
  display: block;
  margin-bottom: 5px;
}
</style>
