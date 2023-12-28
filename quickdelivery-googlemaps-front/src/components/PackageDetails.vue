<template>
    <div>
        <h2>{{$t('createNewPackage')}}</h2>
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
        <div>
            {{$t('packagePicture')}}: {{documentS[0].name}}
        </div>
        <div>
            {{$t('packageInvoice')}}: {{documentS[1].name}}
        </div>
    </div>
    <div>
        <h2>{{$t('packageAddressDepartureAddresses')}}</h2>
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
    <div>
        <h2>{{$t('packageAddressArrivalAddresses')}}</h2>
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
</style>