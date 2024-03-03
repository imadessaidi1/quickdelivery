<template>
    <h2>Collie N° : {{ package_.id }}</h2>
    <div class="package_details_group">
        <div class="package_details">
            <h3>{{$t('createNewPackage')}}</h3>
            <div class="details">
                <div>
                    <strong>{{$t('packageHeight')}}:</strong> {{package_.height}}
                </div>
                <div>
                    <strong>{{$t('packageWidth')}}:</strong> {{package_.width}}
                </div>
                <div>
                    <strong>{{$t('packageDepth')}}:</strong> {{package_.depth}}
                </div>
                <div>
                    <strong>{{$t('packageWeight')}}:</strong> {{package_.weight}}
                </div>
            </div>
            <!--<div>
                {{$t('packagePicture')}}: {{documentS[0].name}}
            </div>
            <div>
                {{$t('packageInvoice')}}: {{documentS[1].name}}
            </div>-->
        </div>
        <div class="package_details">
            <h3>{{$t('packageAddressDepartureAddresses')}}</h3>
            <div class="details">
                <div>
                    <strong>{{$t('packageAddressFirstName')}}:</strong> {{package_.addresses[0].firstName}}
                </div>
                <div>
                    <strong>{{$t('packageAddressLastName')}}:</strong> {{package_.addresses[0].lastName}}
                </div>
                <div>
                    <strong>{{$t('packageAddressEmail')}}:</strong> {{package_.addresses[0].email}}
                </div>
                <div>
                    <strong>{{$t('packageAddressPhone')}}:</strong> {{package_.addresses[0].phone}}
                </div>
                <div class="adresse_line">
                    <strong>{{$t('packageAddressLine1')}}:</strong> {{package_.addresses[0].line1}}
                </div>
                <div>
                    <strong>{{$t('packageAddressZip')}}:</strong> {{package_.addresses[0].zipCode}}
                </div>
                <div>
                    <strong>{{$t('packageAddressCity')}}:</strong> {{package_.addresses[0].town}}
                </div>
                <div>
                    <strong>{{$t('packageAddressCountry')}}:</strong> {{package_.addresses[0].country}}
                </div>
                <div>
                    <strong>{{$t('packageAddressFloor',{ state: $t('packageAddressFloorStatePickup') })}}:</strong> {{package_.addresses[0].floor}}
                </div>
                <div class="dateTime_line">
                    <strong>{{$t('packageAddressDepartureTime',{ state: $t('packageAddressFloorStatePickup') })}}:</strong> {{ formatDate(package_.addresses[0].dateTime) }}
                </div>
            </div>
            
        </div>
        <div class="package_details">
            <h3>{{$t('packageAddressArrivalAddresses')}}</h3>
            <div class="details">
                <div>
                    <strong>{{$t('packageAddressFirstName')}}:</strong> {{package_.addresses[1].firstName}}
                </div>
                <div>
                    <strong>{{$t('packageAddressLastName')}}:</strong> {{package_.addresses[1].lastName}}
                </div>
                <div>
                    <strong>{{$t('packageAddressEmail')}}:</strong> {{package_.addresses[1].email}}
                </div>
                <div>
                    <strong>{{$t('packageAddressPhone')}}:</strong> {{package_.addresses[1].phone}}
                </div>
                <div class="adresse_line">
                    <strong>{{$t('packageAddressLine1')}}:</strong> {{package_.addresses[1].line1}}
                </div>
                <div>
                    <strong>{{$t('packageAddressZip')}}:</strong> {{package_.addresses[1].zipCode}}
                </div>
                <div>
                    <strong>{{$t('packageAddressCity')}}:</strong> {{package_.addresses[1].town}}
                </div>
                <div>
                    <strong>{{$t('packageAddressCountry')}}:</strong> {{package_.addresses[1].country}}
                </div>
                <div>
                    <strong>{{$t('packageAddressFloor',{ state: $t('packageAddressFloorStateDelivery') })}}:</strong> {{package_.addresses[1].floor}}
                </div>
                <div class="dateTime_line">
                    <strong>{{$t('packageAddressDepartureTime',{ state: $t('packageAddressFloorStateDelivery') })}}:</strong> {{ formatDate(package_.addresses[1].dateTime) }}
                </div>
            </div>
        </div>
    </div>

</template>
<script>
export default {
    computed: {
        package_() {
          return this.$store.state.package_;
        },
        documentS() {
          return this.$store.state.documentS;
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