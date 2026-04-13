<template>
    <div class="user_details_group_">
        <div class="user_details">
            <h3>{{ $t('userInfo') }}</h3>
            <div class="details">
                <div class="picture">
                    <img v-if="pictureSrc" :src="pictureSrc" alt="personal image">
                    <div v-else class="picture-fallback">{{ initials }}</div>
                </div>
                <div><strong>{{$t('packageAddressFirstName')}}:</strong> {{ user.firstName }}</div>
                <div><strong>{{$t('packageAddressLastName')}}:</strong> {{ user.lastName }}</div>
                <div><strong>{{$t('userGender')}}:</strong> {{ user.sex }}</div>
                <div><strong>{{$t('userBirthDate')}}:</strong> {{ formatDate(user.birthDate) }}</div>
                <div class="long_text"><strong>{{$t('packageAddressEmail')}}:</strong> {{ user.emailAddress || user.email }}</div>
                <div class="long_text"><strong>{{$t('packageAddressPhone')}}:</strong> {{ user.phone }}</div>
                <div class="long_text" v-if="displayAddress"><strong>{{$t('packageAddressAddress')}}:</strong> {{ displayAddress }}</div>
            </div>
        </div>
        <div v-if="hasVehicleInfo" class="user_details">
            <h3>{{$t('userVehicle')}}</h3>
            <div class="details">
                <div class="long_text"><strong>{{$t('userVehicleRegistration')}}:</strong> {{ vehicle.registrationNumber }}</div>
                <div><strong>{{$t('userVehicleBrand')}}:</strong> {{ vehicle.brand }}</div>
                <div><strong>{{$t('userVehicleModel')}}:</strong> {{ vehicle.model }}</div>
                <div><strong>{{$t('userVehicleEnergy')}}:</strong> {{ vehicle.energyType }}</div>
            </div>
        </div>
        <button v-if="showUpdateButton" class="qd-btn-primary" @click="toUpdate">{{$t('userAccountUpdate')}}</button>
    </div>
</template>
<script>
import { blobToDataUrl, fetchProtectedBlob } from '@/config/binaryContent';
import { normalizeDocumentCollection } from '@/config/documents';

export default {
    props: {
        user: null,
        vehicle: null,
        userDocuments: [],
        showUpdateButton: {
            type: Boolean,
            default: true,
        },
      },
    data() {
        return {
            pictureSrc: '',
            pictureUrl: '',
            pictureRequestToken: 0,
        };
    },
    computed: {
        normalizedUserDocuments() {
            return normalizeDocumentCollection(this.user?.document || this.userDocuments || this.user?.documents || {});
        },
        initials() {
            const firstName = (this.user?.firstName || '').trim();
            const lastName = (this.user?.lastName || '').trim();
            return `${firstName.charAt(0)}${lastName.charAt(0)}`.trim().toUpperCase() || '?';
        },
        hasVehicleInfo() {
            return !!(
                this.vehicle?.registrationNumber
                || this.vehicle?.brand
                || this.vehicle?.model
                || this.vehicle?.energyType
            );
        },
        displayAddress() {
            if (this.user?.addressAuto) {
                return this.user.addressAuto;
            }
            const residence = Array.isArray(this.user?.personalAddress) ? this.user.personalAddress[0] : null;
            if (!residence) {
                return '';
            }
            return [residence.line1, residence.zipCode ? `${residence.zipCode} ${residence.town || ''}`.trim() : residence.town, residence.country]
                .filter((value) => !!value)
                .join(', ');
        },
    },
    watch: {
        normalizedUserDocuments: {
            immediate: true,
            handler() {
                this.loadPicture();
            },
        },
    },
    beforeUnmount() {
        this.revokePictureUrl();
    },
    methods: {
        async loadPicture() {
            this.pictureRequestToken += 1;
            const requestToken = this.pictureRequestToken;
            this.revokePictureUrl();
            const pictureId = this.normalizedUserDocuments?.PICTURE?.id;
            if (!pictureId) {
                this.pictureSrc = '';
                return;
            }
            try {
                const blob = await fetchProtectedBlob(`${this.$i18n.t('userRootURL')}${this.$i18n.t('getUserDocumentContent')}${encodeURIComponent(pictureId)}`);
                if (requestToken !== this.pictureRequestToken) {
                    return;
                }
                this.pictureSrc = await blobToDataUrl(blob);
            } catch (_error) {
                if (requestToken === this.pictureRequestToken) {
                    this.pictureSrc = '';
                }
            }
        },
        revokePictureUrl() {
            if (this.pictureUrl) {
                URL.revokeObjectURL(this.pictureUrl);
                this.pictureUrl = '';
            }
        },
        formatDate(dateTime) {
            if (!dateTime) {
                return '';
            }
            const date = new Date(dateTime);
            const options = {
                day: '2-digit',
                month: '2-digit',
                year: '2-digit',
            };
            const userLanguage = navigator.languages && navigator.languages.length ? navigator.languages[0] : navigator.language || 'fr-FR';
            return date.toLocaleDateString(userLanguage, options);
        },
        toUpdate() {
            const userEmail = this.user?.emailAddress || this.user?.email;
            if (!userEmail) {
                return;
            }
            this.$router.push('/userSignInPage?id=' + encodeURIComponent(userEmail));
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
.picture-fallback{
    display: flex;
    align-items: center;
    justify-content: center;
    width: 128px;
    height: 128px;
    margin: 0 auto 4px;
    border: 1px solid #dbe1ea;
    border-radius: 50%;
    background: #e2e8f0;
    color: #0f172a;
    font-size: 2rem;
    font-weight: 700;
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
.user_details_group_ .user_details .details{
    width: 100%;
    display: grid;
    grid-template-columns: 1fr;
    gap: 0;
}
.user_details_group_ .user_details:first-child{
    border-bottom: 1px solid #eef2f7;
}
.user_details_group_ button{
    display: inline-block;
    margin: 12px 16px 16px;
    width: calc(100% - 32px);
    border-radius: 999px !important;
}
@media screen and (max-width: 1100px){
    .user_details_group_ .user_details .details{
        width: 100%;
        display: grid;
        grid-template-columns: 1fr;
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
        grid-column-end: 2;
    }
}
</style>
