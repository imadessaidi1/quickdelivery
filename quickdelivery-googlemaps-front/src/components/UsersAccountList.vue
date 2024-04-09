<template>
    <div class="table">
        <table>
            <thead>
                <tr>
                    <th>{{$t('packageAddressLastName')}}</th>
                    <th>{{$t('packageAddressFirstName')}}</th>
                    <th>{{$t('packageAddressEmail')}}</th>
                    <th>{{$t('packageAddressPhone')}}</th>
                    <th>{{$t('userVehicleRegistration')}}</th>
                    <th>{{$t('userVehicleBrand')}}</th>
                    <th>{{$t('userVehicleModel')}}</th>
                    <th>{{$t('userVehicleEnergy')}}</th>
                    <th>{{$t('packagesArroundMArkerDetailActionsDetails')}}</th>
                </tr>
            </thead>
            <tbody>
                <tr v-for="(user, index) in users" :key="index">
                    <td>{{ user.lastName }}</td>
                    <td>{{ user.firstName }}</td>
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
        <div v-if="selectedUser" class="modal">
            <div class="modal-content">
                <div class="information_viewer">
                  <div class="user_details_component">
                      <UserDetails :user="selectedUser" :vehicle="selectedUser.vehicles[0]" :userDocuments="selectedUser.documents"/>
                  </div>
                  <div class="document_viewer">
                      <DocumentViewer :documents = "selectedUser.document"/>
                  </div>
                </div>
                <div class="decision_section">
                  <div class="conditionCheckbox">
                    <input type="checkbox" id="validationCheckbox" v-model="selectedUser.activeAccount">
                    <label for="validationCheckbox">{{$t('userValidationCheckboxLabel')}}</label>
                  </div>
                  <button class="btn primary_btn" @click="saveValidation">{{$t('userValidationSave')}}</button>
                </div>
                <button class="close-btn" @click="hideDetails"><span class="material-symbols-outlined size-24">cancel</span></button>
            </div>
        </div>
  </div>
</template>

<script>
import UserDetails from '../components/UserDetails.vue';
import DocumentViewer from '../components/DocumentViwer.vue';
import http from '@/config/httpInterceptor';

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
    },
    saveValidation(){
        const formData = new FormData();
        const userLanguage = navigator.languages && navigator.languages.length ? navigator.languages[0] : navigator.language || 'fr-FR';
        const url = this.$i18n.t('userRootURL') + this.$i18n.t('validateUser');
        formData.append('locale', userLanguage);
        const userCopy = JSON.parse(JSON.stringify(this.selectedUser));
        for (const docType in userCopy.document) {
            userCopy.document[docType].data = "";
        }
        formData.append('user', JSON.stringify(userCopy));
        return new Promise((resolve, reject) => {
            http.put(url, formData)
            .then(response => {
                resolve(response.data);
            })
            .catch(error => {
                console.log("Unable to process your request at this time. Please try again later.", error);
                reject(error);
            });
        }).then(() => {
            this.hideDetails();
            this.$parent.loadUsers();
        });
    }
  }
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
  margin: 50px auto 0 auto;
  border-top: solid 2px black;
}
.decision_section button{
  display: block;
  margin: 5px auto;
}
</style>
