<template>
  <div v-if="isEnabled" class="turnstile-shell">
    <div ref="container"></div>
  </div>
</template>

<script>
let turnstileLoaderPromise = null;

function loadTurnstileScript() {
  if (window.turnstile) {
    return Promise.resolve(window.turnstile);
  }
  if (turnstileLoaderPromise) {
    return turnstileLoaderPromise;
  }

  turnstileLoaderPromise = new Promise((resolve, reject) => {
    const existing = document.querySelector('script[data-turnstile-loader="true"]');
    if (existing) {
      existing.addEventListener('load', () => resolve(window.turnstile));
      existing.addEventListener('error', reject);
      return;
    }

    const script = document.createElement('script');
    script.src = 'https://challenges.cloudflare.com/turnstile/v0/api.js?render=explicit';
    script.async = true;
    script.defer = true;
    script.dataset.turnstileLoader = 'true';
    script.onload = () => resolve(window.turnstile);
    script.onerror = reject;
    document.head.appendChild(script);
  });

  return turnstileLoaderPromise;
}

export default {
  name: 'TurnstileCaptcha',
  props: {
    action: {
      type: String,
      default: 'submit',
    },
  },
  data() {
    return {
      widgetId: null,
      renderFailed: false,
    };
  },
  computed: {
    siteKey() {
      return process.env.VUE_APP_TURNSTILE_SITE_KEY || '';
    },
    isEnabled() {
      return !!this.siteKey;
    },
  },
  methods: {
    async renderWidget() {
      if (!this.isEnabled || !this.$refs.container) {
        return;
      }
      try {
        const turnstile = await loadTurnstileScript();
        if (!turnstile || typeof turnstile.render !== 'function') {
          throw new Error('Turnstile unavailable');
        }
        this.widgetId = turnstile.render(this.$refs.container, {
          sitekey: this.siteKey,
          action: this.action,
          theme: 'light',
          callback: (token) => this.$emit('verified', token),
          'expired-callback': () => this.$emit('expired'),
          'error-callback': () => this.$emit('error'),
        });
      } catch (error) {
        this.renderFailed = true;
        this.$emit('error', error);
      }
    },
    resetCaptcha() {
      if (!this.isEnabled || this.widgetId === null || !window.turnstile) {
        return;
      }
      window.turnstile.reset(this.widgetId);
      this.$emit('expired');
    },
  },
  mounted() {
    this.renderWidget();
  },
};
</script>

<style scoped>
.turnstile-shell {
  display: flex;
  justify-content: center;
  margin-top: 12px;
}
</style>
