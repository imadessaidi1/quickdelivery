<template>
  <div class="vehicle-step">
    <section class="section-head">
      <span class="eyebrow">{{ $t('wizardUserStepVehicle') }}</span>
      <h3>{{ $t('userRegistrationVehicleTitle') }}</h3>
      <p>{{ $t('userRegistrationVehicleSubtitle') }}</p>
    </section>

    <div v-if="user.type !== 'DELIVERY_PERSON'" class="empty-panel">
      {{ $t('userRegistrationVehicleOptional') }}
    </div>

    <template v-else>
      <div class="vehicle-grid">
        <div class="field-wrap">
          <label for="registrationNumber">{{ $t('userVehicleRegistration') }}</label>
          <Field id="registrationNumber" v-model="vehicle.registrationNumber" type="text" name="registrationNumber" :rules="validateCarRegistrationNumber" @input="normalizeRegistrationNumber" />
          <ErrorMessage class="errorMessage" name="registrationNumber" />
        </div>

        <div class="field-wrap">
          <label for="brand">{{ $t('userVehicleBrand') }}</label>
          <Field id="brand" v-model="vehicle.brand" type="text" name="brand" :rules="validateRequired" />
          <ErrorMessage class="errorMessage" name="brand" />
        </div>

        <div class="field-wrap">
          <label for="model">{{ $t('userVehicleModel') }}</label>
          <Field id="model" v-model="vehicle.model" type="text" name="model" :rules="validateRequired" />
          <ErrorMessage class="errorMessage" name="model" />
        </div>

        <div class="field-wrap">
          <label for="energyType">{{ $t('userVehicleEnergy') }}</label>
          <select id="energyType" v-model="vehicle.energyType">
            <option value="ELECTRIC">{{ $t('ELECTRIC') }}</option>
            <option value="HYBRID">{{ $t('HYBRID') }}</option>
            <option value="METHANE">{{ $t('METHANE') }}</option>
            <option value="ETHANOL">{{ $t('ETHANOL') }}</option>
            <option value="GASOLINE">{{ $t('GASOLINE') }}</option>
            <option value="DIESEL">{{ $t('DIESEL') }}</option>
          </select>
        </div>
      </div>

      <div class="upload-grid">
        <label v-for="document in visibleVehicleDocuments" :key="document.key" class="upload-card">
          <span>{{ $t(document.label) }}</span>
          <input :ref="document.ref" type="file" accept="image/*, application/pdf" @change="handleVehicleFileChange(document.ref, document.key)">
          <strong>{{ fileName(document.key) }}</strong>
          <span v-if="filesErrorMessages[document.key]" class="errorMessage">{{ filesErrorMessages[document.key] }}</span>
        </label>
      </div>
    </template>
  </div>
</template>

<script>
import { ErrorMessage, Field } from 'vee-validate';
import { validateCarRegistrationNumber, validateRequired } from '@/config/comonFunction';

export default {
  components: {
    Field,
    ErrorMessage,
  },
  props: {
    isForUpdate: {
      type: Boolean,
      default: false,
    },
  },
  computed: {
    user() {
      return this.$store.state.user;
    },
    vehicle() {
      return this.$store.state.vehicle;
    },
    vehicleDocuments() {
      return this.$store.state.vehicleDocuments;
    },
    canUpdateRejectedOnly() {
      return this.isForUpdate && this.user?.activeAccount !== true;
    },
    visibleVehicleDocuments() {
      const documents = [
        { key: 'GRAY_CARD', label: 'userVehicleGryCard', ref: 'fileInputGRAY_CARD' },
        { key: 'INSURANCE', label: 'userVehicleInsurance', ref: 'fileInputINSURANCE' },
      ];
      if (!this.canUpdateRejectedOnly) {
        return documents;
      }
      return documents.filter((document) => ['REJECTED', 'UPDATED'].includes(this.vehicleDocuments?.[document.key]?.documentStatus));
    },
  },
  data() {
    return {
      filesErrorMessages: [],
    };
  },
  methods: {
    validateCarRegistrationNumber,
    validateRequired,
    handleVehicleFileChange(refName, type) {
      const file = this.$refs[refName]?.[0]?.files?.[0] || this.$refs[refName]?.files?.[0];
      if (!file) {
        return;
      }
      this.vehicleDocuments[type] = {
        file,
        name: file.name,
        documentStatus: this.isForUpdate ? 'UPDATED' : 'ACCEPTED',
      };
      delete this.filesErrorMessages[type];
    },
    normalizeRegistrationNumber(event) {
      const rawValue = event?.target?.value ?? this.vehicle.registrationNumber ?? '';
      this.vehicle.registrationNumber = rawValue.toUpperCase();
    },
    fileName(type) {
      const storedName = this.vehicleDocuments?.[type]?.name;
      if (storedName && storedName !== type) {
        return storedName;
      }
      if (this.vehicleDocuments?.[type]) {
        return this.$t(type);
      }
      return this.$t('packageDocumentMissing');
    },
  },
  watch: {
    vehicleDocuments: {
      deep: true,
      handler(documents) {
        Object.keys(this.filesErrorMessages).forEach((key) => {
          if (documents?.[key]?.file) {
            delete this.filesErrorMessages[key];
          }
        });
      },
    },
  },
};
</script>

<style scoped>
.vehicle-step {
  display: grid;
  gap: 22px;
  min-width: 0;
}

.vehicle-step,
.vehicle-step * {
  box-sizing: border-box;
}

.section-head h3 {
  margin: 6px 0 8px;
  font-size: 1.4rem;
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
}

.vehicle-grid,
.upload-grid {
  display: grid;
  grid-template-columns: repeat(2, minmax(0, 1fr));
  gap: 18px;
}

.field-wrap,
.upload-card,
.empty-panel {
  padding: 18px;
  border: 1px solid #dde5f0;
  border-radius: 20px;
  background: #fff;
  box-shadow: 0 18px 38px rgba(24, 39, 75, 0.07);
}

.field-wrap label {
  display: block;
  margin-bottom: 8px;
  font-weight: 600;
  color: #24364f;
}

.field-wrap :deep(input),
.field-wrap select {
  width: 100%;
  max-width: 100%;
  min-height: 48px;
  padding: 0 14px;
  border: 1px solid #ced7e4;
  border-radius: 14px;
  background: #fff;
  box-sizing: border-box;
}

.upload-card {
  display: grid;
  gap: 10px;
  min-width: 0;
}

.upload-card > span {
  color: #14213d;
  font-weight: 700;
}

.upload-card strong {
  color: #2b5a96;
  word-break: break-word;
}

.empty-panel {
  color: #617086;
}

.upload-card .errorMessage,
.errorMessage {
  display: block;
  font-size: 0.78rem;
  color: #b42318;
  word-break: break-word;
}

@media screen and (max-width: 1080px) {
  .vehicle-grid,
  .upload-grid {
    grid-template-columns: 1fr;
  }
}
</style>
