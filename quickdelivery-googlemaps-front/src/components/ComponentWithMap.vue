<template>
    <div v-for="(m, index) in markers" :key="index">
        Position : {{m.lat}},{{m.lng}}
      </div>
      <div>
  <GMapMap
    :center="{ lat: latitude, lng: longitude }"
    :zoom="13"
    map-type-id="terrain"
    style="width: 100vw; height: 35rem"
  >
    <GMapCluster :zoomOnClick="true" :styles="[
                                               {
                                                 textColor: 'black',
                                                 url: 'https://raw.githubusercontent.com/googlemaps/v3-utility-library/37c2a570c318122df57b83140f5f54665b9359e5/packages/markerclustererplus/images/m1.png',
                                                 height: 52,
                                                 width: 53,
                                               },
                                               {
                                                 textColor: 'black',
                                                 url: 'https://raw.githubusercontent.com/googlemaps/v3-utility-library/37c2a570c318122df57b83140f5f54665b9359e5/packages/markerclustererplus/images/m2.png',
                                                 height: 55,
                                                 width: 56,
                                               },
                                               {
                                                 textColor: 'black',
                                                 url: 'https://raw.githubusercontent.com/googlemaps/v3-utility-library/37c2a570c318122df57b83140f5f54665b9359e5/packages/markerclustererplus/images/m3.png',
                                                 height: 65,
                                                 width: 66,
                                               },
                                               {
                                                 textColor: 'black',
                                                 url: 'https://raw.githubusercontent.com/googlemaps/v3-utility-library/37c2a570c318122df57b83140f5f54665b9359e5/packages/markerclustererplus/images/m4.png',
                                                 height: 77,
                                                 width: 78,
                                               },
                                               {
                                                 textColor: 'black',
                                                 url: 'https://raw.githubusercontent.com/googlemaps/v3-utility-library/37c2a570c318122df57b83140f5f54665b9359e5/packages/markerclustererplus/images/m5.png',
                                                 height: 89,
                                                 width: 90,
                                               },
                                             ]">
      <GMapMarker
        :key="index"
        v-for="(m, index) in markers"
        :position="m.position"
        :clickable="true"
        :draggable="true"
        @click="center = m.position"
      />
    </GMapCluster>
  </GMapMap>
  </div>
</template>

<script>
import axios from 'axios';

export default {
  data() {
    return {
      latitude: null,
      longitude: null,
      markers: null,
    };
  },
  async mounted() {
      await this.getLocation();
      this.markers = await this.fetchDataFromSpringBoot();
      console.log(this.markers);
    },
    methods: {
      async getLocation() {
        return new Promise((resolve, reject) => {
          if (navigator.geolocation) {
            navigator.geolocation.getCurrentPosition(
              (position) => {
                this.latitude = parseFloat(position.coords.latitude);
                this.longitude = parseFloat(position.coords.longitude);
                resolve({ latitude: this.latitude, longitude: this.longitude });
              },
              (error) => {
                console.error('Erreur de géolocalisation :', error.message);
                reject(error);
              }
            );
          } else {
            console.error('La géolocalisation n\'est pas prise en charge par ce navigateur.');
            reject(new Error('La géolocalisation n\'est pas prise en charge par ce navigateur.'));
          }
        });
      },
      async fetchDataFromSpringBoot() {
        const apiUrl = `http://localhost:8082/packages/v1/packages-around?latitude=${this.latitude}&longitude=${this.longitude}&rayonEnMetres=500`;

        console.log(apiUrl);

        try {
          const response = await axios.get(apiUrl);
          const data = response.data;
          const addressPoints = this.initMarkers(data);
          return Promise.resolve(addressPoints);
        } catch (error) {
          console.error('Erreur lors de la récupération des données:', error);
          return Promise.reject(error);
        }
      },
      initMarkers(data){
        var addressPoints = [];
        data.forEach(item => {
            item.addresses.forEach(address => {
                if(address.type == 'DEPARTURE'){
                    const position = {
                        lat: address.latitude,
                        lng: address.longitude
                    };
                    addressPoints.push(position);
                }
            });
        });
        return addressPoints;
      }
    },
};
</script>

<style>
body {
  margin: 0;
}
</style>
