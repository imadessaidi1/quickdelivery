<template>
    <div class="user_profil_container">
        <div class="information_viewer">
          <div class="user_details_component">
              <UserDetails :user="selectedUser" :vehicle="selectedUser.vehicles[0]" :userDocuments="selectedUser.documents"/>
          </div>
          <div class="document_viewer">
              <DocumentViewer :documents = "selectedUser.document"/>
          </div>
      </div>
    </div>
</template>

<script>
import UserDetails from '../components/UserDetails.vue';
import DocumentViewer from '../components/DocumentViwer.vue';

export default {
  components: {
    UserDetails,
    DocumentViewer,
  },
  props: {
    selectedUser: null,
  },
  methods: {
    toUpdate() {
      this.$router.push('/userSignInPage?id='+this.selectedUser.emailAddress);
    },
  },

}
</script>
<style scoped>
  .user_profil_container{
    width: 1150px;
    margin: 0 auto; 
  }
  /**modal part */
  .information_viewer{
    padding: 20px 0;
    display: flex;
    justify-content: space-between;
}
.information_viewer .user_details_component{
  width: 35%;
  height: 95%;
  overflow: hidden;
}
.information_viewer .document_viewer {
  width: 65%;
  position: relative;
}
.modal {
  position: fixed;
  top: 0;
  left: 0;
  width: 100%;
  height: 100%;
  background: rgba(0, 0, 0, 0.25);
  display: flex;
  justify-content: center;
  overflow-y: scroll;
}

.modal-content {
  width: 70%;
  height: max-content;
  padding: 20px;
  margin: 20px 0;
  border-radius: 5px;
  background: rgba(255, 255, 255, 0.5);
  box-shadow: 0 4px 30px rgba(0, 0, 0, 0.1);
  backdrop-filter: blur(6.6px);
  -webkit-backdrop-filter: blur(6.6px);
}

.close-btn {
  /* Styles pour le bouton de fermeture (position absolue en haut à droite, couleur, curseur, etc.) */
  position: absolute;
  top: 10px;
  right: 10px;
  cursor: pointer;
  color: #555;
  border: none;
  background: none;
  transition: all .3s;
}
.close-btn:hover{
  color: #000000;
}
.conditionCheckbox{
  display: flex;
  align-items: center;
}
.conditionCheckbox label{
    margin: 0;
}
.conditionCheckbox span{
  font-size: 12px;
}
.decision_section{
  width: max-content;
  margin: 0 auto;
}
.decision_section button{
  display: block;
  margin: 5px auto;
}
@media only screen and (max-width: 1150px){
  .user_profil_container{
    width: 95%;
  }
  .user_profil_container .user_details_group_ {
    width: 85%;
  }
}
@media only screen and (max-width: 760px){
  .information_viewer{
    flex-direction: column;
  }
  .user_details_component{
      width: 100% !important;
  }
  .user_details_group_{
    width: 100% !important;
    padding: 0 0 15px 0;
    margin: 0 0 15px 0;
  }
  .user_profil_container .user_details_group_ .user_details{
    padding: 15px 0 !important;
  }
  .user_profil_container .information_viewer .document_viewer{
    width: 100%;
    height: 700px;
  }
}
</style>