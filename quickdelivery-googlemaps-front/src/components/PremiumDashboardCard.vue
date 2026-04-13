<template>
  <div
    class="premium-card"
    :class="[variantClass, { 'is-interactive': interactive, 'has-glow': glow }]"
    @click="handleClick"
  >
    <div v-if="$slots.header || title" class="card-header">
      <div class="header-main">
        <slot name="header">
          <h3 v-if="title">{{ title }}</h3>
          <p v-if="subtitle">{{ subtitle }}</p>
        </slot>
      </div>
      <div v-if="$slots.actions" class="header-actions">
        <slot name="actions" />
      </div>
    </div>

    <div class="card-body" :class="{ 'no-padding': noPadding }">
      <slot />
    </div>

    <div v-if="$slots.footer" class="card-footer">
      <slot name="footer" />
    </div>
  </div>
</template>

<script>
export default {
  name: 'PremiumDashboardCard',
  props: {
    title: String,
    subtitle: String,
    variant: {
      type: String,
      default: 'glass', // 'glass' | 'flat' | 'solid' | 'gradient'
    },
    interactive: {
      type: Boolean,
      default: false,
    },
    glow: {
      type: Boolean,
      default: false,
    },
    noPadding: {
      type: Boolean,
      default: false,
    },
    tone: {
      type: String,
      default: 'indigo', // 'indigo' | 'emerald' | 'amber' | 'rose' | 'slate'
    },
  },
  computed: {
    variantClass() {
      return `variant-${this.variant} tone-${this.tone}`;
    },
  },
  methods: {
    handleClick(event) {
      if (this.interactive) {
        this.$emit('click', event);
      }
    },
  },
};
</script>

<style scoped>
.premium-card {
  position: relative;
  display: flex;
  flex-direction: column;
  border-radius: 24px;
  overflow: hidden;
  transition: var(--qd-transition);
  border: 1px solid rgba(226, 232, 240, 0.8);
  background: #ffffff;
}

/* Variants */
.variant-glass {
  background: rgba(255, 255, 255, 0.7);
  backdrop-filter: blur(12px);
  -webkit-backdrop-filter: blur(12px);
  border: 1px solid rgba(255, 255, 255, 0.4);
  box-shadow: 0 8px 32px rgba(15, 23, 42, 0.05);
}

.variant-flat {
  background: #ffffff;
  box-shadow: 0 10px 25px rgba(15, 23, 42, 0.04);
}

.variant-solid {
  background: #f8fafc;
  box-shadow: none;
  border-color: #e2e8f0;
}

.variant-gradient {
  background: linear-gradient(135deg, #ffffff 0%, #f1f5f9 100%);
  box-shadow: 0 12px 30px rgba(15, 23, 42, 0.06);
}

/* Interaction */
.is-interactive {
  cursor: pointer;
}

.is-interactive:hover {
  transform: translateY(-4px) scale(1.01);
  box-shadow: 0 20px 40px rgba(15, 23, 42, 0.1);
}

.is-interactive:active {
  transform: translateY(0) scale(0.98);
}

/* Glow Effect */
.has-glow.tone-indigo { box-shadow: 0 0 20px rgba(79, 70, 229, 0.15); }
.has-glow.tone-emerald { box-shadow: 0 0 20px rgba(16, 185, 129, 0.15); }
.has-glow.tone-amber { box-shadow: 0 0 20px rgba(245, 158, 11, 0.15); }
.has-glow.tone-rose { box-shadow: 0 0 20px rgba(244, 63, 94, 0.15); }

/* Internal Layout */
.card-header {
  display: flex;
  justify-content: space-between;
  align-items: flex-start;
  padding: 24px 28px 16px;
  gap: 16px;
}

.header-main h3 {
  margin: 0;
  font-size: 1.25rem;
  font-weight: 700;
  color: #0f172a;
  letter-spacing: -0.01em;
}

.header-main p {
  margin: 4px 0 0;
  font-size: 0.875rem;
  color: #64748b;
}

.card-body {
  flex: 1;
  padding: 0 28px 24px;
}

.card-body.no-padding {
  padding: 0;
}

.card-footer {
  padding: 16px 28px;
  background: rgba(15, 23, 42, 0.02);
  border-top: 1px solid rgba(15, 23, 42, 0.05);
}

@media (max-width: 640px) {
  .premium-card {
    border-radius: 14px;
  }
  .card-header {
    padding: 14px 14px 8px;
    gap: 10px;
  }
  .header-main h3 {
    font-size: 0.98rem;
    line-height: 1.2;
  }
  .header-main p {
    font-size: 0.76rem;
    line-height: 1.3;
  }
  .card-body {
    padding: 0 14px 14px;
  }
  .card-footer {
    padding: 10px 14px;
  }
}
</style>
