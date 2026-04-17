<template>
  <div class="address-book-search">
    <label :for="inputId">{{ $t('addressBookSearchLabel') }}</label>
    <div class="search-input-wrap">
      <span class="material-symbols-outlined">person_search</span>
      <input
        :id="inputId"
        v-model="query"
        type="search"
        autocomplete="off"
        :placeholder="$t('addressBookSearchPlaceholder')"
        @focus="isOpen = true"
      >
    </div>

    <div v-if="isOpen && canShowPanel" class="search-results">
      <button
        v-for="entry in results"
        :key="entry.id"
        class="search-result"
        type="button"
        @click="selectEntry(entry)"
      >
        <strong>{{ fullName(entry) }}</strong>
        <span>{{ entry.email }}</span>
        <small>{{ compactAddress(entry) }}</small>
      </button>

      <p v-if="!isLoading && hasSearched && !results.length" class="search-empty">
        {{ $t('addressBookNoResult') }}
      </p>
      <p v-if="isLoading" class="search-empty">
        {{ $t('addressBookSearching') }}
      </p>
    </div>
  </div>
</template>

<script>
import http from '@/config/httpInterceptor';

export default {
  emits: ['recipient-selected'],
  props: {
    ownerUserId: {
      type: [Number, String],
      required: true,
    },
    inputId: {
      type: String,
      default: 'address-book-recipient-search',
    },
  },
  data() {
    return {
      query: '',
      results: [],
      searchTimer: null,
      searchSequence: 0,
      isLoading: false,
      hasSearched: false,
      isOpen: false,
    };
  },
  computed: {
    canShowPanel() {
      return this.query.trim().length >= 2;
    },
  },
  watch: {
    query() {
      this.scheduleSearch();
    },
  },
  beforeUnmount() {
    if (this.searchTimer) {
      clearTimeout(this.searchTimer);
    }
  },
  methods: {
    scheduleSearch() {
      if (this.searchTimer) {
        clearTimeout(this.searchTimer);
      }
      const query = this.query.trim();
      if (query.length < 2) {
        this.results = [];
        this.hasSearched = false;
        this.isLoading = false;
        return;
      }
      this.searchTimer = setTimeout(() => {
        this.search(query);
      }, 280);
    },
    async search(query) {
      if (!this.ownerUserId) {
        return;
      }
      const sequence = ++this.searchSequence;
      this.isLoading = true;
      this.hasSearched = true;
      try {
        const response = await http.get(
          `${this.$i18n.t('userRootURL')}address-book?ownerUserId=${encodeURIComponent(this.ownerUserId)}&q=${encodeURIComponent(query)}&limit=10`,
          { silent: true },
        );
        if (sequence === this.searchSequence) {
          this.results = Array.isArray(response?.data) ? response.data : [];
        }
      } catch (error) {
        if (sequence === this.searchSequence) {
          this.results = [];
        }
        console.warn('Unable to search address book:', error);
      } finally {
        if (sequence === this.searchSequence) {
          this.isLoading = false;
        }
      }
    },
    selectEntry(entry) {
      this.query = this.fullName(entry);
      this.isOpen = false;
      this.$emit('recipient-selected', entry);
    },
    fullName(entry) {
      return [entry?.firstName, entry?.lastName].filter(Boolean).join(' ') || entry?.email || '';
    },
    compactAddress(entry) {
      return entry?.addressAuto || [entry?.line1, entry?.zipCode, entry?.town, entry?.country]
        .filter(Boolean)
        .join(', ');
    },
  },
};
</script>

<style scoped>
.address-book-search {
  position: relative;
  display: grid;
  gap: 8px;
  margin-bottom: 18px;
}

.address-book-search label {
  font-weight: 700;
  color: #24364f;
}

.search-input-wrap {
  position: relative;
}

.search-input-wrap .material-symbols-outlined {
  position: absolute;
  left: 14px;
  top: 50%;
  transform: translateY(-50%);
  color: #2a6fcf;
  font-size: 1.1rem;
  pointer-events: none;
}

.search-input-wrap input {
  width: 100%;
  min-height: 48px;
  padding: 0 14px 0 42px;
  border: 1px solid #ced7e4;
  border-radius: 8px;
  background: #fff;
  box-sizing: border-box;
}

.search-results {
  position: absolute;
  z-index: 8;
  top: calc(100% + 4px);
  left: 0;
  right: 0;
  display: grid;
  gap: 4px;
  max-height: 260px;
  overflow: auto;
  padding: 8px;
  border: 1px solid #d7dfeb;
  border-radius: 8px;
  background: #fff;
  box-shadow: 0 14px 28px rgba(24, 39, 75, 0.14);
}

.search-result {
  display: grid;
  gap: 3px;
  width: 100%;
  padding: 10px;
  border: 0;
  border-radius: 8px;
  background: transparent;
  color: #24364f;
  text-align: left;
  cursor: pointer;
}

.search-result:hover,
.search-result:focus {
  background: #edf4ff;
}

.search-result span,
.search-result small,
.search-empty {
  color: #617086;
}

.search-empty {
  margin: 0;
  padding: 10px;
}
</style>
