<template>
  <PremiumDashboardCard :title="title" :subtitle="subtitle" variant="glass" :tone="toneClass" class="trend-card">
    <div v-if="points.length && maxValue > 0" class="chart-content">
      <div class="chart-shell">
        <svg viewBox="0 0 340 220" class="trend-svg" preserveAspectRatio="xMidYMid meet">
          <defs>
            <linearGradient :id="`gradient-${id}`" x1="0" y1="0" x2="0" y2="1">
              <stop offset="0%" :stop-color="tone" stop-opacity="0.32" />
              <stop offset="100%" :stop-color="tone" stop-opacity="0" />
            </linearGradient>
            <filter :id="`shadow-${id}`" x="-20%" y="-20%" width="140%" height="140%">
              <feGaussianBlur in="SourceAlpha" stdDeviation="3.5" />
              <feOffset dx="0" dy="5" result="offsetblur" />
              <feComponentTransfer>
                <feFuncA type="linear" slope="0.4" />
              </feComponentTransfer>
              <feMerge>
                <feMergeNode />
                <feMergeNode in="SourceGraphic" />
              </feMerge>
            </filter>
          </defs>

          <!-- Grid Lines -->
          <line
            v-for="tick in ticks"
            :key="`grid-${tick.value}`"
            x1="32"
            x2="310"
            :y1="tick.y"
            :y2="tick.y"
            class="grid-line"
          />

          <!-- Y-Axis Labels -->
          <text
            v-for="tick in ticks"
            :key="`label-${tick.value}`"
            x="24"
            :y="tick.y + 4"
            class="axis-label"
            text-anchor="end"
          >
            {{ formatter(tick.displayValue) }}
          </text>

          <!-- Area Under Curve -->
          <path
            :d="areaPath"
            :fill="`url(#gradient-${id})`"
            class="area-path"
          />

          <!-- Trend Line -->
          <path
            :d="linePath"
            :stroke="tone"
            :filter="`url(#shadow-${id})`"
            class="trend-line"
          />

          <!-- Interaction Points -->
          <g v-for="point in points" :key="point.label" class="point-group">
            <circle
              :cx="point.x"
              :cy="point.y"
              r="12"
              fill="transparent"
              class="hover-trigger"
            />
            <circle
              :cx="point.x"
              :cy="point.y"
              r="5"
              :fill="tone"
              class="data-point"
            />
          </g>
        </svg>
      </div>

      <div class="point-legend">
        <div 
          v-for="point in points" 
          :key="point.label" 
          class="legend-item"
          :class="{ 'is-zero': point.value === 0 }"
        >
          <span class="legend-dot" :style="{ backgroundColor: tone }"></span>
          <div class="legend-meta">
            <span class="legend-label">{{ point.label }}</span>
            <span class="legend-value">{{ formatter(point.value) }}</span>
          </div>
        </div>
      </div>
    </div>

    <div v-else class="empty-state">
      <div class="empty-visual">
        <span class="material-symbols-outlined">analytics</span>
      </div>
      <p>{{ emptyLabel }}</p>
    </div>
  </PremiumDashboardCard>
</template>

<script>
import PremiumDashboardCard from './PremiumDashboardCard.vue';

const CHART_HEIGHT = 160;
const CHART_TOP = 20;

export default {
  name: 'DashboardTrendChart',
  components: {
    PremiumDashboardCard,
  },
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
      default: '#4f46e5',
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
  data() {
    return {
      id: Math.random().toString(36).substr(2, 9),
    };
  },
  computed: {
    toneClass() {
      if (this.tone === '#4f46e5' || this.tone === '#1f5fae') return 'indigo';
      if (this.tone === '#10b981' || this.tone === '#159947') return 'emerald';
      if (this.tone === '#f59e0b' || this.tone === '#ef7d32') return 'amber';
      return 'slate';
    },
    maxValue() {
      return Math.max(0, ...this.points.map((point) => Number(point.value || 0)));
    },
    ticks() {
      if (!this.maxValue) return [];
      const tickValues = [1, 0.66, 0.33, 0].map((ratio) => this.maxValue * ratio);
      return tickValues.map((value, index) => ({
        value,
        displayValue: this.normalizeTickValue(value),
        y: CHART_TOP + ((CHART_HEIGHT / 3) * index),
      }));
    },
    linePath() {
      if (!this.points.length) return '';
      return this.points.reduce((path, point, index) => {
        return index === 0 ? `M ${point.x} ${point.y}` : `${path} L ${point.x} ${point.y}`;
      }, '');
    },
    areaPath() {
      if (!this.points.length) return '';
      const lPath = this.linePath;
      const lastPoint = this.points[this.points.length - 1];
      const firstPoint = this.points[0];
      const baselineY = CHART_TOP + CHART_HEIGHT;
      return `${lPath} L ${lastPoint.x} ${baselineY} L ${firstPoint.x} ${baselineY} Z`;
    },
  },
  methods: {
    normalizeTickValue(value) {
      if (!Number.isFinite(value)) return 0;
      if (this.maxValue > 1000) return Math.round(value / 100) * 100;
      if (this.maxValue > 10) return Math.round(value);
      return Number(value.toFixed(1));
    },
  },
};
</script>

<style scoped>
.trend-card {
  min-height: 100%;
}

.chart-content {
  display: flex;
  flex-direction: column;
  gap: 24px;
}

.trend-svg {
  width: 100%;
  height: auto;
  overflow: visible;
  filter: drop-shadow(0 4px 6px rgba(0, 0, 0, 0.05));
}

.grid-line {
  stroke: rgba(100, 116, 139, 0.08);
  stroke-width: 1;
}

.axis-label {
  fill: #94a3b8;
  font-size: 10px;
  font-weight: 500;
}

.trend-line {
  fill: none;
  stroke-width: 3.5;
  stroke-linecap: round;
  stroke-linejoin: round;
  transition: all 0.5s ease;
}

.area-path {
  transition: all 0.5s ease;
}

.data-point {
  stroke: #ffffff;
  stroke-width: 2.5;
  transition: r 0.3s ease;
}

.point-group:hover .data-point {
  r: 7;
}

.point-legend {
  display: grid;
  grid-template-columns: repeat(auto-fill, minmax(100px, 1fr));
  gap: 12px;
}

.legend-item {
  display: flex;
  align-items: center;
  gap: 8px;
  padding: 8px 10px;
  background: rgba(15, 23, 42, 0.03);
  border-radius: 12px;
  transition: var(--qd-transition);
}

.legend-item:hover {
  background: rgba(15, 23, 42, 0.06);
}

.legend-dot {
  width: 8px;
  height: 8px;
  border-radius: 50%;
  flex-shrink: 0;
}

.legend-meta {
  display: flex;
  flex-direction: column;
  min-width: 0;
}

.legend-label {
  font-size: 10px;
  text-transform: uppercase;
  color: #64748b;
  letter-spacing: 0.02em;
}

.legend-value {
  font-size: 13px;
  font-weight: 700;
  color: #0f172a;
}

.is-zero {
  opacity: 0.5;
}

.empty-state {
  display: flex;
  flex-direction: column;
  align-items: center;
  justify-content: center;
  padding: 48px 0;
  text-align: center;
  color: #94a3b8;
}

.empty-visual {
  width: 56px;
  height: 56px;
  border-radius: 50%;
  background: #f1f5f9;
  display: flex;
  align-items: center;
  justify-content: center;
  margin-bottom: 12px;
}

.empty-visual span {
  font-size: 28px;
}

.empty-state p {
  margin: 0;
  font-size: 0.875rem;
}
</style>
