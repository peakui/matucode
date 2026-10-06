<template>
  <view class="card feed-card" @tap="open">
    <view class="row author"
      ><UserAvatar :src="avatarOf(item)" /><view class="grow"
        ><view class="name">{{ nameOf(item) }}</view
        ><view v-if="badges.length" class="identity"
          ><text v-for="b in badges" :key="b.key" class="badge" :class="b.color">{{ b.label }}</text></view
        ><view class="muted date">{{ shortDate(item.createdAt || item.checkDate) }}</view></view
      ><text v-if="type === 'qa'" class="badge" :class="item.status === 1 ? 'green' : 'gray'">{{
        item.status === 1 ? '已解决' : item.status === 2 ? '已关闭' : '待解答'
      }}</text
      ><text v-else-if="type === 'check'" class="badge green">学习打卡</text></view
    >
    <view class="row article"
      ><view class="grow"
        ><view class="list-title">{{ item.title || item.recordTitle || '学习记录' }}</view
        ><view v-if="summary" class="summary">{{ summary }}</view></view
      ><image v-if="cover" class="cover" :src="cover" mode="aspectFill"
    /></view>
    <view class="row between meta"
      ><view class="tags"
        ><text v-if="item.categoryName" class="tag">{{ item.categoryName }}</text
        ><text v-if="item.learnHours" class="tag">学习 {{ item.learnHours }}h</text></view
      ><text>{{
        type === 'qa'
          ? (item.answerCount || 0) + ' 回答'
          : (item.likeCount || 0) + ' 赞 · ' + (item.commentCount || 0) + ' 评论'
      }}</text></view
    >
  </view>
</template>
<script setup>
import { computed } from 'vue'
import UserAvatar from './UserAvatar.vue'
import { nameOf, avatarOf, idOf, shortDate, plain, assetUrl, identityBadges } from '../utils/format.js'
const props = defineProps({
  item: { type: Object, required: true },
  type: { type: String, default: 'article' },
})
const badges = computed(() => identityBadges(props.item))
const summary = computed(() => plain(props.item.summary || props.item.content).slice(0, 100))
const cover = computed(() => assetUrl(props.item.coverImage || props.item.imageUrls?.[0]))
function open() {
  uni.navigateTo({
    url: `/pages/content/detail?type=${props.type}&id=${encodeURIComponent(idOf(props.item))}`,
  })
}
</script>
<style scoped>
.author {
  margin-bottom: 24rpx;
}
.name {
  font-size: 25rpx;
  font-weight: 550;
}
.date {
  font-size: 21rpx;
}
.article {
  align-items: flex-start;
  gap: 22rpx;
}
.summary {
  color: #7f8998;
  font-size: 25rpx;
  margin-top: 12rpx;
  display: -webkit-box;
  -webkit-line-clamp: 2;
  -webkit-box-orient: vertical;
  overflow: hidden;
}
.cover {
  width: 156rpx;
  height: 126rpx;
  border-radius: 14rpx;
  flex-shrink: 0;
}
.meta {
  color: #9aa3b0;
  font-size: 22rpx;
  margin-top: 26rpx;
}
.tag {
  color: #6b86b0;
  margin-right: 12rpx;
}
.tags {
  min-width: 0;
}
</style>
