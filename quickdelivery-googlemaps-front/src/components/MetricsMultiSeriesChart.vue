<template>
  <article class="panel-card chart-card">
    <div class="panel-head">
      <h2>{{ title }}</h2>
      <p>{{ subtitle }}</p>
    </div>

    <div v-if="hasData" class="chart-shell">
      <svg viewBox="0 0 760 260" class="chart-svg" aria-hidden="true">
        <text
          v-for="tick in ticks"
          :key="`label-${tick.value}`"
          x="46"
          :y="tick.y + 4"
          class="axis-label"
          text-anchor="end"
        >
          {{ tick.label }}
        </text>

        <line
          v-for="tick in ticks"
          :key="`grid-${tick.value}`"
          x1="54"
          x2="720"
          :y1="tick.y"
          :y2="tick.y"
          class="grid-line"
        />

        <g v-for="seriesItem in normalizedSeries" :key="seriesItem.key">
          <polyline :points="seriesItem.polyline" :stroke="seriesItem.color" class="trend-line" />
          <circle
            v-for="point in seriesItem.points"
            :key="`${seriesItem.key}-${point.label}`"
            :cx="point.x"
            :cy="point.y"
            r="3.5"
            :fill="seriesItem.color"
          />
        </g>
      </svg>

      <div class="axis-row">
        <span v-for="label in displayLabels" :key="label">{{ label }}</span>
      </div>

      <div class="legend-list">
        <div v-for="seriesItem in normalizedSeries" :key="`legend-${seriesItem.key}`" class="legend-item">
          <span class="legend-dot" :style="{ backgroundColor: seriesItem.color }"></span>
          <strong>{{ seriesItem.label }}</strong>
          <span>{{ formatter(seriesItem.currentValue) }}</span>
        </div>
      </div>
    </div>

    <div v-else class="empty-state">{{ emptyLabel }}</div>
  </article>
</template>

<script>
const CHART_LEFT = 54;
const CHART_RIGHT = 720;
const CHART_TOP = 20;
const CHART_HEIGHT = 190;

export default {
  props: {
    title: { type: String, required: true },
    subtitle: { type: String, required: true },
    labels: { type: Array, default: () => [] },
    series: { type: Array, default: () => [] },
    formatter: { type: Function, default: (value) => `${value}` },
    emptyLabel: { type: String, required: true },
  },
  computed: {
    maxValue() {
      return Math.max(0, ...this.series.flatMap((seriesItem) => seriesItem.values || []).map((value) => Number(value || 0)));
    },
    hasData() {
      return this.maxValue > 0 && this.labels.length > 1 && this.series.some((seriesItem) => (seriesItem.values || []).some((value) => Number(value || 0) > 0));
    },
    ticks() {
      const tickValues = [1, 0.75, 0.5, 0.25, 0].map((ratio) => this.maxValue * ratio);
      return tickValues.map((value, index) => ({
        value,
        y: CHART_TOP + ((CHART_HEIGHT / 4) * index),
        label: this.formatter(this.normalizeTickValue(value)),
      }));
    },
    normalizedSeries() {
      const count = Math.max(this.labels.length - 1, 1);
      const step = (CHART_RIGHT - CHART_LEFT) / count;
      return this.series.map((seriesItem, seriesIndex) => {
        const points = this.labels.map((label, index) => {
          const value = Number(seriesItem.values?.[index] || 0);
          const ratio = this.maxValue > 0 ? value / this.maxValue : 0;
          return {
            label,
            value,
            x: CHART_LEFT + (index * step),
            y: CHART_TOP + (CHART_HEIGHT - (ratio * CHART_HEIGHT)),
          };
        });
        return {
          key: seriesItem.key || `series-${seriesIndex}`,
          label: seriesItem.label,
          color: seriesItem.color || '#1f5fae',
          currentValue: points[points.length - 1]?.value || 0,
          points,
          polyline: points.map((point) => `${point.x},${point.y}`).join(' '),
        };
      });
    },
    displayLabels() {
      if (this.labels.length <= 6) {
        return this.labels;
      }
      const lastIndex = this.labels.length - 1;
      return this.labels.map((label, index) => {
        if (index === 0 || index === lastIndex || index % 4 === 0) {
          return label;
        }
        return ' ';
      });
    },
    normalizeTickValue() {
      return (value) => {
        if (!Number.isFinite(value)) {
          return 0;
        }
        if (Number.isInteger(value) || this.maxValue <= 10) {
          return Number(value.toFixed(1));
        }
        return Math.round(value);
      };
    },
  },
};
</script>

<style scoped>
.chart-card {
  min-height: 100%;
}

.panel-head p {
  margin: 6px 0 0;
  color: #64748b;
}

.chart-shell {
  display: grid;
  gap: 16px;
}

.chart-svg {
  width: 100%;
  height: auto;
}

.grid-line {
  stroke: rgba(100, 116, 139, 0.16);
  stroke-width: 1;
  stroke-dasharray: 5 6;
}

.axis-label {
  fill: #64748b;
  font-size: 11px;
}

.trend-line {
  fill: none;
  stroke-width: 3;
  stroke-linecap: round;
  stroke-linejoin: round;
}

.axis-row {
  display: grid;
  grid-template-columns: repeat(auto-fit, minmax(28px, 1fr));
  gap: 6px;
  color: #64748b;
  font-size: 0.74rem;
}

.legend-list {
  display: grid;
  grid-template-columns: repeat(auto-fit, minmax(170px, 1fr));
  gap: 10px;
}

.legend-item {
  display: flex;
  align-items: center;
  gap: 10px;
  padding: 10px 12px;
  border: 1px solid #e5ebf3;
  border-radius: 14px;
  background: #ffffff;
}

.legend-item strong {
  color: #0f172a;
}

.legend-item span:last-child {
  margin-left: auto;
  color: #64748b;
}

.legend-dot {
  width: 12px;
  height: 12px;
  border-radius: 50%;
  flex: 0 0 auto;
}
</style>
