<template>
  <div class="user-address-step">
    <aside class="preview-card">
      <span class="preview-chip">{{ $t('wizardUserStepAddress') }}</span>
      <h3>{{ $t('userRegistrationAddressTitle') }}</h3>
      <p>{{ $t('userRegistrationAddressSubtitle') }}</p>

      <div class="preview-box">
        <span class="material-symbols-outlined">home_pin</span>
        <strong>{{ user.addressAuto || $t('userRegistrationAddressEmpty') }}</strong>
      </div>

      <div class="preview-box">
        <span class="material-symbols-outlined">person</span>
        <strong>{{ fullName }}</strong>
      </div>
    </aside>

    <div class="form-card">
      <div class="section-head">
        <span class="eyebrow">{{ $t('packageAddressAddress') }}</span>
        <h3>{{ $t('userRegistrationAddressSearchTitle') }}</h3>
      </div>

      <div class="field-wrap">
        <label for="address">{{ $t('packageAddressAddress') }}</label>
        <AddressAutocomplete ref="addressAutoComplete" v-model="user.addressAuto" :existingAddress="user.addressAuto" />
        <span v-if="isAddressError" class="errorMessage">{{ errorAddressMessage }}</span>
      </div>
    </div>
  </div>
</template>

<script>
import AddressAutocomplete from './AddressAutocomplete.vue';

export default {
  components: {
    AddressAutocomplete,
  },
  data() {
    return {
      isAddressError: false,
      errorAddressMessage: '',
    };
  },
  computed: {
    user() {
      return this.$store.state.user;
    },
    fullName() {
      return [this.user.firstName, this.user.lastName].filter(Boolean).join(' ') || '--';
    },
  },
};
</script>

<style scoped>
.user-address-step {
  display: grid;
  grid-template-columns: 300px minmax(0, 1fr);
  gap: 24px;
}

.user-address-step,
.user-address-step * {
  box-sizing: border-box;
}

.preview-card,
.form-card {
  border: 1px solid #dde5f0;
  border-radius: 22px;
  background: #fff;
  box-shadow: 0 18px 38px rgba(24, 39, 75, 0.07);
}

.preview-card {
  display: grid;
  align-content: start;
  gap: 14px;
  padding: 22px;
  background: linear-gradient(180deg, #edf4ff 0%, #ffffff 78%);
}

.preview-chip,
.eyebrow {
  display: inline-flex;
  width: fit-content;
  align-items: center;
  padding: 5px 10px;
  border-radius: 999px;
  background: #dceaff;
  color: #214f88;
  font-size: 0.76rem;
  font-weight: 700;
  text-transform: uppercase;
}

.preview-card h3,
.section-head h3 {
  margin: 0;
  color: #14213d;
}

.preview-card p {
  margin: 0;
  color: #617086;
}

.preview-box {
  display: grid;
  grid-template-columns: 22px minmax(0, 1fr);
  gap: 10px;
  align-items: start;
  padding: 14px;
  border-radius: 16px;
  background: rgba(255, 255, 255, 0.82);
  color: #24364f;
}

.preview-box .material-symbols-outlined {
  color: #2a6fcf;
}

.form-card {
  padding: 24px;
}

.section-head {
  margin-bottom: 18px;
}

.field-wrap label {
  display: block;
  margin-bottom: 8px;
  font-weight: 600;
  color: #24364f;
}

.field-wrap {
  min-width: 0;
}

.field-wrap :deep(input) {
  width: 100%;
  max-width: 100%;
  box-sizing: border-box;
}

.errorMessage {
  display: block;
  margin-top: 6px;
  font-size: 0.78rem;
  color: #b42318;
  word-break: break-word;
}

@media screen and (max-width: 980px) {
  .user-address-step {
    grid-template-columns: 1fr;
  }
}
</style>
