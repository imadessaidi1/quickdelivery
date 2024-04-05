<template>
    <div class="user_verification_main" v-if="usersList && usersList.length > 0">
      <h1>Liste des utilisateur a verifier</h1>
      <UsersAccountList :users="usersList"/>
    </div>
</template>

<script>
import UsersAccountList from '../components/UsersAccountList.vue';
import http from '@/config/httpInterceptor';

export default {
  components: {
    UsersAccountList,
  },
  data() {
    return {
      usersList: [],
    };
  },
  mounted() {
    this.loadUsers();
  },
  methods: {
    loadUsers() {
      http.get(this.$i18n.t('userRootURL') + this.$i18n.t('getUsersForValidation'))
      .then(response => {
        this.usersList = response.data;
        }).catch(() => {
          console.log("unable to process your request this time. please try again latter.");
        });
    },
  },
};
</script>
<style>
  .user_verification_main{
    width: 78%;
    height: max-content;
    padding: 20px;
    margin: 0 auto;
  }
  .user_verification_main h1{
    border-left: solid 5px #ff5e00;
    padding-left: 15px;
  }
</style>