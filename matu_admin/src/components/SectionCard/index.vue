<script setup lang="ts">
defineProps<{
  title?: string
  description?: string
  bodyClass?: string
}>()

import './index.scss'
</script>

<template>
  <el-card class="section-card" shadow="never">
    <div v-if="title || description" class="section-card__header">
      <h3 v-if="title">{{ title }}</h3>
      <p v-if="description">{{ description }}</p>
    </div>
    <div :class="bodyClass">
      <slot />
    </div>
  </el-card>
</template>
