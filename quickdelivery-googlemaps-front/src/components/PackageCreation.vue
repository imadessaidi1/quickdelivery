<!-- PackageForm.vue -->

<template>
    <div class="input_container">
      <div class="input_only">
        <label for="height">{{$t('packageHeight')}}:</label>
          <Field id="height" type="number" v-model="package_.height" name="package_.height" :rules="validateNumericField"/>
          <ErrorMessage class="errorMessage" name="package_.height" />
      </div>
      <div class="input_only">
        <label for="width">{{$t('packageWidth')}}:</label>
        <Field id="width" type="number" v-model="package_.width" name="package_.width" :rules="validateNumericField"/>
        <ErrorMessage class="errorMessage" name="package_.width" />
      </div>
    </div>
    <div class="input_container">
      <div class="input_only">
        <label for="depth">{{$t('packageDepth')}}:</label>
        <Field id="depth" type="number" v-model="package_.depth" name="package_.depth" :rules="validateNumericField"/>
        <ErrorMessage class="errorMessage" name="package_.depth" />
      </div>
      <div class="input_only">
        <label for="weight">{{$t('packageWeight')}}:</label>
        <Field id="weight" type="number" v-model="package_.weight" name="package_.weight" :rules="validateNumericField"/>
        <ErrorMessage class="errorMessage" name="package_.weight" />
      </div>
    </div>
    <div class="picture_file_container">
      <div class="input_only">
        <label for="pictureFile">{{$t('packagePicture')}} : <span class="info">{{$t('packagePictureInfo')}}</span></label>
        <input  ref="fileInput0"
                :id="pictureFile"
                type="file"
                accept="image/*"
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
import { validateNumericField } from '@/config/comonFunction';
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
    validateNumericField,
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
