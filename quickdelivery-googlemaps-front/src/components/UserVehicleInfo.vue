<template>
  <div class="vehicle-step">
    <section class="section-head">
      <span class="badge info uppercase">{{ $t('wizardUserStepVehicle') }}</span>
      <h3>{{ $t('userRegistrationVehicleTitle') }}</h3>
      <p>{{ $t('userRegistrationVehicleSubtitle') }}</p>
    </section>

    <div v-if="!requiresVehicleSection" class="panel-card empty-panel">
      <span class="material-symbols-outlined">info</span>
      {{ $t('userRegistrationVehicleOptional') }}
    </div>

    <template v-else>
      <div class="vehicle-grid">
        <div class="full-width">
          <label class="field-label">{{ $t('userVehicleType') }}</label>
          <div class="type-cards-container">
            <label v-for="type in vehicleTypes" :key="type.value" class="vehicle-type-card" :class="{ active: vehicle.type === type.value }">
              <input type="radio" :value="type.value" v-model="vehicle.type" class="hidden-radio" />
              <div class="card-inner" :class="{ 'tone-indigo': vehicle.type === type.value }">
                <span class="material-symbols-outlined type-icon">{{ type.icon }}</span>
                <span class="type-name">{{ $t(type.label) }}</span>
                
                <div class="tooltip-trigger">
                  <span class="material-symbols-outlined info-badge">info</span>
                  <div class="glass-tooltip glass-pane">
                    {{ $t(type.hint) }}
                  </div>
                </div>
              </div>
            </label>
          </div>
        </div>

        <div class="field-group">
          <label for="registrationNumber">{{ $t('userVehicleRegistration') }}</label>
          <Field id="registrationNumber" v-model="vehicle.registrationNumber" type="text" name="registrationNumber" :rules="validateCarRegistrationNumber" @input="normalizeRegistrationNumber" />
          <ErrorMessage class="errorMessage" name="registrationNumber" />
        </div>

        <div class="field-group">
          <label for="brand">{{ $t('userVehicleBrand') }}</label>
          <Field id="brand" v-model="vehicle.brand" type="text" name="brand" :rules="validateRequired" />
          <ErrorMessage class="errorMessage" name="brand" />
        </div>

        <div class="field-group">
          <label for="model">{{ $t('userVehicleModel') }}</label>
          <Field id="model" v-model="vehicle.model" type="text" name="model" :rules="validateRequired" />
          <ErrorMessage class="errorMessage" name="model" />
        </div>

        <div class="field-group">
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

      <div class="documents-grid">
        <DocumentUploadCard
          v-for="document in visibleVehicleDocuments"
          :key="document.key"
          :label="$t(document.label)"
          :status="vehicleDocuments?.[document.key]?.documentStatus"
          :file-name="fileName(document.key)"
          :review-comment="documentReview(document.key)"
          :error-message="filesErrorMessages[document.key]"
          @change="handleVehicleFileChange($event, document.key)"
        />
      </div>
    </template>
  </div>
</template>

<script>
import { ErrorMessage, Field } from 'vee-validate';
import { validateCarRegistrationNumber, validateRequired } from '@/config/comonFunction';
import { getRequiredVehicleDocuments, requiresVehicleDetails } from '@/config/deliveryMode';
import DocumentUploadCard from './DocumentUploadCard.vue';

export default {
  components: {
    Field,
    ErrorMessage,
    DocumentUploadCard,
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
    isResumeOnboarding() {
      const status = this.user?.onboarding?.status;
      return ['ACCOUNT_CREATED', 'PROFILE_COMPLETED', 'DOCUMENTS_UPLOADED', 'FAILED', 'READY_FOR_VALIDATION'].includes(status);
    },
    canUpdateRejectedOnly() {
      return this.isForUpdate && this.user?.activeAccount !== true && !this.isResumeOnboarding;
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
    handleVehicleFileChange(event, type) {
      const file = event?.target?.files?.[0];
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
    getStatusToneClass(type) {
      const status = this.vehicleDocuments?.[type]?.documentStatus;
      if (status === 'ACCEPTED') return 'tone-emerald';
      if (status === 'REJECTED') return 'tone-rose';
      return 'tone-indigo';
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
  display: flex;
  flex-direction: column;
  gap: 32px;
}

.section-head h3 {
  font-size: 1.6rem;
  font-weight: 800;
  margin: 12px 0 8px;
  color: var(--qd-text);
}

.section-head p {
  color: var(--qd-muted);
}

.empty-panel {
  display: flex;
  align-items: center;
  justify-content: center;
  gap: 12px;
  padding: 40px;
  color: var(--qd-muted);
  font-weight: 600;
}

.vehicle-grid {
  display: grid;
  grid-template-columns: 1fr 1fr;
  gap: 24px;
}

.full-width {
  grid-column: 1 / -1;
}

.field-label {
  display: block;
  font-weight: 700;
  color: var(--qd-text);
  margin-bottom: 12px;
}

.type-cards-container {
  display: grid;
  grid-template-columns: repeat(5, 1fr);
  gap: 16px;
}

.vehicle-type-card {
  position: relative;
  cursor: pointer;
  perspective: 1000px;
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
  padding: 20px 12px;
  background: var(--qd-surface-strong);
  border: 2px solid var(--qd-border);
  border-radius: var(--qd-radius);
  transition: var(--qd-transition);
  min-height: 124px;
  text-align: center;
  gap: 10px;
}

.vehicle-type-card:hover .card-inner {
  border-color: var(--qd-primary-soft);
  transform: translateY(-4px);
  background: var(--qd-bg);
}

.vehicle-type-card.active .card-inner {
  border-color: var(--qd-primary);
  background: var(--qd-primary-soft);
  box-shadow: var(--qd-shadow-soft);
}

.type-icon {
  font-size: 2.8rem;
  color: var(--qd-muted);
  transition: var(--qd-transition);
}

.vehicle-type-card.active .type-icon {
  color: var(--qd-primary);
}

.type-name {
  font-size: 0.85rem;
  font-weight: 800;
  color: var(--qd-text);
}

.tooltip-trigger {
  position: absolute;
  top: 10px;
  right: 10px;
}

.info-badge {
  font-size: 1.1rem;
  color: var(--qd-muted);
  cursor: help;
  opacity: 0.6;
}

.glass-tooltip {
  position: absolute;
  bottom: 130%;
  left: 50%;
  transform: translateX(-50%) translateY(10px);
  width: 200px;
  padding: 12px;
  border-radius: 12px;
  font-size: 0.8rem;
  text-align: center;
  opacity: 0;
  visibility: hidden;
  transition: var(--qd-transition);
  z-index: 20;
  pointer-events: none;
  font-weight: 500;
  color: var(--qd-text);
}

.glass-tooltip::after {
  content: "";
  position: absolute;
  top: 100%;
  left: 50%;
  transform: translateX(-50%);
  border-width: 6px;
  border-style: solid;
  border-color: rgba(255, 255, 255, 0.4) transparent transparent transparent;
}

.vehicle-type-card:hover .glass-tooltip {
  opacity: 1;
  visibility: visible;
  transform: translateX(-50%) translateY(0);
}

/* Fields */
.field-group {
  display: flex;
  flex-direction: column;
  gap: 8px;
}

.field-group label {
  font-weight: 700;
  color: var(--qd-text);
  font-size: 0.9rem;
}

.field-group :deep(input),
.field-group select {
  height: 52px;
  padding: 0 16px;
  border: 1px solid var(--qd-border-strong);
  border-radius: 14px;
  background: #fff;
  font-weight: 600;
  transition: var(--qd-transition);
}

.field-group :deep(input):focus {
  border-color: var(--qd-primary);
  outline: none;
  box-shadow: 0 0 0 4px var(--qd-primary-soft);
}

.errorMessage {
  font-size: 0.8rem;
  color: var(--qd-danger);
  font-weight: 600;
}

@media (max-width: 1024px) {
  .type-cards-container { grid-template-columns: repeat(3, 1fr); }
  .vehicle-grid, .documents-grid { grid-template-columns: 1fr; }
}

@media (max-width: 600px) {
  .type-cards-container { grid-template-columns: repeat(2, 1fr); }
}
</style>
