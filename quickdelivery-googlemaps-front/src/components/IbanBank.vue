<template>
    <div class="input_container">
        <div class="input_only">
            <label for="iban">{{$t('userIBAN')}}:</label>
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
            <label for="bic">{{$t('userIBANBIC')}}:</label>
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
            <label for="RIB">{{$t('userRIB')}} :</label>
            <input  ref="fileInput4"
                    :id="RIB"
                    type="file"
                    accept="image/*, application/pdf"
                    @change="handleRibFileChange(4, 'RIB')"
            />
            <br/><span v-if="userDocuments['RIB'] != undefined"><strong>{{userDocuments['RIB'].name}}</strong></span>
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
    userDocuments() {
      return this.$store.state.userDocuments;
    },
  },
  data() {
    return {
     RIB: 'RIB'
    };
  },
  methods: {
    validateIBAN,
    validateBIC,
    handleRibFileChange(index, type) {
      const fileInput = this.$refs[`fileInput${index}`];
      const file_ = fileInput.files[0];
      if (file_) {
        let ribDocument = {
            type: type,
            file: file_,
        };
        console.log(ribDocument);
        this.userDocuments[type] = file_;
      }
    },
  },
};
</script>