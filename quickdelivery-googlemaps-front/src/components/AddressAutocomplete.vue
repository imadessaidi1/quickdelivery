<template>
    <GoogleAddressAutocomplete
            apiKey="AIzaSyCBZNoGNPCcqSsTYTlOUhkXLhb47HqF9mM"
            v-model="address"
            @callback="callbackFunction"
            placeholder="placeholder if you wish"
    />
</template>

<script>
import { ref } from 'vue';
import GoogleAddressAutocomplete from 'vue3-google-address-autocomplete'

export default {
    components: {
          GoogleAddressAutocomplete,
        },
    data() {
        return {
            address: ref('')
        }
    },
    methods: {
        callbackFunction(place) {
           console.log(place);
        },
    },
}
</script>