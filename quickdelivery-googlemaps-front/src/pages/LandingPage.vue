<template>
  <div class="landing-page">
    <nav class="navbar" :class="{ scrolled: isScrolled }">
      <div class="nav-content">
        <div class="logo">
          <router-link to="/">{{ $t('applicationName') }}</router-link>
        </div>
        <ul>
          <li><a href="#services">{{ $t('landingNavServices') }}</a></li>
          <li><a href="#how-it-works">{{ $t('landingNavHowItWorks') }}</a></li>
          <li><a href="#testimonials">{{ $t('landingNavTestimonials') }}</a></li>
          <li><router-link to="/createPackage">{{ $t('menuNewPackage') }}</router-link></li>
          <li><a href="#" @click.prevent="startLogin">{{ $t('landingLoginAction') }}</a></li>
          <li><router-link to="/register">{{ $t('landingRegisterAction') }}</router-link></li>
        </ul>
      </div>
    </nav>

    <section class="banner" :style="bannerStyle">
      <div class="banner-content">
        <div class="container hero-copy">
          <h1>{{ $t('landingHeroTitle') }}</h1>
          <p>{{ $t('landingHeroSubtitle') }}</p>
          <div class="hero-actions">
            <router-link class="hero-btn tertiary" to="/createPackage">{{ $t('menuNewPackage') }}</router-link>
            <a class="hero-btn" href="#" @click.prevent="startLogin">{{ $t('landingLoginAction') }}</a>
            <router-link class="hero-btn secondary" to="/register">{{ $t('landingRegisterAction') }}</router-link>
          </div>
        </div>
      </div>
    </section>

    <section class="statistics_section container">
      <div class="statistics_head">
        <h2>{{ $t('landingStatsTitle') }}</h2>
        <p>{{ $t('landingStatsSubtitle') }}</p>
      </div>
      <div class="statistics">
        <div class="statistic">
          <img src="/landing/img/001-magasin.png" alt="">
          <h2 style="color: #ef5b00;">500+</h2>
          <p>{{ $t('landingStatStores') }}</p>
        </div>
        <div class="statistic">
          <img src="/landing/img/002-immeubles.png" alt="">
          <h2 style="color: #009deb;">35</h2>
          <p>{{ $t('landingStatCities') }}</p>
        </div>
        <div class="statistic">
          <img src="/landing/img/003-5-toiles.png" alt="">
          <h2 style="color: #f3d600;">4.9/5</h2>
          <p>{{ $t('landingStatRating') }}</p>
        </div>
        <div class="statistic">
          <img src="/landing/img/groupe.png" alt="">
          <h2 style="color: #15AB00;">24/7</h2>
          <p>{{ $t('landingStatSupport') }}</p>
        </div>
      </div>
    </section>

    <section id="services" class="services">
      <div class="container">
        <h1>{{ $t('landingServicesTitle') }}</h1>
        <p>{{ $t('landingServicesSubtitle') }}</p>
        <div class="cards">
          <div class="card">
            <img src="/landing/img/005-fast-delivery.png" alt="">
            <h3>{{ $t('landingServiceFastTitle') }}</h3>
            <p>{{ $t('landingServiceFastText') }}</p>
          </div>
          <div class="card">
            <img src="/landing/img/002-place.png" alt="">
            <h3>{{ $t('landingServiceTrackingTitle') }}</h3>
            <p>{{ $t('landingServiceTrackingText') }}</p>
          </div>
          <div class="card">
            <img src="/landing/img/001-cardboard.png" alt="">
            <h3>{{ $t('landingServiceFlexibleTitle') }}</h3>
            <p>{{ $t('landingServiceFlexibleText') }}</p>
          </div>
        </div>
      </div>
    </section>

    <section id="how-it-works" class="how-it-works">
      <div class="container">
        <h1>{{ $t('landingHowTitle') }}</h1>
        <p class="how-subtitle">{{ $t('landingHowSubtitle') }}</p>
        <div class="journeys">
          <div class="journey-card">
            <div class="journey-head">
              <div class="icon left-div"><img src="/landing/img/004-formulaire-de-contact.png" alt=""></div>
              <div>
                <h2>{{ $t('landingCustomerJourneyTitle') }}</h2>
                <p>{{ $t('landingCustomerJourneySubtitle') }}</p>
              </div>
            </div>
            <div class="steps">
              <div class="step">
                <div class="detail">
                  <h2>{{ $t('landingCustomerStep1Label') }}</h2>
                  <h3>{{ $t('landingCustomerStep1Title') }}</h3>
                  <p>{{ $t('landingCustomerStep1Text') }}</p>
                </div>
              </div>
              <div class="step">
                <div class="detail">
                  <h2>{{ $t('landingCustomerStep2Label') }}</h2>
                  <h3>{{ $t('landingCustomerStep2Title') }}</h3>
                  <p>{{ $t('landingCustomerStep2Text') }}</p>
                </div>
              </div>
              <div class="step">
                <div class="detail">
                  <h2>{{ $t('landingCustomerStep3Label') }}</h2>
                  <h3>{{ $t('landingCustomerStep3Title') }}</h3>
                  <p>{{ $t('landingCustomerStep3Text') }}</p>
                </div>
              </div>
              <div class="step">
                <div class="detail">
                  <h2>{{ $t('landingCustomerStep4Label') }}</h2>
                  <h3>{{ $t('landingCustomerStep4Title') }}</h3>
                  <p>{{ $t('landingCustomerStep4Text') }}</p>
                </div>
              </div>
            </div>
            <div class="journey-actions">
              <router-link class="hero-btn" to="/createPackage">{{ $t('menuNewPackage') }}</router-link>
            </div>
          </div>

          <div class="journey-card">
            <div class="journey-head">
              <div class="icon right-div"><img src="/landing/img/003-delivery-man.png" alt=""></div>
              <div>
                <h2>{{ $t('landingCourierJourneyTitle') }}</h2>
                <p>{{ $t('landingCourierJourneySubtitle') }}</p>
              </div>
            </div>
            <div class="steps">
              <div class="step">
                <div class="detail">
                  <h2>{{ $t('landingCourierStep1Label') }}</h2>
                  <h3>{{ $t('landingCourierStep1Title') }}</h3>
                  <p>{{ $t('landingCourierStep1Text') }}</p>
                </div>
              </div>
              <div class="step">
                <div class="detail">
                  <h2>{{ $t('landingCourierStep2Label') }}</h2>
                  <h3>{{ $t('landingCourierStep2Title') }}</h3>
                  <p>{{ $t('landingCourierStep2Text') }}</p>
                </div>
              </div>
              <div class="step">
                <div class="detail">
                  <h2>{{ $t('landingCourierStep3Label') }}</h2>
                  <h3>{{ $t('landingCourierStep3Title') }}</h3>
                  <p>{{ $t('landingCourierStep3Text') }}</p>
                </div>
              </div>
              <div class="step">
                <div class="detail">
                  <h2>{{ $t('landingCourierStep4Label') }}</h2>
                  <h3>{{ $t('landingCourierStep4Title') }}</h3>
                  <p>{{ $t('landingCourierStep4Text') }}</p>
                </div>
              </div>
            </div>
            <div class="journey-actions">
              <router-link class="hero-btn" to="/register">{{ $t('landingRegisterAction') }}</router-link>
            </div>
          </div>
        </div>
      </div>
    </section>

    <section id="testimonials" class="users_feedback container">
      <h2>{{ $t('landingTestimonialsTitle') }}</h2>
      <p>{{ $t('landingTestimonialsSubtitle') }}</p>
      <div class="reviews">
        <div class="user_review">
          <img class="user_img" src="/landing/img/ppl picks/3.png" alt="">
          <div class="user"><h3>Sarah K.</h3></div>
          <div class="user_comment"><p>{{ $t('landingTestimonial1') }}</p></div>
        </div>
        <div class="user_review">
          <img class="user_img" src="/landing/img/ppl picks/1.png" alt="">
          <div class="user"><h3>Éléonore M.</h3></div>
          <div class="user_comment"><p>{{ $t('landingTestimonial2') }}</p></div>
        </div>
        <div class="user_review">
          <img class="user_img" src="/landing/img/ppl picks/4.png" alt="">
          <div class="user"><h3>Omar B.</h3></div>
          <div class="user_comment"><p>{{ $t('landingTestimonial3') }}</p></div>
        </div>
      </div>
    </section>

    <section class="join-us">
      <div class="container">
        <h1>{{ $t('landingCtaTitle') }}</h1>
        <p>{{ $t('landingCtaSubtitle') }}</p>
        <div class="hero-actions">
          <router-link class="hero-btn tertiary" to="/createPackage">{{ $t('menuNewPackage') }}</router-link>
          <a class="hero-btn" href="#" @click.prevent="startLogin">{{ $t('landingLoginAction') }}</a>
          <router-link class="hero-btn secondary" to="/register">{{ $t('landingRegisterAction') }}</router-link>
        </div>
      </div>
    </section>

    <footer class="footer">
      <p>{{ $t('landingFooter') }}</p>
    </footer>
  </div>
</template>

<script>
import { redirectToLogin } from '@/config/auth';

export default {
  data() {
    return {
      isScrolled: false,
    };
  },
  mounted() {
    window.addEventListener('scroll', this.handleScroll, { passive: true });
    this.handleScroll();
  },
  beforeUnmount() {
    window.removeEventListener('scroll', this.handleScroll);
  },
  methods: {
    handleScroll() {
      this.isScrolled = window.scrollY > 120;
    },
    async startLogin() {
      await redirectToLogin();
    },
  },
  computed: {
    bannerStyle() {
      return {
        backgroundImage: "url('/landing/img/6169192.jpg')",
      };
    },
  },
};
</script>

<style scoped>
.landing-page {
  min-height: 100vh;
  background: #f1f1f1;
  color: #111111;
}

.container {
  width: min(1200px, calc(100% - 32px));
  margin: 0 auto;
}

.navbar {
  position: fixed;
  inset: 0 0 auto 0;
  width: 100%;
  padding: 2rem 0;
  z-index: 10;
  transition: all 0.2s;
}

.navbar.scrolled {
  background-color: var(--qd-primary-dark);
  padding: 1rem 0;
}

.nav-content {
  display: flex;
  justify-content: space-between;
  align-items: center;
  gap: 18px;
  width: min(1200px, calc(100% - 32px));
  margin: 0 auto;
}

.logo a,
.navbar a {
  color: #e3e3e3;
  text-decoration: none;
}

.logo {
  font-size: 1.5rem;
  font-weight: 600;
}

.navbar ul {
  display: flex;
  align-items: center;
  gap: 18px;
  list-style: none;
  margin: 0;
  padding: 0;
}

.navbar ul a {
  padding-bottom: 2px;
  border-bottom: 2px solid transparent;
  transition: var(--qd-transition);
}

.navbar ul a:hover,
.router-link-active {
  color: #fff;
}

.navbar ul a:hover {
  border-color: #fff;
}

.banner {
  min-height: 100vh;
  background-size: cover;
  background-position: center;
  background-repeat: no-repeat;
  color: #f1f1f1;
}

.banner-content {
  min-height: 100vh;
  display: flex;
  align-items: center;
  background: linear-gradient(rgba(15, 23, 42, 0.4), rgba(15, 23, 42, 0.7));
}

.hero-copy {
  text-align: center;
}

.hero-copy h1 {
  margin: 0 0 1rem;
  font-size: clamp(2.5rem, 5vw, 4rem);
}

.hero-copy p,
.statistics_head p,
.services p,
.how-subtitle,
.users_feedback > p,
.join-us p {
  font-size: 1rem;
  line-height: 1.7;
}

.hero-actions {
  display: flex;
  justify-content: center;
  gap: 12px;
  flex-wrap: wrap;
  margin-top: 24px;
}

.hero-btn {
  display: inline-flex;
  align-items: center;
  justify-content: center;
  min-height: 48px;
  padding: 0 22px;
  border-radius: 8px;
  background: var(--qd-accent);
  color: #fff;
  text-decoration: none;
  font-weight: 600;
  transition: var(--qd-transition);
  position: relative;
  overflow: hidden;
  transform: translateZ(0);
}

.hero-btn::after {
  content: "";
  position: absolute;
  top: 0;
  left: -100%;
  width: 100%;
  height: 100%;
  background: linear-gradient(90deg, transparent, rgba(255, 255, 255, 0.15), transparent);
  transition: none;
}

.hero-btn:hover::after {
  left: 100%;
  transition: 0.7s;
}

.hero-btn.secondary {
  background: transparent;
  border: 1px solid rgba(255, 255, 255, 0.7);
}

.hero-btn.tertiary {
  background: rgba(255, 255, 255, 0.12);
  border: 1px solid rgba(255, 255, 255, 0.35);
}

.hero-btn:hover {
  transform: translateY(-3px) scale(1.03) translateZ(0);
  filter: brightness(1.05);
  box-shadow: 0 14px 28px rgba(0, 0, 0, 0.15), 0 10px 10px rgba(0, 0, 0, 0.1);
}

.hero-btn:active {
  transform: translateY(0) scale(0.96) translateZ(0);
  filter: brightness(0.95);
  box-shadow: 0 4px 6px rgba(0, 0, 0, 0.1);
}

.statistics_section,
.services,
.how-it-works,
.users_feedback,
.join-us {
  padding: 5rem 0;
  text-align: center;
}

.statistics {
  display: grid;
  grid-template-columns: repeat(4, minmax(0, 1fr));
  gap: 20px;
  margin-top: 2rem;
}

.statistic {
  text-align: center;
  padding: 12px;
}

.statistic img {
  margin-bottom: 20px;
}

.services {
  background: #ffffff;
}

.cards,
.reviews {
  display: grid;
  grid-template-columns: repeat(3, minmax(0, 1fr));
  gap: 24px;
  margin-top: 2rem;
}

.card,
.user_review {
  padding: 24px;
  border-radius: 18px;
  background: #fff;
  box-shadow: 0 12px 30px rgba(0, 0, 0, 0.08);
}

.card img {
  width: 72px;
  height: 72px;
}

.how-it-works {
  background: #f7f7f7;
}

.journeys {
  display: grid;
  grid-template-columns: repeat(2, minmax(0, 1fr));
  gap: 24px;
  margin-top: 2rem;
}

.journey-card {
  padding: 24px;
  border-radius: 20px;
  background: #fff;
  box-shadow: 0 12px 30px rgba(0, 0, 0, 0.08);
}


.journey-head {
  display: grid;
  grid-template-columns: 96px minmax(0, 1fr);
  gap: 18px;
  align-items: center;
  margin-bottom: 18px;
  text-align: left;
}

.journey-head h2 {
  margin: 0 0 8px;
}

.journey-head p {
  margin: 0;
}

.steps {
  display: grid;
  gap: 16px;
}

.journey-actions {
  display: flex;
  justify-content: center;
  margin-top: 20px;
}

.step {
  padding: 20px;
  border-radius: 16px;
  background: #f7f7f7;
  text-align: left;
}

.icon img {
  width: 80px;
  height: 80px;
  object-fit: contain;
}

.user_img {
  width: 72px;
  height: 72px;
  border-radius: 50%;
  object-fit: cover;
  margin-bottom: 14px;
}

.join-us {
  background: var(--qd-primary-dark);
  color: #fff;
}

.footer {
  padding: 24px 16px;
  text-align: center;
  background: #111111;
  color: #f1f1f1;
}

@media screen and (max-width: 960px) {
  .statistics,
  .cards,
  .reviews {
    grid-template-columns: repeat(2, minmax(0, 1fr));
  }

  .journeys {
    grid-template-columns: 1fr;
  }
}

@media screen and (max-width: 760px) {
  .navbar {
    position: static;
    padding: 1rem 0;
    background: var(--qd-primary-dark);
  }

  .nav-content,
  .navbar ul {
    flex-direction: column;
  }

  .banner,
  .banner-content {
    min-height: auto;
  }

  .banner-content {
    padding: 48px 0;
  }

  .hero-copy h1 {
    font-size: clamp(1.45rem, 7vw, 1.6rem);
    line-height: 1.12;
  }

  .hero-copy p,
  .statistics_head p,
  .services p,
  .how-subtitle,
  .users_feedback > p,
  .join-us p {
    font-size: 0.88rem;
    line-height: 1.35;
  }

  .statistics_section,
  .services,
  .how-it-works,
  .users_feedback,
  .join-us {
    padding: 2.25rem 0;
  }

  .statistics,
  .cards,
  .reviews,
  .journeys {
    margin-top: 1rem;
    gap: 12px;
  }

  .statistics_head h1,
  .services h1,
  .how-it-works h1,
  .join-us h1 {
    font-size: 1.45rem;
    line-height: 1.16;
  }

  .card,
  .user_review,
  .journey-card,
  .step {
    padding: 14px;
    border-radius: 12px;
  }

  .step {
    text-align: left;
  }

  .journey-head {
    gap: 10px;
    margin-bottom: 12px;
  }

  .hero-actions {
    gap: 8px;
    margin-top: 16px;
  }

  .hero-btn {
    min-height: 40px;
    padding: 0 14px;
    font-size: 0.86rem;
  }

  .statistics,
  .cards,
  .reviews {
    grid-template-columns: 1fr;
  }

  .journey-head,
  .step {
    text-align: center;
  }

  .journey-head {
    grid-template-columns: 1fr;
  }
}
</style>
