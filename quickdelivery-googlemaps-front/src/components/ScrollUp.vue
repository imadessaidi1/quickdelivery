<template>
  <teleport to="body">
    <button v-show="showButton" class="scroll-to-top" type="button" @click="scrollToTop">
      <span class="material-symbols-outlined">arrow_upward</span>
    </button>
  </teleport>
</template>

<script>
export default {
  data() {
    return {
      showButton: false,
      scrollContainers: [],
      refreshTimer: null,
    };
  },
  mounted() {
    this.bindScrollSources();
    this.handleScroll();
  },
  beforeUnmount() {
    this.unbindScrollSources();
  },
  watch: {
    $route() {
      requestAnimationFrame(() => {
        this.bindScrollSources();
        this.handleScroll();
      });
    },
  },
  methods: {
    bindScrollSources() {
      this.unbindScrollSources();
      this.scrollContainers = this.findScrollContainers();
      window.addEventListener('scroll', this.handleScroll, { passive: true });
      document.addEventListener('scroll', this.handleScroll, true);
      this.scrollContainers.forEach((element) => {
        element.addEventListener('scroll', this.handleScroll, { passive: true });
      });
      this.refreshTimer = window.setInterval(this.handleScroll, 400);
    },
    unbindScrollSources() {
      window.removeEventListener('scroll', this.handleScroll);
      document.removeEventListener('scroll', this.handleScroll, true);
      this.scrollContainers.forEach((element) => {
        element.removeEventListener('scroll', this.handleScroll);
      });
      this.scrollContainers = [];
      if (this.refreshTimer) {
        window.clearInterval(this.refreshTimer);
        this.refreshTimer = null;
      }
    },
    findScrollContainers() {
      return Array.from(document.querySelectorAll('*')).filter((element) => {
        const style = window.getComputedStyle(element);
        const allowsScroll = ['auto', 'scroll', 'overlay'].includes(style.overflowY);
        return allowsScroll && element.scrollHeight - element.clientHeight > 40;
      });
    },
    handleScroll() {
      const containerOffsets = this.scrollContainers.map((element) => element.scrollTop || 0);
      const offset = Math.max(
        window.pageYOffset || 0,
        document.documentElement.scrollTop || 0,
        document.body.scrollTop || 0,
        ...containerOffsets
      );
      this.showButton = offset > 80;
    },
    scrollToTop() {
      window.scrollTo({
        top: 0,
        behavior: 'smooth',
      });
      document.documentElement.scrollTo?.({
        top: 0,
        behavior: 'smooth',
      });
      document.body.scrollTo?.({
        top: 0,
        behavior: 'smooth',
      });
      this.scrollContainers.forEach((element) => {
        element.scrollTo?.({
          top: 0,
          behavior: 'smooth',
        });
      });
      window.scrollTo({
        top: 0,
        behavior: 'smooth',
      });
    },
  },
};
</script>

<style scoped>
.scroll-to-top {
  position: fixed;
  cursor: pointer;
  bottom: max(24px, env(safe-area-inset-bottom, 0px) + 16px);
  right: max(18px, env(safe-area-inset-right, 0px) + 16px);
  width: 50px;
  height: 50px;
  border: none;
  border-radius: 50%;
  display: inline-flex;
  align-items: center;
  justify-content: center;
  background: linear-gradient(135deg, #ef7d32, #cf6320);
  box-shadow: rgba(17, 17, 26, 0.1) 0px 4px 16px, rgba(17, 17, 26, 0.05) 0px 8px 32px;
  transition: all 300ms;
  z-index: 2200;
}

.scroll-to-top:hover {
  filter: brightness(1.04);
  box-shadow: rgba(239, 125, 50, 0.45) 0px 4px 16px, rgba(239, 125, 50, 0.35) 0px 8px 24px, rgba(239, 125, 50, 0.28) 0px 16px 56px;
}

.scroll-to-top span {
  font-size: 24px;
  color: #fff;
}

@media screen and (max-width: 767px) {
  .scroll-to-top {
    width: 46px;
    height: 46px;
    right: max(14px, env(safe-area-inset-right, 0px) + 12px);
  }
}
</style>
