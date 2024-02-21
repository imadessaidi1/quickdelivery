<template>
    <div class="input_container">
        <div class="input_only">
            <div class="form-group">
                <label for="cardNumber">Card Number</label>
                <input type="text" id="cardNumber" v-model="cardNumber" required />
            </div>
        </div>
        <div class="input_only">
            <div class="form-group">
                <label for="expiryDate">Expiry Date</label>
                <input type="text" id="expiryDate" v-model="expiryDate" required />
            </div>
        </div>
    </div>
    <div class="input_container">
        <div class="input_only">
            <div class="form-group">
                <label for="cvv">CVV</label>
                <input type="text" id="cvv" v-model="cvv" required />
            </div>
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