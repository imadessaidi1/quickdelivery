<template>
    <div>
        <h2>User Info.</h2>

        <div class="input_container">
            <div class="input_only">
                <label for="firstName">{{$t('packageAddressFirstName')}}:</label>
                <Field id="firstName" v-model="firstName" name="firstName" :rules="validateString"/>
                <ErrorMessage class="errorMessage" name="firstName" />
            </div>
            <div class="input_only">
                <label for="lastName">{{$t('packageAddressLastName')}}:</label>
                <Field id="lastName" type="text" v-model="lastName" name="lastName" :rules="validateString"/>
                <ErrorMessage class="errorMessage" name="lastName" />
            </div>
        </div>
        <div class="input_container">
            <div class="input_only">
                <label for="email">{{$t('packageAddressEmail')}}:</label>
                <Field id="email" type="email" v-model="email" name="email" :rules="validateEmail"/>
                <ErrorMessage class="errorMessage" name="email" />
            </div>
            <div class="input_only">
                <label for="phone">{{$t('packageAddressPhone')}}:</label>
                <Field id="phone" type="text" v-model="phone" name="phone" :rules="validatePhone"/>
                <ErrorMessage class="errorMessage" name="phone" />
            </div>
        </div>
        <div>
            <label for="address">{{$t('packageAddressAddress')}}:</label>
            <AddressAutocomplete id="address" ref="addressAutoComplete"/>
            <span v-if="isAddressError" class="errorMessage">{{errorAddressMessage}}</span>
        </div>

        <div class="picture_file_container">
            <div class="input_only">
                <label for="pictureFile">ID :</label>
                <input  ref="fileInput0"
                        :id="pictureFile"
                        type="file"
                        accept="image/*"
                @change="handleFileChange(0)"
                />
            </div>
            <div class="input_only">
                <label for="documentFile">Driver licence :</label>
                <input  ref="fileInput1"
                        :id="documentFile"
                        type="file"
                        accept="image/*, application/pdf"
                @change="handleFileChange(1)"
                />
            </div>
        </div>
        <div>
            <h2>Payment mode</h2>
            <div class="payment-method">
                <label for="card-option">
                    <input type="radio" id="card-option" name="payment-type" v-model="selectedPaymentType" value="CARD"/>
                    Carte bancaire
                </label>
                <label for="iban-option">
                    <input type="radio" id="iban-option" name="payment-type" v-model="selectedPaymentType" value="IBAN"/>
                    Virement bancaire (IBAN)
                </label>
                <label for="paypal-option">
                    <input type="radio" id="paypal-option" name="payment-type" v-model="selectedPaymentType" value="PAYPAL"/>
                    PayPal
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
import { Field, ErrorMessage } from 'vee-validate';
import { validatePhone, validateEmail, validateString, validateNumericFieldAcceptZero } from '@/config/comonFunction';
import AddressAutocomplete from './AddressAutocomplete.vue';
import CreditCard from './CreditCard.vue';
import IBAN from './IbanBank.vue';
export default {
  components: {
    AddressAutocomplete,
    CreditCard,
    IBAN,
    Field,
    ErrorMessage,
  },
  data() {
    return {
      user: {
        id: 0,
        version: "2024-02-20T15:07:36.946Z",
        type: "string",
        firstName: "",
        lastName: "",
        emailAddress: "",
        emailAddressValidation: true,
        phone: "",
        phoneValidation: true,
        activeAccount: true,
        personalAddress: [
          {
            id: 0,
            version: "2024-02-20T15:07:36.946Z",
            firstName: "",
            lastName: "",
            line1: "",
            line2: "",
            town: "",
            zipCode: "",
            country: "",
            floor: 0,
            dateTime: "2024-02-20T15:07:36.946Z",
            type: "RESIDENCE",
            latitude: 0,
            longitude: 0,
            email: "",
            phone: "",
          },
        ],
      },
      selectedPaymentType: '',
    };
  },
  methods: {
    validatePhone,
    validateEmail,
    validateString,
    validateNumericFieldAcceptZero,
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