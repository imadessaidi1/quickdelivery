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
        <p v-if="stripeStatus === 'success'" class="payment-status success">{{ $t('paymentStripeSuccess') }}</p>
        <p v-else-if="stripeStatus === 'cancel'" class="payment-status warning">{{ $t('paymentStripeCancel') }}</p>
        <p v-if="paymentError" class="payment-status error">{{ $t('paymentStripeError') }}</p>
        <form v-if="paymentMethod === 'card'" @submit.prevent="processCardPayment">
            <button class="btn primary_btn" type="submit" :disabled="processingPayment">
              {{ processingPayment ? $t('paymentRedirectingToStripe') : $t('paymentRedirectToStripe') }}
            </button>
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
import { formatDisplayedPackageAmount } from '@/config/packagePricing';

export default {
  data() {
    return {
      paymentMethod: 'card',
      processingPayment: false,
      paymentError: false,
    };
  },
  computed: {
    package_() {
      return this.$store.state.package_;
    },
    amountToPay() {
      const currentPackage = this.resolveCurrentPackage();
      return formatDisplayedPackageAmount(this.$i18n, currentPackage);
    },
    stripeStatus() {
      return this.$route.query.stripeStatus || '';
    },
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

        this.processingPayment = true;
        this.paymentError = false;

        const query = `?packageID=${currentPackage.id}`
          + (currentPackage.guestMode ? `&guestAccessToken=${encodeURIComponent(currentPackage.guestAccessToken || '')}` : '');

        http.post(
          `${this.$i18n.t('rootURL')}${this.$i18n.t('createStripeCheckoutSession')}${query}`,
          null,
          currentPackage.guestMode ? { skipAuth: true } : undefined
        )
          .then(response => {
            if (response.data && response.data.checkoutUrl) {
              window.location.assign(response.data.checkoutUrl);
              return;
            }
            throw new Error('Missing Stripe checkout URL');
          })
          .catch(error => {
            this.processingPayment = false;
            this.paymentError = true;
            console.error('Error creating Stripe checkout session:', error);
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
.payment-status {
  border-radius: 8px;
  font-size: 0.9rem;
  line-height: 1.35;
  margin: 16px 0 0;
  padding: 10px 12px;
}
.payment-status.success {
  background: #e9f7ef;
  color: #146c43;
}
.payment-status.warning {
  background: #fff3cd;
  color: #7a5b00;
}
.payment-status.error {
  background: #fdecea;
  color: #842029;
}

.payment-form form .primary_btn {
  margin-top: 24px;
}
</style>

