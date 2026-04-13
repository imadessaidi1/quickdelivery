<template>
  <div class="document-page qd-page">
    <div class="page-shell">
      <template v-if="isLoading">
        <header class="qd-page-header">
          <div class="header-main">
            <button class="back-link qd-btn-secondary" type="button" @click="goBack" style="margin-bottom: 12px;">
              <span class="icon">←</span> {{ $t('actionBack') }}
            </button>
            <h1>{{ documentTitle }}</h1>
            <p>{{ packageReference }}</p>
          </div>
        </header>
        <div class="page-state">{{ $t('stateLoading') }}</div>
      </template>
      <template v-else-if="loadError">
        <header class="qd-page-header">
          <div class="header-main">
            <button class="back-link qd-btn-secondary" type="button" @click="goBack" style="margin-bottom: 12px;">
              <span class="icon">←</span> {{ $t('actionBack') }}
            </button>
            <h1>{{ documentTitle }}</h1>
            <p>{{ packageReference }}</p>
          </div>
        </header>
        <div class="page-state error">{{ $t('stateLoadError') }}</div>
      </template>
      <template v-else>
        <header class="qd-page-header">
          <div class="header-main">
            <button class="back-link qd-btn-secondary" type="button" @click="goBack" style="margin-bottom: 12px;">
              <span class="icon">←</span> {{ $t('actionBack') }}
            </button>
            <h1>{{ documentTitle }}</h1>
            <p>{{ packageReference }}</p>
          </div>
        </header>

        <div v-if="!documentSrc && loadError" class="page-state error">{{ $t('stateLoadError') }}</div>
        <div v-else-if="!documentSrc" class="page-state">{{ $t('packageDocumentMissing') }}</div>
        <div v-else class="document-frame-shell">
          <img v-if="isImageDocument" :src="documentSrc" class="document-image" :alt="documentTitle">
          <div v-else-if="usePdfJsViewer && isPdfDocument && pdfPageImages.length" class="document-pdf-shell">
            <img
              v-for="(pageImage, index) in pdfPageImages"
              :key="`${reference || packageReference}-${index + 1}`"
              :src="pageImage"
              :alt="`${documentTitle} - ${index + 1}`"
              class="document-pdf-page-image"
            >
          </div>
          <iframe v-else :src="documentSrc" class="document-frame"></iframe>
        </div>
      </template>
    </div>
  </div>
</template>

<script>
import http from '@/config/httpInterceptor';
import { createLoadingTask } from 'vue3-pdfjs/esm';
import { blobToDataUrl, fetchProtectedBlob } from '@/config/binaryContent';
import { shouldUseCapacitorSafeDocumentRendering } from '@/config/network';

function inferMimeType(document, blob) {
  const blobType = blob?.type || '';
  if (blobType && blobType !== 'application/octet-stream') {
    return blobType;
  }

  const source = `${document?.docURL || document?.fileName || ''}`.toLowerCase();
  if (source.endsWith('.pdf')) {
    return 'application/pdf';
  }
  if (source.endsWith('.png')) {
    return 'image/png';
  }
  if (source.endsWith('.jpg') || source.endsWith('.jpeg')) {
    return 'image/jpeg';
  }
  if (source.endsWith('.webp')) {
    return 'image/webp';
  }
  return blobType;
}

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
      documentMimeType: '',
      pdfRenderSource: null,
      pdfPageImages: [],
      pdfPageCount: 0,
      currentDocumentUrl: '',
      packageReference: this.reference || '',
    };
  },
  computed: {
    documentTitle() {
      return this.documentType ? this.$t(this.documentType) : this.$t('packagesArroundMArkerDetailActionsDetails');
    },
    isImageDocument() {
      return typeof this.documentMimeType === 'string' && this.documentMimeType.startsWith('image/');
    },
    isPdfDocument() {
      return this.documentMimeType === 'application/pdf';
    },
    useCapacitorSafeRendering() {
      return shouldUseCapacitorSafeDocumentRendering();
    },
    usePdfJsViewer() {
      return this.useCapacitorSafeRendering;
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
      this.documentMimeType = '';
      this.pdfRenderSource = null;
      this.pdfPageImages = [];
      this.pdfPageCount = 0;
      try {
        const response = await http.get(this.$i18n.t('rootURL') + this.$i18n.t('getPackage') + this.reference);
        const docs = response.data?.documentS || {};
        const currentDocument = docs[this.documentType];
        if (!currentDocument?.id) {
          this.documentSrc = '';
          return;
        }
        this.packageReference = response.data?.reference || this.reference;
        const blob = await fetchProtectedBlob(`${this.$i18n.t('rootURL')}${this.$i18n.t('getPackageDocumentContent')}${encodeURIComponent(currentDocument.id)}`);
        this.revokeDocumentUrl();
        this.documentMimeType = inferMimeType(currentDocument, blob);
        if (this.documentMimeType === 'application/pdf' && this.usePdfJsViewer) {
          const pdfData = new Uint8Array(await blob.arrayBuffer());
          this.currentDocumentUrl = URL.createObjectURL(blob);
          this.documentSrc = this.currentDocumentUrl;
          this.pdfRenderSource = { data: pdfData };
          await this.renderPdfPages(this.pdfRenderSource);
          return;
        }
        if (this.useCapacitorSafeRendering) {
          this.documentSrc = await blobToDataUrl(blob);
          return;
        }
        this.currentDocumentUrl = URL.createObjectURL(blob);
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
    async renderPdfPages(pdfSource) {
      const loadingTask = createLoadingTask(pdfSource);
      const pdfDocument = await loadingTask.promise;
      this.pdfPageCount = pdfDocument?.numPages || 0;
      const nextPageImages = [];
      for (let pageNumber = 1; pageNumber <= this.pdfPageCount; pageNumber += 1) {
        const page = await pdfDocument.getPage(pageNumber);
        const viewport = page.getViewport({ scale: 1.5 });
        const canvas = document.createElement('canvas');
        const context = canvas.getContext('2d');
        canvas.width = Math.ceil(viewport.width);
        canvas.height = Math.ceil(viewport.height);
        await page.render({ canvasContext: context, viewport }).promise;
        nextPageImages.push(canvas.toDataURL('image/png'));
      }
      this.pdfPageImages = nextPageImages;
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
  padding-bottom: 40px;
}

.page-shell {
  display: flex;
  flex-direction: column;
  gap: 32px;
  min-height: calc(100vh - 130px);
}

.page-head {
  display: flex;
  flex-direction: column;
  gap: 24px;
}

.page-head h1 {
  margin: 0;
  font-size: 2.5rem;
  font-weight: 800;
  color: #0f172a;
}

.page-head p {
  margin: 4px 0 0;
  color: #64748b;
  font-size: 1.125rem;
}

.back-link {
  display: inline-flex;
  align-items: center;
  gap: 8px;
  height: 36px;
  padding: 0 16px;
  border-radius: 10px;
  font-size: 0.85rem;
  font-weight: 700;
}

.document-frame-shell {
  flex: 1;
  min-height: 0;
  border: 1px solid #f1f5f9;
  border-radius: 24px;
  overflow: hidden;
  background: #111827;
  box-shadow: 0 20px 40px rgba(15, 23, 42, 0.1);
}

.document-frame {
  width: 100%;
  height: 100%;
  min-height: 70vh;
  border: none;
}
.document-image {
  display: block;
  width: 100%;
  height: 100%;
  min-height: 70vh;
  object-fit: contain;
  background: #111827;
}
.document-pdf-shell {
  display: flex;
  flex-direction: column;
  gap: 20px;
  min-height: 70vh;
  padding: 24px;
  overflow: auto;
  box-sizing: border-box;
  background: #111827;
}
.document-pdf-page-image {
  display: block;
  width: 100%;
  background: #fff;
  border-radius: 12px;
  box-shadow: 0 4px 12px rgba(0, 0, 0, 0.3);
}


.page-state {
  display: flex;
  align-items: center;
  justify-content: center;
  padding: 80px;
  background: #fff;
  border-radius: 24px;
  color: #64748b;
  font-weight: 600;
  text-align: center;
  box-shadow: 0 10px 30px rgba(0, 0, 0, 0.03);
}

.page-state.error {
  background: #fef2f2;
  color: #b91c1c;
}

@media screen and (max-width: 767px) {
  .document-page {
    padding: 12px;
  }
}
</style>
