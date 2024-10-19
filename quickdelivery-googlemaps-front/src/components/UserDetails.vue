<template>
    <div class="user_details_group_">
        <div class="user_details">
            <h3>User Info.</h3>
            <div class="details">
                <div class="picture"><img :src="getPicture()" alt="personal image"></div>
                <div><strong>{{$t('packageAddressFirstName')}}:</strong> {{ user.firstName }}</div>
                <div><strong>{{$t('packageAddressLastName')}}:</strong> {{ user.lastName }}</div>
                <div><strong>{{$t('userGender')}}:</strong> {{ user.sex }}</div>
                <div><strong>{{$t('userBirthDate')}}:</strong> {{ formatDate(user.birthDate) }}</div>
                <div class="long_text"><strong>{{$t('packageAddressEmail')}}:</strong> {{ user.emailAddress }}</div>
                <div class="long_text"><strong>{{$t('packageAddressPhone')}}:</strong> {{ user.phone }}</div>
                <div class="long_text"><strong>{{$t('packageAddressAddress')}}:</strong> {{ user.addressAuto }}</div>
            </div>
        </div>
        <div class="user_details">
            <h3>{{$t('userVehicle')}}</h3>
            <div class="details">
                <div class="long_text"><strong>{{$t('userVehicleRegistration')}}:</strong> {{ vehicle.registrationNumber }}</div>
                <div><strong>{{$t('userVehicleBrand')}}:</strong> {{ vehicle.brand }}</div>
                <div><strong>{{$t('userVehicleModel')}}:</strong> {{ vehicle.model }}</div>
                <div><strong>{{$t('userVehicleEnergy')}}:</strong> {{ vehicle.energyType }}</div>
            </div>
        </div>
        <button class="btn primary_btn" @click="toUpdate">{{$t('userAccountUpdate')}}</button>
    </div>
</template>
<script>
export default {
    props: {
        user: null,
        vehicle: null,
        userDocuments: []
      },
    methods: {
        formatDate(dateTime) {
            const date = new Date(dateTime);
            const options = {
                day: '2-digit',
                month: '2-digit',
                year: '2-digit',
            };
            const userLanguage = navigator.languages && navigator.languages.length ? navigator.languages[0] : navigator.language || 'fr-FR';
            return date.toLocaleDateString(userLanguage, options);
        },
        getPicture(){
            return 'data:image/png;base64,'+this.user.document['PICTURE'].data;
        }
    },
}
</script>
<style>
.picture{
    width: 100%;
}
.picture img{
    display: block;
    width: 150px;
    height: auto;
    margin: 0 auto;
    border: solid 2px black;
    border-radius: 10px;
}
.conditionCheckbox_{
  align-items: center;
  padding: 0 0 0 20px;
}
.conditionCheckbox span{
    font-size: 12px;
}
.user_details_group_{
    width: 100%;
    padding: 10px 15px;
}
.user_details_group_ .user_details h3{
    margin-left: 15px;
    padding-left: 6px;
    border-left: solid 3px #ff5e00;
} 
.user_details_group_ .user_details div{
    padding: 6px 0 6.5px 0;
}
.user_details_group_ .user_details .details div{
    font-size: 14px;
}
.user_profil_container .user_details_group_ .user_details:first-child{
    border-bottom: solid 1px #d5d5d5;
}
.user_profil_container .user_details_group_{
    width: 75%;
    border-radius: 15px;
    box-shadow: 0 3px 6px rgba(0,0,0,0.23);
    background-color: #f9f9f9;
}
.user_profil_container .user_details_group_ .user_details .picture img{
    border-radius: 50%;
    border: none;
    width: 200px;
    height: 200px;
}
.mini_title{
    padding-left: 10px;
    font-size: 14px;
    font-weight: 700;
}
.user_profil_container .user_details_group_ button{
    display: block;
    margin: 0 auto;
}
@media screen and (max-width: 1100px){
    .user_details_group {
        flex-direction: column;
    }
    .user_details_group .user_details{
        width: 95%;
        margin: 5px 0;
    }
    .user_details_group .user_details .details{
        width: 100%;
        display: inline-grid;
        grid-template-columns: auto auto;
    }
    .user_details_group .user_details .details div{
        font-size: 13px;
    }
}
@media only screen and (max-width: 760px){
  .user_profil_container .user_details_group_ .user_details{
    padding: 15px;
  }
}
@media screen and (max-width: 600px){
    .user_details_group .user_details .details .long_text{
        grid-column-start: 1;
        grid-column-end: 3;
    }
}
</style>