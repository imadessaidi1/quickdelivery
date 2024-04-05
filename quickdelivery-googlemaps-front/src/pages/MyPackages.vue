<template>
  <div class="myPackeges" @scroll="handleScroll">
    <div>
      <PackagesFilter />
      <div v-for="status in packagesByStatus" :key="status.satuts_">
        <h2 :id="status.satuts_">{{$t(status.satuts_)}}</h2>
        <div class="grid-container">
            <div v-for="package_ in status.groupedPackagesList" :key="package_.id" class="grid-item">
                <div class="item-content">
                    <SummarizedPackageDetail :package_="package_" :modal ="getPackageModal()"/>
                </div>
            </div>
        </div>
      </div>
    </div>
    <PackageDetailsModal ref="AppModal" classe="modal"/>
  </div>
  <PackageDetailsModal ref="AppModal" classe="modal"/>
</template>

<script>
import PackagesFilter from '../components/PackagesFilter.vue';
import SummarizedPackageDetail from '../components/SummarizedPackageDetail.vue';
import PackageDetailsModal from '../components/PackageDetailsModal.vue';
import http from '@/config/httpInterceptor';

export default {
  components: {
    PackagesFilter,
    SummarizedPackageDetail,
    PackageDetailsModal,
  },
  data() {
    return {
      packagesByStatus: [],
    };
  },
  mounted() {
    this.fetchData();
  },
  methods: {
    getPackageModal(){
        return this.$refs.AppModal;
    },
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
  height: 100%;
  padding: 0 15px;
  overflow: scroll;
  background-color: #FAD961;
  background-image: linear-gradient(135deg, #FAD961 0%, #F76B1C 100%);
}
.myPackeges h2{
  padding-left: 10px;
  margin-left: 50px;
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
  width: 350px;
  margin: 15px 15px;
  padding-bottom: 40px;
  border-radius: 10px;
  box-shadow: rgba(50, 50, 93, 0.25) 0px 2px 5px -1px, rgba(0, 0, 0, 0.3) 0px 1px 3px -1px;
  position: relative;
  background: #eeeeee;
}
#RESERVED{
  border-left: solid 5px #8350c2;
  color: #8350c2;
}
#NEW{
  border-left: solid 5px #009a00;
  color: #009a00;
}
#PICKEDUP{
  border-left: solid 5px #800080;
  color: #800080;
}
#PAYMENTPENDING{
  border-left: solid 5px #ff2f00;
  color: #ff2f00;
}
#DELIVERED{
  border-left: solid 5px #228B22;
  color: #228B22;
}
#INDELIVERY{
  border-left: solid 5px #FFA500;
  color: #FFA500;
}
.item-buttons{
  width: 100%;
  height: 60px;
  position: absolute;
  bottom: 0;
  display: flex;  
  justify-content: center;  
  align-items: center;
}
.item-buttons button{
  display: block;
}
@media screen and (max-width: 600px) {
  .grid-container{
    width: 95%;
  }
}
</style>