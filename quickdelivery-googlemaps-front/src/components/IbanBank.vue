<template>
    <div class="input_container">
        <div class="input_only">
            <label for="iban">IBAN:</label>
            <input
                    type="text"
                    id="iban"
                    v-model="iban"
                    :class="{ invalid: !isIbanValid }"
            @blur="validateIban"
            />
        </div>
    </div>
    <div class="input_container">
        <div class="input_only">
            <label for="bic">Code BIC:</label>
            <input
                    type="text"
                    id="bic"
                    v-model="bic"
                    :class="{ invalid: !isBicValid }" />
        </div>
    </div>
    <div class="picture_file_container">
        <div class="input_only">
            <label for="documentFile">RIB :</label>
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
export default {
  data() {
    return {
      card: {
            paymentMethod: 'card',
            cardNumber: '',
            expiryDate: '',
            cvv: '',
            },
    };
  },
  methods: {
    processCardData() {
        console.log(this.card);
    },
  },
};
</script>