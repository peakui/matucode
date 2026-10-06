<script setup lang="ts">
import { computed } from 'vue'
import MarkdownIt from 'markdown-it'

const props = defineProps<{
  content?: string | null
}>()

const md = new MarkdownIt({
  html: false,
  breaks: true,
  linkify: true,
})

const renderedHtml = computed(() => md.render(props.content ?? ''))

import './index.scss'
</script>

<template>
  <div class="markdown-content" v-html="renderedHtml" />
</template>
