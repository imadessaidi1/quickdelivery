<template>
  <div class="payment-vue">
    <div class="payment-form">
        <h2>Payment Information</h2>
        <div class="payment-method">
            <input type="radio" id="payByCard" value="card" v-model="paymentMethod"/>
            <label for="payByCard">Pay by Card</label>
            <input type="radio" id="payByPayPal" value="paypal" v-model="paymentMethod"/>
            <label for="payByPayPal">Pay with PayPal</label>
        </div>
        <form v-if="paymentMethod === 'card'" @submit.prevent="processCardPayment">
            <CreditCard />
            <button class="btn primary_btn" type="submit">Pay by Card</button>
        </form>
        <button class="btn primary_btn" v-if="paymentMethod === 'paypal'" @click="redirectToPayPal">Pay with PayPal</button>
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
    const packageStatus = 'NEW';
    const packageId =  this.package_.id;

    // Données à envoyer au contrôleur
    const requestData = {
      [packageId]: packageId,
      [packageId]: packageStatus
    };

    // Appel de l'API avec Axios
    http.put(this.$i18n.t('rootURL') + this.$i18n.t('updatePackageStatus'), requestData)
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
  height: 87%;
  display: flex;
  justify-content: center;
  align-items: center;
}
.payment-form {
  width: max-content;
  height: max-content;
  padding: 20px 20px;
  border-radius: 10px;
  box-shadow: rgba(0, 0, 0, 0.24) 0px 3px 8px;
}
.payment-form h2 {
  margin: 0;
}
.payment-icons {
  display: flex;
  align-items: center;
  margin-top: 10px;
}

.payment-icon {
  width: 30px;
  margin-right: 10px;
}

.form-group {
  margin-bottom: 20px;
}

.payment-method {
  margin-bottom: 20px;
  display: flex;
  align-items: center;
}

</style>

