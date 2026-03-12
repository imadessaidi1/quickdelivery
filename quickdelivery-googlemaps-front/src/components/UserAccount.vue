<template>
  <div class="user-profile-layout">
    <div class="information-viewer">
      <div class="user-details-shell">
        <div class="panel-card">
          <div class="panel-head">
            <h2>{{ $t('userInfo') || 'Informations' }}</h2>
          </div>
          <UserDetails :user="selectedUser" :vehicle="selectedUser.vehicles[0]" :userDocuments="selectedUser.documents" :show-update-button="true"/>
        </div>
      </div>
      <div class="document-viewer">
        <div class="panel-card panel-card-fill">
          <div class="panel-head">
            <h2>{{ $t('userDocuments') }}</h2>
          </div>
          <DocumentViewer :documents="selectedUser.document"/>
        </div>
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
}
</script>
<style scoped>
.user-profile-layout {
  width: 100%;
}

.information-viewer {
  display: grid;
  grid-template-columns: minmax(320px, 380px) minmax(0, 1fr);
  gap: 16px;
  align-items: stretch;
}

.user-details-shell,
.document-viewer {
  min-width: 0;
}

.panel-card {
  border: 1px solid #e5e7eb;
  border-radius: 18px;
  background: #ffffff;
  box-shadow: 0 10px 24px rgba(15, 23, 42, 0.06);
  overflow: hidden;
  width: 100%;
}

.panel-card-fill {
  min-height: 100%;
  width: 100%;
}

.panel-head {
  display: flex;
  align-items: center;
  justify-content: space-between;
  gap: 8px;
  padding: 14px 16px;
  border-bottom: 1px solid #eef2f7;
}

.panel-head h2 {
  margin: 0;
  font-size: 1rem;
  line-height: 1.1;
  color: #0f172a;
}

.document-viewer {
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
  .information-viewer {
    grid-template-columns: 1fr;
  }
}
@media only screen and (max-width: 760px){
  .information-viewer .document-viewer{
    width: 100%;
    min-height: 560px;
  }
}
</style>
