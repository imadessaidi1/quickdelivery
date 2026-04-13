<template>
    <div class="package-details-v3">
        <!-- Overview Section: Metadata Chips -->
        <div class="details-section overview-card">
            <div class="section-header">
                <span class="material-symbols-outlined icon-main">inventory_2</span>
                <h3>{{$t('createNewPackage')}}</h3>
            </div>
            
            <div class="metadata-grid">
                <div class="meta-chip">
                    <span class="material-symbols-outlined">height</span>
                    <div class="meta-info">
                        <label>{{$t('packageHeight')}}</label>
                        <span>{{package_.height}} cm</span>
                    </div>
                </div>
                <div class="meta-chip">
                    <span class="material-symbols-outlined">width</span>
                    <div class="meta-info">
                        <label>{{$t('packageWidth')}}</label>
                        <span>{{package_.width}} cm</span>
                    </div>
                </div>
                <div class="meta-chip">
                    <span class="material-symbols-outlined">straighten</span>
                    <div class="meta-info">
                        <label>{{$t('packageDepth')}}</label>
                        <span>{{package_.depth}} cm</span>
                    </div>
                </div>
                <div class="meta-chip">
                    <span class="material-symbols-outlined">weight</span>
                    <div class="meta-info">
                        <label>{{$t('packageWeight')}}</label>
                        <span>{{package_.weight}} kg</span>
                    </div>
                </div>
            </div>

            <div v-if="displayedPrice !== '-'" class="price-banner">
                <div class="price-content">
                    <span class="material-symbols-outlined">payments</span>
                    <span class="price-label">{{displayedPriceLabel}}</span>
                </div>
                <span class="price-value">{{displayedPrice}}</span>
            </div>

            <div v-if="package_.documentS && package_.documentS['PACKAGE_INVOICE']" class="invoice-box">
                <span class="material-symbols-outlined">description</span>
                <a @click="openDocumentPDFModal" class="invoice-link">
                    {{ $t(package_.documentS['PACKAGE_INVOICE'].fileName) }}
                </a>
            </div>

            <div v-if="packagePictureAvailable" class="photo-section">
                <button type="button" class="photo-preview-btn" @click="openDocumentIMGModal">
                    <img v-if="picturePreviewSrc" :src="picturePreviewSrc" :alt="$t('PACKAGE_PICTURE')" class="photo-image">
                    <div v-else-if="isPictureLoading" class="photo-loader">
                        <div class="spinner"></div>
                        <span>{{ $t('stateLoading') }}</span>
                    </div>
                    <div v-else class="photo-error">
                        <span class="material-symbols-outlined">broken_image</span>
                        <span>{{ $t('stateLoadError') }}</span>
                    </div>
                </button>
            </div>
        </div>

        <!-- Departure Section -->
        <div v-if="showDepartureAddress" class="details-section address-card departure">
            <div class="section-header">
                <span class="material-symbols-outlined icon-main">location_on</span>
                <h3>{{$t('packageAddressDepartureAddresses')}}</h3>
            </div>
            
            <div class="address-content">
                <div class="user-info-row">
                    <div class="user-main">
                        <span class="material-symbols-outlined">person</span>
                        <span class="user-name">{{departureAddress.firstName}} {{departureAddress.lastName}}</span>
                    </div>
                </div>
                
                <div class="contact-grid">
                    <div class="contact-item">
                        <span class="material-symbols-outlined">mail</span>
                        <span>{{departureAddress.email}}</span>
                    </div>
                    <div class="contact-item">
                        <span class="material-symbols-outlined">call</span>
                        <span>{{departureAddress.phone}}</span>
                    </div>
                </div>

                <div class="full-address-box">
                    <span class="material-symbols-outlined address-pin">map</span>
                    <div class="address-text">
                        <div class="main-address">{{departureAddress.addressAuto}}</div>
                        <div class="sub-address">
                            <span class="floor-tag">{{$t('packageAddressFloor',{ state: $t('packageAddressFloorStatePickup') })}}: {{departureAddress.floor}}</span>
                        </div>
                    </div>
                </div>

                <div v-if="departureAddress.dateTime" class="time-callout">
                    <span class="material-symbols-outlined">schedule</span>
                    <span>{{ formatDate(departureAddress.dateTime) }}</span>
                </div>
            </div>
        </div>

        <!-- Arrival Section -->
        <div v-if="showArrivalAddress" class="details-section address-card arrival">
            <div class="section-header">
                <span class="material-symbols-outlined icon-main">flag</span>
                <h3>{{$t('packageAddressArrivalAddresses')}}</h3>
            </div>

            <div class="address-content">
                <div class="user-info-row">
                    <div class="user-main">
                        <span class="material-symbols-outlined">person</span>
                        <span class="user-name">{{arrivalAddress.firstName}} {{arrivalAddress.lastName}}</span>
                    </div>
                </div>

                <div class="contact-grid">
                    <div class="contact-item">
                        <span class="material-symbols-outlined">mail</span>
                        <span>{{arrivalAddress.email}}</span>
                    </div>
                    <div class="contact-item">
                        <span class="material-symbols-outlined">call</span>
                        <span>{{arrivalAddress.phone}}</span>
                    </div>
                </div>

                <div class="full-address-box">
                    <span class="material-symbols-outlined address-pin">navigation</span>
                    <div class="address-text">
                        <div class="main-address">{{arrivalAddress.addressAuto}}</div>
                        <div class="sub-address">
                            <span class="floor-tag">{{$t('packageAddressFloor',{ state: $t('packageAddressFloorStateDelivery') })}}: {{arrivalAddress.floor}}</span>
                        </div>
                    </div>
                </div>
            </div>
        </div>
    </div>
</template>
<script>
import { getArrivalAddress, getDepartureAddress } from '@/config/comonFunction';
import { fetchProtectedBlob } from '@/config/binaryContent';
import { formatDisplayedPackageAmount, resolveDisplayedPackagePriceLabel } from '@/config/packagePricing';

export default {
    computed: {
        package_() {
          return this.$store.state.package_;
        },
        documentS() {
          return this.$store.state.documentS;
        },
        departureAddress() {
          return getDepartureAddress(this.package_?.addresses || []) || {};
        },
        arrivalAddress() {
          return getArrivalAddress(this.package_?.addresses || []) || {};
        },
        normalizedStatus() {
          return (this.package_?.status || '').toUpperCase();
        },
        showDepartureAddress() {
          return ['NEW', 'DELIVERED', 'RESERVED'].includes(this.normalizedStatus) || this.showAllAddressesForUnknownStatus;
        },
        showArrivalAddress() {
          return ['NEW', 'DELIVERED', 'PICKEDUP', 'INDELIVERY'].includes(this.normalizedStatus) || this.showAllAddressesForUnknownStatus;
        },
        showAllAddressesForUnknownStatus() {
          return !['NEW', 'DELIVERED', 'RESERVED', 'PICKEDUP', 'INDELIVERY'].includes(this.normalizedStatus);
        },
        packagePictureDocument() {
          return this.package_?.documentS?.PACKAGE_PICTURE || null;
        },
        packagePictureAvailable() {
          return Boolean(this.packagePictureDocument?.id);
        },
        displayedPrice() {
          return formatDisplayedPackageAmount(this.$i18n, this.package_);
        },
        displayedPriceLabel() {
          return resolveDisplayedPackagePriceLabel(this.$i18n);
        },
    },
    data() {
      return {
        name: 'my-pdf-file.pdf', //change which pdf file loads
        path: 'pdfjs-2.3.200-dist/web/viewer.html',
        picturePreviewSrc: '',
        currentPictureObjectUrl: '',
        isPictureLoading: false,
      };
    },
    watch: {
        'package_.reference': {
          immediate: true,
          handler() {
            this.loadPicturePreview();
          },
        },
    },
    beforeUnmount() {
      this.revokePicturePreview();
    },
    methods: {
        getDepartureAddress,
        getArrivalAddress,
        async loadPicturePreview() {
          if (!this.packagePictureAvailable) {
            this.revokePicturePreview();
            this.isPictureLoading = false;
            return;
          }
          this.isPictureLoading = true;
          try {
            const blob = await fetchProtectedBlob(
              `${this.$i18n.t('rootURL')}${this.$i18n.t('getPackageDocumentContent')}${encodeURIComponent(this.packagePictureDocument.id)}`
            );
            this.revokePicturePreview();
            this.currentPictureObjectUrl = URL.createObjectURL(blob);
            this.picturePreviewSrc = this.currentPictureObjectUrl;
          } catch (error) {
            this.revokePicturePreview();
            console.error('Unable to load package picture preview.', error);
          } finally {
            this.isPictureLoading = false;
          }
        },
        revokePicturePreview() {
          if (this.currentPictureObjectUrl) {
            URL.revokeObjectURL(this.currentPictureObjectUrl);
            this.currentPictureObjectUrl = '';
          }
          this.picturePreviewSrc = '';
        },
        openDocumentPDFModal(){
          this.$router.push({
            path: '/document',
            query: {
              reference: this.package_.reference,
              documentType: 'PACKAGE_INVOICE',
              returnTo: this.$route.fullPath,
            },
          });
        },
        openDocumentIMGModal(){
          this.$router.push({
            path: '/document',
            query: {
              reference: this.package_.reference,
              documentType: 'PACKAGE_PICTURE',
              returnTo: this.$route.fullPath,
            },
          });
        },
        formatDate(dateTime) {
            const date = new Date(dateTime);
            const options = {
                day: '2-digit',
                month: '2-digit',
                year: '2-digit',
                hour: '2-digit',
                minute: '2-digit',
            };
            const userLanguage = navigator.languages && navigator.languages.length ? navigator.languages[0] : navigator.language || 'fr-FR';
            return date.toLocaleDateString(userLanguage, options);
        },
    },
}
</script>
<style>
.package-details-v3 {
  display: grid;
  grid-template-columns: repeat(3, 1fr);
  gap: 20px;
  width: 100%;
}

.details-section {
  background: #ffffff;
  border-radius: 20px;
  padding: 24px;
  box-shadow: 0 4px 20px rgba(15, 23, 42, 0.04);
  display: flex;
  flex-direction: column;
  gap: 20px;
  border: 1px solid #f1f5f9;
}

.section-header {
  display: flex;
  align-items: center;
  gap: 12px;
  padding-bottom: 16px;
  border-bottom: 1px solid #f1f5f9;
}

.section-header h3 {
  margin: 0;
  font-size: 1.1rem;
  font-weight: 800;
  color: #0f172a;
}

.icon-main {
  color: #4f46e5;
  font-variation-settings: 'FILL' 1;
}

.departure .icon-main { color: #6366f1; }
.arrival .icon-main { color: #10b981; }

/* Metadata Grid */
.metadata-grid {
  display: grid;
  grid-template-columns: 1fr 1fr;
  gap: 12px;
}

.meta-chip {
  display: flex;
  align-items: center;
  gap: 10px;
  padding: 10px 14px;
  background: #f8fafc;
  border-radius: 12px;
  border: 1px solid #f1f5f9;
}

.meta-chip .material-symbols-outlined {
  font-size: 1.25rem;
  color: #64748b;
}

.meta-info {
  display: flex;
  flex-direction: column;
}

.meta-info label {
  font-size: 0.7rem;
  font-weight: 700;
  color: #94a3b8;
  text-transform: uppercase;
  letter-spacing: 0.02em;
}

.meta-info span {
  font-size: 0.9rem;
  font-weight: 700;
  color: #0f172a;
}

/* Price Banner */
.price-banner {
  display: flex;
  justify-content: space-between;
  align-items: center;
  padding: 16px;
  background: linear-gradient(135deg, #f0fdf4, #dcfce7);
  border-radius: 16px;
  border: 1px solid #bbfcce;
}

.price-content {
  display: flex;
  align-items: center;
  gap: 8px;
  color: #15803d;
}

.price-label {
  font-size: 0.85rem;
  font-weight: 700;
}

.price-value {
  font-size: 1.25rem;
  font-weight: 900;
  color: #166534;
}

/* Invoice & Photo */
.invoice-box {
  display: flex;
  align-items: center;
  gap: 10px;
  padding: 12px 16px;
  background: #f1f5f9;
  border-radius: 12px;
}

.invoice-link {
  font-size: 0.85rem;
  color: #475569;
  font-weight: 600;
  text-decoration: underline;
  cursor: pointer;
}

.photo-preview-btn {
  width: 100%;
  aspect-ratio: 16/10;
  border-radius: 16px;
  overflow: hidden;
  border: 1px solid #e2e8f0;
  background: #f8fafc;
  cursor: pointer;
  padding: 0;
}

.photo-image {
  width: 100%;
  height: 100%;
  object-fit: cover;
}

/* Address Card Inner */
.address-content {
  display: flex;
  flex-direction: column;
  gap: 16px;
}

.user-main {
  display: flex;
  align-items: center;
  gap: 8px;
  color: #0f172a;
}

.user-name {
  font-weight: 800;
  font-size: 1.05rem;
}

.contact-grid {
  display: grid;
  grid-template-columns: 1fr;
  gap: 8px;
}

.contact-item {
  display: flex;
  align-items: center;
  gap: 10px;
  font-size: 0.9rem;
  color: #64748b;
  font-weight: 500;
}

.contact-item .material-symbols-outlined {
  font-size: 1.15rem;
  color: #cbd5e1;
}

.full-address-box {
  display: flex;
  gap: 12px;
  padding: 16px;
  background: #f8fafc;
  border-radius: 16px;
  border: 1px solid #f1f5f9;
}

.address-pin {
  color: #94a3b8;
  font-variation-settings: 'FILL' 1;
}

.main-address {
  font-size: 0.95rem;
  font-weight: 700;
  color: #1e293b;
  line-height: 1.4;
}

.floor-tag {
  display: inline-block;
  margin-top: 6px;
  padding: 4px 10px;
  background: #ffffff;
  border: 1px solid #e2e8f0;
  border-radius: 8px;
  font-size: 0.75rem;
  font-weight: 700;
  color: #64748b;
}

.time-callout {
  display: flex;
  align-items: center;
  gap: 8px;
  padding: 10px 14px;
  background: #fffbeb;
  border-radius: 12px;
  border: 1px solid #fde68a;
  color: #92400e;
  font-weight: 700;
  font-size: 0.85rem;
}

/* Mobile Adjustments */
@media screen and (max-width: 1100px) {
  .package-details-v3 {
    grid-template-columns: 1fr;
    gap: 16px;
  }
}

@media screen and (max-width: 500px) {
  .details-section {
    padding: 18px;
  }
}
</style>
