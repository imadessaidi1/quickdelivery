<template>
    <button class="toTop" @click="scrollToTop"><span class="material-symbols-outlined">arrow_upward</span></button>
</template>

<script>
    export default {
        methods: {
          scrollToTop() {
            this.$emit('scroll-to-top');
          }
        }
    }
</script>

<style>
  .toTop{
    position: fixed;
    cursor: pointer;
    bottom: 75px;
    right: 50px;
    width: 50px;
    height: 50px;
    border: none;
    border-radius: 50%;
    background-color: #ffc457;
    box-shadow: rgba(17, 17, 26, 0.1) 0px 4px 16px, rgba(17, 17, 26, 0.05) 0px 8px 32px;
    transition: all 300ms;
    z-index: 9999;
    /*box-shadow: rgba(0, 0, 0, 0.25) 0px 54px 55px, rgba(0, 0, 0, 0.12) 0px -12px 30px, rgba(0, 0, 0, 0.12) 0px 4px 6px, rgba(0, 0, 0, 0.17) 0px 12px 13px, rgba(0, 0, 0, 0.09) 0px -3px 5px;*/
  }
  .toTop:hover{
    background-color: #fdb632;
    box-shadow: #ffc45799 0px 4px 16px, #ffc45799 0px 8px 24px, #ffc45799 0px 16px 56px;
  }
</style>