<template>
  <div class="payment-vue">
    <div class="payment-form">
        <h2>{{ $t('paymentInformationTitle') }}</h2>
        <div class="payment-summary">
          <small>{{ $t('packageConfirmEstimateTitle') }}</small>
          <strong>{{ amountToPay }}</strong>
        </div>
        <div class="payment-method">
            <div>
              <input type="radio" id="payByCard" value="card" v-model="paymentMethod"/>
              <label for="payByCard">{{ $t('paymentPayByCard') }}</label>
            </div>
        </div>
        <form v-if="paymentMethod === 'card'" @submit.prevent="processCardPayment">
            <CreditCard />
            <button class="btn primary_btn" type="submit">{{ $t('paymentPayByCard') }}</button>
        </form>
        <div class="payment-icons">
          <img src="/visa-ico.png" class="payment-icon"/>
          <img src="/master-card-ico.png" class="payment-icon"/>
          <img src="/amex-ico.png" class="payment-icon"/>
      </div>
    </div>
  </div>
</template>

<script>
import http from '@/config/httpInterceptor';
import CreditCard from './CreditCard.vue';

const EMPTY_PACKAGE = {
  id: null,
  version: null,
  creationDate: null,
  reference: '',
  height: 0,
  width: 0,
  depth: 0,
  weight: 0,
  pictureURL: '',
  status: '',
  deliveryPrice: null,
  deliverySpeed: 'STANDARD',
  insuranceSelected: false,
  declaredValue: null,
  senderID: null,
  packageReservations: [],
  addresses: [
    {
      firstName: '',
      lastName: '',
      line1: '',
      line2: '',
      town: '',
      zipCode: '',
      country: '',
      floor: 0,
      hasElevator: null,
      dateTime: null,
      email: '',
      phone: '',
      type: 'DEPARTURE',
      latitude: null,
      longitude: null,
    },
    {
      firstName: '',
      lastName: '',
      line1: '',
      line2: '',
      town: '',
      zipCode: '',
      country: '',
      floor: 0,
      hasElevator: null,
      dateTime: null,
      email: '',
      phone: '',
      type: 'ARRIVAL',
      latitude: null,
      longitude: null,
    },
  ],
  lastPositionLatitude: null,
  lastPositionLongitude: null,
};

export default {
  computed: {
    package_() {
      return this.$store.state.package_;
    },
    amountToPay() {
      const currentPackage = this.resolveCurrentPackage();
      if (currentPackage.deliveryPrice == null) {
        return '--';
      }
      return `${Number(currentPackage.deliveryPrice).toFixed(2)} ${this.$t('currency')}`;
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
    };
  },
  methods: {
    resolveCurrentPackage() {
      const packageFromStore = this.package_ || {};
      const packageId = packageFromStore.id ?? this.$route.query.packageId ?? null;
      const reference = packageFromStore.reference || this.$route.query.reference || '';
      const guestMode = typeof packageFromStore.guestMode === 'boolean'
        ? packageFromStore.guestMode
        : this.$route.query.guestMode === 'true';
      const guestAccessToken = packageFromStore.guestAccessToken || this.$route.query.guestAccessToken || '';

      return {
        ...packageFromStore,
        id: packageId ? Number(packageId) : null,
        reference,
        guestMode,
        guestAccessToken,
      };
    },
    processCardPayment() {
        const currentPackage = this.resolveCurrentPackage();
        if (!currentPackage.id) {
          console.error('Missing package identifier for payment confirmation.');
          return;
        }

        const request = currentPackage.guestMode
          ? http.put(
              `${this.$i18n.t('rootURL')}${this.$i18n.t('confirmGuestPackagePayment')}?packageID=${currentPackage.id}&guestAccessToken=${encodeURIComponent(currentPackage.guestAccessToken || '')}`,
              null,
              { skipAuth: true }
            )
          : http.put(
              `${this.$i18n.t('rootURL')}${this.$i18n.t('updatePackageStatus')}?${currentPackage.id}=NEW`
            );

        request
          .then(response => {
            if (response.status === 200) {
               this.$store.commit('updatePackage', { ...EMPTY_PACKAGE });
               this.$store.commit('updateDocuments', []);
               this.$router.push('/');
            }
          })
          .catch(error => {
            console.error('Error updating package status:', error);
          });
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
.payment-summary {
  margin: 14px 0 18px;
  padding: 12px 14px;
  border-radius: 10px;
  background: #eef4fb;
}
.payment-summary small,
.payment-summary strong {
  display: block;
}
.payment-summary small {
  color: #516274;
}
.payment-summary strong {
  margin-top: 4px;
  color: #0f172a;
  font-size: 1.35rem;
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

.payment-form form .primary_btn {
  margin-top: 24px;
}
</style>

