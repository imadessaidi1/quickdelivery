<template>
    <h2 v-show="package_.reference">{{$t('packageReference')}}: {{ package_.reference }}</h2>
    <div class="package_details_group">
            <div class="package_details">
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
                <div v-if="package_.deliveryPrice">
                    <strong>{{$t('packagePrice')}}:</strong> {{package_.deliveryPrice}} {{$t('currency')}}
                </div>
            </div>
            <div v-if="package_.documentS && package_.documentS['PACKAGE_INVOICE']"><a @click="openDocumentPDFModal" class="custom-link">{{ $t(package_.documentS['PACKAGE_INVOICE'].fileName) }}</a></div>
            <div v-if="package_.documentS && package_.documentS['PACKAGE_PICTURE']"><a @click="openDocumentIMGModal" class="custom-link">{{ $t(package_.documentS['PACKAGE_PICTURE'].fileName) }}</a></div>
            <!--<div v-if="documentS">
                <div>
                    {{$t('packagePicture')}}: {{documentS[0].name}}
                </div>
                <div>
                    {{$t('packageInvoice')}}: {{documentS[1].name}}
                </div>
            </div>-->
        </div>
        <div class="package_details">
            <h3>{{$t('packageAddressDepartureAddresses')}}</h3>
            <div class="details">
                <div>
                    <strong>{{$t('packageAddressFirstName')}}:</strong> {{getDepartureAddress(this.package_.addresses).firstName}}
                </div>
                <div>
                    <strong>{{$t('packageAddressLastName')}}:</strong> {{getDepartureAddress(this.package_.addresses).lastName}}
                </div>
                <div>
                    <strong>{{$t('packageAddressEmail')}}:</strong> {{getDepartureAddress(this.package_.addresses).email}}
                </div>
                <div>
                    <strong>{{$t('packageAddressPhone')}}:</strong> {{getDepartureAddress(this.package_.addresses).phone}}
                </div>
                <div class="adresse_line">
                    <strong>{{$t('packageAddressLine1')}}:</strong> {{getDepartureAddress(this.package_.addresses).line1}}
                </div>
                <div>
                    <strong>{{$t('packageAddressZip')}}:</strong> {{getDepartureAddress(this.package_.addresses).zipCode}}
                </div>
                <div>
                    <strong>{{$t('packageAddressCity')}}:</strong> {{getDepartureAddress(this.package_.addresses).town}}
                </div>
                <div>
                    <strong>{{$t('packageAddressCountry')}}:</strong> {{getDepartureAddress(this.package_.addresses).country}}
                </div>
                <div>
                    <strong>{{$t('packageAddressFloor',{ state: $t('packageAddressFloorStatePickup') })}}:</strong> {{getDepartureAddress(this.package_.addresses).floor}}
                </div>
                <div class="dateTime_line" v-show="getDepartureAddress(this.package_.addresses).dateTime">
                    <strong>{{$t('packageAddressDepartureTime',{ state: $t('packageAddressFloorStatePickup') })}}:</strong> {{ formatDate(getDepartureAddress(this.package_.addresses).dateTime) }}
                </div>
            </div>
            
        </div>
        <div class="package_details">
            <h3>{{$t('packageAddressArrivalAddresses')}}</h3>
            <div class="details">
                <div>
                    <strong>{{$t('packageAddressFirstName')}}:</strong> {{getArrivalAddress(this.package_.addresses).firstName}}
                </div>
                <div>
                    <strong>{{$t('packageAddressLastName')}}:</strong> {{getArrivalAddress(this.package_.addresses).lastName}}
                </div>
                <div>
                    <strong>{{$t('packageAddressEmail')}}:</strong> {{getArrivalAddress(this.package_.addresses).email}}
                </div>
                <div>
                    <strong>{{$t('packageAddressPhone')}}:</strong> {{getArrivalAddress(this.package_.addresses).phone}}
                </div>
                <div class="adresse_line">
                    <strong>{{$t('packageAddressLine1')}}:</strong> {{getArrivalAddress(this.package_.addresses).line1}}
                </div>
                <div>
                    <strong>{{$t('packageAddressZip')}}:</strong> {{getArrivalAddress(this.package_.addresses).zipCode}}
                </div>
                <div>
                    <strong>{{$t('packageAddressCity')}}:</strong> {{getArrivalAddress(this.package_.addresses).town}}
                </div>
                <div>
                    <strong>{{$t('packageAddressCountry')}}:</strong> {{getArrivalAddress(this.package_.addresses).country}}
                </div>
                <div>
                    <strong>{{$t('packageAddressFloor',{ state: $t('packageAddressFloorStateDelivery') })}}:</strong> {{getArrivalAddress(this.package_.addresses).floor}}
                </div>
                <!--<div class="dateTime_line">
                    <strong>{{$t('packageAddressDepartureTime',{ state: $t('packageAddressFloorStateDelivery') })}}:</strong> {{ formatDate(getArrivalAddress(this.package_.addresses).dateTime) }}
                </div>-->
            </div>
        </div>
    </div>
    <DocumentPdfModal v-if="package_.documentS && package_.documentS['PACKAGE_PICTURE']" ref="docImgModal" classe="modal" :byteArrayPDF="getData('PACKAGE_PICTURE')"/>
    <DocumentPdfModal v-if="package_.documentS && package_.documentS['PACKAGE_INVOICE']" ref="docPdfModal" classe="modal" :byteArrayPDF="getData('PACKAGE_INVOICE')"/>
</template>
<script>
import { getArrivalAddress, getDepartureAddress } from '@/config/comonFunction';
import DocumentPdfModal from '../components/DocumentPdfModal.vue';

export default {
    components: {
      DocumentPdfModal,
    },
    computed: {
        package_() {
          return this.$store.state.package_;
        },
        documentS() {
          return this.$store.state.documentS;
        },
    },
    data() {
      return {
        departureAddress: Object,
        arrivalAddress: Object,
        name: 'my-pdf-file.pdf', //change which pdf file loads
        path: 'pdfjs-2.3.200-dist/web/viewer.html'
      };
    },
    mounted() {
        this.departureAddress = getDepartureAddress(this.package_.addresses);
        this.arrivalAddress = getArrivalAddress(this.package_.addresses);
    },
    methods: {
        getDepartureAddress,
        getArrivalAddress,
        openDocumentPDFModal(){
          this.$refs.docPdfModal.openModal();
        },
        openDocumentIMGModal(){
          this.$refs.docImgModal.openModal();
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
        getData(docType){
            var data;
            if(docType === 'PACKAGE_INVOICE'){
                data = 'data:application/pdf;base64,'+this.package_.documentS[docType].data;
            }else{
                data = 'data:image/png;base64,'+this.package_.documentS[docType].data;
            }
            return data;
        },
        getIMGData(){
            return 'data:image/png;base64,'+this.package_.documentS[1].data;
        },
    },
}
</script>
<style>
.custom-link {
  color: blue; /* Couleur bleue pour le lien */
  cursor: pointer; /* Curseur de la souris en forme de main */
}

.custom-link:hover {
  text-decoration: underline; /* Souligner le lien au passage de la souris */
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
    margin-left: 15px;
    padding-left: 6px;
    border-left: solid 3px #10b3ff;
} 
.tracking_summary_component .package_details_group{
    flex-direction: column;
    box-sizing: border-box;
    padding: 10px;
}
.tracking_summary_component .package_details_group .package_details{
    box-sizing: border-box;
    width: 100%;
    padding: 10px;
    background-color: #fff;
}
.tracking_summary_component .package_details_group .package_details:first-child{
    border-bottom: 1px solid #e7e7e7;
    border-radius: 10px 10px 0 0;
}
.tracking_summary_component .package_details_group .package_details:last-child{
    border-top: 1px solid #e7e7e7;
    border-radius: 0 0 10px 10px;
}
/*package_details_group{
    
}*/
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
}
</style>