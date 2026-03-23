<template>
  <section v-if="readiness" class="readiness-card" :class="variantClass">
    <div class="readiness-head">
      <div>
        <span class="eyebrow">{{ $t('courierReadinessEyebrow') }}</span>
        <h2>{{ $t('courierReadinessTitle') }}</h2>
        <p>{{ readiness.readyForActivation ? $t('courierReadinessReady') : $t('courierReadinessPending') }}</p>
      </div>
      <div class="readiness-score">
        <strong>{{ readiness.completedSteps }}/{{ readiness.totalSteps }}</strong>
        <span>{{ $t('courierReadinessScore') }}</span>
      </div>
    </div>

    <div class="readiness-grid">
      <div
        v-for="step in readiness.steps"
        :key="step.key"
        class="readiness-item"
        :class="{ done: step.done }"
      >
        <span class="material-symbols-outlined">{{ step.done ? 'check_circle' : 'radio_button_unchecked' }}</span>
        <span>{{ $t(step.labelKey) }}</span>
      </div>
    </div>

    <div v-if="readiness.rejectedDocuments.length" class="rejected-box">
      <strong>{{ $t('courierReadinessRejectedTitle') }}</strong>
      <div class="rejected-chips">
        <span v-for="documentKey in readiness.rejectedDocuments" :key="documentKey" class="rejected-chip">
          {{ $t(documentKey) }}
        </span>
      </div>
    </div>
  </section>
</template>

<script>
export default {
  props: {
    readiness: {
      type: Object,
      default: null,
    },
    variant: {
      type: String,
      default: 'default',
    },
  },
  computed: {
    variantClass() {
      return `variant-${this.variant}`;
    },
  },
};
</script>

<style scoped>
.readiness-card {
  margin-bottom: 22px;
  padding: 22px;
  border: 1px solid #dde5f0;
  border-radius: 22px;
  background: #ffffff;
  box-shadow: 0 18px 44px rgba(24, 39, 75, 0.06);
}

.readiness-card.variant-compact {
  margin-bottom: 18px;
  padding: 18px;
  border-radius: 20px;
  box-shadow: 0 12px 28px rgba(24, 39, 75, 0.05);
}

.readiness-head {
  display: flex;
  justify-content: space-between;
  align-items: flex-start;
  gap: 16px;
  margin-bottom: 18px;
}

.eyebrow {
  display: inline-flex;
  align-items: center;
  padding: 4px 10px;
  border-radius: 999px;
  background: #edf4ff;
  color: #27548a;
  font-size: 0.78rem;
  font-weight: 700;
  text-transform: uppercase;
}

.readiness-head h2 {
  margin: 8px 0 6px;
  color: #14213d;
}

.readiness-head p {
  margin: 0;
  color: #617086;
}

.readiness-card.variant-compact .readiness-head h2 {
  font-size: 1.35rem;
}

.readiness-card.variant-compact .readiness-head p {
  font-size: 0.92rem;
}

.readiness-score {
  min-width: 120px;
  padding: 14px;
  border-radius: 18px;
  background: #0f172a;
  color: #ffffff;
  text-align: center;
}

.readiness-score strong,
.readiness-score span {
  display: block;
}

.readiness-card.variant-compact .readiness-score {
  min-width: 104px;
  padding: 12px;
}

.readiness-grid {
  display: grid;
  grid-template-columns: repeat(2, minmax(0, 1fr));
  gap: 12px;
}

.readiness-item {
  display: flex;
  align-items: center;
  gap: 10px;
  padding: 14px;
  border: 1px solid #dde5f0;
  border-radius: 16px;
  background: #f8fafc;
  color: #475569;
}

.readiness-item.done {
  border-color: #c7ead7;
  background: #f3fbf6;
  color: #166534;
}

.readiness-card.variant-compact .readiness-grid {
  grid-template-columns: repeat(2, minmax(0, 1fr));
  gap: 10px;
}

.readiness-card.variant-compact .readiness-item {
  padding: 12px;
  font-size: 0.92rem;
}

.readiness-item .material-symbols-outlined {
  font-size: 20px;
}

.rejected-box {
  margin-top: 18px;
  padding: 16px;
  border: 1px solid #fecaca;
  border-radius: 18px;
  background: #fff7f7;
}

.rejected-box strong {
  display: block;
  margin-bottom: 10px;
  color: #b91c1c;
}

.rejected-chips {
  display: flex;
  flex-wrap: wrap;
  gap: 8px;
}

.rejected-chip {
  display: inline-flex;
  align-items: center;
  padding: 6px 10px;
  border-radius: 999px;
  background: #fee2e2;
  color: #b91c1c;
  font-size: 0.82rem;
  font-weight: 700;
}

@media screen and (max-width: 767px) {
  .readiness-card {
    padding: 18px;
  }

  .readiness-head {
    flex-direction: column;
  }

  .readiness-score {
    min-width: 0;
    width: 100%;
  }

  .readiness-grid {
    grid-template-columns: 1fr;
  }

  .readiness-card.variant-compact .readiness-grid {
    grid-template-columns: 1fr;
  }
}
</style>
