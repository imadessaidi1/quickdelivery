<template>
    <div>
        <table>
            <thead>
                <tr>
                    <th>First Name</th>
                    <th>Last Name</th>
                    <th>Email Address</th>
                    <th>Phone</th>
                    <th>Vehicle Registration Number</th>
                    <th>Vehicle Brand</th>
                    <th>Vehicle Model</th>
                    <th>Vehicle Energy Type</th>
                    <th>Details</th>
                </tr>
            </thead>
            <tbody>
                <tr v-for="(user, index) in users" :key="index">
                    <td>{{ user.firstName }}</td>
                    <td>{{ user.lastName }}</td>
                    <td>{{ user.emailAddress }}</td>
                    <td>{{ user.phone }}</td>
                    <td>{{ user.vehicles[0].registrationNumber }}</td>
                    <td>{{ user.vehicles[0].brand }}</td>
                    <td>{{ user.vehicles[0].model }}</td>
                    <td>{{ user.vehicles[0].energyType }}</td>
                    <td>
                        <button class="btn primary_btn" @click="showDetails(user)">Details</button>
                </td>
                </tr>
            </tbody>
        </table>
        <!-- Popup pour afficher les détails de l'utilisateur sélectionné -->
            <div v-if="selectedUser" class="modal">
                <div class="modal-content">
                    <div class="d_flex">
                        <div class="user_details_component">
                            <UserDetails :user="selectedUser" :vehicle="selectedUser.vehicles[0]" :vehicleDocuments="selectedUser.vehicles[0].vehicleDocuments" :userDocuments="selectedUser.documents"/>
                        </div>
                        <div class="document-viewer">
                            <DocumentViewer :documents = "selectedUser.document"/>
                        </div>
                    </div>
                    <button class="close-btn" @click="hideDetails"><span class="material-symbols-outlined size-24">Close</span></button>
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
    users: {
      type: Array,
      required: true
    }
  },
  data() {
    return {
      selectedUser: null
    };
  },
  methods: {
    showDetails(user) {
      this.selectedUser = user;
    },
    hideDetails() {
      this.selectedUser = null;
    }
  }
}
</script>

<style scoped>
.d_flex{
  display: flex;
  height: 94vh !important;
}
.d_flex .user_details_component{
  width: 35%;
  height: 95%;
  overflow: scroll;
  box-shadow: rgba(0, 0, 0, 0.45) 20px 0px 30px -34px;
  background-color: #eeeeee;
  z-index: 1;
}
.d_flex .document-viewer {
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
  align-items: center;
}

.modal-content {
  width: 70%;
  padding: 0 20px 20px 20px;
  border-radius: 10px;
  position: relative;
  /* From https://css.glass */
  background: rgba(255, 255, 255, 0.5);
  border-radius: 16px;
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
@media screen and (max-width: 1100px){
  .modal {
    align-items: baseline;
    overflow: scroll;
  }
  .modal-content{
    margin: 60px 0;
  }
}
@media screen and (max-width: 600px){
  .modal-content{
    width: 85%;
  }
}
</style>
