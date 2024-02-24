<template>
    <div class="input_container">
        <div class="input_only">
            <div class="form-group">
                <label for="cardNumber">Card Number</label>
                <Field type="text" id="cardNumber" v-model="user.paymentModes['CREDIT_CARD'].cardNumber" name="cardNumber" :rules="validateCreditCardNumber" />
                <ErrorMessage class="errorMessage" name="cardNumber" />
            </div>
        </div>
        <div class="input_only">
            <div class="form-group">
                <label for="expiryDate">Expiry Date</label>
                <Field type="text" id="expiryDate" v-model="user.paymentModes['CREDIT_CARD'].expiryDate" name="expiryDate" :rules="validateCreditCardExpiration" />
                <ErrorMessage class="errorMessage" name="expiryDate" />
            </div>
        </div>
    </div>
    <div class="input_container">
        <div class="input_only">
            <div class="form-group">
                <label for="cvv">CVV</label>
                <Field type="text" id="cvv" v-model="user.paymentModes['CREDIT_CARD'].cvv" name="cvv" :rules="validateCreditCardCVV" />
                <ErrorMessage class="errorMessage" name="cvv" />
            </div>
        </div>
    </div>
</template>
<script>
import { Field, ErrorMessage } from 'vee-validate';
import { validateCreditCardNumber, validateCreditCardExpiration, validateCreditCardCVV } from '@/config/comonFunction';
export default {
 computed: {
    user() {
      return this.$store.state.user;
    },
  },
 components: {
    Field,
    ErrorMessage,
  },
  methods: {
    validateCreditCardNumber,
    validateCreditCardExpiration,
    validateCreditCardCVV,
    processCardData() {
        console.log(this.card);
    },
  },
};
</script>