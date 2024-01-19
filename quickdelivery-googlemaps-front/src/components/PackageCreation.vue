<!-- PackageForm.vue -->

<template>
    <div class="input_container">
      <div class="input_only">
        <label for="height">{{$t('packageHeight')}}:</label>
          <Field id="height" type="number" name="package_.height" :rules="validatePackageHeight"/>
          <ErrorMessage name="package_.height" />
      </div>
      <div class="input_only">
        <label for="width">{{$t('packageWidth')}}:</label>
        <input id="width" type="number" v-model="package_.width" required="true" />
      </div>
    </div>
    <div class="input_container">
      <div class="input_only">
        <label for="depth">{{$t('packageDepth')}}:</label>
        <input id="depth" type="number" v-model="package_.depth" required="true" />
      </div>
      <div class="input_only">
        <label for="weight">{{$t('packageWeight')}}:</label>
        <input id="weight" type="number" v-model="package_.weight" required="true" />
      </div>
    </div>
    <div class="picture_file_container">
      <div class="input_only">
        <label for="pictureFile">{{$t('packagePicture')}} : <span class="info">{{$t('packagePictureInfo')}}</span></label>
        <input  ref="fileInput0"
                :id="pictureFile"
                type="file"
                accept="image/*, application/pdf"
        @change="handleFileChange(0)"
        />
      </div>
      <div class="input_only">
        <label for="documentFile">{{ $t('packageInvoice') }} : <span class="info">{{ $t('packageInvoiceInfo') }}</span></label>
        <input  ref="fileInput1"
                :id="documentFile"
                type="file"
                accept="image/*, application/pdf"
                @change="handleFileChange(1)"
        />
      </div>
    </div>
</template>
<script>
import { Field, ErrorMessage } from 'vee-validate';
export default {
    components: {
        Field,
        ErrorMessage,
    },
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
    validatePackageHeight(value) {
       console.log(value);
      if (!value) {
        return 'This field is required';
      }
      return true;
    },
  },
};
</script>

<style>
.picture_file_container input,
#address{
  width: 83%;
}
label {
  display: block;
  margin-bottom: 5px;
}
.info{
  font-size: 12px;
  color: #2a6fcf;
}
</style>
