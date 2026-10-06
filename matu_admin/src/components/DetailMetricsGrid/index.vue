<script setup lang="ts">
defineProps<{
  items: Array<{
    label: string
    value: string | number
  }>
}>()

import './index.scss'
</script>

<template>
  <div class="detail-metrics-grid">
    <div v-for="item in items" :key="item.label" class="detail-metrics-grid__item">
      <span>{{ item.label }}</span>
      <strong>{{ item.value }}</strong>
    </div>
  </div>
</template>
