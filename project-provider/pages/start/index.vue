<!-- 启动页 -->
<template>
  <view class="starPage">
    <view class="page"> </view>
    <view class="bottom"> </view>
  </view>
</template>

<script>
import { onMounted } from 'vue';
export default {
  name: 'StarPage',
  setup: () => {
    onMounted(() => {
      const times = setTimeout(() => {
        uni.redirectTo({
          url: '/pages/login/user',
        });
        clearTimeout(times);
      }, 3000);
    });
    return {};
  },
};
</script>

<style lang="scss" scoped>
.starPage {
  .page {
    background: #fff url(../../static/new/start.png) no-repeat;
    background-size: 100% 100%;
    width: 100%;
    height: calc(100vh - 360rpx);
  }
  .bottom {
    background: #fff url(../../static/new/img_logo@2x.jpg) no-repeat;
    background-position: center;
    width: 100%;
    height: 360rpx;
    background-size: 310rpx 122rpx;
  }
}
</style>
