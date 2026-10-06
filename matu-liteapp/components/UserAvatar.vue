<template>
  <image
    class="avatar"
    :src="failed || !src ? '/static/avatar.png' : src"
    mode="aspectFill"
    @error="failed = true"
  />
</template>
<script setup>
import { ref, watch } from 'vue'
const props = defineProps({ src: String })
const failed = ref(false)
watch(
  () => props.src,
  () => {
    failed.value = false
  }
)
</script>
