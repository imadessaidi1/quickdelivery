<template>
    <div class="user_verification_main">
      <div class="container">
        <h2>Liste des utilisateur a verifier</h2>
        <div v-if="isLoadingPage" class="page-state">{{ $t('stateLoading') }}</div>
        <div v-else-if="loadError" class="page-state error">{{ $t('stateLoadError') }}</div>
        <div v-else-if="usersList.length === 0" class="page-state">{{ $t('stateEmptyUsersValidation') }}</div>
        <UsersAccountList v-else :users="usersList"/>
      </div>
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
      isLoadingPage: false,
      loadError: false,
    };
  },
  mounted() {
    this.loadUsers();
  },
  methods: {
    loadUsers() {
      this.isLoadingPage = true;
      this.loadError = false;
      http.get(this.$i18n.t('userRootURL') + this.$i18n.t('getUsersForValidation'))
      .then(response => {
        this.usersList = response.data;
      }).catch((error) => {
        this.loadError = true;
        console.error("Unable to process your request this time. Please try again later.", error);
      }).finally(() => {
        this.isLoadingPage = false;
      });
    },
  },
};
</script>
<style>
  .user_verification_main{
    width: 100%;
    height: 100%;
    background: #F9F7F7;
  }
  .user_verification_main .container{
    width: 85%;
    height: max-content;
    margin: 0 auto;
    overflow-x: scroll;
  }
  .user_verification_main .container h2{
    border-left: solid 5px #ff5e00;
    padding-left: 15px;
  }
  .page-state {
    margin: 12px 0;
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
  @media only screen and (max-width: 500px){
    .user_verification_main .container{
      width: 100%;
      padding: 0;
    }
    .user_verification_main .container h2{
      font-size: 1.2em !important;
      margin-left: 15px;
    }
}
</style>
