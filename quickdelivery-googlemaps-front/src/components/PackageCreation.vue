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
  computed: {
    package_() {
      return this.$store.state.package_;
    },
    documentS() {
      return this.$store.state.documentS;
    },
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
