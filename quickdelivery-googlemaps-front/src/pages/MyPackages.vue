<template>
  <div class="myPackeges">
    <div v-for="status in packagesByStatus" :key="status.satuts_">
        <h2 :id="status.satuts_">{{status.satuts_}}</h2>
      <div class="grid-container">
          <div v-for="package_ in status.groupedPackagesList" :key="package_.id" class="grid-item">
              <div class="item-content">
                  <SummarizedPackageDetail :package_="package_" />
              </div>
          </div>
      </div>
    </div>
  </div>
</template>

<script>
import SummarizedPackageDetail from '../components/SummarizedPackageDetail.vue';
import http from '@/config/httpInterceptor';

export default {
  components: {
    SummarizedPackageDetail,
  },
  data() {
    return {
      packagesByStatus:[] ,
    };
  },
  mounted() {
    this.fetchData();
  },
  methods: {
    async fetchData() {
      try {
        const response = await http.get(this.$i18n.t('rootURL')+this.$i18n.t('getPackagesByDeliveryPersonUrl')+this.$store.state.connectedUser.id);
        Object.entries(response.data).forEach(([status, packagesArray]) => {
          if(typeof status === 'string' && Array.isArray(packagesArray)){
             this.packagesByStatus.push({satuts_: status, groupedPackagesList: packagesArray});
          }
        });
      } catch (error) {
        console.error('Erreur lors de la requête API', error);
      }
    },
  },
};
</script>

<style>
.myPackeges{
  height: 87%;
  padding: 0 15px;
  overflow: scroll;
}
.grid-container{
  width: 85%;
  margin: 0 auto;
  height: max-content;
  display: flex;
  flex-wrap: wrap;
  justify-content: space-evenly;
}
.grid-item{
  width: 30%;
  margin: 15px 15px;
  border-radius: 10px;
  box-shadow: rgba(50, 50, 93, 0.25) 0px 2px 5px -1px, rgba(0, 0, 0, 0.3) 0px 1px 3px -1px;
}

#RESERVED{
  padding-left: 10px;
  margin-left: 50px;
  border-left: solid 5px #ff7b00;
  color: #ff7b00;
}
</style>
