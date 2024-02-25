<template>
    <GoogleAddressAutocomplete
            apiKey="AIzaSyCBZNoGNPCcqSsTYTlOUhkXLhb47HqF9mM"
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