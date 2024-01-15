<template>
    <div class="package_details_group">
        <div class="package_details">
            <h3>{{$t('createNewPackage')}}</h3>
            <div class="details">
                <div>
                    {{$t('packageHeight')}}: {{package_.height}}
                </div>
                <div>
                    {{$t('packageWidth')}}: {{package_.width}}
                </div>
                <div>
                    {{$t('packageDepth')}}: {{package_.depth}}
                </div>
                <div>
                    {{$t('packageWeight')}}: {{package_.weight}}
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
                    {{$t('packageAddressFirstName')}}: {{package_.addresses[0].firstName}}
                </div>
                <div>
                    {{$t('packageAddressLastName')}}: {{package_.addresses[0].lastName}}
                </div>
                <div>
                    {{$t('packageAddressEmail')}}: {{package_.addresses[0].email}}
                </div>
                <div>
                    {{$t('packageAddressPhone')}}: {{package_.addresses[0].phone}}
                </div>
                <div>
                    {{$t('packageAddressLine1')}}: {{package_.addresses[0].line1}}
                </div>
                <div>
                    {{$t('packageAddressZip')}}: {{package_.addresses[0].zipCode}}
                </div>
                <div>
                    {{$t('packageAddressCity')}}: {{package_.addresses[0].town}}
                </div>
                <div>
                    {{$t('packageAddressCountry')}}: {{package_.addresses[0].country}}
                </div>
            </div>
            
        </div>
        <div class="package_details">
            <h3>{{$t('packageAddressArrivalAddresses')}}</h3>
            <div class="details">
                <div>
                    {{$t('packageAddressFirstName')}}: {{package_.addresses[1].firstName}}
                </div>
                <div>
                    {{$t('packageAddressLastName')}}: {{package_.addresses[1].lastName}}
                </div>
                <div>
                    {{$t('packageAddressEmail')}}: {{package_.addresses[1].email}}
                </div>
                <div>
                    {{$t('packageAddressPhone')}}: {{package_.addresses[1].phone}}
                </div>
                <div>
                    {{$t('packageAddressLine1')}}: {{package_.addresses[1].line1}}
                </div>
                <div>
                    {{$t('packageAddressZip')}}: {{package_.addresses[1].zipCode}}
                </div>
                <div>
                    {{$t('packageAddressCity')}}: {{package_.addresses[1].town}}
                </div>
                <div>
                    {{$t('packageAddressCountry')}}: {{package_.addresses[1].country}}
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
}
</script>
<style>
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
@media screen and (max-width: 1100px){
    .package_details_group {
        flex-direction: column;
    }
    .package_details_group .package_details{
        width: 95%;
        margin: 5px 0;
    }
    .package_details_group .package_details .details{
        display: flex;
        justify-content: space-between;
        flex-wrap: wrap;
    }
    .package_details_group .package_details .details div{
        width: 50%;
    }
}
</style>