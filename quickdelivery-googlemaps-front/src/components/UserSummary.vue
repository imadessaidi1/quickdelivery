<template>
    <div class="user_details_group">
        <div class="user_details">
            <h3>User Info.</h3>
            <div class="details">
                <div><strong>{{$t('packageAddressFirstName')}}:</strong> {{ user.firstName }}</div>
                <div><strong>{{$t('packageAddressLastName')}}:</strong> {{ user.lastName }}</div>
                <div class="long_text"><strong>{{$t('packageAddressEmail')}}:</strong> {{ user.emailAddress }}</div>
                <div class="long_text"><strong>{{$t('packageAddressPhone')}}:</strong> {{ user.phone }}</div>
                <div class="long_text"><strong>{{$t('packageAddressAddress')}}:</strong> {{ user.addressAuto }}</div>
            </div>
        </div>
        <div class="user_details">
            <h3>{{$t('userVehicle')}}</h3>
            <div class="details">
                <div class="long_text"><strong>{{$t('userVehicleRegistration')}}:</strong> {{ vehicle.registrationNumber }}</div>
                <div><strong>{{$t('userVehicleBrand')}}:</strong> {{ vehicle.brand }}</div>
                <div><strong>{{$t('userVehicleModel')}}:</strong> {{ vehicle.model }}</div>
                <div><strong>{{$t('userVehicleEnergy')}}:</strong> {{ vehicle.energyType }}</div>
            </div>
        </div>
        <div class="user_details">
            <h3>{{$t('userPaymentModes')}}</h3>
                <div v-if="user.paymentModes">
                    <div v-for="(paymentMode, key) in user.paymentModes" :key="key">
                        <span class="mini_title">{{ $t(key) }}</span>
                        <div v-if="key === 'CREDIT_CARD'" class="details">
                            <div class="long_text"><strong>{{$t('userCardNumber')}}:</strong> {{ paymentMode.cardNumber }}</div>
                            <div class="long_text"><strong>{{$t('userCardExpiryDate')}}:</strong> {{ paymentMode.expiryDate }}</div>
                            <div><strong>{{$t('userCardCVV')}}:</strong> {{ paymentMode.cvv }}</div>
                        </div>
                        <div v-if="key === 'IBAN'" class="details">
                            <div class="long_text"><strong>{{$t('userIBAN')}}:</strong> {{ paymentMode.iban }}</div>
                            <div class="long_text"><strong>{{$t('userIBANBIC')}}:</strong> {{ paymentMode.bic }}</div>
                            <div v-if="userDocuments && userDocuments['RIB'] != undefined"><strong>{{$t('userRIB')}}:</strong> {{ userDocuments['RIB'].name }}</div>
                        </div>
                    </div>
                </div>
        </div>
        <div class="user_details">
            <h3>{{$t('userDocuments')}}</h3>
            <span class="mini_title">User Documents</span>
            <div class="details">
                <div class="long_text" v-if="userDocuments['ID']"><strong>{{ $t('ID') }}:</strong> {{ userDocuments['ID'].name }}</div>
                <div class="long_text" v-if="userDocuments['DRIVER_LICENCE']"><strong>{{ $t('DRIVER_LICENCE') }}:</strong> {{ userDocuments['DRIVER_LICENCE'].name }}</div>
                <div class="long_text" v-if="userDocuments['USER_COMPANY_EXTRACT']"><strong>{{ $t('USER_COMPANY_EXTRACT') }}:</strong> {{ userDocuments['USER_COMPANY_EXTRACT'].name }}</div>
                <div class="long_text" v-if="userDocuments['USER_COMPANY_INSURANCE']"><strong>{{ $t('USER_COMPANY_INSURANCE') }}:</strong> {{ userDocuments['USER_COMPANY_INSURANCE'].name }}</div>
            </div>
            <span class="mini_title">Vehicle Documents</span>
            <div class="details">
                <div class="long_text" v-if="vehicleDocuments['GRAY_CARD']"><strong>{{ $t('GRAY_CARD') }}:</strong> {{ vehicleDocuments['GRAY_CARD'].name }}</div>
                <div class="long_text" v-if="vehicleDocuments['INSURANCE']"><strong>{{ $t('INSURANCE') }}:</strong> {{ vehicleDocuments['INSURANCE'].name }}</div>
            </div>
        </div>
    </div>
    <div class="conditionCheckbox">
        <Field name="userCondition" type="checkbox" value="Coffee" /><span>{{$t('packageCreationAgreement')}}</span>
        <ErrorMessage name="userCondition" />
    </div>
</template>
<script>
import { Field, ErrorMessage } from 'vee-validate';
export default {
    components: {
        Field,
        ErrorMessage,
    },
    computed: {
        user() {
            return this.$store.state.user;
        },
        vehicle() {
          return this.$store.state.vehicle;
        },
        vehicleDocuments() {
          return this.$store.state.vehicleDocuments;
        },
        userDocuments() {
          return this.$store.state.userDocuments;
        },
    },
    methods: {
        formatDate(dateTime) {
            const date = new Date(dateTime);
            const options = {
                day: '2-digit',
                month: '2-digit',
                year: '2-digit',
                hour: '2-digit',
                minute: '2-digit',
            };
            const userLanguage = navigator.languages && navigator.languages.length ? navigator.languages[0] : navigator.language || 'fr-FR';
            return date.toLocaleDateString(userLanguage, options);
        }
    },
}
</script>
<style>
.conditionCheckbox{
  display: flex;
  align-items: center;
  padding: 0 0 0 20px;
}
.conditionCheckbox span{
    font-size: 12px;
}
.user_details_group{
    width: 100%;
    display: flex;
    justify-content: space-evenly;
}
.user_details_group .user_details{
    width: 25%;
    padding: 10px;
    margin-top: 10px;
    border-radius: 5px;
    background-color: #f5f5f5ca;
}
.user_details_group .user_details h3{
    margin-left: 15px;
    padding-left: 6px;
    border-left: solid 3px #42ba96;
} 
.user_details_group .user_details div{
    padding: 6px 0 6.5px 0;
}
.user_details_group .user_details .details div{
    font-size: 14px;
}
.mini_title{
    padding-left: 10px;
    font-size: 14px;
    font-weight: 700;
}
@media screen and (max-width: 1100px){
    .user_details_group {
        flex-direction: column;
    }
    .user_details_group .user_details{
        width: 95%;
        margin: 5px 0;
    }
    .user_details_group .user_details .details{
        width: 100%;
        display: inline-grid;
        grid-template-columns: auto auto;
    }
    .user_details_group .user_details .details div{
        font-size: 13px;
    }
}
@media screen and (max-width: 600px){
    .user_details_group .user_details .details .long_text{
        grid-column-start: 1;
        grid-column-end: 3;
    }
}
</style>