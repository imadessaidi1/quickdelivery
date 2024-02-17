<template>
    <div class="payment-form-container">
        <h2>Formulaire de Paiement</h2>
        <form @submit.prevent="submitForm" class="payment-form">
        <div class="form-group">
            <label for="cardName">Nom sur la carte:</label>
            <input type="text" id="cardName" v-model="cardName" required class="form-control">
        </div>
        <div class="form-group">
            <label for="cardNumber">Numéro de carte:</label>
            <input type="text" id="cardNumber" v-model="cardNumber" required class="form-control">
        </div>
        <div class="form-group">
            <label for="expiryDate">Date d'expiration (MM/YY):</label>
            <input type="text" id="expiryDate" v-model="expiryDate" placeholder="MM/YY" required class="form-control">
        </div>
        <div class="form-group">
            <label for="securityCode">Code de sécurité:</label>
            <input type="text" id="securityCode" v-model="securityCode" required class="form-control">
        </div>
        <button type="submit" class="btn btn-primary">Payer</button>
    </form>
</div>
        </template>

<script>
export default {
  data() {
    return {
      cardName: '',
      cardNumber: '',
      expiryDate: '',
      securityCode: ''
    };
  },
  methods: {
    submitForm() {
      console.log('Formulaire soumis !');
      console.log('Nom sur la carte:', this.cardName);
      console.log('Numéro de carte:', this.cardNumber);
      console.log('Date d\'expiration:', this.expiryDate);
      console.log('Code de sécurité:', this.securityCode);
    }
  }
}
</script>

<style scoped>
.payment-form-container {
  max-width: 400px;
  margin: 0 auto;
  padding: 20px;
  border: 1px solid #ccc;
  border-radius: 5px;
  background-color: #f9f9f9;
}

.payment-form {
  display: grid;
  grid-gap: 10px;
}

.form-group {
  margin-bottom: 15px;
}

label {
  font-weight: bold;
}

.form-control {
  width: 100%;
  padding: 10px;
  border: 1px solid #ccc;
  border-radius: 5px;
  font-size: 16px;
}

.btn {
  padding: 10px 20px;
  border: none;
  border-radius: 5px;
  background-color: #007bff;
  color: #fff;
  font-size: 16px;
  cursor: pointer;
}

.btn:hover {
  background-color: #0056b3;
}
</style>
