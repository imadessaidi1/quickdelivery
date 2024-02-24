<template>
    <div class="package_details_group">
        <div class="package_details">
            <h3>User Info.</h3>
            <div class="details">
                <div><strong>{{$t('packageAddressFirstName')}}:</strong> {{ user.firstName }}</div>
                <div><strong>{{$t('packageAddressLastName')}}:</strong> {{ user.lastName }}</div>
                <div><strong>{{$t('packageAddressEmail')}}:</strong> {{ user.emailAddress }}</div>
                <div><strong>{{$t('packageAddressPhone')}}:</strong> {{ user.phone }}</div>
            </div>
            <h3>Address:</h3>
            <div v-for="(address, index) in user.personalAddress" :key="index">
                <div><strong>{{$t('packageAddressLine1')}}:</strong> {{ address.line1 }}</div>
                <div><strong>{{$t('packageAddressCity')}}:</strong> {{ address.town }}</div>
                <div><strong>{{$t('packageAddressZip')}}:</strong> {{ address.zipCode }}</div>
                <div><strong>{{$t('packageAddressCountry')}}:</strong> {{ address.country }}</div>
            </div>
        </div>
        <div class="package_details">
            <h3>{{$t('packageAddressDepartureAddresses')}}</h3>
            <div class="details">
                <h3>{{$t('userDocuments')}}</h3>
                <div v-if="user.documents && user.documents.length">
                    <div v-for="(document, index) in user.documents" :key="index">
                        <div><strong>{{ document.type }}:</strong> {{ document.file.name }}</div>
                    </div>
                </div>

                <h3>{{$t('userPaymentModes')}}</h3>
                <div v-if="user.paymentModes">
                    <div v-for="(paymentMode, key) in user.paymentModes" :key="key">
                        <h4>{{ key }}</h4>
                        <div v-if="key === 'CREDIT_CARD'">
                            <div><strong>{{$t('userCardNumber')}}:</strong> {{ paymentMode.cardNumber }}</div>
                            <div><strong>{{$t('userCardExpiryDate')}}:</strong> {{ paymentMode.expiryDate }}</div>
                            <div><strong>{{$t('userCardCVV')}}:</strong> {{ paymentMode.cvv }}</div>
                        </div>
                        <div v-if="key === 'IBAN'">
                            <div><strong>{{$t('userIBAN')}}:</strong> {{ paymentMode.iban }}</div>
                            <div><strong>{{$t('userIBANBIC')}}:</strong> {{ paymentMode.bic }}</div>
                            <div><strong>{{$t('userRIB')}}:</strong> {{ paymentMode.ribDocument.file.name }}</div>
                        </div>
                    </div>
                </div>
            </div>
        </div>
        <div class="package_details">
            <h3>{{$t('userVehicle')}}</h3>
            <div class="details">
                <div><strong>{{$t('userVehicleRegistration')}}:</strong> {{ user.vehicle.registrationNumber }}</div>
                <div><strong>{{$t('userVehicleBrand')}}:</strong> {{ user.vehicle.brand }}</div>
                <div><strong>{{$t('userVehicleModel')}}:</strong> {{ user.vehicle.model }}</div>
                <div><strong>{{$t('userVehicleEnergy')}}:</strong> {{ user.vehicle.energyType }}</div>
                <h4>Vehicle Documents</h4>
                <div v-if="user.vehicle.vehicleDocuments && user.vehicle.vehicleDocuments.length">
                    <div v-for="(document, index) in user.vehicle.vehicleDocuments" :key="index">
                        <div><strong>{{ document.type }}:</strong> {{ document.file.name }}</div>
                    </div>
                </div>
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
            console.log(this.$store.state.user);
            return this.$store.state.user;
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
.package_details_group{
    width: 100%;
    display: flex;
    justify-content: space-evenly;
}
.package_details_group .package_details{
    width: 30%;
    padding: 10px;
    border-radius: 5px;
    background-color: #f5f5f5ca;
}
.package_details_group .package_details h3{
    margin-left: 15px;
    padding-left: 6px;
    border-left: solid 3px #42ba96;
} 
.package_details_group .package_details div{
    padding: 6px 0 6.5px 0;
}
.package_details_group .package_details .details div{
    font-size: 14px;
}
@media screen and (max-width: 1100px){
    .package_details_group {
        flex-direction: column;
    }
    .package_details_group .package_details{
        width: 95%;
        margin: 5px 0;
    }
    .package_details_group .package_details .details{
        width: 100%;
        display: inline-grid;
        grid-template-columns: auto auto;
    }
    .package_details_group .package_details .details .adresse_line,
    .dateTime_line{
        grid-column-start: 1;
        grid-column-end: 3;
    }
    .package_details_group .package_details .details div{
        font-size: 13px;
    }
}
</style>