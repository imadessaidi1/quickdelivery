<template>
    <div class="payment-form">
        <h2>Payment Information</h2>
        <div class="payment-icons">
            <img src="/visa-ico.png" class="payment-icon"/>
            <img src="/master-card-ico.png" class="payment-icon"/>
            <img src="/amex-ico.png" class="payment-icon"/>
            <img src="/paypal-ico.png" class="payment-icon"/>
        </div>
        <div class="payment-method">
            <input type="radio" id="payByCard" value="card" v-model="paymentMethod"/>
            <label for="payByCard">Pay by Card</label>
            <input type="radio" id="payByPayPal" value="paypal" v-model="paymentMethod"/>
            <label for="payByPayPal">Pay with PayPal</label>
        </div>
        <form v-if="paymentMethod === 'card'" @submit.prevent="processCardPayment">
        <div class="form-group">
            <label for="cardNumber">Card Number</label>
            <input type="text" id="cardNumber" v-model="cardNumber" required />
        </div>
        <div class="form-group">
            <label for="expiryDate">Expiry Date</label>
            <input type="text" id="expiryDate" v-model="expiryDate" required />
        </div>
        <div class="form-group">
            <label for="cvv">CVV</label>
            <input type="text" id="cvv" v-model="cvv" required />
        </div>
        <div class="form-group">
            <label for="amount">Amount</label>
            <input type="text" id="amount" v-model="amount" required />
        </div>
        <button type="submit">Pay by Card</button>
    </form>
    <button v-if="paymentMethod === 'paypal'" @click="redirectToPayPal">Pay with PayPal</button>
        </div>
</template>

<script>
export default {
  data() {
    return {
      paymentMethod: 'card',
      cardNumber: '',
      expiryDate: '',
      cvv: '',
      amount: ''
    };
  },
  methods: {
    processCardPayment() {
      const paymentData = {
        cardNumber: this.cardNumber,
        expiryDate: this.expiryDate,
        cvv: this.cvv,
        amount: this.amount
      };
      console.log('Card payment processed:', paymentData);
      // Logique pour traiter le paiement par carte
    },
    redirectToPayPal() {
      // Redirection vers PayPal pour finaliser le paiement
      window.location.href = `https://www.paypal.com/paypalme/votreentreprise/${this.amount}`;
    }
  }
};
</script>

<style scoped>
.payment-form {
  max-width: 400px;
  margin: 0 auto;
}

.payment-icons {
  display: flex;
  align-items: center;
  margin-bottom: 10px;
}

.payment-icon {
  width: 30px;
  margin-right: 10px;
}

.form-group {
  margin-bottom: 20px;
}

label {
  display: block;
}

input[type="text"] {
  width: 100%;
  padding: 8px;
  font-size: 16px;
}

button {
  background-color: #0070ba;
  color: white;
  border: none;
  padding: 10px 20px;
  font-size: 18px;
  cursor: pointer;
  border-radius: 5px;
}

button:hover {
  background-color: #005187;
}

.payment-method {
  margin-bottom: 20px;
  display: flex;
  align-items: center;
}

input[type="radio"] {
  margin-right: 10px;
}
</style>

