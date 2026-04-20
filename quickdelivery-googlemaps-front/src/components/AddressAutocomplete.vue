<template>
  <div ref="containerRef" class="address-autocomplete-host" />
</template>

<script>
const GOOGLE_MAPS_KEY = process.env.VUE_APP_GOOGLE_MAPS_KEY || '';
const SCRIPT_ID = 'quickdelivery-google-places-script';
const SCRIPT_CALLBACK = '__qdGooglePlacesLoaded';
let googlePlacesPromise = null;

function isPlacesApiReady() {
  return Boolean(
    window.google?.maps?.importLibrary
    || window.google?.maps?.places?.Autocomplete
  );
}

function loadGooglePlaces() {
  if (!GOOGLE_MAPS_KEY) {
    return Promise.reject(new Error('Missing VUE_APP_GOOGLE_MAPS_KEY'));
  }

  if (isPlacesApiReady()) {
    return Promise.resolve(window.google);
  }

  if (googlePlacesPromise) {
    return googlePlacesPromise;
  }

  googlePlacesPromise = new Promise((resolve, reject) => {
    const existingScript = document.getElementById(SCRIPT_ID);
    if (existingScript) {
      existingScript.addEventListener('load', () => {
        if (isPlacesApiReady()) {
          resolve(window.google);
          return;
        }
        setTimeout(() => {
          if (isPlacesApiReady()) {
            resolve(window.google);
          } else {
            reject(new Error('Google Places library did not become ready after script load'));
          }
        }, 250);
      });
      existingScript.addEventListener('error', reject);
      return;
    }

    window[SCRIPT_CALLBACK] = () => {
      if (isPlacesApiReady()) {
        resolve(window.google);
      } else {
        setTimeout(() => {
          if (isPlacesApiReady()) {
            resolve(window.google);
          } else {
            reject(new Error('Google Places callback fired without Places API readiness'));
          }
        }, 250);
      }
    };

    const script = document.createElement('script');
    script.id = SCRIPT_ID;
    script.async = true;
    script.defer = true;
    script.src = `https://maps.googleapis.com/maps/api/js?key=${GOOGLE_MAPS_KEY}&loading=async&v=weekly&libraries=places&callback=${SCRIPT_CALLBACK}`;
    script.onerror = (error) => reject(error);
    document.head.appendChild(script);
  }).catch((error) => {
    googlePlacesPromise = null;
    delete window[SCRIPT_CALLBACK];
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
      autocompleteElement: null,
      placeAutocompleteCtor: null,
    };
  },
  watch: {
    modelValue(value) {
      if (value !== this.address) {
        this.address = value || '';
        this.syncAutocompleteValue();
      }
    },
    existingAddress(value) {
      if (!this.modelValue && value !== this.address) {
        this.address = value || '';
        this.syncAutocompleteValue();
      }
    },
  },
  mounted() {
    this.initAutocomplete();
  },
  beforeUnmount() {
    this.teardownAutocomplete();
  },
  methods: {
    emitAddressUpdate(nextAddress) {
      this.address = nextAddress;
      this.$emit('update:modelValue', this.address);
    },
    handleInput(event) {
      const nextAddress = event?.target?.value || event?.target?.inputValue || '';
      this.emitAddressUpdate(nextAddress);
      this.$emit('place-selected', {
        formattedAddress: this.address,
        latitude: null,
        longitude: null,
      });
    },
    syncAutocompleteValue() {
      if (!this.autocompleteElement) {
        return;
      }
      this.autocompleteElement.value = this.address;
    },
    syncAutocompleteAttributes() {
      if (!this.autocompleteElement) {
        return;
      }
      const passthroughAttributes = ['id', 'name', 'placeholder', 'required', 'aria-label'];
      passthroughAttributes.forEach((attributeName) => {
        const attributeValue = this.$attrs[attributeName];
        if (attributeValue === undefined || attributeValue === false || attributeValue === null) {
          this.autocompleteElement.removeAttribute(attributeName);
          return;
        }
        if (attributeValue === true) {
          this.autocompleteElement.setAttribute(attributeName, '');
          return;
        }
        this.autocompleteElement.setAttribute(attributeName, String(attributeValue));
      });
      this.autocompleteElement.placeholder = String(this.$attrs.placeholder || '');
      this.autocompleteElement.name = String(this.$attrs.name || '');
      this.autocompleteElement.value = this.address;
      this.autocompleteElement.includedRegionCodes = ['fr'];
      if (this.$attrs.disabled) {
        this.autocompleteElement.setAttribute('disabled', '');
      } else {
        this.autocompleteElement.removeAttribute('disabled');
      }
    },
    async handlePlaceSelection(event) {
      const prediction = event?.placePrediction || event?.place;
      if (!prediction) {
        return;
      }
      const place = typeof prediction.toPlace === 'function' ? prediction.toPlace() : prediction;

      // Fix P2 — cost reduction: avoid fetchFields when location is already present.
      // fetchFields({ fields: ['formattedAddress', 'location'] }) costs ~17 $/1000.
      // The suggestion text from the event is free and sufficient for formattedAddress.
      let latitude = place?.location?.lat?.() ?? place?.geometry?.location?.lat?.() ?? null;
      let longitude = place?.location?.lng?.() ?? place?.geometry?.location?.lng?.() ?? null;

      if (latitude === null || longitude === null) {
        // Location not yet available — fetch only the fields we actually need
        if (typeof place.fetchFields === 'function') {
          await place.fetchFields({ fields: ['location'] });
          latitude = place?.location?.lat?.() ?? null;
          longitude = place?.location?.lng?.() ?? null;
        }
      }

      // Prefer the free suggestion text; fall back to formattedAddress only if already fetched
      const formattedAddress = event?.placePrediction?.text?.toString()
        || place?.formattedAddress
        || place?.formatted_address
        || this.address
        || '';

      this.emitAddressUpdate(formattedAddress);
      this.$emit('place-selected', {
        formattedAddress,
        latitude,
        longitude,
      });
    },
    handleLegacyPlaceSelection() {
      const place = this.autocompleteInstance?.getPlace?.();
      const formattedAddress = place?.formatted_address || this.address || '';
      const latitude = place?.geometry?.location?.lat?.() ?? null;
      const longitude = place?.geometry?.location?.lng?.() ?? null;
      this.emitAddressUpdate(formattedAddress);
      this.$emit('place-selected', {
        formattedAddress,
        latitude,
        longitude,
      });
    },
    createPlaceAutocompleteElement() {
      if (!this.placeAutocompleteCtor) {
        throw new Error('Google PlaceAutocompleteElement is unavailable');
      }

      const element = new this.placeAutocompleteCtor({
        includedRegionCodes: ['fr'],
      });
      element.classList.add('qd-place-autocomplete');
      element.addEventListener('input', (event) => this.handleInput(event));
      element.addEventListener('change', (event) => this.handleInput(event));
      element.addEventListener('gmp-select', (event) => this.handlePlaceSelection(event));
      element.addEventListener('gmp-placeselect', (event) => this.handlePlaceSelection(event));
      this.$refs.containerRef.replaceChildren(element);
      this.autocompleteElement = element;
      this.autocompleteInstance = null;
      this.syncAutocompleteAttributes();
    },
    instantiateLegacyAutocomplete() {
      if (!window.google?.maps?.places?.Autocomplete) {
        throw new Error('Google Places Autocomplete is unavailable');
      }
      const input = document.createElement('input');
      input.type = 'text';
      input.autocomplete = 'off';
      input.className = 'qd-place-autocomplete address-autocomplete-input';
      this.autocompleteElement = input;
      this.$refs.containerRef.replaceChildren(input);
      this.syncAutocompleteAttributes();
      input.addEventListener('input', (event) => this.handleInput(event));
      this.autocompleteInstance = new window.google.maps.places.Autocomplete(input, {
        fields: ['formatted_address', 'geometry'],
        componentRestrictions: { country: ['fr'] },
      });
      this.autocompleteInstance.addListener('place_changed', () => this.handleLegacyPlaceSelection());
    },
    teardownAutocomplete() {
      if (this.autocompleteElement) {
        this.autocompleteElement.replaceChildren?.();
        this.autocompleteElement.remove?.();
        this.autocompleteElement = null;
      }
      if (this.autocompleteInstance) {
        window.google?.maps?.event?.clearInstanceListeners?.(this.autocompleteInstance);
        this.autocompleteInstance = null;
      }
    },
    async initAutocomplete() {
      try {
        await loadGooglePlaces();
        if (!this.$refs.containerRef) {
          return;
        }
        let placesLibrary = null;
        if (typeof window.google?.maps?.importLibrary === 'function') {
          placesLibrary = await window.google.maps.importLibrary('places');
          this.placeAutocompleteCtor = placesLibrary?.PlaceAutocompleteElement || null;
        }
        if (this.placeAutocompleteCtor) {
          this.createPlaceAutocompleteElement();
          return;
        }
        this.instantiateLegacyAutocomplete();
      } catch (error) {
        console.error('Failed to initialize Google Places autocomplete', error);
      }
    },
  },
};
</script>

<style scoped>
.address-autocomplete-host {
  width: 100%;
  min-width: 0;
  border-radius: 14px;
}

.address-autocomplete-host :deep(.address-autocomplete-input) {
  width: 100%;
  min-height: 48px;
  display: block;
  box-sizing: border-box;
  padding: 0 14px;
  border: 1px solid #ced7e4;
  border-radius: 14px;
  background: #fff;
  color: #172132;
}

.address-autocomplete-host :deep(.qd-place-autocomplete) {
  width: 100%;
  min-height: 48px;
  display: block;
  background: #fff;
  border: 1px solid #ced7e4;
  border-radius: 14px;
  box-shadow: none;
  box-sizing: border-box;
  color: #172132;
}

.address-autocomplete-host :deep(gmp-place-autocomplete.qd-place-autocomplete) {
  --gmpx-color-surface: #fff;
  --gmpx-color-on-surface: #172132;
  --gmpx-color-outline: #ced7e4;
  --gmpx-shape-medium: 14px;
  --gmpx-font-family-base: inherit;
  padding: 0;
}

.address-autocomplete-host :deep(input.address-autocomplete-input) {
  padding: 0 14px;
}
</style>
