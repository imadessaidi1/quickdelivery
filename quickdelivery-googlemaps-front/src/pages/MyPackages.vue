<template>
  <div class="myPackeges" @scroll="handleScroll">
    <div>
      <PackagesFilter />
      <div v-if="isLoadingPage" class="page-state">{{ $t('stateLoading') }}</div>
      <div v-else-if="loadError" class="page-state error">{{ $t('stateLoadError') }}</div>
      <div v-else-if="packagesByStatus.length === 0" class="page-state">{{ $t('stateEmptyPackages') }}</div>
      <div v-for="status in packagesByStatus" :key="status.satuts_">
        <h2 :id="status.satuts_">{{ $t(status.satuts_) }}</h2>
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
      isLoadingPage: false,
      loadError: false,
    };
  },
  mounted() {
    this.fetchData();
  },
  methods: {
    getPackageModal() {
      return this.$refs.AppModal;
    },
    async fetchData() {
      this.isLoadingPage = true;
      this.loadError = false;
      this.packagesByStatus = [];
      try {
        const response = await http.get(this.$i18n.t('rootURL') + this.$i18n.t('getPackagesByDeliveryPersonUrl') + this.$store.state.connectedUser.id);
        Object.entries(response.data).forEach(([status, packagesArray]) => {
          if (typeof status === 'string' && Array.isArray(packagesArray)) {
             this.packagesByStatus.push({ satuts_: status, groupedPackagesList: packagesArray });
          }
        });
      } catch (error) {
        this.loadError = true;
        console.error('Unable to load packages list.', error);
      } finally {
        this.isLoadingPage = false;
      }
    },
  },
};
</script>

<style>
.myPackeges{
  height: 100%;
  padding: 0 15px;
  background: #F9F7F7;
}
.myPackeges h2{
  padding-left: 10px;
  margin-left: 50px;
}
.page-state {
  width: 85%;
  margin: 10px auto 0 auto;
  padding: 12px;
  border-radius: 8px;
  background: #eef3f9;
  color: #3a4b5f;
  text-align: center;
}
.page-state.error {
  background: #fcecee;
  color: #b1354b;
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
  background: #F5F5F5;
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
