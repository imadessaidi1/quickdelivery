<template>
    <div v-if="showMessage" class="message-container" :class="{ 'success': requestSuccess, 'error': !requestSuccess }">
       <span v-if="requestMessage && requestMessage !== 'success' && requestMessage !== 'error'">{{ requestMessage }}</span>
       <span v-else-if="requestSuccess">{{ $t('requestSuccessful') }}</span>
       <span v-else>{{ $t('requestUnsuccessful') }}</span>
       <button @click="dismissMessage"><span class="material-symbols-outlined">cancel</span></button>
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
  bottom: 70px;
  right: 20px;
  margin-left: 20px;
  border: 1px solid;  
  border-radius: 5px;
  padding: 10px 0 10px 20px;
  transition: all 0.5s ease-in-out;
  box-shadow: 0 4px 6px rgba(0, 0, 0, 0.1);
  display: flex;
  justify-content: center;
  align-items: center;
}
.message-container span{
  margin-right: 10px;
}

.message-container.success {
  background-color: #dff0d8;
  border-color: #3c763d;
  color: #3c763d;
}

.message-container.error {
  background-color: #f2dede;
  border-color: #e74560;
  color: #e74560;
}

.message-container button {
  background: none;
  border: none;
  outline: none;
  padding: 0;
  cursor: pointer;
  color: rgba(0, 0, 0, 0.5);
}

</style>
