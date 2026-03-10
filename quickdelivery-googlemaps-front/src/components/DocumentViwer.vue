<template>
    <div class="document-viewer">
        <strong><span class="document_title">{{ currentDocInfo() }}</span></strong>
        <iframe :src="currentDocData"></iframe>
        <div class="document_state">
            <label for="accept-option">
                <input type="radio" id="accept-option" name="accept-option" v-model="currentDocument.documentStatus" value="ACCEPTED"/>
                {{$t('userDocumentAccepted')}}
            </label>
            <label for="reject-option">
                <input type="radio" id="reject-option" name="reject-option" v-model="currentDocument.documentStatus" value="REJECTED"/>
                {{$t('userDocumentRejected')}}
            </label>
        </div>
        <div class="navigation-info">
            <div class="navigation-buttons">
                <button class="nav_btn" @click="previousDocument" :disabled="currentDocIndex === 0"><span class="material-symbols-outlined">chevron_left</span></button>
                <button class="nav_btn" @click="nextDocument" :disabled="currentDocIndex === documents.length - 1"><span class="material-symbols-outlined">chevron_right</span></button>
            </div>
        </div>
    </div>
</template>
<script>
import { ref } from 'vue';
export default {
    props: {
        documents: ref([]),
    },
    data() {
        return {
          currentDocIndex: ref(0),
          currentDocName: ref(''),
          currentDocData: ref(0),
          documentsCount: ref(0),
          currentDocument: Object,
        };
    },
    mounted() {
        const documentType = Object.keys(this.documents)[this.currentDocIndex];
        this.currentDocument = this.documents[documentType];
        this.formatDocData(this.currentDocument);
        this.documentsCount = Object.keys(this.documents).length;
    },
    methods: {
        nextDocument(){
             if (this.currentDocIndex < Object.keys(this.documents).length - 1) {
                this.currentDocIndex++;
                const documentType = Object.keys(this.documents)[this.currentDocIndex];
                this.currentDocument = this.documents[documentType];
                this.formatDocData(this.currentDocument);
              }
        },
        previousDocument(){
            if (this.currentDocIndex > 0) {
                this.currentDocIndex--;
                const documentType = Object.keys(this.documents)[this.currentDocIndex];
                this.currentDocument = this.documents[documentType];
                this.formatDocData(this.currentDocument);
            }
        },
        formatDocData(document){
            const lastDotIndex = document.docURL.lastIndexOf('.');
            const documentNameEndsWith = document.docURL.substring(lastDotIndex + 1);
            if(documentNameEndsWith === 'pdf'){
                this.currentDocData = 'data:application/pdf;base64,'+document.data;
            }else{
                this.currentDocData = 'data:image/png;base64,'+document.data;
            }
        },
        currentDocInfo(){
            const documentType = Object.keys(this.documents)[this.currentDocIndex];
            const documentName = this.$t(documentType);
            return this.$t('documentPagerLabel', {
                current: this.currentDocIndex + 1,
                total: Object.keys(this.documents).length,
                name: documentName,
            });
        }
    },
}
</script>
<style>
.document-viewer {
  display: flex;
  flex-direction: column;
  align-items: stretch;
  width: 100%;
  height: 100%;
  min-height: 0;
  background-color: #323639;
  border-radius: 15px;
  position: relative;
  overflow: hidden;
}
.document-viewer iframe {
  flex: 1;
  width: 100%;
  height: 100%;
  min-height: 0;
  border: none;
}
.document_title{
    margin: 8px 0;
    color: #fff;
    display: block;
    width: 100%;
    text-align: center;
}
.document_state {
  margin: 0 auto 10px auto;
  display: flex;
  justify-content: space-evenly;
  position: absolute;
  bottom: 10px;
  width: 230px;
  border-radius: 10px;
  background-color: #000;
  color: white;
  opacity: .3;
  transition: opacity 0.3s ease-in-out;
}
.user_profil_container .document_state{
    display: none;
}
.document_state:hover{
    opacity: .8;
}
.document_state label{
  font-size: 12px;
  display: flex;
  align-items: center;
}
.document_state input{
  margin-right: 5px;
  display: block;
}
.navigation-buttons {
  width: 90%;
  display: flex;
  justify-content: space-between;
  align-items: center;
  position: absolute;
  left: 50%;
  top: 50%;
  transform: translate(-50%, -50%);
}
.nav_btn{
    width: 35px;
    height: 35px;
    background-color: #000;
    border: none;
    border-radius: 50%;
    opacity: .3;
    transition: opacity .3s ease-in-out;
    display: flex;
    justify-content: center;
    align-items: center;
}
.nav_btn:hover{
    opacity: .8;
}
.nav_btn span{
    font-size: 26px;
    color: #fff;
}
</style>
