<template>
  <view class="content-body"
    ><mp-html
      :content="html"
      :domain="domain"
      :tag-style="tagStyle"
      :selectable="true"
      :scroll-table="true"
      :preview-img="true"
      :copy-link="false"
      @linktap="linkTap"
  /></view>
</template>
<script setup>
import { computed } from 'vue'
import MarkdownIt from 'markdown-it'
import MpHtml from 'mp-html/dist/uni-app/components/mp-html/mp-html.vue'
import { API_BASE_URL } from '../config/index.js'
const props = defineProps({ content: { type: String, default: '' } })
const md = new MarkdownIt({ html: false, breaks: true, linkify: true })
const domain = API_BASE_URL
// mp-html discards executable tags. Markdown is rendered with raw HTML disabled.
const html = computed(() =>
  /<(p|div|h[1-6]|ul|ol|pre|blockquote|img|table)(\s|>)/i.test(props.content)
    ? props.content
    : md.render(props.content)
)
const tagStyle = {
  p: 'margin:0 0 16px;line-height:1.9;word-break:break-word;',
  img: 'max-width:100%;height:auto;border-radius:10px;',
  pre: 'background:#f1f4f8;padding:14px;border-radius:10px;overflow:auto;white-space:pre;font-size:13px;',
  code: 'font-family:monospace;background:#f1f4f8;',
  h1: 'font-size:22px;',
  h2: 'font-size:20px;',
  blockquote: 'border-left:3px solid #2563eb;padding-left:12px;color:#7a8697;',
  table: 'border-collapse:collapse;font-size:13px;',
  td: 'border:1px solid #e9edf2;padding:8px;',
}
function linkTap(event) {
  if (!/^https?:\/\//i.test(event.href || '')) return
  uni.showModal({
    title: '外部链接',
    content: '复制链接后可在浏览器中查看',
    confirmText: '复制链接',
    success: (res) => {
      if (res.confirm) uni.setClipboardData({ data: event.href })
    },
  })
}
</script>
<style scoped>
.content-body {
  font-size: 29rpx;
  line-height: 1.9;
  overflow: hidden;
  word-break: break-word;
}
</style>
