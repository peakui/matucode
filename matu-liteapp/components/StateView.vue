<template>
  <view v-if="loading && !count" class="skeleton"
    ><view v-for="n in 3" :key="n" class="card"
      ><view class="bar short" /><view class="bar" /><view class="bar medium" /></view
  ></view>
  <view v-else-if="error" class="state"
    ><view class="empty-icon">!</view><view class="state-title">暂时没有连接上</view
    ><view class="muted">{{ error }}</view
    ><button class="outline small retry" @tap="$emit('retry')">重新加载</button></view
  >
  <view v-else-if="!count" class="state"
    ><view class="empty-icon">⌁</view><view class="state-title">{{ emptyTitle }}</view
    ><view class="muted">{{ emptyText }}</view></view
  >
  <view v-else class="footnote">{{
    loading ? '正在加载…' : done ? '已经看到最后了' : '继续上滑，发现更多'
  }}</view>
</template>
<script setup>
defineProps({
  loading: Boolean,
  error: String,
  count: { type: Number, default: 0 },
  done: Boolean,
  emptyTitle: { type: String, default: '这里还没有内容' },
  emptyText: { type: String, default: '换个筛选条件，或稍后再来看看' },
})
defineEmits(['retry'])
</script>
<style scoped>
.state {
  text-align: center;
  padding: 70rpx 28rpx;
}
.state-title {
  font-size: 31rpx;
  font-weight: 600;
  margin-bottom: 14rpx;
}
.retry {
  display: inline-block;
  margin-top: 26rpx;
}
.bar {
  height: 24rpx;
  border-radius: 8rpx;
  background: #f0f3f7;
  margin: 18rpx 0;
}
.short {
  width: 28%;
}
.medium {
  width: 65%;
}
</style>
