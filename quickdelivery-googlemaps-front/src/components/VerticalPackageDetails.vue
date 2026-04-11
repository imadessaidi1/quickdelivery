<template>
    <div class="package_details_group tracking_summary_component">
            <div class="package_details package_overview">
            <h3>{{$t('createNewPackage')}}</h3>
            <div class="details">
                <div>
                    <strong>{{$t('packageHeight')}}:</strong> {{package_.height}}
                </div>
                <div>
                    <strong>{{$t('packageWidth')}}:</strong> {{package_.width}}
                </div>
                <div>
                    <strong>{{$t('packageDepth')}}:</strong> {{package_.depth}}
                </div>
                <div>
                    <strong>{{$t('packageWeight')}}:</strong> {{package_.weight}}
                </div>
                <div v-if="displayedPrice !== '-'">
                    <strong>{{displayedPriceLabel}}:</strong> {{displayedPrice}}
                </div>
                <div v-if="package_.documentS && package_.documentS['PACKAGE_INVOICE']">
                    <a @click="openDocumentPDFModal" class="custom-link">{{ $t(package_.documentS['PACKAGE_INVOICE'].fileName) }}</a>
                </div>
            </div>
            <div v-if="packagePictureAvailable" class="package_photo_block">
                <button type="button" class="package_photo_preview" @click="openDocumentIMGModal">
                    <img
                        v-if="picturePreviewSrc"
                        :src="picturePreviewSrc"
                        :alt="$t('PACKAGE_PICTURE')"
                        class="package_photo_image"
                    >
                    <div v-else-if="isPictureLoading" class="package_photo_state">
                        {{ $t('stateLoading') }}
                    </div>
                    <div v-else class="package_photo_state package_photo_state_error">
                        {{ $t('stateLoadError') }}
                    </div>
                </button>
                <button type="button" class="package_photo_link" @click="openDocumentIMGModal">
                    {{ $t('PACKAGE_PICTURE') }}
                </button>
            </div>
        </div>
        <div class="package_details">
            <h3>{{$t('packageAddressDepartureAddresses')}}</h3>
            <div class="details">
                <div>
                    <strong>{{ $t('userFullName') }}:</strong> {{departureAddress.firstName}} {{departureAddress.lastName}}
                </div>
                <div>
                    <strong>{{$t('packageAddressPhone')}}:</strong> {{departureAddress.phone}}
                </div>
                <div class="adresse_line">
                    <strong>{{departureAddress.addressAuto}}</strong>
                </div>
                <div>
                    <strong>{{$t('packageAddressFloor',{ state: $t('packageAddressFloorStatePickup') })}}:</strong> {{departureAddress.floor}}
                </div>
                <div class="dateTime_line" v-show="departureAddress.dateTime">
                    <strong>{{$t('packageAddressDepartureTime',{ state: $t('packageAddressFloorStatePickup') })}}:</strong> {{ formatDate(departureAddress.dateTime) }}
                </div>
            </div>
            
        </div>
        <div class="package_details">
            <h3>{{$t('packageAddressArrivalAddresses')}}</h3>
            <div class="details">
                <div>
                    <strong>{{ $t('userFullName') }}:</strong> {{arrivalAddress.firstName}} {{arrivalAddress.lastName}}
                </div>
                <div>
                    <strong>{{$t('packageAddressPhone')}}:</strong> {{arrivalAddress.phone}}
                </div>
                <div class="adresse_line">
                    <strong>{{arrivalAddress.addressAuto}}</strong>
                </div>
                <div>
                    <strong>{{$t('packageAddressFloor',{ state: $t('packageAddressFloorStateDelivery') })}}:</strong> {{arrivalAddress.floor}}
                </div>
                <!--<div class="dateTime_line">
                    <strong>{{$t('packageAddressDepartureTime',{ state: $t('packageAddressFloorStateDelivery') })}}:</strong> {{ formatDate(getArrivalAddress(this.package_.addresses).dateTime) }}
                </div>-->
            </div>
        </div>
    </div>
</template>
<script>
import { getArrivalAddress, getDepartureAddress } from '@/config/comonFunction';
import { fetchProtectedBlob } from '@/config/binaryContent';
import { formatDisplayedPackageAmount, resolveDisplayedPackagePriceLabel } from '@/config/packagePricing';

export default {
    computed: {
        package_() {
          return this.$store.state.package_;
        },
        documentS() {
          return this.$store.state.documentS;
        },
        departureAddress() {
          return getDepartureAddress(this.package_?.addresses || []) || {};
        },
        arrivalAddress() {
          return getArrivalAddress(this.package_?.addresses || []) || {};
        },
        packagePictureDocument() {
          return this.package_?.documentS?.PACKAGE_PICTURE || null;
        },
        packagePictureAvailable() {
          return Boolean(this.packagePictureDocument?.id);
        },
        displayedPrice() {
          return formatDisplayedPackageAmount(this.$i18n, this.package_);
        },
        displayedPriceLabel() {
          return resolveDisplayedPackagePriceLabel(this.$i18n);
        },
    },
    data() {
      return {
        name: 'my-pdf-file.pdf', //change which pdf file loads
        path: 'pdfjs-2.3.200-dist/web/viewer.html',
        picturePreviewSrc: '',
        currentPictureObjectUrl: '',
        isPictureLoading: false,
      };
    },
    watch: {
        'package_.reference': {
          immediate: true,
          handler() {
            this.loadPicturePreview();
          },
        },
    },
    beforeUnmount() {
      this.revokePicturePreview();
    },
    methods: {
        getDepartureAddress,
        getArrivalAddress,
        async loadPicturePreview() {
          if (!this.packagePictureAvailable) {
            this.revokePicturePreview();
            this.isPictureLoading = false;
            return;
          }
          this.isPictureLoading = true;
          try {
            const blob = await fetchProtectedBlob(
              `${this.$i18n.t('rootURL')}${this.$i18n.t('getPackageDocumentContent')}${encodeURIComponent(this.packagePictureDocument.id)}`
            );
            this.revokePicturePreview();
            this.currentPictureObjectUrl = URL.createObjectURL(blob);
            this.picturePreviewSrc = this.currentPictureObjectUrl;
          } catch (error) {
            this.revokePicturePreview();
            console.error('Unable to load package picture preview.', error);
          } finally {
            this.isPictureLoading = false;
          }
        },
        revokePicturePreview() {
          if (this.currentPictureObjectUrl) {
            URL.revokeObjectURL(this.currentPictureObjectUrl);
            this.currentPictureObjectUrl = '';
          }
          this.picturePreviewSrc = '';
        },
        openDocumentPDFModal(){
          this.$router.push({
            path: '/document',
            query: {
              reference: this.package_.reference,
              documentType: 'PACKAGE_INVOICE',
              returnTo: this.$route.fullPath,
            },
          });
        },
        openDocumentIMGModal(){
          this.$router.push({
            path: '/document',
            query: {
              reference: this.package_.reference,
              documentType: 'PACKAGE_PICTURE',
              returnTo: this.$route.fullPath,
            },
          });
        },
        formatDate(dateTime) {
            const date = new Date(dateTime);
            const options = {
                day: '2-digit',
                month: '2-digit',
                year: '2-digit',
                hour: '2-digit',
                minute: '2-digit',
            };
            const userLanguage = navigator.languages && navigator.languages.length ? navigator.languages[0] : navigator.language || 'fr-FR';
            return date.toLocaleDateString(userLanguage, options);
        },
    },
}
</script>
<style>
.tracking_summary_component h2{
  margin-left: 20px;
}
.custom-link {
  text-decoration: none;
  color: black;
  border-bottom: solid 1px black;
  opacity: .65;
  cursor: pointer;
  transition: all .3s;
}

.custom-link:hover {
  opacity: 1;
}
.package_photo_block{
  display: flex;
  flex-direction: column;
  gap: 6px;
  padding: 0 6px 8px;
}
.package_photo_preview{
  width: 100%;
  min-height: 150px;
  border: 1px solid #dbe1ea;
  border-radius: 12px;
  padding: 0;
  overflow: hidden;
  background: linear-gradient(135deg, #f8fafc 0%, #e2e8f0 100%);
  cursor: pointer;
}
.package_photo_image{
  display: block;
  width: 100%;
  height: 100%;
  max-height: 220px;
  object-fit: cover;
}
.package_photo_state{
  display: flex;
  align-items: center;
  justify-content: center;
  min-height: 150px;
  padding: 12px;
  color: #475569;
  font-size: 0.68rem;
  font-weight: 700;
}
.package_photo_state_error{
  color: #b91c1c;
}
.package_photo_link{
  align-self: flex-start;
  padding: 0;
  border: none;
  background: transparent;
  color: #0f172a;
  border-bottom: 1px solid #0f172a;
  opacity: .75;
  cursor: pointer;
  transition: opacity .3s;
  font-size: 0.64rem;
}
.package_photo_link:hover{
  opacity: 1;
}
.conditionCheckbox{
  display: flex;
  align-items: center;
  padding: 0 0 0 20px;
}
.conditionCheckbox span{
    font-size: 12px;
}
.package_details_group{
    width: 100%;
    display: flex;
    justify-content: space-evenly;
}
.package_details_group .package_details{
    width: 30%;
    padding: 10px;
    border-radius: 5px;
    background-color: #f5f5f5ca;
    box-shadow: rgba(50, 50, 93, 0.25) 0px 2px 5px -1px, rgba(0, 0, 0, 0.3) 0px 1px 3px -1px;
}
.package_details_group .package_details h3{
    margin-left: 15px;
    padding-left: 6px;
    border-left: solid 3px #42ba96;
} 
.package_details_group .package_details .details{
    padding: 6px 0 6.5px 0;
}
.package_details_group .package_details .details div{
    font-size: 14px;
    padding: 3px 10px 3px 20px;
}
.package_details_group .package_details{
    width: 100%;
    padding: 10px;
    border-radius: 0;
    background-color: #f5f5f5a8;
}
.tracking_summary_component .package_details_group .package_details .details div{
    font-size: 15px;
}
.tracking_summary_component .package_details_group .package_details h3{
    margin: 10px 0 10px 15px;
    padding-left: 6px;
    border-left: solid 3px #10b3ff;
} 
.tracking_summary_component.package_details_group{
    flex-direction: column;
    box-sizing: border-box;
    padding: 4px;
}
.tracking_summary_component.package_details_group .package_details{
    box-sizing: border-box;
    width: 100%;
    padding: 4px 6px;
    background-color: #fff;
    text-align: left;
}
.tracking_summary_component.package_details_group .package_details:first-child{
    border-bottom: 1px solid #e7e7e7;
    border-radius: 10px 10px 0 0;
}
.tracking_summary_component.package_details_group .package_details:last-child{
    border-top: 1px solid #e7e7e7;
    border-radius: 0 0 10px 10px;
}
.tracking_summary_component.package_details_group .package_details h3{
    margin: 4px 0 4px 6px;
    padding-left: 5px;
    font-size: 0.78rem;
    font-weight: 700;
    line-height: 1.05;
}
.tracking_summary_component.package_details_group .package_details .details{
    padding: 1px 0;
}
.tracking_summary_component.package_details_group .package_details .details div{
    padding: 1px 6px;
    font-size: 0.64rem;
    line-height: 1.05;
    text-align: left;
}
.tracking_summary_component.package_details_group .package_details .details div strong{
    font-size: 0.68rem;
    font-weight: 700;
}
.tracking_summary_component.package_details_group .custom-link{
    font-size: 0.64rem;
}
.tracking_summary_component.package_details_group .package_details:first-child .details{
    display: grid;
    grid-template-columns: repeat(2, minmax(0, 1fr));
    gap: 2px 6px;
    align-items: start;
}
.tracking_summary_component.package_details_group .package_details:first-child .details div{
    padding: 1px 4px;
}
@media screen and (max-width: 1100px){
    .package_details_group {
        flex-direction: column;
    }
    .package_details_group .package_details{
        width: 95%;
        margin: 5px 0;
    }
    .package_details_group .package_details .details{
        width: 100%;
        display: inline-grid;
        grid-template-columns: auto auto;
    }
    .package_details_group .package_details .details .adresse_line,
    .dateTime_line{
        grid-column-start: 1;
        grid-column-end: 3;
    }
    .package_details_group .package_details .details div{
        font-size: 13px;
    }
    .tracking_summary_component.package_details_group .package_details{
        width: 100%;
    }
}
@media screen and (max-width: 500px){
    .modal-content h2{
        font-size: 1em;
    }
    .tracking_summary_component.package_details_group .package_details:first-child .details{
        grid-template-columns: 1fr;
    }
    .package_photo_preview{
        min-height: 130px;
    }
    .package_photo_image{
        max-height: 180px;
        object-fit: contain;
        background: #f8fafc;
    }
}
</style>
