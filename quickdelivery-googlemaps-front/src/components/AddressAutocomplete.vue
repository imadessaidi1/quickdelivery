<template>
    <GoogleAddressAutocomplete
            apiKey="AIzaSyCaxhp9_GMSkdHTlv23mIVVlO_mq17Yn6U"
            v-model="address"
    />
</template>

<script>
import { ref } from 'vue';
import GoogleAddressAutocomplete from 'vue3-google-address-autocomplete'

export default {
    components: {
          GoogleAddressAutocomplete,
        },
    props: {
        existingAddress: String,
    },
    mounted() {
        this.address = this.existingAddress;
    },
    data() {
        return {
            address: ref('')
        }
    },
}
</script>