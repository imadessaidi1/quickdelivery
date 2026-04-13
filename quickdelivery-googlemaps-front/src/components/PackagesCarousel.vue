<template>
    <div class="carousel-wrapper">
    <div v-if="isMobile" class="segmented-control mobile-toggle">
      <div class="segmented-control-bg" :class="{ 'at-right': mobileViewMode === 'list' }"></div>
      <button class="segment-btn" :class="{ active: mobileViewMode === 'map' }" @click="mobileViewMode = 'map'">
        <span class="material-symbols-outlined">map</span>
        {{ $t('mobileModeMap') }}
      </button>
      <button class="segment-btn" :class="{ active: mobileViewMode === 'list' }" @click="mobileViewMode = 'list'">
        <span class="material-symbols-outlined">format_list_bulleted</span>
        {{ $t('mobileModeList') }}
      </button>
    </div>
    <template v-if="isLoadingPackages"></template>
    <div v-else-if="loadError" class="carousel-state error">{{ $t('stateLoadError') }}</div>
    <div v-else-if="packagesList.length === 0" class="carousel-state">{{ $t('stateEmptyPackagesAround') }}</div>
    <template v-else>
      <div v-if="activeRouteNavigationUrl" class="mission-status-card">
        <div class="mission-info">
          <div class="mission-badge">
            <span class="pulse-dot"></span>
            <span class="badge-text">{{ $t('activeRouteReservationLocked') }}</span>
          </div>
        </div>
        <div class="mission-actions">
          <router-link class="mission-btn secondary" to="/myRoute">
            <span class="material-symbols-outlined">route</span>
            {{ $t('menuMyRoute') }}
          </router-link>
          <button class="mission-btn primary" type="button" @click="openActiveRouteInGoogleMaps()">
            <span class="material-symbols-outlined">explore</span>
            {{ $t('activeRouteOpenInGoogleMaps') }}
          </button>
        </div>
      </div>
      <div v-if="!isMobile" class="packages-panel">
        <div class="panel-header horizontal-stack">
          <div class="header-main-info vertical-header">
            <div class="title-row">
              <h3>{{ $t('availablePackagesTitle') }}</h3>
              <div class="count-chip">{{ $t('totalPackagesCount', { count: packagesList.length }) }}</div>
            </div>
            <p>{{ $t('availablePackagesSubtitle') }}</p>
          </div>
          
          <div v-if="showBatchReserveAction" class="batch-control-card glass-pane">
            <div class="batch-config">
              <div class="batch-slider-wrap">
                <div class="slider-labels">
                  <label>{{ $t('mapSearchBatchSelectionLabel') }}</label>
                  <span class="selection-count"><strong>{{ batchPackageCount }}</strong>/{{ batchPackageMax }}</span>
                </div>
                <input type="range" min="1" :max="batchPackageMax" step="1" v-model.number="batchPackageCount" @change="rebuildBatchRoutePlan" />
              </div>

              <div class="metrics-grid">
                <div class="metric-item">
                  <span class="material-symbols-outlined">distance</span>
                  <span>{{ totalPlannedDistanceLabel }}</span>
                </div>
                <div class="metric-item highlight success">
                  <span class="material-symbols-outlined">payments</span>
                  <span>{{ totalPlannedGainLabel }}</span>
                </div>
                <div v-if="routeAddedMinutesLabel" class="metric-item highlight warning">
                  <span class="material-symbols-outlined">schedule</span>
                  <span>{{ routeAddedMinutesLabel }}</span>
                </div>
                <div v-if="routePayoutPerKmLabel" class="metric-item highlight info">
                  <span class="material-symbols-outlined">trending_up</span>
                  <span>{{ routePayoutPerKmLabel }}</span>
                </div>
                <div v-if="routeQualityScoreLabel" class="metric-item highlight success">
                  <span class="material-symbols-outlined">stars</span>
                  <span>{{ routeQualityScoreLabel }}</span>
                </div>
              </div>
            </div>

            <div class="batch-actions-side">
              <div v-if="!canBatchReserve" class="batch-alert">
                <span class="material-symbols-outlined">warning</span>
                <p>{{ batchReserveDisabledReason }}</p>
              </div>
              
              <button
                class="btn primary_btn batch-reserve-btn"
                type="button"
                :disabled="!canBatchReserve"
                :title="batchReserveDisabledReason"
                @click.stop="reserveOnMyRoad()"
              >
                <span class="material-symbols-outlined">auto_awesome</span>
                {{ $t('packagesArroundMArkerDetailActionsReserveOnMyRoad') }}
              </button>
            </div>
          </div>
        </div>
        <div class="vertical-carousel">
          <button class="v-nav up" :disabled="desktopSlideIndex === 0" @click="goDesktop(-2)" aria-label="Previous package">
            <span class="material-symbols-outlined">expand_less</span>
          </button>
          <div class="vertical-slide-window">
            <div
              v-for="(package_, visibleIndex) in visibleDesktopPackages"
              :key="package_.id || visibleIndex"
              class="package-item tone-indigo"
              :class="{ selected: selectedPackageId === package_.id }"
              @click="displayDirection(desktopSlideIndex + visibleIndex, { preserveDesktopWindow: true })"
            >
              <MarkerDetails
                :package_="package_"
                :mapVue="getMapVue()"
                :modal="getPackageModal()"
                :isSelected="selectedPackageId === package_.id"
                :dense="true"
              />
            </div>
          </div>
          <button class="v-nav down" :disabled="desktopSlideIndex >= packagesList.length - 2" @click="goDesktop(2)" aria-label="Next package">
            <span class="material-symbols-outlined">expand_more</span>
          </button>
          <div class="v-counter">{{ desktopSlideIndex + 1 }}-{{ Math.min(desktopSlideIndex + visibleDesktopPackages.length, packagesList.length) }} / {{ packagesList.length }}</div>
        </div>
      </div>
      <template v-else>
        <div v-if="showBatchReserveAction" class="mobile-batch-bar glass-pane">
          <div class="batch-config mobile">
             <div class="batch-slider-wrap">
                <div class="slider-labels">
                  <label>{{ $t('mapSearchBatchSelectionLabel') }}</label>
                  <span class="selection-count"><strong>{{ batchPackageCount }}</strong>/{{ batchPackageMax }}</span>
                </div>
                <input type="range" min="1" :max="batchPackageMax" step="1" v-model.number="batchPackageCount" @change="rebuildBatchRoutePlan" />
              </div>
              
              <div class="metrics-grid">
                <div class="metric-item">
                  <span class="material-symbols-outlined">distance</span>
                  <span>{{ totalPlannedDistanceLabel }}</span>
                </div>
                <div class="metric-item success">
                  <span class="material-symbols-outlined">payments</span>
                  <span>{{ totalPlannedGainLabel }}</span>
                </div>
                <div v-if="routeAddedMinutesLabel" class="metric-item warning">
                  <span class="material-symbols-outlined">schedule</span>
                  <span>{{ routeAddedMinutesLabel }}</span>
                </div>
              </div>

              <div v-if="!canBatchReserve" class="batch-alert">
                <span class="material-symbols-outlined">warning</span>
                <p>{{ batchReserveDisabledReason }}</p>
              </div>

              <button class="btn primary_btn batch-reserve-btn" type="button" :disabled="!canBatchReserve" :title="batchReserveDisabledReason" @click.stop="reserveOnMyRoad()">
                <span class="material-symbols-outlined">auto_awesome</span>
                {{ $t('packagesArroundMArkerDetailActionsReserveOnMyRoad') }}
              </button>
          </div>
        </div>
        <div v-if="mobileViewMode === 'map'" class="mobile-map-card">
          <carousel
            v-if="packagesList.length"
            v-model="mobileMapSlideIndex"
            :items-to-show="1"
            snap-align="start"
            :wrap-around="false"
            :mouse-drag="true"
            :touch-drag="true"
            @slide-start="handleMobileMapSlideStart"
            class="mobile-map-carousel"
          >
            <slide v-for="(package_, index) in packagesList" :key="package_.id || index">
              <div class="mobile-map-item tone-indigo" :class="{ selected: selectedPackageId === package_.id }" @click="displayDirection(index)">
                <MarkerDetails
                  :package_="package_"
                  :mapVue="getMapVue()"
                  :modal="getPackageModal()"
                  :isSelected="selectedPackageId === package_.id"
                  :compact="true"
                />
              </div>
            </slide>
            <template #addons>
              <navigation />
            </template>
          </carousel>
          <div v-else class="carousel-state">{{ $t('stateEmptyPackagesAround') }}</div>
        </div>
        <div v-else class="mobile-list">
          <div
            v-for="(package_, index) in packagesList"
            :key="package_.id || index"
            class="package-item tone-indigo"
            :class="{ selected: selectedPackageId === package_.id }"
            @click="displayDirection(index)"
          >
            <MarkerDetails
              :package_="package_"
              :mapVue="getMapVue()"
              :modal="getPackageModal()"
              :isSelected="selectedPackageId === package_.id"
            />
          </div>
        </div>
      </template>
    </template>
    </div>
</template>

<script>
// If you are using PurgeCSS, make sure to whitelist the carousel CSS classes
import 'vue3-carousel/dist/carousel.css'
import { Carousel, Slide, Navigation } from 'vue3-carousel';
import MarkerDetails from './MarkerDetails.vue';
//import { Geolocation } from '@capacitor/geolocation';
import http from '@/config/httpInterceptor';
import { getDeliveryModeSearchRadius } from '@/config/deliveryMode';
import { resolveDisplayedPackageAmount } from '@/config/packagePricing';
import { buildDirectRoutePlan, buildPersonalRoutePlan, decoratePackagesWithRoutePlan, haversineMeters } from '@/services/routePlanning';

export default {
  name: 'App',
  emits: ['mobile-view-change', 'loading-state-change'],
  components: {
    Carousel,
    Slide,
    Navigation,
    MarkerDetails,
  },
  data() {
    return {
      packagesList:[] ,
      location: null,
      coordinates: null,
      positionData: null,
      selectedPackageId: null,
      onMyRoadPackageIds: [],
      searchRadius: this.$store.state.mapSearchRadius || getDeliveryModeSearchRadius(this.$store.state.connectedUser) || 10000,
      isLoadingPackages: false,
      loadError: false,
      isMobile: window.innerWidth < 768,
      searchMode: 'aroundMe',
      addressCriteria: null,
      personalRouteCriteria: null,
      lastSearchedAddress: '',
      mapViewport: null,
      viewportRefreshTimer: null,
      lastViewportRequestKey: '',
      lastViewportPayload: null,
      lastViewportRefreshAt: 0,
      desktopSlideIndex: 0,
      mobileMapSlideIndex: 0,
      mobileViewMode: 'map',
      refreshPackagesPromise: null,
      hasCompletedInitialAroundMeLoad: false,
      isMapIframeReady: false,
      pendingMapMessages: [],
      isRouteLoading: false,
      personalRoutePlan: null,
      batchPackageCount: 1,
    };
  },
  computed: {
    visibleDesktopPackages() {
      return this.packagesList.slice(this.desktopSlideIndex, this.desktopSlideIndex + 2);
    },
    selectedPackageForMobileMap() {
      if (!this.packagesList.length) {
        return null;
      }
      const byId = this.packagesList.find((pkg) => pkg.id === this.selectedPackageId);
      return byId || this.packagesList[0];
    },
    isBusy() {
      return this.isLoadingPackages || this.isRouteLoading;
    },
    showBatchReserveAction() {
      return ['personalRoute', 'directAddress'].includes(this.searchMode) && this.packagesList.length > 0;
    },
    batchPackageMax() {
      return Math.max(1, this.packagesList.length);
    },
    batchSelectedPackages() {
      return this.packagesList.slice(0, Math.min(this.batchPackageCount, this.packagesList.length));
    },
    batchPackageIds() {
      return this.batchSelectedPackages.map((pkg) => pkg.id).filter(Boolean);
    },
    canBatchReserve() {
      return Boolean(this.$store.state.connectedUser?.id) && Boolean(this.$store.state.reservationAvailability?.canReserve);
    },
    batchReserveDisabledReason() {
      const availability = this.$store.state.reservationAvailability || {};
      if (availability.activeRouteBlocking || this.$store.state.isUserWithOngoingDelivery) {
        return this.$t('reservationBlockedActiveRoute');
      }
      if (availability.capacityReached) {
        const maxReservations = Number(availability.maxReservations || 0);
        const activeReservations = Number(availability.activeReservations || 0);
        const capacityLabel = maxReservations > 0
          ? ` (${this.$t('reservationCapacityStatus', { active: activeReservations, max: maxReservations })})`
          : '';
        return `${this.$t('reservationBlockedCapacityReached')}${capacityLabel}`;
      }
      if (availability.canReserve === false) {
        return this.$t('reservationBlockedGeneric');
      }
      return '';
    },
    activeRouteNavigationUrl() {
      const activeRoute = this.$store.state.activeDeliveryRoute;
      if (!activeRoute) {
        return '';
      }
      return activeRoute.googleMapsNavigationUrl
        || (Array.isArray(activeRoute.googleMapsNavigationUrls) ? activeRoute.googleMapsNavigationUrls[0] : '')
        || '';
    },
    totalPlannedDistanceKm() {
      if (this.personalRoutePlan?.metrics?.totalDistanceMeters) {
        return this.personalRoutePlan.metrics.totalDistanceMeters / 1000;
      }
      if (!this.personalRoutePlan?.start || !this.personalRoutePlan?.end) {
        return 0;
      }
      const nodes = [
        this.personalRoutePlan.start,
        ...((this.personalRoutePlan.stops || []).map((stop) => ({ lat: stop.lat, lng: stop.lng }))),
        this.personalRoutePlan.end,
      ];
      let totalMeters = 0;
      for (let index = 0; index < nodes.length - 1; index += 1) {
        totalMeters += this.haversineMeters(nodes[index], nodes[index + 1]);
      }
      return totalMeters / 1000;
    },
    totalPlannedDistanceLabel() {
      if (!this.showBatchReserveAction) {
        return '';
      }
      return `${this.totalPlannedDistanceKm.toFixed(1)} km`;
    },
    totalPlannedGainAmount() {
      const sourcePackages = this.showBatchReserveAction ? this.batchSelectedPackages : this.packagesList;
      return sourcePackages.reduce((total, pkg) => total + Number(resolveDisplayedPackageAmount(pkg) || 0), 0);
    },
    totalPlannedGainLabel() {
      if (!this.showBatchReserveAction) {
        return '';
      }
      return `${this.totalPlannedGainAmount.toFixed(2)} €`;
    },
    routeAddedMinutesLabel() {
      const minutes = this.personalRoutePlan?.metrics?.estimatedDurationMinutes;
      if (!this.showBatchReserveAction || !Number.isFinite(minutes) || minutes <= 0) {
        return '';
      }
      return `${Math.round(minutes)} min`;
    },
    routePayoutPerKmLabel() {
      const payoutPerKm = this.personalRoutePlan?.metrics?.payoutPerKm;
      if (!this.showBatchReserveAction || !Number.isFinite(payoutPerKm) || payoutPerKm <= 0) {
        return '';
      }
      return `${payoutPerKm.toFixed(2)} €/km`;
    },
    routeQualityScoreLabel() {
      const qualityScore = this.personalRoutePlan?.metrics?.qualityScore;
      if (!this.showBatchReserveAction || !Number.isFinite(qualityScore) || qualityScore <= 0) {
        return '';
      }
      return `Score ${qualityScore.toFixed(1)}`;
    },
  },
  watch: {
    mobileViewMode(newValue) {
      this.$emit('mobile-view-change', newValue);
    },
    isLoadingPackages: {
      immediate: true,
      handler(newValue) {
        this.$emit('loading-state-change', Boolean(newValue));
      },
    },
  },
  mounted() {
    this.initializeSearchRadiusFromProfile();
    this.$emit('mobile-view-change', this.mobileViewMode);
    window.addEventListener('resize', this.handleResize);
    window.addEventListener('qd-search-address', this.handleAddressSearch);
    window.addEventListener('qd-refresh-package-search', this.handleSearchRefresh);
    window.addEventListener('qd-map-iframe-loaded', this.handleMapIframeLoaded);
    window.addEventListener('qd-package-soft-lock-updated', this.handleSoftLockUpdated);
    window.onmessage = (e) => {
        this.handleMapMessage(e);
    };
    this.initializeAroundMe();
  },
  beforeUnmount() {
    window.removeEventListener('resize', this.handleResize);
    window.removeEventListener('qd-search-address', this.handleAddressSearch);
    window.removeEventListener('qd-refresh-package-search', this.handleSearchRefresh);
    window.removeEventListener('qd-map-iframe-loaded', this.handleMapIframeLoaded);
    window.removeEventListener('qd-package-soft-lock-updated', this.handleSoftLockUpdated);
    if (this.viewportRefreshTimer) {
      clearTimeout(this.viewportRefreshTimer);
      this.viewportRefreshTimer = null;
    }
  },
  methods: {
    initializeSearchRadiusFromProfile() {
      const profileRadius = getDeliveryModeSearchRadius(this.$store.state.connectedUser);
      if (!profileRadius) {
        return;
      }
      if (!this.$store.state.mapSearchRadius || this.$store.state.mapSearchRadius === 10000) {
        this.searchRadius = profileRadius;
        this.$store.commit('updateMapSearchRadius', profileRadius);
      }
    },
    currentDeliveryMode() {
      return this.$store.state.connectedUser?.deliveryMode || '';
    },
    currentVehicleType() {
      return this.$store.state.connectedUser?.primaryVehicleType
        || this.$store.state.connectedUser?.vehicles?.find((vehicle) => vehicle?.type)?.type
        || '';
    },
    computePackageSize(package_) {
      const weight = Number(package_?.weight || 0);
      const maxDim = Math.max(Number(package_?.width || 0), Number(package_?.height || 0), Number(package_?.depth || package_?.dept || 0));
      if (weight <= 2 && maxDim <= 25) return 'SMALL';
      if (weight <= 10 && maxDim <= 45) return 'MEDIUM';
      if (weight <= 25 && maxDim <= 65) return 'LARGE';
      return 'EXTRA_LARGE';
    },
    normalizePackages(rawPackages) {
      return Array.isArray(rawPackages) ? rawPackages.map((pkg) => ({
        ...pkg,
        packageSizeCategory: pkg?.packageSizeCategory || this.computePackageSize(pkg),
        routeDisplayedAmount: resolveDisplayedPackageAmount(pkg) || 0,
      })) : [];
    },
    sortPackagesDefault(packages) {
      return [...packages].sort((left, right) => {
        const leftLocked = left.isSoftLockedBy ? 1 : 0;
        const rightLocked = right.isSoftLockedBy ? 1 : 0;
        if (leftLocked !== rightLocked) {
          return leftLocked - rightLocked;
        }
        const leftHasDetour = typeof left.detourMeters === 'number';
        const rightHasDetour = typeof right.detourMeters === 'number';
        if (leftHasDetour && rightHasDetour && left.detourMeters !== right.detourMeters) {
          return left.detourMeters - right.detourMeters;
        }
        if (leftHasDetour !== rightHasDetour) {
          return leftHasDetour ? -1 : 1;
        }
        return Number(left.id || 0) - Number(right.id || 0);
      });
    },
    hasMatchingRoutePlan(packages, routePlan) {
      if (!routePlan || !Array.isArray(routePlan.packageIds) || !routePlan.packageIds.length) {
        return false;
      }
      const packageIds = [...packages].map((pkg) => Number(pkg.id)).filter((id) => !Number.isNaN(id)).sort((left, right) => left - right);
      const planIds = [...routePlan.packageIds].map((id) => Number(id)).filter((id) => !Number.isNaN(id)).sort((left, right) => left - right);
      if (planIds.length > packageIds.length) {
        return false;
      }
      const packageSet = new Set(packageIds);
      return planIds.every((id) => packageSet.has(id));
    },
    decoratePackages(rawPackages) {
      const packages = this.normalizePackages(rawPackages);
      const defaultSortedPackages = this.sortPackagesDefault(packages);
      if (!['personalRoute', 'directAddress'].includes(this.searchMode) || !this.positionData) {
        this.personalRoutePlan = null;
        return defaultSortedPackages;
      }
      const routeTarget = this.searchMode === 'personalRoute' ? this.personalRouteCriteria : this.addressCriteria;
      if (!routeTarget) {
        this.personalRoutePlan = null;
        return defaultSortedPackages;
      }
      const routePlan = this.hasMatchingRoutePlan(defaultSortedPackages, this.personalRoutePlan)
        ? this.personalRoutePlan
        : (this.searchMode === 'personalRoute'
          ? buildPersonalRoutePlan(defaultSortedPackages, this.positionData, routeTarget)
          : buildDirectRoutePlan(defaultSortedPackages, this.positionData, routeTarget));
      this.personalRoutePlan = routePlan;
      if (!routePlan) {
        return defaultSortedPackages;
      }
      const annotatedPackages = decoratePackagesWithRoutePlan(defaultSortedPackages, routePlan);
      return annotatedPackages.sort((left, right) => {
        const leftOrder = Number.isFinite(left.routeSortOrder) ? left.routeSortOrder : Number.MAX_SAFE_INTEGER;
        const rightOrder = Number.isFinite(right.routeSortOrder) ? right.routeSortOrder : Number.MAX_SAFE_INTEGER;
        if (leftOrder !== rightOrder) {
          return leftOrder - rightOrder;
        }
        const leftDetour = typeof left.detourMeters === 'number' ? left.detourMeters : Number.MAX_SAFE_INTEGER;
        const rightDetour = typeof right.detourMeters === 'number' ? right.detourMeters : Number.MAX_SAFE_INTEGER;
        if (leftDetour !== rightDetour) {
          return leftDetour - rightDetour;
        }
        return Number(left.id || 0) - Number(right.id || 0);
      });
    },
    syncPackagesState(rawPackages) {
      this.packagesList = this.decoratePackages(rawPackages);
      if (!this.batchPackageCount || this.batchPackageCount > this.packagesList.length) {
        this.batchPackageCount = Math.max(1, this.packagesList.length);
      }
      this.$store.commit('updatePackagesArround', this.packagesList);
    },
    async buildRoutePlanFromBackend(mode, rawPackages, routeTarget) {
      const packages = this.normalizePackages(rawPackages);
      if (!['personalRoute', 'directAddress'].includes(mode) || !packages.length || !this.positionData || !routeTarget) {
        return null;
      }
      const payload = {
        mode,
        deliveryMode: this.currentDeliveryMode(),
        vehicleType: this.currentVehicleType(),
        selectedPackageId: packages[0]?.id || null,
        start: {
          lat: Number(this.normalizeCoordinate(this.positionData.actuallatitude)),
          lng: Number(this.normalizeCoordinate(this.positionData.actuallongitude)),
        },
        end: {
          lat: Number(this.normalizeCoordinate(routeTarget.latitude)),
          lng: Number(this.normalizeCoordinate(routeTarget.longitude)),
        },
        packageIds: packages.map((pkg) => pkg.id),
      };
      try {
        const response = await http.post(`${this.$i18n.t('rootURL')}plan-route`, payload, { silent: true });
        return response?.data || null;
      } catch (error) {
        console.error('Unable to build route plan from backend, using local fallback', error);
        return null;
      }
    },
    haversineMeters(start, end) {
      return haversineMeters(start, end);
    },
    postOptimizedRoutePlan() {
      if (!['personalRoute', 'directAddress'].includes(this.searchMode) || !this.personalRoutePlan) {
        return;
      }
      const payload = {
        type: 'QD_OPTIMIZED_PERSONAL_ROUTE',
        selectedPackageId: this.selectedPackageId,
        packageIds: this.personalRoutePlan.packageIds,
        start: this.personalRoutePlan.start,
        end: this.personalRoutePlan.end,
        stops: this.personalRoutePlan.stops,
      };
      this.postToMap(`OptimizedPersonalRoute:${JSON.stringify(payload)}`);
    },
    openActiveRouteInGoogleMaps() {
      if (!this.activeRouteNavigationUrl) {
        return;
      }
      window.open(this.activeRouteNavigationUrl, '_blank', 'noopener');
    },
    postPersonalRouteContext(criteria) {
      if (!criteria || !this.positionData) {
        return;
      }
      const payload = {
        deliveryMode: this.currentDeliveryMode(),
        vehicleType: this.currentVehicleType(),
        radiusMeters: this.searchRadius,
        departureLatitude: this.positionData.actuallatitude,
        departureLongitude: this.positionData.actuallongitude,
        arrivalLatitude: criteria.latitude,
        arrivalLongitude: criteria.longitude,
      };
      this.postToMap(`OnMyRoadSearchContext:${JSON.stringify(payload)}`);
    },
    postDirectRouteContext(criteria) {
      if (!criteria || !this.positionData) {
        return;
      }
      const payload = {
        deliveryMode: this.currentDeliveryMode(),
        vehicleType: this.currentVehicleType(),
        radiusMeters: this.searchRadius,
        departureLatitude: this.positionData.actuallatitude,
        departureLongitude: this.positionData.actuallongitude,
        arrivalLatitude: criteria.latitude,
        arrivalLongitude: criteria.longitude,
        mode: 'directAddress',
      };
      this.postToMap(`DirectSearchContext:${JSON.stringify(payload)}`);
    },
    buildDirectionMessageForPackage(package_) {
      if (!package_ || !Array.isArray(package_.addresses)) {
        return '';
      }
      let stringDeparture = '';
      let stringArrival = '';
      package_.addresses.forEach((address) => {
        if (address.type === 'DEPARTURE') {
          stringDeparture = `${address.latitude},${address.longitude}`;
        } else if (address.type === 'ARRIVAL') {
          stringArrival = `${address.latitude},${address.longitude}`;
        }
      });
      if (!stringDeparture || !stringArrival) {
        return '';
      }
      return `SelectedDirection:${stringDeparture};${stringArrival}`;
    },
    syncSelectedRouteOnMap() {
      if (!this.selectedPackageId) {
        return;
      }
      const selectedPackage = this.packagesList.find((pkg) => pkg.id === this.selectedPackageId);
      if (!selectedPackage) {
        return;
      }
      this.postToMap(`SelectedPackage:${selectedPackage.id}`);
      if (['personalRoute', 'directAddress'].includes(this.searchMode) && this.personalRoutePlan) {
        this.postOptimizedRoutePlan();
        return;
      }
      const directionMessage = this.buildDirectionMessageForPackage(selectedPackage);
      if (directionMessage) {
        this.postToMap(directionMessage);
      }
    },
    async initializeAroundMe() {
      try {
        this.positionData = await this.getCurrentLocation();
        await this.refreshPackagesList(this.positionData);
      } catch (error) {
        this.loadError = true;
        console.error('Unable to initialize around me location', error);
      }
    },
    getMapIframeWindow() {
      return this.$parent?.$refs?.mapVue?.$refs?.map?.contentWindow || null;
    },
    postToMap(message) {
      const mapWindow = this.getMapIframeWindow();
      if (!this.isMapIframeReady || !mapWindow) {
        this.pendingMapMessages.push(message);
        return false;
      }
      mapWindow.postMessage(message, '*');
      return true;
    },
    flushPendingMapMessages() {
      const mapWindow = this.getMapIframeWindow();
      if (!this.isMapIframeReady || !mapWindow) {
        return;
      }
      this.pendingMapMessages.splice(0).forEach((message) => {
        mapWindow.postMessage(message, '*');
      });
    },
    handleMapIframeLoaded() {
      this.isMapIframeReady = true;
      this.flushPendingMapMessages();
      if (this.positionData) {
        this.postToMap(JSON.stringify(this.positionData));
      }
      if (this.searchMode === 'personalRoute' && this.personalRouteCriteria) {
        this.postPersonalRouteContext(this.personalRouteCriteria);
        this.postOptimizedRoutePlan();
      } else if (this.searchMode === 'directAddress' && this.addressCriteria) {
        this.postDirectRouteContext(this.addressCriteria);
        this.postOptimizedRoutePlan();
      }
      if (this.packagesList.length) {
        this.postToMap(JSON.stringify(this.packagesList));
        this.syncSelectedRouteOnMap();
      }
    },
    handleSoftLockUpdated(event) {
      const packageId = Number(event?.detail?.packageId);
      if (Number.isNaN(packageId)) {
        return;
      }
      this.syncPackagesState(this.packagesList.map((pkg) => (
        pkg.id === packageId ? { ...pkg, isSoftLockedBy: event?.detail?.lockedBy || null } : pkg
      )));
    },
    handleMapMessage(e) {
      let parsedData = e?.data;
      if (typeof parsedData === 'string') {
        try {
          parsedData = JSON.parse(parsedData);
        } catch (_ignored) {
          // Non-JSON postMessage payloads are handled below.
        }
      }
      if (parsedData?.type === 'QD_MAP_VIEWPORT') {
        this.handleViewportUpdate(parsedData);
        return;
      }
      if (typeof e?.data === 'string' && e.data.includes('SelectedPackage:')) {
        const packageID = e.data.split(':')[1];
        this.syncSelectedPackageFromMap(parseInt(packageID, 10));
      } else if (typeof e?.data === 'string' && e.data.startsWith('OnMyRoadPackages:')) {
        const raw = e.data.substring('OnMyRoadPackages:'.length);
        try {
          const ids = JSON.parse(raw);
          this.onMyRoadPackageIds = Array.isArray(ids) ? ids.map((id) => parseInt(id, 10)).filter((id) => !Number.isNaN(id)) : [];
        } catch (_ignored) {
          this.onMyRoadPackageIds = [];
        }
      } else if (typeof e?.data === 'string' && e.data === 'StartLoading') {
        this.isRouteLoading = true;
      } else if (typeof e?.data === 'string' && e.data === 'EndLoading') {
        this.isRouteLoading = false;
      } else if (typeof e?.data === 'string' && e.data.includes('RefreshPackagesList')) {
        this.removePackageFromListe(e.data.split(' ')[1]);
      }
    },
    handleViewportUpdate(payload) {
      if (this.isLoadingPackages || (this.refreshPackagesPromise && !this.hasCompletedInitialAroundMeLoad)) {
        return;
      }
      this.mapViewport = payload;
      if (this.searchMode !== 'aroundMe' || this.addressCriteria || !this.positionData) {
        return;
      }
      if (!this.hasCompletedInitialAroundMeLoad) {
        this.lastViewportPayload = payload;
        return;
      }
      if (this.lastViewportPayload && !this.hasMeaningfulViewportChange(payload, this.lastViewportPayload)) {
        return;
      }
      const now = Date.now();
      if ((now - this.lastViewportRefreshAt) < 1000) {
        return;
      }
      const requestKey = [
        payload.bounds?.minLat,
        payload.bounds?.maxLat,
        payload.bounds?.minLng,
        payload.bounds?.maxLng,
        payload.zoom,
        this.searchRadius,
      ].join(':');
      if (requestKey === this.lastViewportRequestKey) {
        return;
      }
      if (this.viewportRefreshTimer) {
        clearTimeout(this.viewportRefreshTimer);
      }
      this.viewportRefreshTimer = setTimeout(() => {
        this.lastViewportRequestKey = requestKey;
        this.lastViewportPayload = payload;
        this.refreshPackagesListByViewport(payload);
      }, 350);
    },
    hasMeaningfulViewportChange(nextViewport, previousViewport) {
      const epsilon = 0.0035;
      const nextCenter = nextViewport?.center || {};
      const previousCenter = previousViewport?.center || {};
      const nextBounds = nextViewport?.bounds || {};
      const previousBounds = previousViewport?.bounds || {};
      return [
        Math.abs((nextCenter.lat ?? 0) - (previousCenter.lat ?? 0)),
        Math.abs((nextCenter.lng ?? 0) - (previousCenter.lng ?? 0)),
        Math.abs((nextBounds.minLat ?? 0) - (previousBounds.minLat ?? 0)),
        Math.abs((nextBounds.maxLat ?? 0) - (previousBounds.maxLat ?? 0)),
        Math.abs((nextBounds.minLng ?? 0) - (previousBounds.minLng ?? 0)),
        Math.abs((nextBounds.maxLng ?? 0) - (previousBounds.maxLng ?? 0)),
      ].some((delta) => delta >= epsilon);
    },
    handleResize() {
      this.isMobile = window.innerWidth < 768;
    },
    handleSlideStart(data) {
      this.displayDirection(data.slidingToIndex);
    },
    handleMobileMapSlideStart(data) {
      this.displayDirection(data.slidingToIndex);
    },
    syncSelectedPackageFromMap(packageId) {
      if (Number.isNaN(packageId)) {
        return;
      }
      const index = this.packagesList.findIndex((pkg) => pkg.id === packageId);
      if (index === -1) {
        this.selectedPackageId = packageId;
        return;
      }
      this.mobileViewMode = 'map';
      this.displayDirection(index);
    },
    goDesktop(delta) {
      if (!this.packagesList.length) {
        return;
      }
      const nextIndex = Math.min(this.packagesList.length - 1, Math.max(0, this.desktopSlideIndex + delta));
      if (nextIndex === this.desktopSlideIndex) {
        return;
      }
      this.desktopSlideIndex = nextIndex;
      this.displayDirection(nextIndex);
    },
    handleSearchRefresh(event) {
      if (this.viewportRefreshTimer) {
        clearTimeout(this.viewportRefreshTimer);
        this.viewportRefreshTimer = null;
      }
      this.syncSearchRadiusFromStore();
      const mode = event?.detail?.mode;
      if (mode === 'aroundMe') {
        this.addressCriteria = null;
        this.personalRouteCriteria = null;
        this.refreshPackagesList(this.positionData);
        return;
      }
      this.refreshWithCurrentRadius();
    },
    refreshWithCurrentRadius() {
      if (this.searchMode === 'personalRoute' && this.personalRouteCriteria) {
        this.refreshPackagesListOnPersonalRoute(this.personalRouteCriteria);
      } else if (this.searchMode === 'directAddress' && this.addressCriteria) {
        this.refreshPackagesListDirect(this.addressCriteria);
      } else if (this.mapViewport) {
        this.refreshPackagesListByViewport(this.mapViewport);
      } else if (this.positionData) {
        this.refreshPackagesList(this.positionData);
      }
    },
    handleAddressSearch(event) {
      if (this.viewportRefreshTimer) {
        clearTimeout(this.viewportRefreshTimer);
        this.viewportRefreshTimer = null;
      }
      this.syncSearchRadiusFromStore();
      const requestedMode = event?.detail?.mode;
      const criteria = event?.detail?.criteria;
      if (!criteria || criteria.latitude == null || criteria.longitude == null) {
        return;
      }
      this.lastSearchedAddress = criteria.rawAddress || '';
      if (requestedMode === 'direct') {
        this.personalRouteCriteria = null;
        this.addressCriteria = criteria;
        this.refreshPackagesListDirect(criteria);
        return;
      }
      this.addressCriteria = null;
      this.personalRouteCriteria = criteria;
      this.refreshPackagesListOnPersonalRoute(criteria);
    },
    syncSearchRadiusFromStore() {
      const radiusMeters = Number(this.$store.state.mapSearchRadius || this.searchRadius);
      if (Number.isFinite(radiusMeters) && radiusMeters > 0) {
        this.searchRadius = radiusMeters;
      }
    },
    async reserveOnMyRoad(packageId) {
      if (!this.canBatchReserve) {
        return;
      }
      const packageIds = ['personalRoute', 'directAddress'].includes(this.searchMode)
        ? this.batchPackageIds
        : Array.from(new Set([packageId || this.selectedPackageId, ...this.onMyRoadPackageIds].filter(Boolean)));
      if (packageIds.length === 0) {
        return;
      }
      const userLanguage = navigator.languages && navigator.languages.length ? navigator.languages[0] : navigator.language || 'fr-FR';
      try {
        const hasPlannedRoute = ['personalRoute', 'directAddress'].includes(this.searchMode)
          && this.personalRoutePlan?.start
          && this.personalRoutePlan?.end
          && Array.isArray(this.personalRoutePlan?.packageIds)
          && this.personalRoutePlan.packageIds.length > 0;
        const url = hasPlannedRoute
          ? `${this.$i18n.t('rootURL')}${this.$i18n.t('reserveBatchPlannedPackageUrl')}?deliveryPersonID=${this.$store.state.connectedUser.id}&locale=${userLanguage}`
          : `${this.$i18n.t('rootURL')}${this.$i18n.t('reserveBatchPackageUrl')}?deliveryPersonID=${this.$store.state.connectedUser.id}&locale=${userLanguage}`;
        const payload = hasPlannedRoute
          ? {
            routePlan: {
              mode: this.personalRoutePlan.mode || this.searchMode,
              deliveryMode: this.currentDeliveryMode(),
              vehicleType: this.currentVehicleType(),
              selectedPackageId: this.selectedPackageId,
              start: this.personalRoutePlan.start,
              end: this.personalRoutePlan.end,
              packageIds: this.batchPackageIds.length ? this.batchPackageIds : this.personalRoutePlan.packageIds,
            },
          }
          : packageIds;
        const response = await http.put(url, payload);
        const reservedRoutePlan = response?.data?.reservedRoutePlan || null;
        const reservedIds = response?.data?.reservedPackageIds || [];
        if (reservedRoutePlan) {
          this.$store.commit('setOngoingDeliveryState', {
            isUserWithOngoingDelivery: true,
            activeDeliveryRoute: reservedRoutePlan,
          });
          window.dispatchEvent(new CustomEvent('qd-refresh-reservation-availability'));
          const navigationUrl = reservedRoutePlan.googleMapsNavigationUrl
            || (Array.isArray(reservedRoutePlan.googleMapsNavigationUrls) ? reservedRoutePlan.googleMapsNavigationUrls[0] : '');
          if (navigationUrl) {
            window.open(navigationUrl, '_blank', 'noopener');
          }
        }
        if (Array.isArray(reservedIds) && reservedIds.length > 0) {
          const reservedSet = new Set(reservedIds.map((id) => parseInt(id, 10)));
          this.personalRoutePlan = null;
          this.syncPackagesState(this.packagesList.filter((pkg) => !reservedSet.has(pkg.id)));
          this.onMyRoadPackageIds = this.onMyRoadPackageIds.filter((id) => !reservedSet.has(id));
          this.postToMap(JSON.stringify(this.packagesList));
          if (this.packagesList.length > 0) {
            this.displayDirection(0);
          } else if (this.searchMode === 'personalRoute' && this.personalRouteCriteria && this.positionData) {
            this.postPersonalRouteContext(this.personalRouteCriteria);
            this.postToMap(`OnMyDirection:${this.positionData.actuallatitude},${this.positionData.actuallongitude};${this.personalRouteCriteria.latitude},${this.personalRouteCriteria.longitude}`);
          }
        }
        window.dispatchEvent(new CustomEvent('qd-refresh-reservation-availability'));
      } catch (error) {
        console.error('Bulk reserve failed', error);
      }
    },
    async rebuildBatchRoutePlan() {
      if (!['personalRoute', 'directAddress'].includes(this.searchMode) || !this.batchSelectedPackages.length) {
        return;
      }
      const routeTarget = this.searchMode === 'personalRoute' ? this.personalRouteCriteria : this.addressCriteria;
      if (!routeTarget) {
        return;
      }
      const plannedRoute = await this.buildRoutePlanFromBackend(this.searchMode, this.batchSelectedPackages, routeTarget);
      if (plannedRoute) {
        this.personalRoutePlan = plannedRoute;
        this.syncPackagesState(this.packagesList);
        this.onMyRoadPackageIds = plannedRoute.packageIds || [];
        this.postOptimizedRoutePlan();
      }
    },
    displayDirection(index, options = {}) {
      const selectedPackage = this.packagesList?.[index];
      if (!selectedPackage || !Array.isArray(selectedPackage.addresses) || selectedPackage.addresses.length === 0) {
        return;
      }
      this.selectedPackageId = selectedPackage.id;
      if (!options.preserveDesktopWindow) {
        this.desktopSlideIndex = index;
      }
      this.mobileMapSlideIndex = index;
      this.postToMap(`SelectedPackage:${selectedPackage.id}`);
      if (['personalRoute', 'directAddress'].includes(this.searchMode) && this.personalRoutePlan) {
        this.onMyRoadPackageIds = this.personalRoutePlan.packageIds || [];
        this.postOptimizedRoutePlan();
        return;
      }
      this.onMyRoadPackageIds = [];
      let stringDeparture = "";
      let stringArrival = "";
      selectedPackage.addresses.forEach(address => {
        if (address.type === "DEPARTURE") {
          stringDeparture = address.latitude + "," + address.longitude;
        } else if (address.type === "ARRIVAL") {
          stringArrival = address.latitude + "," + address.longitude;
        }
      });
      if (!stringDeparture || !stringArrival) {
        return;
      }
      this.postToMap("SelectedDirection:" + stringDeparture + ";" + stringArrival);
    },
    getMapVue(){
        return this.$parent.$refs.mapVue;
    },
    getPackageModal(){
        return this.$parent.$refs.AppModal;
    },
    getCurrentLocation() {
        return new Promise((resolve, reject) => {
          if (navigator.geolocation) {
            navigator.geolocation.getCurrentPosition(
              (position) => {
                const positionData = {
                  actuallatitude: position.coords.latitude,
                  actuallongitude: position.coords.longitude,
                };
                resolve(positionData); // Résoudre la promesse avec les coordonnées
              },
              (error) => {
                reject('Unable to retrieve location');
                console.error('Unable to retrieve location', error);
              },
              {
                enableHighAccuracy: false,
                maximumAge: 15000,
                timeout: 8000,
              }
            );
          } else {
            reject('Geolocation not supported');
            console.error('Geolocation not supported');
          }
        });
      },
    normalizeCoordinate(value) {
      return Number.parseFloat(Number(value).toFixed(4));
    },
    normalizeViewportBounds(bounds) {
      return {
        minLat: this.normalizeCoordinate(bounds.minLat),
        maxLat: this.normalizeCoordinate(bounds.maxLat),
        minLng: this.normalizeCoordinate(bounds.minLng),
        maxLng: this.normalizeCoordinate(bounds.maxLng),
      };
    },
    async refreshPackagesList(positionData){
        if (this.refreshPackagesPromise) {
          return this.refreshPackagesPromise;
        }
        this.isLoadingPackages = true;
        this.isRouteLoading = false;
        this.loadError = false;
        this.searchMode = 'aroundMe';
        this.addressCriteria = null;
        this.personalRouteCriteria = null;
        this.refreshPackagesPromise = (async () => {
          this.postToMap(JSON.stringify(positionData));
          const latitude = this.normalizeCoordinate(positionData.actuallatitude);
          const longitude = this.normalizeCoordinate(positionData.actuallongitude);
          const url = this.$i18n.t('rootURL')
            + this.$i18n.t('getPackagesAroundMe')
            + latitude
            + '&longitude=' + longitude
            + '&rayonEnMetres=' + this.searchRadius
            + '&deliveryMode=' + encodeURIComponent(this.currentDeliveryMode());
          const response = await http.get(url, { silent: true });
          this.syncPackagesState(response.data);
          this.onMyRoadPackageIds = [];
          this.selectedPackageId = this.packagesList.length > 0 ? this.packagesList[0].id : null;
          this.desktopSlideIndex = 0;
          this.mobileMapSlideIndex = 0;
          this.postToMap(JSON.stringify(this.packagesList));
          if (this.packagesList.length > 0) {
            this.displayDirection(0);
          }
          this.hasCompletedInitialAroundMeLoad = true;
          if (this.mapViewport) {
            this.lastViewportPayload = this.mapViewport;
            this.lastViewportRequestKey = [
              this.mapViewport.bounds?.minLat,
              this.mapViewport.bounds?.maxLat,
              this.mapViewport.bounds?.minLng,
              this.mapViewport.bounds?.maxLng,
              this.mapViewport.zoom,
              this.searchRadius,
            ].join(':');
            this.lastViewportRefreshAt = Date.now();
          }
        })().catch((error) => {
          this.loadError = true;
          console.error('Unable to refresh packages list', error);
          this.syncPackagesState([]);
        }).finally(() => {
          this.isLoadingPackages = false;
          this.refreshPackagesPromise = null;
        });
        return this.refreshPackagesPromise;
    },
    async refreshPackagesListByViewport(viewport) {
      if (!viewport?.bounds || this.refreshPackagesPromise) {
        return this.refreshPackagesPromise;
      }
      this.isLoadingPackages = true;
      this.isRouteLoading = false;
      this.loadError = false;
      this.hasCompletedInitialAroundMeLoad = true;
      this.searchMode = 'aroundMe';
      this.addressCriteria = null;
      this.personalRouteCriteria = null;
      this.refreshPackagesPromise = (async () => {
        const bounds = this.normalizeViewportBounds(viewport.bounds);
        const centerLat = this.normalizeCoordinate(viewport.center?.lat ?? this.positionData?.actuallatitude);
        const centerLng = this.normalizeCoordinate(viewport.center?.lng ?? this.positionData?.actuallongitude);
        const params = new URLSearchParams({
          minLat: String(bounds.minLat),
          maxLat: String(bounds.maxLat),
          minLng: String(bounds.minLng),
          maxLng: String(bounds.maxLng),
          centerLat: String(centerLat),
          centerLng: String(centerLng),
          limit: this.isMobile ? '50' : '100',
          deliveryMode: this.currentDeliveryMode(),
        });
        const response = await http.get(`${this.$i18n.t('rootURL')}${this.$i18n.t('getPackagesInBounds')}${params.toString()}`, { silent: true });
          this.syncPackagesState(response.data);
          this.onMyRoadPackageIds = [];
          this.selectedPackageId = this.packagesList.length > 0 ? this.packagesList[0].id : null;
          this.desktopSlideIndex = 0;
          this.mobileMapSlideIndex = 0;
          this.postToMap(JSON.stringify(this.packagesList));
          if (this.packagesList.length > 0) {
            this.displayDirection(0);
          } else if (this.selectedPackageId) {
            this.syncSelectedRouteOnMap();
          }
        })().catch((error) => {
          this.loadError = true;
          console.error('Unable to refresh packages list from viewport', error);
          this.syncPackagesState([]);
        }).finally(() => {
          this.lastViewportRefreshAt = Date.now();
          this.isLoadingPackages = false;
          this.refreshPackagesPromise = null;
        });
      return this.refreshPackagesPromise;
    },
    async refreshPackagesListOnPersonalRoute(criteria) {
      this.isLoadingPackages = true;
      this.isRouteLoading = false;
      this.loadError = false;
      this.hasCompletedInitialAroundMeLoad = true;
      try {
        if (!this.positionData?.actuallatitude || !this.positionData?.actuallongitude) {
          this.positionData = await this.getCurrentLocation();
        }
        this.searchMode = 'personalRoute';
        this.personalRouteCriteria = criteria;
        const params = new URLSearchParams({
          departureLatitude: String(this.normalizeCoordinate(this.positionData.actuallatitude)),
          departureLongitude: String(this.normalizeCoordinate(this.positionData.actuallongitude)),
          arrivalLatitude: String(this.normalizeCoordinate(criteria.latitude)),
          arrivalLongitude: String(this.normalizeCoordinate(criteria.longitude)),
          deliveryMode: this.currentDeliveryMode(),
          vehicleType: this.currentVehicleType(),
          radiusMeters: String(this.searchRadius),
        });
        const url = `${this.$i18n.t('rootURL')}packages-on-my-road?${params.toString()}`;
        const response = await http.get(url, { silent: true });
        this.batchPackageCount = Math.max(1, Array.isArray(response.data) ? response.data.length : 1);
        this.personalRoutePlan = await this.buildRoutePlanFromBackend('personalRoute', response.data, criteria);
        this.syncPackagesState(response.data);
        this.onMyRoadPackageIds = this.packagesList.map((pkg) => pkg.id);
        this.selectedPackageId = this.packagesList.length > 0 ? this.packagesList[0].id : null;
        this.desktopSlideIndex = 0;
        this.mobileMapSlideIndex = 0;
        this.postToMap(JSON.stringify(this.packagesList));
        this.postPersonalRouteContext(criteria);
        this.postOptimizedRoutePlan();
      } catch (error) {
        this.loadError = true;
        console.error('Unable to refresh packages list on personal route', error);
        this.syncPackagesState([]);
        this.onMyRoadPackageIds = [];
      } finally {
        this.isLoadingPackages = false;
      }
    },
    async refreshPackagesListDirect(criteria) {
      this.isLoadingPackages = true;
      this.isRouteLoading = false;
      this.loadError = false;
      this.hasCompletedInitialAroundMeLoad = true;
      try {
        if (!this.positionData?.actuallatitude || !this.positionData?.actuallongitude) {
          this.positionData = await this.getCurrentLocation();
        }
        this.searchMode = 'directAddress';
        this.addressCriteria = criteria;
        this.personalRouteCriteria = null;
        this.personalRoutePlan = null;
        const params = new URLSearchParams({
          latitude: String(this.normalizeCoordinate(this.positionData.actuallatitude)),
          longitude: String(this.normalizeCoordinate(this.positionData.actuallongitude)),
          line1: criteria.rawAddress || '',
          zipCode: criteria.zipCode || '',
          town: criteria.town || '',
          country: criteria.country || '',
          rayonEnMetres: String(this.searchRadius),
          deliveryMode: this.currentDeliveryMode(),
          vehicleType: this.currentVehicleType(),
          destinationLatitude: String(this.normalizeCoordinate(criteria.latitude)),
          destinationLongitude: String(this.normalizeCoordinate(criteria.longitude)),
        });
        const url = `${this.$i18n.t('rootURL')}${this.$i18n.t('getPackagesAroundMeByDestination')}${params.toString()}`;
        const response = await http.get(url, { silent: true });
        this.batchPackageCount = Math.max(1, Array.isArray(response.data) ? response.data.length : 1);
        this.personalRoutePlan = await this.buildRoutePlanFromBackend('directAddress', response.data, criteria);
        this.syncPackagesState(response.data);
        this.onMyRoadPackageIds = [];
        this.selectedPackageId = this.packagesList.length > 0 ? this.packagesList[0].id : null;
        this.desktopSlideIndex = 0;
        this.mobileMapSlideIndex = 0;
        this.postToMap(JSON.stringify(this.positionData));
        this.postToMap(JSON.stringify(this.packagesList));
        this.postDirectRouteContext(criteria);
        this.postOptimizedRoutePlan();
        if (this.packagesList.length > 0) {
          this.displayDirection(0);
        }
      } catch (error) {
        this.loadError = true;
        console.error('Unable to refresh packages list for direct search', error);
        this.syncPackagesState([]);
        this.onMyRoadPackageIds = [];
      } finally {
        this.isLoadingPackages = false;
      }
    },
    removePackageFromListe(packageId){
      const simpleList = JSON.parse(JSON.stringify(this.packagesList));
      const index = simpleList.findIndex(pkg => pkg.id === parseInt(packageId, 10));
      if (index !== -1) {
        simpleList.splice(index, 1); // Supprime 1 élément à l'index trouvé
      }
      this.syncPackagesState(simpleList);
      if (this.selectedPackageId === parseInt(packageId, 10)) {
        this.selectedPackageId = this.packagesList.length > 0 ? this.packagesList[0].id : null;
      }
      if (this.desktopSlideIndex >= this.packagesList.length) {
        this.desktopSlideIndex = Math.max(0, this.packagesList.length - 1);
      }
      if (this.mobileMapSlideIndex >= this.packagesList.length) {
        this.mobileMapSlideIndex = Math.max(0, this.packagesList.length - 1);
      }
      this.postToMap(JSON.stringify(simpleList));
      this.syncSelectedRouteOnMap();
    },
  },
}
</script>

<style>
.carousel-wrapper {
  width: 100%;
  max-width: 100%;
  box-sizing: border-box;
  height: 100%;
  display: flex;
  flex-direction: column;
  gap: 10px;
  min-width: 0;
  overflow-x: hidden;
}
.mobile-mode-switch {
  display: none;
}
.mode-btn {
  flex: 1;
  height: 34px;
  border: 1px solid #d9dfeb;
  border-radius: 10px;
  background: #ffffff;
  color: #334155;
  font-size: 12px;
  font-weight: 600;
}
.mode-btn.active {
  background: #0f172a;
  color: #ffffff;
  border-color: #0f172a;
}
.carousel-state {
  padding: 10px;
  border-radius: 8px;
  background: #f1f5f9;
  border: 1px solid #e2e8f0;
  color: #334155;
  text-align: center;
  font-size: 12px;
}
.carousel-state.error {
  background: #fef2f2;
  border-color: #fecaca;
  color: #b91c1c;
}
.active-route-banner {
  display: flex;
  align-items: center;
  justify-content: space-between;
  gap: 16px;
  padding: 16px 24px !important;
  margin-bottom: 20px;
}
.banner-text {
  font-weight: 700;
  color: var(--qd-primary);
}
.banner-actions {
  display: flex;
  gap: 12px;
}
.packages-panel {
  display: flex;
  flex-direction: column;
  height: 100%;
  min-height: 0;
  border-radius: 14px;
  background: #f8fafc;
  border: 1px solid #e5e7eb;
}
.panel-header.horizontal-stack {
  display: flex;
  flex-direction: column;
  align-items: flex-start;
  padding: 20px 24px;
  background: #ffffff;
  border-bottom: 1px solid var(--qd-border);
  gap: 20px;
}

.title-row {
  display: flex;
  align-items: center;
  gap: 16px;
  margin-bottom: 4px;
}

.title-row h3 {
  margin: 0;
}

.header-main-info.vertical-header p {
  margin: 0;
  opacity: 0.7;
}

.header-main-info h3 {
  margin: 0;
  font-size: 1.4rem;
  font-weight: 800;
  color: var(--qd-primary-dark);
}

.header-main-info p {
  margin: 4px 0 10px;
  font-size: 0.9rem;
  color: var(--qd-muted);
}

.count-chip {
  display: inline-flex;
  padding: 4px 12px;
  background: var(--qd-primary-soft);
  color: var(--qd-primary);
  border-radius: 99px;
  font-size: 0.75rem;
  font-weight: 800;
  text-transform: uppercase;
  letter-spacing: 0.05em;
}

/* Batch Control Card */
.batch-control-card {
  align-self: stretch;
  max-width: none;
  width: 100%;
  padding: 24px 28px;
  border-radius: 20px;
  display: flex;
  flex-direction: row;
  flex-wrap: wrap;
  align-items: center;
  gap: 16px;
  background: rgba(255, 255, 255, 0.6) !important;
}

.batch-actions-side {
  display: flex;
  flex-direction: column;
  gap: 10px;
  min-width: 0;
  flex: 1 1 220px;
  align-items: stretch;
}

.batch-reserve-btn {
  display: flex;
  align-items: center;
  justify-content: center;
  gap: 8px;
  width: 100%;
  padding: 10px 20px !important;
  font-size: 0.9rem !important;
  border-radius: 999px !important;
}


.mission-status-card {
  flex-shrink: 0;
  height: auto;
  min-height: fit-content;
  margin: 0 0 16px 0;
  padding: 18px 20px;
  background: linear-gradient(135deg, #4f46e5 0%, #06b6d4 100%) !important;
  color: #ffffff;
  border-radius: 20px;
  box-shadow: 0 12px 32px rgba(79, 70, 229, 0.35);
  display: flex;
  flex-direction: column;
  gap: 16px;
  border: 1px solid rgba(255, 255, 255, 0.3) !important;
  animation: slide-in-top 0.4s cubic-bezier(0.175, 0.885, 0.32, 1.275);
  z-index: 10;
}

@keyframes slide-in-top {
  from { transform: translateY(-10px); opacity: 0; }
  to { transform: translateY(0); opacity: 1; }
}

.mission-info {
  display: flex;
  align-items: center;
}

.mission-badge {
  display: flex;
  align-items: center;
  gap: 8px;
  background: rgba(255, 255, 255, 0.2);
  padding: 6px 14px;
  border-radius: 99px;
  border: 1px solid rgba(255, 255, 255, 0.3);
}

.badge-text {
  font-size: 0.85rem;
  font-weight: 700;
  letter-spacing: 0.02em;
}

.pulse-dot {
  width: 8px;
  height: 8px;
  background: #22c55e;
  border-radius: 50%;
  box-shadow: 0 0 0 0 rgba(34, 197, 94, 0.7);
  animation: pulse-green 2s infinite;
}

@keyframes pulse-green {
  0% { transform: scale(0.95); box-shadow: 0 0 0 0 rgba(34, 197, 94, 0.7); }
  70% { transform: scale(1); box-shadow: 0 0 0 10px rgba(34, 197, 94, 0); }
  100% { transform: scale(0.95); box-shadow: 0 0 0 0 rgba(34, 197, 94, 0); }
}

.mission-actions {
  display: flex;
  gap: 10px;
}

.mission-btn {
  flex: 1;
  display: flex;
  align-items: center;
  justify-content: center;
  gap: 8px;
  height: 46px;
  border-radius: 14px;
  font-weight: 800;
  font-size: 0.9rem;
  transition: all 0.25s cubic-bezier(0.4, 0, 0.2, 1);
  border: none;
  cursor: pointer;
  text-decoration: none;
}

.mission-btn.primary {
  background: #ffffff;
  color: #4f46e5;
  box-shadow: 0 4px 12px rgba(0, 0, 0, 0.1);
}

.mission-btn.secondary {
  background: rgba(255, 255, 255, 0.15);
  color: #ffffff;
  border: 1px solid rgba(255, 255, 255, 0.2);
}

.mission-btn:hover {
  transform: translateY(-2px);
  filter: brightness(1.1);
  box-shadow: 0 6px 16px rgba(0, 0, 0, 0.15);
}

.mission-btn.primary {
  background: #ffffff;
  color: #4f46e5;
}

.mission-btn.secondary {
  background: rgba(255, 255, 255, 0.15);
  color: #ffffff;
  border: 1px solid rgba(255, 255, 255, 0.3);
}

.mission-btn:hover {
  transform: translateY(-2px);
  filter: brightness(1.1);
}

.mission-btn:active {
  transform: translateY(0);
}

@media screen and (max-width: 767px) {
  .mission-status-card {
    margin: -6px -6px 12px -6px;
    border-radius: 0 0 20px 20px;
    padding: 18px 16px;
  }
}

.batch-alert {
  display: flex;
  align-items: center;
  gap: 10px;
  padding: 10px 14px;
  background: #fffbeb;
  border: 1px solid #fef3c7;
  border-radius: 12px;
  color: #92400e;
}

.batch-alert .material-symbols-outlined {
  font-size: 1.2rem;
  color: #d97706;
}

.batch-alert p {
  margin: 0;
  font-size: 0.8rem;
  font-weight: 600;
}

.batch-config {
  display: flex;
  flex-direction: column;
  gap: 14px;
  width: 100%;
  min-width: 0;
  flex: 1 1 280px;
}

.batch-slider-wrap {
  display: flex;
  flex-direction: column;
  gap: 8px;
  width: 100%;
  min-width: 0;
}

.slider-labels {
  display: flex;
  justify-content: space-between;
  align-items: center;
  font-size: 0.8rem;
  font-weight: 700;
  color: var(--qd-text);
}

.selection-count {
  font-size: 0.9rem;
  color: var(--qd-primary);
}

/* Custom Range Input */
input[type=range] {
  -webkit-appearance: none;
  width: 100%;
  height: 8px;
  background: #e2e8f0;
  border-radius: 10px;
  outline: none;
  cursor: pointer;
}

input[type=range]::-webkit-slider-thumb {
  -webkit-appearance: none;
  width: 22px;
  height: 22px;
  background: var(--qd-primary);
  border: 3px solid #fff;
  border-radius: 50%;
  box-shadow: 0 4px 10px rgba(79, 70, 229, 0.3);
  transition: transform 0.2s cubic-bezier(0.175, 0.885, 0.32, 1.275);
}

input[type=range]:active::-webkit-slider-thumb {
  transform: scale(1.2);
}

/* Metrics Grid */
.metrics-grid {
  display: grid;
  grid-template-columns: repeat(5, 1fr);
  gap: 4px;
}

.metric-item {
  display: flex;
  align-items: center;
  justify-content: center;
  gap: 4px;
  padding: 6px 4px;
  background: #f8fafc;
  border: 1px solid var(--qd-border);
  border-radius: 10px;
  font-size: 0.72rem;
  font-weight: 700;
  color: var(--qd-text);
  white-space: nowrap;
}

.metric-item.highlight {
  border-color: rgba(255, 255, 255, 0.5);
}

.metric-item.success { background: var(--qd-success-soft); color: var(--qd-success); }
.metric-item.warning { background: var(--qd-warning-soft); color: var(--qd-warning); }
.metric-item.info { background: var(--qd-primary-soft); color: var(--qd-primary); }

.metric-item .material-symbols-outlined {
  font-size: 1.1rem;
  opacity: 0.8;
}

/* Mobile Bar */
.mobile-batch-bar {
  display: none;
  position: relative;
  z-index: 50;
  padding: 16px;
  border-radius: 20px 20px 0 0;
  margin: -10px -6px 10px -6px;
}

.mobile-batch-bar .batch-config.mobile {
  gap: 16px;
  width: 100%;
}

.mobile-batch-bar .metrics-grid {
  grid-template-columns: repeat(3, 1fr);
}

@media screen and (max-width: 1024px) {
  .panel-header {
    flex-direction: column;
    align-items: stretch;
  }
  .batch-control-card {
    max-width: none;
    flex-direction: column;
    padding: 16px;
  }
}
.packages-list {
  display: flex;
  flex-direction: column;
  gap: 10px;
  padding: 12px;
  overflow-y: auto;
  min-height: 0;
}
.vertical-carousel {
  display: flex;
  flex-direction: column;
  gap: 8px;
  padding: 10px;
  height: 100%;
  min-height: 0;
}
.vertical-slide-window {
  flex: 1;
  min-height: 0;
  display: grid;
  grid-template-rows: repeat(2, minmax(190px, 1fr));
  gap: 8px;
  align-content: start;
}
.v-nav {
  display: inline-flex;
  align-items: center;
  justify-content: center;
  height: 36px;
  border: 1px solid #d7deea;
  border-radius: 10px;
  background: #ffffff;
  color: #1e293b;
  cursor: pointer;
}
.v-nav:disabled {
  opacity: 0.5;
  cursor: not-allowed;
}
.v-counter {
  text-align: center;
  font-size: 12px;
  font-weight: 600;
  color: #64748b;
}
.package-item {
  border-radius: 14px;
  transition: box-shadow 0.2s ease;
  min-height: 0;
}
.package-item:hover {
  box-shadow: 0 6px 16px rgba(15, 23, 42, 0.06);
}
.package-item.selected {
  box-shadow: none;
}
.mobile-list {
  display: flex;
  flex-direction: column;
  gap: 10px;
  padding-right: 2px;
  width: 100%;
  max-width: 100%;
  min-width: 0;
  overflow-x: clip;
}
.mobile-map-carousel {
  padding: 2px 0 4px;
  width: 100%;
  max-width: 100%;
  overflow: hidden;
}
.mobile-map-item {
  width: 100%;
  border-radius: 14px;
  padding: 0 2px 2px;
  min-width: 0;
  box-sizing: border-box;
}
.mobile-map-item.selected {
  box-shadow: 0 0 0 2px #93a7cf inset;
}
@media screen and (min-width: 768px) {
  .carousel-wrapper {
    height: 100%;
  }
}
@media screen and (max-width: 767px) {
  .carousel-wrapper {
    background: rgba(255, 255, 255, 0.84);
    border-radius: 12px;
    padding: 6px;
    border: 1px solid rgba(226, 232, 240, 0.8);
    box-shadow: 0 8px 20px rgba(15, 23, 42, 0.08);
  }
  /* Premium Segmented Control for Mobile */
  .segmented-control {
    position: relative;
    height: 48px;
    display: flex;
    background: #f1f5f9;
    border-radius: 14px;
    padding: 4px;
    box-sizing: border-box;
    margin-bottom: 12px;
  }

  .segmented-control-bg {
    position: absolute;
    top: 4px;
    left: 4px;
    width: calc(50% - 4px);
    height: calc(100% - 8px);
    background: #fff;
    border-radius: 11px;
    box-shadow: 0 4px 12px rgba(0, 0, 0, 0.05);
    transition: transform 0.3s cubic-bezier(0.4, 0, 0.2, 1);
  }

  .segmented-control-bg.at-right {
    transform: translateX(100%);
  }

  .segment-btn {
    position: relative;
    flex: 1;
    display: flex;
    align-items: center;
    justify-content: center;
    gap: 8px;
    border: none;
    background: none;
    padding: 0 16px;
    font-weight: 700;
    font-size: 0.85rem;
    color: #64748b;
    cursor: pointer;
    z-index: 1;
    transition: color 0.3s;
  }

  .segment-btn.active {
    color: #0f172a;
  }

  .segment-btn .material-symbols-outlined {
    font-size: 20px;
  }

  .mobile-batch-bar {
    display: flex;
    justify-content: stretch;
  }
  .mobile-map-card {
    width: 100%;
    max-width: 100%;
    min-width: 0;
    overflow: hidden;
    box-sizing: border-box;
  }
  .mobile-map-carousel {
    width: 100%;
    max-width: 100%;
    min-width: 0;
    box-sizing: border-box;
    overflow: hidden;
  }
  :deep(.mobile-map-carousel .carousel) {
    width: 100%;
    max-width: 100%;
    min-width: 0;
    box-sizing: border-box;
    overflow: hidden;
  }
  :deep(.mobile-map-carousel .carousel__viewport) {
    width: 100%;
    max-width: 100%;
    min-width: 0;
    overflow: hidden;
  }
  :deep(.mobile-map-carousel .carousel__track) {
    min-width: 0;
    max-width: 100%;
    align-items: stretch;
    width: 100%;
  }
  :deep(.mobile-map-carousel .carousel__slide) {
    width: 100% !important;
    max-width: 100% !important;
    min-width: 0 !important;
    box-sizing: border-box;
    flex: 0 0 100%;
  }
  :deep(.mobile-map-carousel .carousel__slide) {
    padding: 0;
  }
  :deep(.mobile-map-carousel .carousel__prev),
  :deep(.mobile-map-carousel .carousel__next) {
    width: 28px;
    height: 28px;
    border-radius: 999px;
    background: rgba(15, 23, 42, 0.82);
    color: #ffffff;
    border: none;
  }
  :deep(.mobile-map-carousel .carousel__prev) {
    left: 6px;
  }
  :deep(.mobile-map-carousel .carousel__next) {
    right: 6px;
  }
  .carousel-state {
    margin-bottom: 0;
  }
}

@media screen and (max-width: 420px) {
  .carousel-wrapper {
    padding: 5px;
    border-radius: 10px;
  }

  .mobile-mode-switch {
    gap: 6px;
  }

  .mode-btn {
    height: 36px;
    font-size: 12px;
  }

  :deep(.mobile-map-carousel .carousel__prev),
  :deep(.mobile-map-carousel .carousel__next) {
    width: 26px;
    height: 26px;
  }
}
</style>
