<template>
  <div class="document-page">
    <div class="page-shell">
      <template v-if="isLoading">
        <div class="page-head">
          <button class="btn primary_btn back-btn" type="button" @click="goBack">
            {{ $t('actionBack') }}
          </button>
          <div>
            <h1>{{ documentTitle }}</h1>
            <p>{{ packageReference }}</p>
          </div>
        </div>
        <div class="page-state">{{ $t('stateLoading') }}</div>
      </template>
      <template v-else-if="loadError">
        <div class="page-head">
          <button class="btn primary_btn back-btn" type="button" @click="goBack">
            {{ $t('actionBack') }}
          </button>
          <div>
            <h1>{{ documentTitle }}</h1>
            <p>{{ packageReference }}</p>
          </div>
        </div>
        <div class="page-state error">{{ $t('stateLoadError') }}</div>
      </template>
      <template v-else>
        <div class="page-head">
          <button class="btn primary_btn back-btn" type="button" @click="goBack">
            {{ $t('actionBack') }}
          </button>
          <div>
            <h1>{{ documentTitle }}</h1>
            <p>{{ packageReference }}</p>
          </div>
        </div>

        <div v-if="!documentSrc" class="page-state">{{ $t('packageDocumentMissing') }}</div>
        <div v-else class="document-frame-shell">
          <iframe :src="documentSrc" class="document-frame"></iframe>
        </div>
      </template>
    </div>
  </div>
</template>

<script>
import http from '@/config/httpInterceptor';

export default {
  props: {
    reference: {
      type: String,
      default: '',
    },
    documentType: {
      type: String,
      default: '',
    },
    returnTo: {
      type: String,
      default: '',
    },
  },
  data() {
    return {
      isLoading: false,
      loadError: false,
      documentSrc: '',
      currentDocumentUrl: '',
      packageReference: this.reference || '',
    };
  },
  computed: {
    documentTitle() {
      return this.documentType ? this.$t(this.documentType) : this.$t('packagesArroundMArkerDetailActionsDetails');
    },
  },
  mounted() {
    this.loadDocument();
  },
  beforeUnmount() {
    this.revokeDocumentUrl();
  },
  methods: {
    async loadDocument() {
      if (!this.reference || !this.documentType) {
        this.loadError = true;
        return;
      }
      this.isLoading = true;
      this.loadError = false;
      try {
        const response = await http.get(this.$i18n.t('rootURL') + this.$i18n.t('getPackage') + this.reference);
        const docs = response.data?.documentS || {};
        const currentDocument = docs[this.documentType];
        if (!currentDocument?.id) {
          this.documentSrc = '';
          return;
        }
        this.packageReference = response.data?.reference || this.reference;
        const contentResponse = await http.get(`${this.$i18n.t('rootURL')}${this.$i18n.t('getPackageDocumentContent')}${encodeURIComponent(currentDocument.id)}`, {
          responseType: 'blob',
        });
        this.revokeDocumentUrl();
        this.currentDocumentUrl = URL.createObjectURL(contentResponse.data);
        this.documentSrc = this.currentDocumentUrl;
      } catch (error) {
        this.loadError = true;
        console.error('Unable to load document.', error);
      } finally {
        this.isLoading = false;
      }
    },
    revokeDocumentUrl() {
      if (this.currentDocumentUrl) {
        URL.revokeObjectURL(this.currentDocumentUrl);
        this.currentDocumentUrl = '';
      }
    },
    goBack() {
      if (this.returnTo) {
        this.$router.push(this.returnTo);
        return;
      }
      if (window.history.length > 1) {
        this.$router.back();
        return;
      }
      this.$router.push('/');
    },
  },
};
</script>

<style scoped>
.document-page {
  min-height: 100%;
  padding: 24px;
  background: #f6f7f9;
  box-sizing: border-box;
}

.page-shell {
  display: flex;
  flex-direction: column;
  gap: 18px;
  min-height: calc(100vh - 130px);
}

.page-head {
  display: flex;
  align-items: flex-start;
  gap: 16px;
}

.page-head h1 {
  margin: 0;
  color: #0f172a;
}

.page-head p {
  margin: 6px 0 0;
  color: #64748b;
}

.back-btn {
  min-width: 110px;
  display: inline-flex;
  align-items: center;
  justify-content: center;
  height: 42px;
  padding: 0 18px;
  border: none;
  border-radius: 12px;
  background: #020617;
  color: #ffffff;
  font-weight: 600;
  box-shadow: 0 10px 22px rgba(15, 23, 42, 0.12);
}

.document-frame-shell {
  flex: 1;
  min-height: 0;
  border: 1px solid #dbe1ea;
  border-radius: 20px;
  overflow: hidden;
  background: #1f2937;
  box-shadow: 0 18px 40px rgba(15, 23, 42, 0.12);
}

.document-frame {
  width: 100%;
  height: 100%;
  min-height: 70vh;
  border: none;
}

.page-state {
  padding: 14px;
  border-radius: 12px;
  background: #eef3f9;
  color: #334155;
  text-align: center;
}

.page-state.error {
  background: #fef2f2;
  color: #b91c1c;
}

@media screen and (max-width: 767px) {
  .document-page {
    padding: 16px;
  }

  .page-head {
    flex-direction: column;
  }
}
</style>
