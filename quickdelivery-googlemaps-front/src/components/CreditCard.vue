<template>
  <div class="credit-card-form">
    <div class="field-wrap">
      <label for="holderNam">{{ $t('userRegistrationCardHolder') }}</label>
      <Field id="holderNam" v-model="user.paymentModes['CREDIT_CARD'].holderNam" type="text" name="holderNam" />
    </div>

    <div class="field-wrap wide">
      <label for="cardNumber">{{ $t('userCardNumber') }}</label>
      <Field id="cardNumber" v-model="user.paymentModes['CREDIT_CARD'].cardNumber" type="text" name="cardNumber" :rules="validateCreditCardNumber" />
      <ErrorMessage class="errorMessage" name="cardNumber" />
    </div>

    <div class="field-wrap">
      <label for="expiryDate">{{ $t('userCardExpiryDate') }}</label>
      <Field id="expiryDate" v-model="user.paymentModes['CREDIT_CARD'].expiryDate" type="text" name="expiryDate" :rules="validateCreditCardExpiration" />
      <ErrorMessage class="errorMessage" name="expiryDate" />
    </div>

    <div class="field-wrap">
      <label for="cvv">{{ $t('userCardCVV') }}</label>
      <Field id="cvv" v-model="user.paymentModes['CREDIT_CARD'].cvv" type="text" name="cvv" :rules="validateCreditCardCVV" />
      <ErrorMessage class="errorMessage" name="cvv" />
    </div>
  </div>
</template>

<script>
import { ErrorMessage, Field } from 'vee-validate';
import { validateCreditCardCVV, validateCreditCardExpiration, validateCreditCardNumber } from '@/config/comonFunction';

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
    validateCreditCardNumber,
    validateCreditCardExpiration,
    validateCreditCardCVV,
  },
};
</script>

<style scoped>
.credit-card-form {
  display: grid;
  grid-template-columns: repeat(2, minmax(0, 1fr));
  gap: 14px;
}

.field-wrap {
  min-width: 0;
  display: flex;
  flex-direction: column;
}

.field-wrap.wide {
  grid-column: 1 / -1;
}

.field-wrap label {
  display: block;
  margin-bottom: 8px;
  font-weight: 600;
  color: #24364f;
}

.field-wrap :deep(input) {
  width: 100%;
  max-width: 100%;
  min-height: 48px;
  padding: 0 14px;
  border: 1px solid #ced7e4;
  border-radius: 14px;
  background: #fff;
  box-sizing: border-box;
}

.errorMessage {
  margin-top: 6px;
  font-size: 0.78rem;
  color: #b42318;
  word-break: break-word;
}

@media screen and (max-width: 980px) {
  .credit-card-form {
    grid-template-columns: 1fr;
  }
}
</style>
