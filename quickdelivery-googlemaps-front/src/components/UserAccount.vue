<template>
    <div v-if="selectedUser">
            <div class="modal-content">
                <div class="information_viewer">
                  <div class="user_details_component">
                      <UserDetails :user="selectedUser" :vehicle="selectedUser.vehicles[0]" :userDocuments="selectedUser.documents"/>
                  </div>
                  <div class="document_viewer">
                      <DocumentViewer :documents = "selectedUser.document"/>
                  </div>
                </div>
                <button class="btn primary_btn" @click="toUpdate">{{$t('userAccountUpdate')}}</button>
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
  table {
    background: #ffffffc5;
    border-collapse: collapse;
    margin: 5em auto;
    font-size: 0.85em;
  }
  thead{
    border-bottom: 1px solid #364043;
  }
  th {
    color: #252525;
    background-color:  #ffc350;
    font-weight: 600;
    padding: 0.75em 1em;
    text-align: left;
  }
  td {
    color: #1d1d1d;
    font-weight: 400;
    padding: 0.85em 1em;
    border-bottom: 1px solid #36404348;
  }
  tbody tr {
    transition: background 0.25s ease;
  }
  tbody tr:hover {
    background: #ff5e002d;
  }

  /**modal part */
  .information_viewer{
  display: flex;
  justify-content: space-between;
}
.information_viewer .user_details_component{
  width: 35%;
  height: 95%;
  overflow: hidden;
  background-color: #eeeeee;
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
@media only screen and (max-width: 700px){
  table, tbody, tr, th, td{
    display: block;
  }
  table{
    width: 90%;
  }
  td{
    padding-left: 200px;
    position: relative
  }
  td::before{
    position: absolute;
    top: 0;
    left: 0;
    bottom: 0;
    content: attr(data-label);
    width: 160px;
    color: #252525;
    background-color:  #ffc350;
    font-weight: 600;
    padding: 0.75em 1em;
    display: flex;
    align-items: center;
  }
  thead{
    display: none;
  }
  .table-responsive tr {
    margin-bottom: 1rem;
  }

  .table-responsive th + td {
    padding-left: 10px;
  }
}
@media only screen and (max-width: 500px){
  table{
    margin: 2em auto;
  }
  td{
    padding-left: 150px;
  }
  td::before{
    width: 120px;
  }
}
</style>