<template>
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
                <div class="adresse_line">
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
                <div class="adresse_line">
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
    background: linear-gradient(135deg, rgba(255, 255, 255, 0.1), rgba(255, 255, 255, 0));
    backdrop-filter: blur(10px);
    border-radius: 20px;
    border:1px solid rgba(0, 0, 0, 0.18);
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
        margin: 12px 0;
        box-shadow: 2px 2px 11px -2px rgba(0, 0, 0, 0.37);
    }
    .package_details_group .package_details .details{
        width: 100%;
        display: inline-grid;
        grid-template-columns: auto auto;
    }
    .package_details_group .package_details .details .adresse_line{
        grid-column-start: 1;
        grid-column-end: 3;
    }
    .package_details_group .package_details .details div{
        font-size: 13px;
    }
}
</style>