<template>
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
        :position="m[2]"
        :clickable="true"
        :draggable="false"
        @click="openInfoWindow(m[0])">
            <GMapInfoWindow
                        v-if="m[1] != ''"
                        @closeclick="infoWindowOpened = false"
                        :opened="infoWindowOpened && selectedMarker == m[0]"
                        :key="m[0]"
                        :options="{
                          pixelOffset: {
                            width: 10,
                            height: 0
                          },
                          maxWidth: 320,
                          maxHeight: 320
                        }"

                      >
                        <div class="location-details">
                            <p> {{m[1].line1}} </p>
                            <p> {{m[1].line2}} </p>
                            <p> {{m[1].zipCode}} </p>
                            <p> {{m[1].town}} </p>
                            <p> {{m[1].country}} </p>
                            <a @click="reservePackage(m[0])">Reserve</a>
                        </div>
                      </GMapInfoWindow>
      </GMapMarker>
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
      infoWindowOpened: false,
      selectedMarker: null,
    };
  },
  async mounted() {
      await this.getLocation();
      this.markers = await this.fetchDataFromSpringBoot();
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
                    var addressPointDetail = [];
                    const position = {
                        lat: parseFloat(address.latitude)+0,
                        lng: parseFloat(address.longitude)+0
                    };

                    addressPointDetail.push(item.id);
                    addressPointDetail.push(address);
                    addressPointDetail.push(position);
                    addressPoints.push(addressPointDetail);
                }
            });
        });
        return addressPoints;
      },
      openInfoWindow(m) {
              this.infoWindowOpened = true;
              this.selectedMarker = m;
      },
      reservePackage(m){
        const apiUrl = `http://localhost:8082/packages/v1/update-packages-status?${m}=RESERVED`;
        axios.put(apiUrl, null)
          .then(response => {
            console.log('Réponse de la requête POST:', response.data);
          })
          .catch(error => {
            console.error('Erreur de la requête POST:', error);
          });
          console.log(m);
      }
    },
};
</script>

<style>
body {
  margin: 0;
}
</style>
