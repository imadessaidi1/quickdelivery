<template>
    <div class="document-viewer">
        <div class="document_topbar">
            <strong><span class="document_title">{{ currentDocInfo }}</span></strong>
        </div>
        <div v-if="currentDocument" class="document_match_panel">
            <div class="match_head">
                <strong>{{ $t('documentMatchTitle') }}</strong>
                <span class="match_status">{{ matchingStatusLabel }}</span>
            </div>
            <div v-if="autoReviewSuggestion" class="match_recommendation">
                <strong>{{ autoReviewSuggestion.title }}</strong>
                <span>{{ autoReviewSuggestion.message }}</span>
            </div>
            <div v-if="currentDocument.matchScore != null" class="match_score">
                {{ $t('documentMatchScore') }}: {{ Math.round(currentDocument.matchScore * 100) }}%
            </div>
            <div v-if="matchIssues.length" class="match_issues">
                <div v-for="issue in matchIssues" :key="issue.field" class="match_issue">
                    <strong>{{ issue.field }}</strong>: {{ issue.actual || '-' }}
                </div>
            </div>
        </div>
        <div class="document_preview">
            <iframe v-if="currentDocData" :src="currentDocData"></iframe>
            <div v-else class="document-empty">{{ $t('packageDocumentMissing') }}</div>
        </div>
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
        <div v-if="currentDocument && currentDocument.documentStatus === 'REJECTED'" class="document_review_panel">
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
import http from '@/config/httpInterceptor';

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
          currentDocumentUrl: '',
          isDocumentLoading: false,
          documentRequestToken: 0,
        };
    },
    mounted() {
        this.syncCurrentDocument();
    },
    beforeUnmount() {
        this.revokeCurrentDocumentUrl();
    },
    computed: {
        documentKeys() {
            return Object.keys(this.documents || {}).filter(key => key !== 'PICTURE');
        },
        parsedMatchDetails() {
            if (!this.currentDocument?.matchDetails) {
                return null;
            }
            try {
                return JSON.parse(this.currentDocument.matchDetails);
            } catch (_error) {
                return null;
            }
        },
        matchIssues() {
            const checks = this.parsedMatchDetails?.checks;
            if (!Array.isArray(checks)) {
                return [];
            }
            return checks.filter(check => check.status === 'mismatch');
        },
        matchingStatusLabel() {
            const status = this.currentDocument?.matchStatus;
            if (!status) {
                return this.$t('documentMatchStatusUnknown');
            }
            return this.$t(`documentMatchStatus${status}`);
        },
        autoReviewSuggestion() {
            const document = this.currentDocument;
            if (!document) {
                return null;
            }
            if (document.ocrErrorCode) {
                return {
                    title: this.$t('documentRejectSuggestionReject'),
                    message: `${this.$t('documentRejectSuggestionOcrError')} ${document.ocrErrorCode}`,
                };
            }
            if (document.matchStatus === 'MISMATCH') {
                return {
                    title: this.$t('documentRejectSuggestionReject'),
                    message: this.$t('documentRejectSuggestionMismatch'),
                };
            }
            if (document.ocrConfidenceScore != null && document.ocrConfidenceScore < 1) {
                return {
                    title: this.$t('documentRejectSuggestionReview'),
                    message: this.$t('documentRejectSuggestionConfidence', {
                        score: Math.round(document.ocrConfidenceScore * 100),
                    }),
                };
            }
            if (document.matchStatus === 'REVIEW_REQUIRED' || document.matchStatus === 'UNAVAILABLE') {
                return {
                    title: this.$t('documentRejectSuggestionReview'),
                    message: this.$t('documentRejectSuggestionManual'),
                };
            }
            return null;
        },
        suggestedReviewComment() {
            const document = this.currentDocument;
            if (!document) {
                return '';
            }
            const reasons = [];
            if (document.ocrErrorCode) {
                reasons.push(`${this.$t('documentRejectSuggestionOcrError')} ${document.ocrErrorCode}`);
            }
            if (document.matchStatus === 'MISMATCH') {
                reasons.push(this.$t('documentRejectSuggestionMismatch'));
            } else if (document.matchStatus === 'REVIEW_REQUIRED' || document.matchStatus === 'UNAVAILABLE') {
                reasons.push(this.$t('documentRejectSuggestionManual'));
            }
            if (document.ocrConfidenceScore != null && document.ocrConfidenceScore < 1) {
                reasons.push(this.$t('documentRejectSuggestionConfidence', {
                    score: Math.round(document.ocrConfidenceScore * 100),
                }));
            }
            return reasons.join(' ');
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
                return;
            }
            if (status === 'REJECTED' && this.currentDocument && !this.currentDocument.reviewComment?.trim() && this.suggestedReviewComment) {
                this.currentDocument.reviewComment = this.suggestedReviewComment;
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
            this.loadCurrentDocumentContent();
        },
        async loadCurrentDocumentContent() {
            this.documentRequestToken += 1;
            const requestToken = this.documentRequestToken;
            this.revokeCurrentDocumentUrl();
            const document = this.currentDocument;
            if (!document?.id || !document?.docURL) {
                this.currentDocData = '';
                return;
            }
            this.isDocumentLoading = true;
            try {
                const response = await http.get(`${this.$i18n.t('userRootURL')}${this.$i18n.t('getUserDocumentContent')}${encodeURIComponent(document.id)}`, {
                    responseType: 'blob',
                });
                if (requestToken !== this.documentRequestToken) {
                    return;
                }
                this.currentDocumentUrl = URL.createObjectURL(response.data);
                this.currentDocData = this.currentDocumentUrl;
            } catch (_error) {
                if (requestToken === this.documentRequestToken) {
                    this.currentDocData = '';
                }
            } finally {
                if (requestToken === this.documentRequestToken) {
                    this.isDocumentLoading = false;
                }
            }
        },
        revokeCurrentDocumentUrl() {
            if (this.currentDocumentUrl) {
                URL.revokeObjectURL(this.currentDocumentUrl);
                this.currentDocumentUrl = '';
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
.document_topbar{
  flex: 0 0 auto;
  padding: 8px 16px 2px;
}
.document_preview{
  flex: 1 1 auto;
  min-height: 0;
  position: relative;
}
.document-viewer iframe {
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
    margin: 0;
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
  bottom: 74px;
  padding: 12px;
  border-radius: 14px;
  background: rgba(15, 23, 42, 0.82);
  color: #fff;
}
.document_match_panel{
  flex: 0 0 auto;
  margin: 0 16px 8px;
  padding: 10px 12px;
  border-radius: 14px;
  background: rgba(2, 6, 23, 0.86);
  color: #fff;
}
.match_head{
  display: flex;
  justify-content: space-between;
  gap: 12px;
  margin-bottom: 4px;
  font-size: 12px;
}
.match_status{
  color: #cbd5e1;
  font-weight: 700;
}
.match_score,
.match_issue{
  font-size: 12px;
  line-height: 1.35;
}
.match_recommendation{
  display: flex;
  flex-direction: column;
  gap: 4px;
  margin-bottom: 8px;
  padding: 8px 10px;
  border-radius: 10px;
  background: rgba(249, 115, 22, 0.14);
  color: #fed7aa;
  font-size: 12px;
  line-height: 1.4;
}
.match_issues{
  display: flex;
  flex-direction: column;
  gap: 2px;
  margin-top: 6px;
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
  top: calc(50% + 18px);
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
    bottom: 88px;
  }
  .document_match_panel{
    margin: 0 12px 8px;
  }
}
</style>
