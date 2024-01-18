<template>
<Form @submit="submitForm" ref="packageCreationForm">
    <div class="input_container">
        <div class="input_only">
            <label for="height">{{$t('packageHeight')}}:</label>
            <Field id="height" type="number" name="package_.height" :rules="validateEmail"/>
            <ErrorMessage name="package_.height" />
        </div>
        <div class="input_only">
            <label for="width">{{$t('packageWidth')}}:</label>
            <Field id="width" type="number" name="package_.width"/>
            <ErrorMessage name="package_.width" />
        </div>
    </div>
    <div class="input_container">
        <div class="input_only">
            <label for="depth">{{$t('packageDepth')}}:</label>
            <Field id="depth" type="number" name="package_.depth"/>
            <ErrorMessage name="package_.depth" />
        </div>
        <div class="input_only">
            <label for="weight">{{$t('packageWeight')}}:</label>
            <Field id="weight" type="number" name="package_.weight"/>
            <ErrorMessage name="package_.weight" />
        </div>
    </div>
    <button>Sign up for newsletter</button>
</Form>
</template>
<script>
import { Form, Field, ErrorMessage } from 'vee-validate';
export default {
  components: {
    Field,
    ErrorMessage,
    Form,
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
    async submitForm(values) {
    console.log(values);
    },
    validateEmail(value) {
       console.log(value);
      // if the field is empty
      if (!value) {
        return 'This field is required';
      }
      // if the field is not a valid email
      const regex = /^[A-Z0-9._%+-]+@[A-Z0-9.-]+\.[A-Z]{2,4}$/i;
      if (!regex.test(value)) {
        return 'This field must be a valid email';
      }
      // All is good
      return true;
    },
  },
};
</script>

<style>
.picture_file_container input,
#address{
  width: 80%;
}
label {
  display: block;
  margin-bottom: 5px;
}
</style>
