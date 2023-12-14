<template>
<div>
    <div v-html="htmlFileText">
    </div>
  </div>
</template>

<script>
 import { ref, onMounted } from "vue";
  import htmlFileContent from './google-maps.js'

  const htmlFileText = ref('')

  onMounted(() => {
    htmlFileText.value = htmlFileContent
  })
</script>

