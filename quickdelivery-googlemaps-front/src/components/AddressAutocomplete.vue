<template>
  <input
    v-bind="$attrs"
    ref="inputRef"
    :value="address"
    type="text"
    autocomplete="off"
    class="address-autocomplete-input"
    @input="handleInput"
  >
</template>

<script>
const GOOGLE_MAPS_KEY = 'AIzaSyCaxhp9_GMSkdHTlv23mIVVlO_mq17Yn6U';
const SCRIPT_ID = 'quickdelivery-google-places-script';
let googlePlacesPromise = null;

function loadGooglePlaces() {
  if (window.google?.maps?.places) {
    return Promise.resolve(window.google);
  }

  if (googlePlacesPromise) {
    return googlePlacesPromise;
  }

  googlePlacesPromise = new Promise((resolve, reject) => {
    const existingScript = document.getElementById(SCRIPT_ID);
    if (existingScript) {
      existingScript.addEventListener('load', () => resolve(window.google));
      existingScript.addEventListener('error', reject);
      return;
    }

    const script = document.createElement('script');
    script.id = SCRIPT_ID;
    script.async = true;
    script.defer = true;
    script.src = `https://maps.googleapis.com/maps/api/js?key=${GOOGLE_MAPS_KEY}&libraries=places`;
    script.onload = () => resolve(window.google);
    script.onerror = reject;
    document.head.appendChild(script);
  }).catch((error) => {
    googlePlacesPromise = null;
    throw error;
  });

  return googlePlacesPromise;
}

export default {
  inheritAttrs: false,
  props: {
    modelValue: {
      type: String,
      default: '',
    },
    existingAddress: {
      type: String,
      default: '',
    },
  },
  emits: ['update:modelValue', 'place-selected'],
  data() {
    return {
      address: this.modelValue || this.existingAddress || '',
      autocompleteInstance: null,
    };
  },
  watch: {
    modelValue(value) {
      if (value !== this.address) {
        this.address = value || '';
      }
    },
    existingAddress(value) {
      if (!this.modelValue && value !== this.address) {
        this.address = value || '';
      }
    },
  },
  mounted() {
    this.initAutocomplete();
  },
  beforeUnmount() {
    if (this.autocompleteInstance) {
      window.google?.maps?.event?.clearInstanceListeners?.(this.autocompleteInstance);
    }
  },
  methods: {
    handleInput(event) {
      this.address = event.target.value;
      this.$emit('update:modelValue', this.address);
      this.$emit('place-selected', {
        formattedAddress: this.address,
        latitude: null,
        longitude: null,
      });
    },
    async initAutocomplete() {
      try {
        await loadGooglePlaces();
        if (!this.$refs.inputRef || !window.google?.maps?.places) {
          return;
        }

        this.autocompleteInstance = new window.google.maps.places.Autocomplete(this.$refs.inputRef, {
          fields: ['formatted_address', 'geometry'],
        });

        this.autocompleteInstance.addListener('place_changed', () => {
          const place = this.autocompleteInstance.getPlace();
          const formattedAddress = place?.formatted_address || this.$refs.inputRef.value || '';
          const latitude = place?.geometry?.location?.lat?.() ?? null;
          const longitude = place?.geometry?.location?.lng?.() ?? null;
          this.address = formattedAddress;
          this.$emit('update:modelValue', formattedAddress);
          this.$emit('place-selected', {
            formattedAddress,
            latitude,
            longitude,
          });
        });
      } catch (error) {
        console.error('Failed to initialize Google Places autocomplete', error);
      }
    },
  },
};
</script>

<style scoped>
.address-autocomplete-input {
  width: 100%;
  min-height: 48px;
  padding: 0 14px;
  border: 1px solid #ced7e4;
  border-radius: 14px;
  box-sizing: border-box;
  background: #fff;
}
</style>
