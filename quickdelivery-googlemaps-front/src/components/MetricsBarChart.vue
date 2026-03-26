<template>
  <article class="panel-card chart-card">
    <div class="panel-head">
      <h2>{{ title }}</h2>
      <p>{{ subtitle }}</p>
    </div>

    <div v-if="normalizedItems.length" class="bars-list">
      <div v-for="item in normalizedItems" :key="item.key" class="bar-row">
        <div class="bar-labels">
          <strong>{{ item.label }}</strong>
          <span v-if="item.meta">{{ item.meta }}</span>
        </div>
        <div class="bar-track">
          <div class="bar-fill" :style="{ width: `${item.ratio}%`, backgroundColor: item.color }"></div>
        </div>
        <strong class="bar-value">{{ formatter(item.value) }}</strong>
      </div>
    </div>

    <div v-else class="empty-state">{{ emptyLabel }}</div>
  </article>
</template>

<script>
export default {
  props: {
    title: { type: String, required: true },
    subtitle: { type: String, required: true },
    items: { type: Array, default: () => [] },
    formatter: { type: Function, default: (value) => `${value}` },
    emptyLabel: { type: String, required: true },
  },
  computed: {
    maxValue() {
      return Math.max(0, ...this.items.map((item) => Number(item?.value || 0)));
    },
    normalizedItems() {
      return this.items
        .filter((item) => Number(item?.value || 0) > 0)
        .map((item, index) => ({
          key: item.key || `bar-${index}`,
          label: item.label,
          meta: item.meta || '',
          color: item.color || '#1f5fae',
          value: Number(item.value || 0),
          ratio: this.maxValue > 0 ? (Number(item.value || 0) / this.maxValue) * 100 : 0,
        }));
    },
  },
};
</script>

<style scoped>
.chart-card {
  min-height: 100%;
}

.bars-list {
  display: grid;
  gap: 12px;
}

.bar-row {
  display: grid;
  grid-template-columns: minmax(0, 1.4fr) minmax(180px, 2fr) auto;
  gap: 12px;
  align-items: center;
}

.bar-labels {
  display: grid;
  gap: 4px;
}

.bar-labels strong {
  color: #0f172a;
  font-size: 0.92rem;
}

.bar-labels span {
  color: #64748b;
  font-size: 0.78rem;
}

.bar-track {
  overflow: hidden;
  height: 12px;
  border-radius: 999px;
  background: #e7edf6;
}

.bar-fill {
  height: 100%;
  border-radius: 999px;
}

.bar-value {
  color: #0f172a;
  white-space: nowrap;
}

@media screen and (max-width: 767px) {
  .bar-row {
    grid-template-columns: 1fr;
  }
}
</style>
