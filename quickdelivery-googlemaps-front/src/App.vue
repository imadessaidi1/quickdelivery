<template>
  <div>
  <button @click="openModal">Ouvrir la modal</button>
    <SearchBar />
    <div class="main-content">
      <MarkerList :style="{ width: '30%' }"/>
      <GoogleMap :style="{ width: '60%' }"/>
    </div>
    <AppModal ref="AppModal">
          <!-- Contenu de votre modal -->
          <h2>Contenu de la modal</h2>
          <!-- Autres éléments de la modal -->
        </AppModal>
    <AppFooter />
  </div>
</template>

<script>
import SearchBar from './components/SearchBar.vue';
import GoogleMap from './components/GoogleMap.vue';
import MarkerList from './components/MarkerList.vue';
import AppFooter from './components/AppFooter.vue';
import AppModal from "@/components/AppModal.vue";

export default {
  components: {
    SearchBar,
    GoogleMap,
    MarkerList,
    AppFooter,
    AppModal,
  },
  methods: {
      openModal() {
            this.$refs.AppModal.openModal();
          },
    },
};
</script>

<style>
.main-content {
  display: flex;
}
</style>
