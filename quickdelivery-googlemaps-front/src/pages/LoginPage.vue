<template>
  <div class="login-page">
    <section class="login-hero" :style="heroStyle">
      <div class="hero-copy">
        <span class="hero-chip">{{ $t('applicationName') }}</span>
        <h1>{{ $t('loginPageTitle') }}</h1>
        <p>{{ $t('loginPageSubtitle') }}</p>
      </div>
    </section>

    <section class="login-panel">
      <div class="login-card">
        <span class="card-chip">{{ $t('landingLoginAction') }}</span>
        <h2>{{ $t('loginPageFormTitle') }}</h2>
        <p>{{ $t('loginPageFormSubtitle') }}</p>

        <form class="login-form" @submit.prevent="startLogin">
          <label for="login-email">{{ $t('packageAddressEmail') }}</label>
          <input id="login-email" v-model.trim="loginHint" type="email" autocomplete="email">

          <p v-if="requiresCaptcha" class="security-note captcha-note">{{ $t('captchaSecurityHint') }}</p>
          <TurnstileCaptcha
            v-if="requiresCaptcha"
            ref="loginCaptcha"
            action="login"
            @verified="captchaToken = $event"
            @expired="captchaToken = ''"
            @error="captchaToken = ''"
          />
          <p v-if="captchaError" class="captcha-error">{{ captchaError }}</p>

          <button class="qd-btn-primary login-btn" type="submit" style="height: 48px; border-radius: 14px; width: 100%;">{{ $t('landingLoginAction') }}</button>
        </form>

        <p class="security-note">{{ $t('loginPageSecurityNote') }}</p>

        <div class="login-links">
          <router-link to="/register">{{ $t('landingRegisterAction') }}</router-link>
          <router-link to="/">{{ $t('loginPageBackToLanding') }}</router-link>
        </div>
      </div>
    </section>
  </div>
</template>

<script>
import TurnstileCaptcha from '@/components/TurnstileCaptcha.vue';
import { getLoginAttemptCount, redirectToLogin, registerLoginAttempt } from '@/config/auth';

export default {
  components: {
    TurnstileCaptcha,
  },
  data() {
    return {
      loginHint: '',
      captchaToken: '',
      captchaError: '',
    };
  },
  computed: {
    requiresCaptcha() {
      return !!process.env.VUE_APP_TURNSTILE_SITE_KEY && getLoginAttemptCount() >= 3;
    },
    heroStyle() {
      return {
        backgroundImage: "linear-gradient(rgba(0, 51, 102, 0.58), rgba(0, 51, 102, 0.82)), url('/landing/img/call-agent.jpg')",
      };
    },
  },
  methods: {
    async startLogin() {
      if (this.requiresCaptcha && !this.captchaToken) {
        this.captchaError = this.$t('captchaRequiredMessage');
        return;
      }
      this.captchaError = '';
      registerLoginAttempt();
      await redirectToLogin({
        loginHint: this.loginHint,
      });
    },
  },
};
</script>

<style scoped>
.login-page {
  min-height: 100vh;
  display: grid;
  grid-template-columns: 1.1fr minmax(360px, 460px);
  background: #eef2f6;
}

.login-hero {
  display: flex;
  align-items: center;
  justify-content: center;
  padding: 48px;
  background-position: center;
  background-size: cover;
  background-repeat: no-repeat;
  color: #fff;
}

.hero-copy {
  max-width: 520px;
}

.hero-chip,
.card-chip {
  display: inline-flex;
  align-items: center;
  padding: 6px 12px;
  border-radius: 999px;
  font-size: 0.76rem;
  font-weight: 700;
  letter-spacing: 0.04em;
  text-transform: uppercase;
}

.hero-chip {
  background: rgba(255, 255, 255, 0.16);
}

.hero-copy h1 {
  margin: 18px 0 14px;
  font-size: clamp(2rem, 3.8vw, 3.6rem);
  line-height: 1.1;
}

.hero-copy p {
  margin: 0;
  font-size: 1rem;
  line-height: 1.7;
}

.login-panel {
  display: flex;
  align-items: center;
  justify-content: center;
  padding: 32px;
}

.login-card {
  width: 100%;
  padding: 34px;
  border: 1px solid #d9e1ea;
  border-radius: 24px;
  background: #fff;
  box-shadow: 0 20px 46px rgba(24, 39, 75, 0.08);
}

.card-chip {
  background: #edf4ff;
  color: #1f4f89;
}

.login-card h2 {
  margin: 18px 0 10px;
  font-size: 1.8rem;
  color: #14213d;
}

.login-card p {
  margin: 0;
  color: #5d6b80;
}

.login-form {
  display: grid;
  gap: 12px;
  margin-top: 26px;
}

.login-form label {
  color: #14213d;
  font-weight: 600;
}

.login-form input {
  width: 100%;
  height: 46px;
  padding: 0 14px;
  border: 1px solid #d7dfeb;
  border-radius: 12px;
  background: #f8fafc;
  box-sizing: border-box;
}

.login-btn {
  display: flex;
  align-items: center;
  justify-content: center;
  margin-top: 8px;
}

.security-note {
  margin-top: 18px !important;
  font-size: 0.9rem;
  line-height: 1.6;
}

.captcha-note {
  margin-top: 8px !important;
}

.captcha-error {
  margin: 0;
  color: #b42318;
  font-size: 0.9rem;
}

.login-links {
  display: flex;
  justify-content: space-between;
  gap: 12px;
  margin-top: 24px;
  flex-wrap: wrap;
}

.login-links a {
  color: #1f4f89;
  font-weight: 600;
  text-decoration: none;
}

@media screen and (max-width: 920px) {
  .login-page {
    grid-template-columns: 1fr;
  }

  .login-hero {
    min-height: 300px;
  }
}

@media screen and (max-width: 640px) {
  .login-hero,
  .login-panel {
    padding: 12px;
  }

  .login-hero {
    min-height: 180px;
  }

  .hero-copy h1 {
    margin: 10px 0 8px;
    font-size: 1.55rem;
    line-height: 1.1;
  }

  .hero-copy p {
    font-size: 0.84rem;
    line-height: 1.32;
  }

  .login-card {
    padding: 16px;
  }

  .login-card h2 {
    margin: 12px 0 6px;
    font-size: 1.2rem;
    line-height: 1.16;
  }

  .login-card p,
  .security-note,
  .captcha-error {
    font-size: 0.82rem;
    line-height: 1.32;
  }

  .login-form {
    gap: 10px;
    margin-top: 14px;
  }

  .login-links {
    margin-top: 14px;
  }
}
</style>
