<template>
  <button v-if="showButton" class="scroll-to-top" @click="scrollToTop">
    <span class="material-symbols-outlined">arrow_upward</span>
  </button>
</template>

<script>
export default {
  data() {
    return {
      showButton: false
    };
  },
  mounted() {
    window.addEventListener('scroll', this.handleScroll);
  },
  beforeUnmount() {
    window.removeEventListener('scroll', this.handleScroll);
  },
  methods: {
    handleScroll() {
      if (window.pageYOffset > 300) { // Vous pouvez ajuster cette valeur selon vos besoins
        this.showButton = true;
      } else {
        this.showButton = false;
      }
    },
    scrollToTop() {
      window.scrollTo({
        top: 0,
        behavior: 'smooth'
      });
    }
  }
};
</script>

<style scoped>
.scroll-to-top {
  position: fixed;
  cursor: pointer;
  bottom: 25px;
  right: 25px;
  width: 50px;
  height: 50px;
  border: none;
  border-radius: 50%;
  background-color: #ffc457;
  box-shadow: rgba(17, 17, 26, 0.1) 0px 4px 16px, rgba(17, 17, 26, 0.05) 0px 8px 32px;
  transition: all 300ms;
  z-index: 9999;
}

.scroll-to-top:hover {
  background-color: #fdb632;
    box-shadow: #ffc45799 0px 4px 16px, #ffc45799 0px 8px 24px, #ffc45799 0px 16px 56px;
}
</style>
