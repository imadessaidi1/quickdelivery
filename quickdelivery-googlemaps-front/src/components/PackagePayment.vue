<template>
  <div class="payment-vue">
    <div class="payment-form">
        <h2>Payment Information</h2>
        <div class="payment-method">
            <div>
              <input type="radio" id="payByCard" value="card" v-model="paymentMethod"/>
              <label for="payByCard">Pay by Card</label>
            </div>
            <div>
              <input type="radio" id="payByPayPal" value="paypal" v-model="paymentMethod"/>
              <label for="payByPayPal">Pay with PayPal</label>
            </div>
        </div>
        <form v-if="paymentMethod === 'card'" @submit.prevent="processCardPayment">
            <CreditCard />
            <button class="btn primary_btn" type="submit">Pay by Card</button>
        </form>
        <div class="paypal" v-if="paymentMethod === 'paypal'" @click="redirectToPayPal">
          <button class="btn primary_btn">Pay with PayPal</button>
        </div>
        <div class="payment-icons">
          <img src="/visa-ico.png" class="payment-icon"/>
          <img src="/master-card-ico.png" class="payment-icon"/>
          <img src="/amex-ico.png" class="payment-icon"/>
          <img src="/paypal-ico.png" class="payment-icon"/>
      </div>
    </div>
  </div>
</template>

<script>
import http from '@/config/httpInterceptor';
import CreditCard from './CreditCard.vue';
export default {
  computed: {
    package_() {
      return this.$store.state.package_;
    },
  },
  components :{
    CreditCard,
  },
  data() {
    return {
      paymentMethod: 'card',
      cardNumber: '',
      expiryDate: '',
      cvv: '',
      amount: '',
      package: {
        id: null,
        version: null,
        creationDate: null,
        reference: "",
        height: 0,
        width: 0,
        depth: 0,
        weight: 0,
        pictureURL: "",
        status: "",
        deliveryPrice: null,
        senderID: null,
        packageReservations: [],
        addresses: [{
        firstName: "",
        lastName: "",
        line1: "",
        line2: "",
        town: "",
        zipCode: "",
        country: "",
        floor:0,
        dateTime: null,
        email: "",
        phone: "",
        type: "DEPARTURE",
        latitude: 0,
        longitude: 0,
      },
      {
        firstName: "",
        lastName: "",
        line1: "",
        line2: "",
        town: "",
        zipCode: "",
        country: "",
        floor:0,
        dateTime: null,
        email: "",
        phone: "",
        type: "ARRIVAL",
        latitude: 0,
        longitude: 0,
      }],
      lastPositionLatitude: null,
      lastPositionLongitude: null
    }
    };
  },
  methods: {
    processCardPayment() {
        http.put(this.$i18n.t('rootURL') + this.$i18n.t('updatePackageStatus')+'?'+this.package_.id+'=NEW')
          .then(response => {
            if(response.status == '200'){
               this.$store.commit('updatePackage', this.package);
               this.$store.commit('updateDocuments', []);
               this.$router.push('/');
            }
          })
          .catch(error => {
            console.error('Error updating package status:', error);
          });
    },
    redirectToPayPal() {
      // Redirection vers PayPal pour finaliser le paiement
      window.location.href = `https://www.paypal.com/paypalme/votreentreprise/${this.amount}`;
    }
  }
};
</script>
<style>
.payment-vue{
  height: 100%;
  display: flex;
  justify-content: center;
  align-items: center;
  background: rgb(1,55,121);
  background: radial-gradient(circle, rgba(1,55,121,1) 0%, rgba(0,83,187,1) 65%, rgba(0,86,194,1) 100%);
}
.payment-form {
  width: 400px;
  height: max-content;
  padding: 20px;
  margin: 10px;
  background: #fcfcfc;
  border-radius: 10px;
}
.payment-form h2 {
  margin: 0;
}
.payment-icons {
  display: flex;
  align-items: center;
  margin-top: 15px;
}
.payment-form .input_only input{
  width: 95%;
}
.payment-icon {
  width: 30px;
  margin-right: 10px;
}

.form-group {
  margin-bottom: 20px;
}

.payment-method {
  margin: 0 auto;
  display: flex;
  justify-content: space-evenly;
  width: 70%;
}
.payment-method div{
  width: max-content;
}
.payment-method div{
  display: flex;
  align-items: center;
}
.payment-method label{
  font-size: 12px;
  margin: 0;
}
.paypal{
  width: max-content;
  margin: 25px auto 90px auto;
}
</style>

