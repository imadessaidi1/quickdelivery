<template>
    <div class="document-viewer">
        <strong><span class="document_title">{{ currentDocInfo }}</span></strong>
        <iframe v-if="currentDocData" :src="currentDocData"></iframe>
        <div v-else class="document-empty">{{ $t('packageDocumentMissing') }}</div>
        <div v-if="currentDocument" class="document_state">
            <label for="accept-option">
                <input type="radio" id="accept-option" name="accept-option" v-model="currentDocument.documentStatus" value="ACCEPTED"/>
                {{$t('userDocumentAccepted')}}
            </label>
            <label for="reject-option">
                <input type="radio" id="reject-option" name="reject-option" v-model="currentDocument.documentStatus" value="REJECTED"/>
                {{$t('userDocumentRejected')}}
            </label>
        </div>
        <div v-if="currentDocument" class="document_review_panel">
            <label class="review_label" for="review-comment">{{ $t('userDocumentReviewCommentLabel') }}</label>
            <textarea
              id="review-comment"
              v-model="currentDocument.reviewComment"
              class="review_input"
              :placeholder="$t('userDocumentReviewCommentPlaceholder')"
            />
        </div>
        <div class="navigation-info">
            <div class="navigation-buttons">
                <button class="nav_btn" @click="previousDocument" :disabled="currentDocIndex === 0"><span class="material-symbols-outlined">chevron_left</span></button>
                <button class="nav_btn" @click="nextDocument" :disabled="currentDocIndex === documentKeys.length - 1"><span class="material-symbols-outlined">chevron_right</span></button>
            </div>
        </div>
    </div>
</template>
<script>
export default {
    props: {
        documents: {
            type: Object,
            default: () => ({}),
        },
    },
    data() {
        return {
          currentDocIndex: 0,
          currentDocData: '',
          currentDocument: null,
        };
    },
    mounted() {
        this.syncCurrentDocument();
    },
    computed: {
        documentKeys() {
            return Object.keys(this.documents || {});
        },
        currentDocInfo() {
            const documentType = this.documentKeys[this.currentDocIndex];
            if (!documentType) {
                return this.$t('packageDocumentMissing');
            }
            const documentName = this.$t(documentType);
            return this.$t('documentPagerLabel', {
                current: this.currentDocIndex + 1,
                total: this.documentKeys.length,
                name: documentName,
            });
        },
    },
    watch: {
        'currentDocument.documentStatus'(status) {
            if (status === 'ACCEPTED' && this.currentDocument) {
                this.currentDocument.reviewComment = '';
            }
        },
        documents: {
            deep: true,
            handler() {
                if (this.currentDocIndex >= this.documentKeys.length) {
                    this.currentDocIndex = 0;
                }
                this.syncCurrentDocument();
            },
        },
    },
    methods: {
        nextDocument(){
             if (this.currentDocIndex < this.documentKeys.length - 1) {
                this.currentDocIndex++;
                this.syncCurrentDocument();
              }
        },
        previousDocument(){
            if (this.currentDocIndex > 0) {
                this.currentDocIndex--;
                this.syncCurrentDocument();
            }
        },
        syncCurrentDocument() {
            const documentType = this.documentKeys[this.currentDocIndex];
            this.currentDocument = documentType ? this.documents[documentType] : null;
            this.formatDocData(this.currentDocument);
        },
        formatDocData(document){
            if (!document?.docURL || !document?.data) {
                this.currentDocData = '';
                return;
            }
            const lastDotIndex = document.docURL.lastIndexOf('.');
            const documentNameEndsWith = document.docURL.substring(lastDotIndex + 1);
            if(documentNameEndsWith === 'pdf'){
                this.currentDocData = 'data:application/pdf;base64,'+document.data;
            }else{
                this.currentDocData = 'data:image/png;base64,'+document.data;
            }
        },
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
.document-empty{
  display: flex;
  align-items: center;
  justify-content: center;
  flex: 1;
  min-height: 240px;
  color: #fff;
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
.document_review_panel{
  position: absolute;
  left: 16px;
  right: 16px;
  bottom: 58px;
  padding: 12px;
  border-radius: 14px;
  background: rgba(15, 23, 42, 0.82);
  color: #fff;
}
.review_label{
  display: block;
  margin-bottom: 6px;
  font-size: 12px;
  font-weight: 700;
}
.review_input{
  width: 100%;
  min-height: 72px;
  resize: vertical;
  padding: 10px 12px;
  border: 1px solid rgba(255,255,255,0.22);
  border-radius: 12px;
  background: rgba(255,255,255,0.12);
  color: #fff;
  box-sizing: border-box;
}
.review_input::placeholder{
  color: rgba(255,255,255,0.66);
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
@media screen and (max-width: 767px) {
  .document_state{
    width: calc(100% - 24px);
    left: 12px;
    right: 12px;
    justify-content: space-between;
    padding: 0 10px;
  }
  .document_review_panel{
    left: 12px;
    right: 12px;
    bottom: 70px;
  }
}
</style>
