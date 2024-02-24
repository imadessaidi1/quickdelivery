<template>
    <div>
        <h2>User Documents.</h2>
        <div class="picture_file_container">
            <div class="input_only">
                <label for="ID">{{$t('userDocumentID')}} :</label>
                <input  ref="fileInput0"
                        :id="ID"
                        type="file"
                        accept="image/*"
                @change="handleUserFileChange(0, 'ID')"
                />
                <br/><span v-if="user.documents[0] != undefined"><strong>{{user.documents[0].file.name}}</strong></span>
            </div>
            <div class="input_only">
                <label for="DRIVER_LICENCE">{{$t('userDocumentDriverLicence')}} :</label>
                <input  ref="fileInput1"
                        :id="DRIVER_LICENCE"
                        type="file"
                        accept="image/*, application/pdf"
                @change="handleUserFileChange(1,'DRIVER_LICENCE')"
                />
                <br/><span v-if="user.documents[1] != undefined"><strong>{{user.documents[1].file.name}}</strong></span>
            </div>
            <div class="input_only">
                <label for="USER_COMPANY_EXTRACT">{{$t('userDocumentCompanyExtract')}} :</label>
                <input  ref="fileInput2"
                        :id="USER_COMPANY_EXTRACT"
                        type="file"
                        accept="image/*, application/pdf"
                @change="handleUserFileChange(2, 'USER_COMPANY_EXTRACT')"
                />
                <br/><span v-if="user.documents[2] != undefined"><strong>{{user.documents[2].file.name}}</strong></span>
            </div>
            <div class="input_only">
                <label for="USER_COMPANY_INSURANCE">{{$t('userDocumentCompanyInsurance')}} :</label>
                <input  ref="fileInput3"
                        :id="USER_COMPANY_INSURANCE"
                        type="file"
                        accept="image/*, application/pdf"
                @change="handleUserFileChange(3, 'USER_COMPANY_INSURANCE')"
                />
                <br/><span v-if="user.documents[3] != undefined"><strong>{{user.documents[3].file.name}}</strong></span>
            </div>
        </div>
        <div>
            <h2>{{$t('userPaymentModes')}}</h2>
            <div class="payment-method">
                <label for="card-option">
                    <input type="radio" id="card-option" name="payment-type" v-model="selectedPaymentType" value="CARD"/>
                    {{$t('userPaymentCreditCard')}}
                </label>
                <label for="iban-option">
                    <input type="radio" id="iban-option" name="payment-type" v-model="selectedPaymentType" value="IBAN"/>
                    {{$t('userIBAN')}}
                </label>
                <label for="paypal-option">
                    <input type="radio" id="paypal-option" name="payment-type" v-model="selectedPaymentType" value="PAYPAL"/>
                    {{$t('userPayPal')}}
                </label>
            </div>
            <div class="payment-details">
                <CreditCard v-if="selectedPaymentType === 'CARD'"></CreditCard>
                <IBAN v-if="selectedPaymentType === 'IBAN'"></IBAN>
            </div>
        </div>
    </div>
</template>
<script>
import CreditCard from './CreditCard.vue';
import IBAN from './IbanBank.vue';
export default {
  components: {
    CreditCard,
    IBAN,
  },
  computed: {
    user() {
      return this.$store.state.user;
    },
  },
  data() {
    return {
      userDocuments: [],
      selectedPaymentType: '',
    };
  },
  methods: {
    handleUserFileChange(index, type) {
      const fileInput = this.$refs[`fileInput${index}`];
      const file_ = fileInput.files[0];
      if (file_) {
        let userDocument = {
            type: type,
            file: file_,
        };
        this.user.documents[index] = userDocument;
      }
    },
  },
}
</script>
<style>
.payment-method {
  margin-bottom: 20px;
  display: flex;
  align-items: center;
}
</style>