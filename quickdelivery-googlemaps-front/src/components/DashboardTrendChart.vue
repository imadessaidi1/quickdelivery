<template>
  <article class="panel-card trend-card">
    <div class="panel-head">
      <h2>{{ title }}</h2>
      <p>{{ subtitle }}</p>
    </div>

    <div v-if="points.length && maxValue > 0" class="chart-shell">
      <svg viewBox="0 0 340 220" class="trend-svg" aria-hidden="true">
        <line
          v-for="tick in ticks"
          :key="`grid-${tick.value}`"
          x1="32"
          x2="308"
          :y1="tick.y"
          :y2="tick.y"
          class="grid-line"
        />
        <polyline :points="polylinePoints" :stroke="tone" class="trend-line" />
        <circle
          v-for="point in points"
          :key="point.label"
          :cx="point.x"
          :cy="point.y"
          r="4.5"
          :fill="tone"
        />
      </svg>

      <div class="tick-list">
        <span v-for="tick in ticks" :key="tick.value">{{ formatter(tick.value) }}</span>
      </div>

      <div class="point-list">
        <div v-for="point in points" :key="point.label" class="point-item">
          <strong>{{ point.label }}</strong>
          <span>{{ formatter(point.value) }}</span>
        </div>
      </div>
    </div>

    <div v-else class="empty-state">{{ emptyLabel }}</div>
  </article>
</template>

<script>
const CHART_HEIGHT = 180;
const CHART_TOP = 16;

export default {
  props: {
    title: {
      type: String,
      required: true,
    },
    subtitle: {
      type: String,
      required: true,
    },
    points: {
      type: Array,
      default: () => [],
    },
    tone: {
      type: String,
      default: '#1f5fae',
    },
    formatter: {
      type: Function,
      default: (value) => `${value}`,
    },
    emptyLabel: {
      type: String,
      required: true,
    },
  },
  computed: {
    maxValue() {
      return Math.max(0, ...this.points.map((point) => Number(point.value || 0)));
    },
    ticks() {
      const tickValues = [1, 0.66, 0.33, 0].map((ratio) => Math.round(this.maxValue * ratio));
      return tickValues.map((value, index) => ({
        value,
        y: CHART_TOP + ((CHART_HEIGHT / 3) * index),
      }));
    },
    polylinePoints() {
      return this.points.map((point) => `${point.x},${point.y}`).join(' ');
    },
  },
};
</script>

<style scoped>
.trend-card {
  min-height: 100%;
}

.chart-shell {
  display: grid;
  gap: 14px;
}

.trend-svg {
  width: 100%;
  height: auto;
  overflow: visible;
}

.grid-line {
  stroke: rgba(100, 116, 139, 0.18);
  stroke-width: 1;
  stroke-dasharray: 4 6;
}

.trend-line {
  fill: none;
  stroke-width: 3;
  stroke-linecap: round;
  stroke-linejoin: round;
}

.tick-list {
  display: flex;
  justify-content: space-between;
  gap: 8px;
  color: #64748b;
  font-size: 0.78rem;
}

.point-list {
  display: grid;
  grid-template-columns: repeat(auto-fit, minmax(92px, 1fr));
  gap: 10px;
}

.point-item {
  padding: 10px 12px;
  border: 1px solid #e5ebf3;
  border-radius: 14px;
  background: #ffffff;
}

.point-item strong,
.point-item span {
  display: block;
}

.point-item strong {
  margin-bottom: 4px;
  color: #0f172a;
  font-size: 0.8rem;
}

.point-item span {
  color: #64748b;
  font-size: 0.82rem;
}
</style>
