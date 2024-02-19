<template>
    <div v-if="showMessage" class="message-container" :class="{ 'success': requestSuccess, 'error': !requestSuccess }">
       <span v-if="requestSuccess">{{ $t('requestSuccessful') }}</span><span v-if="!requestSuccess">{{ $t('requestUnsuccessful') }}</span>
       <button @click="dismissMessage">×</button>
    </div>
</template>

<script>
export default {
  computed: {
    showMessage() {
      return this.$store.state.showMessage;
    },
    requestSuccess() {
      return this.$store.state.requestSuccess;
    },
    requestMessage() {
      return this.$store.state.requestMessage;
    },
  },
  methods: {
    dismissMessage() {
      this.$store.commit('updateShowMessage', false);
    },
  }
};
</script>

<style>
.message-container {
  position: fixed;
  bottom: 20px;
  right: 20px;
  background-color: #fff;
  border: 1px solid;
  border-radius: 5px;
  padding: 10px 20px;
  transition: all 0.5s ease-in-out;
  box-shadow: 0 4px 6px rgba(0, 0, 0, 0.1);
  z-index: 9999;
}

.message-container.success {
  background-color: #dff0d8;
  border-color: #d6e9c6;
  color: #3c763d;
}

.message-container.error {
  background-color: #f2dede;
  border-color: #ebccd1;
  color: #a94442;
}

.message-container button {
  background: none;
  border: none;
  outline: none;
  cursor: pointer;
  position: absolute;
  top: 5px;
  right: 5px;
  font-size: 16px;
  color: #888;
}

</style>
