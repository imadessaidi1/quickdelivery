<template>
    <div class="input_container">
        <div class="input_only">
            <label for="iban">IBAN:</label>
            <Field
                    type="text"
                    id="iban"
                    v-model="user.paymentModes['IBAN'].iban"
                    name="iban" :rules="validateIBAN"
            />
            <ErrorMessage class="errorMessage" name="iban" />
        </div>
    </div>
    <div class="input_container">
        <div class="input_only">
            <label for="bic">Code BIC:</label>
            <Field
                    type="text"
                    id="bic"
                    v-model="user.paymentModes['IBAN'].bic"
                    name="bic" :rules="validateBIC"
            />
            <ErrorMessage class="errorMessage" name="bic" />
        </div>
    </div>
    <div class="picture_file_container">
        <div class="input_only">
            <label for="RIB">RIB :</label>
            <input  ref="fileInput4"
                    :id="RIB"
                    type="file"
                    accept="image/*, application/pdf"
                    @change="handleRibFileChange(4, 'RIB')"
            />
            <br/><span v-if="user.paymentModes['IBAN'].ribDocument.file != undefined"><strong>{{user.paymentModes['IBAN'].ribDocument.file.name}}</strong></span>
        </div>
    </div>
</template>
<script>
import { Field, ErrorMessage } from 'vee-validate';
import { validateIBAN, validateBIC } from '@/config/comonFunction';
export default {
  components: {
    Field,
    ErrorMessage,
  },
  computed: {
    user() {
      return this.$store.state.user;
    },
  },
  methods: {
    validateIBAN,
    validateBIC,
    handleRibFileChange(index, type) {
      const fileInput = this.$refs[`fileInput${index}`];
      const file_ = fileInput.files[0];
      if (file_) {
        this.ribDocument = {
            type: type,
            file: file_,
        };
        this.user.paymentModes['IBAN'].ribDocument = this.ribDocument;
      }
    },
  },
};
</script>