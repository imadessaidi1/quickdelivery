<template>
    <div class="document-viewer">
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
                <button class="btn primary_btn" @click="previousDocument" :disabled="currentDocIndex === 0">{{$t('packagePreviousAction')}}</button>
                <strong><span>{{ currentDocInfo() }}</span></strong>
                <button class="btn primary_btn" @click="nextDocument" :disabled="currentDocIndex === documents.length - 1">{{$t('packageNextAction')}}</button>
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
            return `Document ${this.currentDocIndex + 1} of ${Object.keys(this.documents).length}: ${documentName}`;
        }
    },
}
</script>
<style>
.document-viewer {
  display: flex;
  flex-direction: column;
  align-items: center;
  width: 100%;
  height: 100%;
}
.document-viewer iframe {
  width: 100%;
  height: 100%; /* Ajustez la hauteur de l'iframe selon vos besoins */
  border: none;
}
.document_state {
  margin: 0 auto 10px auto;
  display: flex;
  justify-content: space-evenly;
  position: absolute;
  bottom: 10px;
  width: 230px;
  border-radius: 10px;
  background-color: #000000bb;
  color: white;
  opacity: .3;
  transition: opacity 0.3s ease;
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
.document_state:hover{
    opacity: .7;
}
.navigation-buttons {
  width: 90%;
  display: flex;
  justify-content: space-between;
  align-items: center;
  position: absolute;
  top: 50%;
  transform: translate(-50%, -50%);
}
</style>