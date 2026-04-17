<template>
  <label class="document-upload-card panel-card" :class="toneClass">
    <div class="doc-header">
      <span class="doc-title">{{ label }}</span>
      <span v-if="statusLabel" class="badge outline" :class="badgeClass">
        {{ statusLabel }}
      </span>
    </div>
    
    <div class="file-drop-area" :class="{ 'has-file': fileName }">
      <input type="file" :accept="accept" @change="$emit('change', $event)">
      <span class="material-symbols-outlined upload-icon">cloud_upload</span>
      <strong class="file-name">{{ fileName || $t('packageDocumentMissing') }}</strong>
    </div>

    <div v-if="reviewComment" class="review-feedback badge danger outline">
      <span class="material-symbols-outlined">history_edu</span>
      <div class="review-text">
        <small>{{ $t('userDocumentReviewCommentLabel') }}</small>
        <p>{{ reviewComment }}</p>
      </div>
    </div>
    
    <span v-if="errorMessage" class="errorMessage">{{ errorMessage }}</span>
  </label>
</template>

<script>
export default {
  name: 'DocumentUploadCard',
  props: {
    label: { type: String, required: true },
    status: { type: String, default: '' },
    fileName: { type: String, default: '' },
    reviewComment: { type: String, default: '' },
    errorMessage: { type: String, default: '' },
    accept: { type: String, default: 'image/*, application/pdf' }
  },
  emits: ['change'],
  computed: {
    statusLabel() {
      if (!this.status || this.status === 'UPDATED') return '';
      return this.$t(this.status);
    },
    badgeClass() {
      if (this.status === 'ACCEPTED') return 'success';
      if (this.status === 'REJECTED') return 'danger';
      return 'info';
    },
    toneClass() {
      if (this.status === 'ACCEPTED') return 'tone-emerald';
      if (this.status === 'REJECTED') return 'tone-rose';
      return 'tone-indigo';
    }
  }
};
</script>

<style scoped>
.document-upload-card {
  padding: 24px;
  display: flex;
  flex-direction: column;
  gap: 20px;
  transition: var(--qd-transition);
  cursor: pointer;
}

.doc-header {
  display: flex;
  justify-content: space-between;
  align-items: center;
}

.doc-title {
  font-weight: 800;
  color: var(--qd-text);
}

.file-drop-area {
  position: relative;
  display: flex;
  flex-direction: column;
  align-items: center;
  justify-content: center;
  padding: 24px;
  border: 2px dashed var(--qd-border-strong);
  border-radius: 16px;
  background: var(--qd-bg);
  text-align: center;
  transition: var(--qd-transition);
  min-height: 120px;
}

.document-upload-card:hover .file-drop-area {
  border-color: var(--qd-primary);
  background: var(--qd-primary-soft);
}

.file-drop-area input {
  position: absolute;
  inset: 0;
  opacity: 0;
  cursor: pointer;
  z-index: 2;
}

.upload-icon {
  font-size: 2.4rem;
  color: var(--qd-primary);
  margin-bottom: 8px;
}

.file-name {
  font-size: 0.9rem;
  color: var(--qd-text);
  word-break: break-all;
  max-width: 100%;
}

.review-feedback {
  display: flex;
  gap: 12px;
  align-items: flex-start;
  padding: 12px;
  border-radius: 12px;
}

.review-feedback .material-symbols-outlined { color: var(--qd-danger); }

.review-text small {
  display: block;
  font-weight: 800;
  font-size: 0.7rem;
  text-transform: uppercase;
}

.review-text p {
  margin: 2px 0 0;
  font-size: 0.85rem;
  line-height: 1.4;
}

.errorMessage {
  font-size: 0.8rem;
  color: var(--qd-danger);
  font-weight: 600;
}
</style>
