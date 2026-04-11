<template>
  <div class="package-creation">
    <section class="section-head">
      <div>
        <span class="eyebrow">{{ $t('packageSizeSelectorTitle') }}</span>
        <h3>{{ $t('createPackageInfoTitle') }}</h3>
        <p>{{ $t('packageSizeSelectorSubtitle') }}</p>
      </div>
    </section>

    <div class="preset-grid">
      <button
        v-for="preset in presets"
        :key="preset.key"
        class="preset-card"
        :class="{ active: selectedPreset === preset.key }"
        type="button"
        @click="applyPreset(preset)"
      >
        <strong>{{ $t(preset.label) }}</strong>
        <span>{{ $t(preset.hint) }}</span>
        <small>{{ preset.dimensions }}</small>
      </button>
    </div>

    <div class="measure-grid">
      <div class="field-wrap">
        <label for="height">{{ $t('packageHeight') }}</label>
        <Field id="height" v-model="package_.height" type="number" name="package_.height" :rules="validateNumericField" min="1" max="300" step="1" @change="clampField('height', 1, 300)" />
        <ErrorMessage class="errorMessage" name="package_.height" />
      </div>

      <div class="field-wrap">
        <label for="width">{{ $t('packageWidth') }}</label>
        <Field id="width" v-model="package_.width" type="number" name="package_.width" :rules="validateNumericField" min="1" max="300" step="1" @change="clampField('width', 1, 300)" />
        <ErrorMessage class="errorMessage" name="package_.width" />
      </div>

      <div class="field-wrap">
        <label for="depth">{{ $t('packageDepth') }}</label>
        <Field id="depth" v-model="package_.depth" type="number" name="package_.depth" :rules="validateNumericField" min="1" max="300" step="1" @change="clampField('depth', 1, 300)" />
        <ErrorMessage class="errorMessage" name="package_.depth" />
      </div>

      <div class="field-wrap">
        <label for="weight">{{ $t('packageWeight') }}</label>
        <Field id="weight" v-model="package_.weight" type="number" name="package_.weight" :rules="validateNumericField" min="0.1" max="100" step="0.1" @change="clampField('weight', 0.1, 100)" />
        <ErrorMessage class="errorMessage" name="package_.weight" />
      </div>
    </div>
  </div>
</template>

<script>
import { ErrorMessage, Field } from 'vee-validate';
import { validateNumericField } from '@/config/comonFunction';

const PRESETS = [
  { key: 'SMALL', label: 'packageSizeSmall', hint: 'packageSizeSmallHint', height: 20, width: 20, depth: 20, weight: 2, dimensions: '20 × 20 × 20 cm' },
  { key: 'MEDIUM', label: 'packageSizeMedium', hint: 'packageSizeMediumHint', height: 40, width: 30, depth: 30, weight: 10, dimensions: '40 × 30 × 30 cm' },
  { key: 'LARGE', label: 'packageSizeLarge', hint: 'packageSizeLargeHint', height: 60, width: 40, depth: 40, weight: 25, dimensions: '60 × 40 × 40 cm' },
  { key: 'EXTRA_LARGE', label: 'packageSizeExtraLarge', hint: 'packageSizeExtraLargeHint', height: 80, width: 60, depth: 60, weight: 50, dimensions: '80 × 60 × 60 cm' },
];

export default {
  components: {
    Field,
    ErrorMessage,
  },
  props: {
    selectedPreset: {
      type: String,
      default: 'MEDIUM',
    },
  },
  emits: ['update:selectedPreset'],
  computed: {
    package_() {
      return this.$store.state.package_;
    },
    presets() {
      return PRESETS;
    },
  },
  mounted() {
    const preset = PRESETS.find((entry) => entry.key === this.selectedPreset) || PRESETS[1];
    if (!this.package_.height && !this.package_.width && !this.package_.depth && !this.package_.weight) {
      this.applyPreset(preset);
      return;
    }
    if (!this.package_.packageSizeCategory) {
      this.package_.packageSizeCategory = preset.key;
    }
  },
  methods: {
    validateNumericField,
    clampField(fieldName, min, max) {
      const parsedValue = Number(this.package_[fieldName]);
      if (!Number.isFinite(parsedValue)) {
        return;
      }
      this.package_[fieldName] = Math.min(max, Math.max(min, parsedValue));
    },
    applyPreset(preset) {
      this.package_.height = preset.height;
      this.package_.width = preset.width;
      this.package_.depth = preset.depth;
      this.package_.weight = preset.weight;
      this.package_.packageSizeCategory = preset.key;
      this.$emit('update:selectedPreset', preset.key);
    },
  },
};
</script>

<style scoped>
.package-creation {
  display: grid;
  gap: 24px;
  min-width: 0;
}

.package-creation,
.package-creation * {
  box-sizing: border-box;
}

.section-head h3 {
  margin: 6px 0 8px;
  font-size: 1.45rem;
  color: #14213d;
}

.section-head p {
  margin: 0;
  color: #617086;
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
  letter-spacing: 0.04em;
}

.preset-grid {
  display: grid;
  grid-template-columns: repeat(4, minmax(0, 1fr));
  gap: 14px;
}

.preset-card {
  display: grid;
  gap: 6px;
  padding: 16px;
  border: 1px solid #d7dfeb;
  border-radius: 18px;
  background: #fff;
  text-align: left;
  cursor: pointer;
  transition: border-color 0.2s ease, box-shadow 0.2s ease, transform 0.2s ease;
}

.preset-card strong {
  font-size: 1rem;
  color: #14213d;
}

.preset-card span,
.preset-card small {
  color: #617086;
}

.preset-card.active {
  border-color: #1f4f89;
  box-shadow: 0 14px 30px rgba(23, 48, 87, 0.12);
  transform: translateY(-2px);
}

.measure-grid {
  display: grid;
  grid-template-columns: repeat(2, minmax(0, 1fr));
  gap: 18px;
}

.field-wrap {
  min-width: 0;
  display: flex;
  flex-direction: column;
}

.field-wrap label {
  display: block;
  margin-bottom: 8px;
  font-weight: 600;
  color: #24364f;
}

.field-wrap :deep(input) {
  width: 100%;
  min-height: 48px;
  padding: 0 14px;
  border: 1px solid #ced7e4;
  border-radius: 14px;
  background: #fff;
  box-sizing: border-box;
}

.errorMessage {
  display: block;
  margin-top: 6px;
  font-size: 0.78rem;
  color: #b42318;
}

@media screen and (max-width: 980px) {
  .preset-grid,
  .measure-grid {
    grid-template-columns: repeat(2, minmax(0, 1fr));
  }
}

@media screen and (max-width: 640px) {
  .preset-grid,
  .measure-grid {
    grid-template-columns: 1fr;
  }
}
</style>
