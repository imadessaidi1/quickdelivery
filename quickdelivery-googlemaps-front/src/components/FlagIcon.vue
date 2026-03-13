<template>
  <span class="flag-icon" aria-hidden="true">
    <svg v-if="countryCode === 'FR'" viewBox="0 0 24 16">
      <rect width="8" height="16" fill="#0055A4" />
      <rect x="8" width="8" height="16" fill="#FFFFFF" />
      <rect x="16" width="8" height="16" fill="#EF4135" />
    </svg>

    <svg v-else-if="countryCode === 'BE'" viewBox="0 0 24 16">
      <rect width="8" height="16" fill="#000000" />
      <rect x="8" width="8" height="16" fill="#FFD90C" />
      <rect x="16" width="8" height="16" fill="#EF3340" />
    </svg>

    <svg v-else-if="countryCode === 'CH'" viewBox="0 0 24 16">
      <rect width="24" height="16" fill="#DA291C" />
      <rect x="10" y="3" width="4" height="10" fill="#FFFFFF" />
      <rect x="7" y="6" width="10" height="4" fill="#FFFFFF" />
    </svg>

    <svg v-else-if="countryCode === 'DE'" viewBox="0 0 24 16">
      <rect width="24" height="5.33" fill="#000000" />
      <rect y="5.33" width="24" height="5.34" fill="#DD0000" />
      <rect y="10.67" width="24" height="5.33" fill="#FFCE00" />
    </svg>

    <svg v-else-if="countryCode === 'ES'" viewBox="0 0 24 16">
      <rect width="24" height="16" fill="#AA151B" />
      <rect y="4" width="24" height="8" fill="#F1BF00" />
    </svg>

    <svg v-else-if="countryCode === 'GB'" viewBox="0 0 24 16">
      <rect width="24" height="16" fill="#012169" />
      <path d="M0 0l24 16M24 0L0 16" stroke="#FFF" stroke-width="3" />
      <path d="M0 0l24 16M24 0L0 16" stroke="#C8102E" stroke-width="1.5" />
      <path d="M12 0v16M0 8h24" stroke="#FFF" stroke-width="5" />
      <path d="M12 0v16M0 8h24" stroke="#C8102E" stroke-width="3" />
    </svg>

    <svg v-else-if="countryCode === 'IT'" viewBox="0 0 24 16">
      <rect width="8" height="16" fill="#009246" />
      <rect x="8" width="8" height="16" fill="#FFFFFF" />
      <rect x="16" width="8" height="16" fill="#CE2B37" />
    </svg>

    <svg v-else-if="countryCode === 'MA'" viewBox="0 0 24 16">
      <rect width="24" height="16" fill="#C1272D" />
      <path d="M12 4.8l1.4 4.2H9l3.6-2.6-1.4 4.1z" fill="none" stroke="#006233" stroke-width="1.05" />
    </svg>

    <svg v-else-if="countryCode === 'PT'" viewBox="0 0 24 16">
      <rect width="10" height="16" fill="#006600" />
      <rect x="10" width="14" height="16" fill="#FF0000" />
      <circle cx="10" cy="8" r="2.5" fill="#FFCC00" />
    </svg>

    <svg v-else-if="countryCode === 'US'" viewBox="0 0 24 16">
      <rect width="24" height="16" fill="#B22234" />
      <path d="M0 2h24M0 4h24M0 6h24M0 8h24M0 10h24M0 12h24M0 14h24" stroke="#FFF" stroke-width="1.2" />
      <rect width="10" height="7" fill="#3C3B6E" />
    </svg>

    <svg v-else viewBox="0 0 24 16">
      <rect width="8" height="16" fill="#0055A4" />
      <rect x="8" width="8" height="16" fill="#FFFFFF" />
      <rect x="16" width="8" height="16" fill="#EF4135" />
    </svg>
  </span>
</template>

<script>
export default {
  props: {
    countryCode: {
      type: String,
      required: true,
    },
  },
};
</script>

<style scoped>
.flag-icon {
  display: inline-flex;
  align-items: center;
  justify-content: center;
  width: 24px;
  height: 16px;
  flex: 0 0 24px;
  overflow: hidden;
  border-radius: 3px;
  box-shadow: 0 0 0 1px rgba(15, 23, 42, 0.08);
}

.flag-icon svg {
  display: block;
  width: 24px;
  height: 16px;
}
</style>
