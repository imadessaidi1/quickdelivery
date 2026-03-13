<template>
  <div class="terms-page">
    <header class="terms-hero">
      <span class="hero-chip">{{ $t('legalPageChip') }}</span>
      <h1>{{ $t('legalPageTitle') }}</h1>
      <p>{{ $t('legalPageSubtitle') }}</p>
    </header>

    <section class="terms-card">
      <article class="terms-section">
        <h2>{{ $t('legalSectionUseTitle') }}</h2>
        <p>{{ $t('legalSectionUseBody') }}</p>
      </article>

      <article class="terms-section">
        <h2>{{ $t('legalSectionAccountTitle') }}</h2>
        <p>{{ $t('legalSectionAccountBody') }}</p>
      </article>

      <article class="terms-section">
        <h2>{{ $t('legalSectionPackageTitle') }}</h2>
        <p>{{ $t('legalSectionPackageBody') }}</p>
      </article>

      <article class="terms-section">
        <h2>{{ $t('legalSectionLiabilityTitle') }}</h2>
        <p>{{ $t('legalSectionLiabilityBody') }}</p>
      </article>

      <article class="terms-section">
        <h2>{{ $t('legalSectionGdprTitle') }}</h2>
        <p>{{ $t('legalSectionGdprBody') }}</p>
      </article>

      <article class="terms-section">
        <h2>{{ $t('legalSectionRightsTitle') }}</h2>
        <p>{{ $t('legalSectionRightsBody') }}</p>
      </article>

      <footer class="terms-actions">
        <button class="btn primary_btn terms-action-btn" type="button" @click="acceptAndReturn">
          {{ $t('legalPageAcknowledgeAction') }}
        </button>
      </footer>
    </section>
  </div>
</template>

<script>
import { markLegalPageConsulted } from '@/config/legal';

export default {
  computed: {
    flow() {
      return this.$route.query.flow || '';
    },
    returnTo() {
      return this.$route.query.returnTo || '/';
    },
  },
  methods: {
    acceptAndReturn() {
      markLegalPageConsulted(this.flow);
      this.$router.push(this.returnTo);
    },
  },
};
</script>

<style scoped>
.terms-page {
  min-height: 100%;
  padding: 24px;
  background: linear-gradient(180deg, #f3f6fb 0%, #eef3f9 100%);
}

.terms-hero {
  max-width: 920px;
  margin: 0 auto 18px;
}

.hero-chip {
  display: inline-flex;
  align-items: center;
  padding: 5px 10px;
  border-radius: 999px;
  background: #e7eefb;
  color: #28558c;
  font-size: 0.78rem;
  font-weight: 700;
  text-transform: uppercase;
  letter-spacing: 0.04em;
}

.terms-hero h1 {
  margin: 10px 0 8px;
  color: #14213d;
}

.terms-hero p {
  margin: 0;
  color: #617086;
}

.terms-card {
  max-width: 920px;
  margin: 0 auto;
  padding: 30px;
  border: 1px solid #dde5f0;
  border-radius: 26px;
  background: rgba(255, 255, 255, 0.94);
  box-shadow: 0 18px 44px rgba(24, 39, 75, 0.08);
}

.terms-section + .terms-section {
  margin-top: 24px;
}

.terms-section h2 {
  margin: 0 0 10px;
  color: #14213d;
}

.terms-section p {
  margin: 0;
  color: #425067;
  line-height: 1.7;
}

.terms-actions {
  margin-top: 28px;
  padding-top: 20px;
  border-top: 1px solid #e6edf6;
}

.terms-action-btn {
  min-width: 220px;
}

@media screen and (max-width: 720px) {
  .terms-page {
    padding: 16px;
  }

  .terms-card {
    padding: 22px 18px;
  }

  .terms-action-btn {
    width: 100%;
  }
}
</style>
