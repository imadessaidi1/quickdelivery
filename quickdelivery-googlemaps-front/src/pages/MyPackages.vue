<template>
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
.grid-container {
  display: grid;
  grid-template-columns: repeat(3, 1fr); /* Trois colonnes égales */
  gap: 20px; /* Marge entre les éléments */
}

.grid-item {
  margin-bottom: 20px;
}

.item-content {
  display: flex;
  align-items: center;
}

.item-image {
  width: 80px;
  margin-right: 20px;
}

.item-details {
  flex-grow: 1;
}

</style>
