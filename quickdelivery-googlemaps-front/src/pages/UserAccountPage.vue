<template>
      <div class="user_account_container">
        <UserAccount v-if="selectedUser" :selectedUser="selectedUser"/>
      </div>
</template>

<script>
import UserAccount from '../components/UserAccount.vue';
import http from '@/config/httpInterceptor';

export default {
  components: {
    UserAccount,
  },
  data() {
    return {
      selectedUser: null,
    };
  },
  async mounted() {
    try {
      this.selectedUser = await this.loadUser();
    } catch (error) {
      console.error("Error loading user:", error);
    }
  },
  methods: {
    async loadUser() {
      try {
        const response = await http.get(this.$i18n.t('userRootURL') + this.$i18n.t('getUserByEmail') + this.$store.state.connectedUser.email);
        console.log(response.data);
        return response.data;
      } catch (error) {
        console.error("Unable to process your request at this time. Please try again later.", error);
        throw error;
      }
    },
  },
};
</script>
<style>
  .user_account_container{
    background: #F9F7F7
  }
</style>
