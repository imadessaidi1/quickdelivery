<template>
    <div class="user_details_group_">
        <div class="user_details">
            <h3>{{ $t('userInfo') }}</h3>
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
    padding-bottom: 4px;
}
.picture img{
    display: block;
    width: 128px;
    height: auto;
    margin: 0 auto 4px;
    border: 1px solid #dbe1ea;
    border-radius: 50%;
    background: #f8fafc;
}
.user_details_group_{
    width: 100%;
    padding: 0;
}
.user_details_group_ .user_details h3{
    margin: 0 0 10px;
    padding-left: 8px;
    border-left: solid 3px #10b3ff;
    color: #0f172a;
    font-size: 0.98rem;
    line-height: 1.1;
} 
.user_details_group_ .user_details{
    padding: 12px 16px;
}
.user_details_group_ .user_details .details div{
    padding: 4px 0;
    font-size: 13px;
    color: #334155;
    line-height: 1.35;
}
.user_details_group_ .user_details:first-child{
    border-bottom: 1px solid #eef2f7;
}
.user_details_group_ button{
    display: inline-block;
    margin: 12px 16px 16px;
}
@media screen and (max-width: 1100px){
    .user_details_group_ .user_details .details{
        width: 100%;
        display: grid;
        grid-template-columns: auto auto;
        gap: 0 12px;
    }
    .user_details_group_ .user_details .details div{
        font-size: 13px;
    }
}
@media only screen and (max-width: 760px){
  .user_details_group_ .user_details{
    padding: 12px;
  }
  .user_details_group_ button{
    margin: 10px 12px 12px;
  }
}
@media screen and (max-width: 600px){
    .user_details_group_ .user_details .details .long_text,
    .user_details_group_ .user_details .details .picture{
        grid-column-start: 1;
        grid-column-end: 3;
    }
}
</style>
