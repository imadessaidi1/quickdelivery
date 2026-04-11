<template>
  <div class="google-map">
    <!-- Google Maps intégré ici -->
    <div id="map">
    <iframe
            ref="map"
            width="100%"
          height="100%"
          :src="googleMapPath"
          style="border:0;"
          v-on:load="onLoadIframe"
          name="map"
        ></iframe>
    </div>
  </div>
</template>

<script>
import { getGatewayBaseUrl } from '@/config/network';

const MAP_IFRAME_ASSET_VERSION = '20260411-no-route-zoom-v3';

export default {
  emits: ['map-iframe-loaded'],
  computed: {
    googleMapPath() {
      const deliveryMode = this.$store.state.connectedUser?.deliveryMode || '';
      const vehicleType = this.$store.state.connectedUser?.primaryVehicleType
        || this.$store.state.connectedUser?.vehicles?.find((vehicle) => vehicle?.type)?.type
        || '';
      const userId = this.$store.state.connectedUser?.id || '';
      const gatewayBaseUrl = getGatewayBaseUrl();
      const params = new URLSearchParams();
      if (userId) {
        params.set('userID', String(userId));
      }
      if (deliveryMode) {
        params.set('deliveryMode', deliveryMode);
      }
      if (vehicleType) {
        params.set('vehicleType', vehicleType);
      }
      if (gatewayBaseUrl) {
        params.set('gatewayBaseUrl', gatewayBaseUrl);
      }
      params.set('mapAssetVersion', MAP_IFRAME_ASSET_VERSION);
      const query = params.toString();
      return `${process.env.BASE_URL}google-maps.html${query ? `?${query}` : ''}`;
    },
  },
  methods: {
    onLoadIframe() {
      this.$emit('map-iframe-loaded');
    },
  },
};
</script>

<style scoped>
#map{
  height: 100%;
}
</style>
