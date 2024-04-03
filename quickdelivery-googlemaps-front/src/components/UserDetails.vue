<template>
    <div class="user_details_group_">
        <div class="user_details">
            <h3>User Info.</h3>
            <div class="details">
                <div class="picture"><iframe ref="documentFrame" :src="getPicture()"></iframe></div>
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
.picture iframe{
  width: 100%;
  height: 25%;
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
    justify-content: space-evenly;
}
.user_details_group_ .user_details h3{
    margin-left: 15px;
    padding-left: 6px;
    border-left: solid 3px #42ba96;
} 
.user_details_group_ .user_details div{
    padding: 6px 0 6.5px 0;
}
.user_details_group_ .user_details .details div{
    font-size: 14px;
}
.mini_title{
    padding-left: 10px;
    font-size: 14px;
    font-weight: 700;
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
@media screen and (max-width: 600px){
    .user_details_group .user_details .details .long_text{
        grid-column-start: 1;
        grid-column-end: 3;
    }
}
</style>