<script setup lang="ts">
defineProps<{
  title: string
  description: string
  actionText?: string
}>()

defineEmits<{
  action: []
}>()

import './index.scss'
</script>

<template>
  <div class="page-section-header">
    <div>
      <h2>{{ title }}</h2>
      <p>{{ description }}</p>
    </div>
    <el-button v-if="actionText" type="primary" size="large" @click="$emit('action')">{{
      actionText
    }}</el-button>
  </div>
</template>
