<template>
    <div class="user_creation_main">
        <div class="user-form" v-if="usersList && usersList.length > 0">
            <UsersAccountList :users="usersList"/>
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
    };
  },
  mounted() {
    http.get(this.$i18n.t('userRootURL') + this.$i18n.t('getUsersForValidation'))
      .then(response => {
        console.log(response.data);
        this.usersList = response.data;
    }).catch(() => {
      console.log("unable to process your request this time. please try again latter.");
    });
  },
};
</script>
<style>
/*.user_creation_main{
  height: 100%;
  display: flex;
  justify-content: center;
  align-items: center;
  overflow: scroll;
  background: rgb(0,79,135);
  background: radial-gradient(circle, rgba(0,79,135,1) 0%, rgba(4,129,218,1) 84%, rgba(0,148,255,1) 100%); 
}
.user_creation_main .user-form {
  width: 75%;
  min-height: 70%;
  padding: 0 20px;
  margin: 0 auto;
  border-radius: 5px;
  background: no-repeat url('../assets/avatar.png') right -160px bottom 50%, #ffffffe1;
  box-shadow: rgba(0, 0, 0, 0.24) 0px 3px 8px;
}
.file_name{
  font-size: 13px;
  display: block;
  padding-left: 15px;
}
.file_name::before{
  width: 180px;
  height: 18px;
  background-image: url('../assets/check.png');
}
.input_only{
  margin-top: 15px;
  height: max-content;
}
.user_creation_main .user-form button{
  margin-bottom: 20px;
}
.user_creation_main .user-form form{
  width: 100%;
}
.user_creation_main .user-form form .components,
.user_creation_main .user-form form .summary_component{
  width: 75%;
}
.user_creation_main .user-form form .components h2{
  border-left: solid 5px #004f87;
  padding-left: 8px;
  margin-left: 20px;
}
.user_creation_main .user-form .package-address {
  display: flex;
  flex-wrap: wrap;
}
.small_width{
  display: none;
}
.user-form .primary_btn{
  margin-bottom: 20px;
}
@media screen and (max-width: 1500px){
  .user_creation_main .user-form form .summary_component{
    width: 100%;
  }
  .user_creation_main .user-form form .components{
    width: 100%;
  }
}
@media screen and (max-width: 1100px) {
  .user_creation_main {
    align-items: baseline;
  }
  .user_creation_main .user-form {
    margin: 50px auto;
  }
}
@media screen and (max-width: 600px) {
  
  .user_creation_main .user-form {
    width: 75%;
    padding: 30px;
  }
}*/
</style>