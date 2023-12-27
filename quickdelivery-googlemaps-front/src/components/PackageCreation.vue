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
      <div>
        <label for="pictureFile">{{$t('packagePicture')}} :</label>
        <input  ref="fileInput0"
                :id="pictureFile"
                type="file"
                accept="image/*, application/pdf"
        @change="handleFileChange(0)"
        />
        <label for="documentFile">{{ $t('packageInvoice') }} :</label>
        <input  ref="fileInput1"
                :id="documentFile"
                type="file"
                accept="image/*, application/pdf"
                @change="handleFileChange(1)"
        />
      </div>
</template>
<script>

export default {
  data() {
    return {
      package_: {
        id: null,
        version: null,
        creationDate: null,
        height: 0,
        width: 0,
        depth: 0,
        weight: 0,
        pictureURL: "",
        status: "",
        deliveryPrice: null,
        senderID: 905,
        packageReservations: [],
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
      lastPositionLatitude: null,
      lastPositionLongitude: null
      },
      documentS: [],
    };
  },
  methods: {
    handleFileChange(index) {
      const fileInput = this.$refs[`fileInput${index}`];
      const file = fileInput.files[0];

      if (file) {
        this.documentS[index] = file;
      }
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
