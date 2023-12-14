<template>
  <div id="app">
    <ComponentWithMap />
  </div>
</template>

<script>
  import ComponentWithMap from './components/ComponentWithMapFromHTML.vue'

export default {
  name: 'App',
  components: {
    ComponentWithMap
  }
}
</script>