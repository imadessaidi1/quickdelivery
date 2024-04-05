<template>
    <div class="table">
        <table>
            <thead>
                <tr>
                    <th>{{$t('packageAddressFirstName')}}</th>
                    <th>{{$t('packageAddressLastName')}}</th>
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
        <div v-if="selectedUser" class="modal">
            <div class="modal-content">
                <div class="d_flex">
                    <div class="user_details_component">
                        <UserDetails :user="selectedUser" :vehicle="selectedUser.vehicles[0]" :userDocuments="selectedUser.documents"/>
                    </div>
                    <div class="document-viewer">
                        <DocumentViewer :documents = "selectedUser.document"/>
                    </div>
                </div>
                <button class="close-btn" @click="hideDetails"><span class="material-symbols-outlined size-24">cancel</span></button>
                <div class="conditionCheckbox">
                    <input type="checkbox" id="validationCheckbox" v-model="selectedUser.activeAccount">
                    <label for="validationCheckbox">{{$t('userValidationCheckboxLabel')}}</label>
                </div>
                <button class="btn primary_btn" @click="saveValidation"><span class="material-symbols-outlined size-24">{{$t('userValidationSave')}}</span></button>
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
    margin: 1em auto;
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
</style>
