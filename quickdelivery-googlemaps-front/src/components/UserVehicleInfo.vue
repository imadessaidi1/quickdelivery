<template>
  <div class="vehicle-step">
    <section class="section-head">
      <span class="eyebrow">{{ $t('wizardUserStepVehicle') }}</span>
      <h3>{{ $t('userRegistrationVehicleTitle') }}</h3>
      <p>{{ $t('userRegistrationVehicleSubtitle') }}</p>
    </section>

    <div v-if="!requiresVehicleSection" class="empty-panel">
      {{ $t('userRegistrationVehicleOptional') }}
    </div>

    <template v-else>
      <div class="vehicle-grid">
        <div class="field-wrap full-width">
          <label>{{ $t('userVehicleType') }}</label>
          <div class="type-selector-grid">
            <label v-for="type in vehicleTypes" :key="type.value" class="type-card" :class="{ active: vehicle.type === type.value }">
              <input type="radio" :value="type.value" v-model="vehicle.type" class="hidden-radio" />
              <div class="card-inner">
                <span class="material-symbols-outlined icon">{{ type.icon }}</span>
                <span class="type-label">{{ $t(type.label) }}</span>
                <div class="tooltip-container">
                   <span class="material-symbols-outlined info-icon">info</span>
                   <div class="tooltip-box">
                     {{ $t(type.hint) }}
                   </div>
                </div>
              </div>
            </label>
          </div>
        </div>
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
          <small v-if="documentStatus(document.key)">{{ documentStatus(document.key) }}</small>
          <div v-if="documentReview(document.key)" class="review-note">
            <span class="review-label">{{ $t('userDocumentReviewCommentLabel') }}</span>
            <p>{{ documentReview(document.key) }}</p>
          </div>
          <span v-if="filesErrorMessages[document.key]" class="errorMessage">{{ filesErrorMessages[document.key] }}</span>
        </label>
      </div>
    </template>
  </div>
</template>

<script>
import { ErrorMessage, Field } from 'vee-validate';
import { validateCarRegistrationNumber, validateRequired } from '@/config/comonFunction';
import { getRequiredVehicleDocuments, requiresVehicleDetails } from '@/config/deliveryMode';

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
    requiresVehicleSection() {
      return requiresVehicleDetails(this.user);
    },
    visibleVehicleDocuments() {
      const definitionByKey = {
        GRAY_CARD: { key: 'GRAY_CARD', label: 'userVehicleGryCard', ref: 'fileInputGRAY_CARD' },
        INSURANCE: { key: 'INSURANCE', label: 'userVehicleInsurance', ref: 'fileInputINSURANCE' },
      };
      const documents = getRequiredVehicleDocuments(this.user).map((key) => definitionByKey[key]).filter(Boolean);
      if (!this.canUpdateRejectedOnly) {
        return documents;
      }
      return documents.filter((document) => ['REJECTED', 'UPDATED'].includes(this.vehicleDocuments?.[document.key]?.documentStatus));
    },
    vehicleTypes() {
      return [
        { value: 'CAR', label: 'userVehicleTypeCar', icon: 'directions_car', hint: 'userVehicleTypeCarHint' },
        { value: 'VAN', label: 'userVehicleTypeVan', icon: 'local_shipping', hint: 'userVehicleTypeVanHint' },
        { value: 'SMALL_TRUCK', label: 'userVehicleTypeSmallTruck', icon: 'front_loader', hint: 'userVehicleTypeSmallTruckHint' },
        { value: 'LARGE_VAN', label: 'userVehicleTypeLargeVan', icon: 'airport_shuttle', hint: 'userVehicleTypeLargeVanHint' },
        { value: 'SEMI_TRAILER', label: 'userVehicleTypeSemiTrailer', icon: 'rv_hookup', hint: 'userVehicleTypeSemiTrailerHint' },
      ];
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
      this.$store.commit('updateVehicleDocuments', {
        ...this.vehicleDocuments,
        [type]: {
          ...this.vehicleDocuments?.[type],
          file,
          name: file.name,
          documentStatus: this.isForUpdate ? 'UPDATED' : 'ACCEPTED',
        },
      });
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
    documentStatus(type) {
      const status = this.vehicleDocuments?.[type]?.documentStatus;
      if (!status || status === 'UPDATED') {
        return '';
      }
      return this.$t(status);
    },
    documentReview(type) {
      const document = this.vehicleDocuments?.[type];
      if (!document?.reviewComment) {
        return '';
      }
      return document.reviewComment;
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

.full-width {
  grid-column: 1 / -1;
}

.type-selector-grid {
  display: grid;
  grid-template-columns: repeat(5, 1fr);
  gap: 12px;
  margin-top: 10px;
}

.type-card {
  position: relative;
  cursor: pointer;
  transition: all 0.2s cubic-bezier(0.4, 0, 0.2, 1);
}

.hidden-radio {
  position: absolute;
  opacity: 0;
  pointer-events: none;
}

.card-inner {
  display: flex;
  flex-direction: column;
  align-items: center;
  justify-content: center;
  padding: 16px 8px;
  border: 2px solid #edeff5;
  border-radius: 16px;
  background: #fcfdfe;
  text-align: center;
  gap: 8px;
  min-height: 110px;
}

.type-card.active .card-inner {
  border-color: #27548a;
  background: #f0f7ff;
  box-shadow: 0 4px 12px rgba(39, 84, 138, 0.15);
}

.type-card:hover .card-inner {
  border-color: #cbd5e1;
  transform: translateY(-2px);
}

.type-card.active:hover .card-inner {
  border-color: #27548a;
}

.icon {
  font-size: 2.4rem;
  color: #64748b;
  transition: color 0.2s ease;
}

.active .icon {
  color: #27548a;
}

.type-label {
  font-size: 0.85rem;
  font-weight: 700;
  color: #1e293b;
}

.tooltip-container {
  position: absolute;
  top: 8px;
  right: 8px;
  color: #94a3b8;
}

.info-icon {
  font-size: 16px;
  cursor: help;
}

.tooltip-box {
  position: absolute;
  bottom: 125%;
  left: 50%;
  transform: translateX(-50%) translateY(10px);
  width: 180px;
  background: #1e293b;
  color: #fff;
  padding: 10px;
  border-radius: 8px;
  font-size: 0.75rem;
  font-weight: 400;
  line-height: 1.4;
  opacity: 0;
  visibility: hidden;
  transition: all 0.2s ease;
  z-index: 10;
  box-shadow: 0 10px 15px -3px rgba(0, 0, 0, 0.1);
  pointer-events: none;
}

.tooltip-box::after {
  content: '';
  position: absolute;
  top: 100%;
  left: 50%;
  transform: translateX(-50%);
  border-width: 6px;
  border-style: solid;
  border-color: #1e293b transparent transparent transparent;
}

.type-card:hover .tooltip-box {
  opacity: 1;
  visibility: visible;
  transform: translateX(-50%) translateY(0);
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

.upload-card small {
  color: #617086;
}

.review-note {
  padding: 12px;
  border-radius: 14px;
  background: #fff1f2;
  border: 1px solid #fecdd3;
}

.review-label {
  display: block;
  margin-bottom: 4px;
  color: #9f1239;
  font-size: 0.75rem;
  font-weight: 700;
  text-transform: uppercase;
}

.review-note p {
  margin: 0;
  color: #7f1d1d;
  font-size: 0.88rem;
  line-height: 1.45;
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
  
  .type-selector-grid {
    grid-template-columns: repeat(3, 1fr);
  }
}

@media screen and (max-width: 640px) {
  .type-selector-grid {
    grid-template-columns: repeat(2, 1fr);
  }
}
</style>
