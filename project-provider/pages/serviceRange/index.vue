<template>
  <view class="serviceRange">
    <!-- nav -->
    <UniNav title="请选择工作地点" @goBack="goBack"></UniNav>
    <map
      class="map"
      :markers="[markers.data]"
      :latitude="location.latitude"
      :longitude="location.longitude"
    >
    </map>
    <cover-view class="address">
      <cover-view class="city">
        <cover-view class="label">服务城市</cover-view>
        <cover-view class="content">
          <cover-view class="cityName" @click="handleSelectCity">{{
            cityName
          }}</cover-view>
          <cover-image
            @click="handleSelectCity"
            class="icon"
            src="../../static/new/icon_more@2x.png"
          ></cover-image>
        </cover-view>
      </cover-view>
      <cover-view class="range">
        <cover-view class="label">意向接单范围</cover-view>
        <cover-view class="content">
          <cover-view class="rangeName" @click="handleChooseRange">{{
            address
          }}</cover-view>
          <cover-image
            @click="handleChooseRange"
            class="icon"
            src="../../static/new/icon_more@2x.png"
          ></cover-image>
        </cover-view>
      </cover-view>
      <cover-view class="btn-red">
        <cover-view class="text" @click="handleSubmit">保存</cover-view>
      </cover-view>
    </cover-view>
  </view>
</template>

<script setup>
import { ref, reactive, onMounted } from 'vue';
import { useStore } from 'vuex';
import { onShow } from '@dcloudio/uni-app';
import { getSettingInfo, setServiceSetting } from '../api/setting.js';
import { data } from '../../utils/h5Data.js';
// 导航组件
import UniNav from '@/components/uni-nav/index.vue';
const cityName = ref('请选择');
const address = ref('请选择');
const store = useStore(); //vuex获取、储存数据
const users = store.state.user;

const location = reactive({
  latitude: '',
  longitude: '',
});
const markers = reactive({
  data: {
    id: 1,
    latitude: 0,
    longitude: 0,
    iconPath: '/static/new/img_weizhi@2x.png',
    width: 60,
    height: 60,
  },
});
//选择服务城市
const handleSelectCity = () => {
  uni.navigateTo({
    url: '/pages/city/index?address=' + address.value,
  });
};
//选择具体服务范围
const handleChooseRange = () => {
  if (process.env.VUE_APP_PLATFORM === 'h5') {
    // H5环境使用QQ地图选择位置
    useQQMapChooseLocation();
  } else {
    // 其他环境使用原生方法
  uni.chooseLocation({
      latitude: Number(location.latitude),
      longitude: Number(location.longitude),
    success: function (res) {
      address.value = res.name;
      location.latitude = res.latitude;
      location.longitude = res.longitude;
      markers.data.latitude = res.latitude;
      markers.data.longitude = res.longitude;
      store.commit('user/setLocation', location);
      store.commit('user/setAddress', address.value);
    },
    fail: function (err) {
      console.log(err, '选择具体服务范围失败');
    },
  });
  }
};

// H5环境下使用QQ地图选择位置
const useQQMapChooseLocation = () => {
  try {
    console.log('正在初始化QQ地图...');
    uni.showLoading({
      title: '地图加载中...',
      mask: true
    });
    
    // 检查qq对象是否存在
    if (typeof qq === 'undefined' || !qq.maps) {
      // 懒加载QQ地图SDK
      const loadScript = new Promise((resolve, reject) => {
        // 创建script元素
        const script = document.createElement('script');
        script.src = 'https://map.qq.com/api/gljs?v=1.exp&key=JXZBZ-WNJ35-NJFIV-QB2L6-RPB5O-BQBV2&libraries=place,convertor,geometry';
        script.onerror = (e) => {
          console.error('QQ地图加载失败:', e);
          reject(e);
        };
        script.onload = () => {
          console.log('QQ地图脚本加载成功!');
          resolve();
        };
        document.head.appendChild(script);
      });
      
      // 等待地图脚本加载完成
      loadScript.then(() => {
        // 加载成功后继续初始化地图
        initializeMap();
      }).catch((error) => {
        uni.hideLoading();
        console.error('地图SDK加载失败:', error);
        // 显示错误提示并提供手动输入选项
        showMapLoadErrorDialog();
      });
    } else {
      // 已加载则直接初始化地图
      initializeMap();
    }
    
    // 初始化地图函数
    function initializeMap() {
      // 确保经纬度是数值类型
	  console.log('before: ' + Number(location.latitude) + '---' + Number(location.longitude))
      // 判断经纬度是否有效，无效时使用h5数据中的值
      let lat = Number(location.latitude);
      let lng = Number(location.longitude);
      
      // 检查是否为有效数值（不是NaN且不是0）
      if (isNaN(lat) || isNaN(lng) || lat === 0 || lng === 0) {
        console.log('经纬度无效，使用h5数据中的默认值');
        lat = Number(data.latitude);
        lng = Number(data.longitude);
      }
      
      console.log('地图中心位置:', lat, lng);
      
      // 创建一个临时的地图容器，确保覆盖整个屏幕
      let mapContainer = document.createElement('div');
      mapContainer.id = 'qqMapContainer';
      
      // 关键修改：使用固定定位并设置最高层级
      mapContainer.style.position = 'fixed';
      mapContainer.style.left = '0';
      mapContainer.style.top = '0';
      mapContainer.style.width = '100vw'; // 使用视口宽度单位
      mapContainer.style.height = '100vh'; // 使用视口高度单位
      mapContainer.style.zIndex = '10000'; // 使用更高的z-index确保在最上层
      mapContainer.style.backgroundColor = '#fff'; // 添加背景色
      
      // 添加到body的最前面，确保它在最上层
      if (document.body.firstChild) {
        document.body.insertBefore(mapContainer, document.body.firstChild);
      } else {
        document.body.appendChild(mapContainer);
      }
      
      // 添加地图加载指示器
      let loadingIndicator = document.createElement('div');
      loadingIndicator.id = 'map-loading-indicator';
      loadingIndicator.style.position = 'fixed';
      loadingIndicator.style.top = '50%';
      loadingIndicator.style.left = '50%';
      loadingIndicator.style.transform = 'translate(-50%, -50%)';
      loadingIndicator.style.backgroundColor = 'rgba(0,0,0,0.7)';
      loadingIndicator.style.color = 'white';
      loadingIndicator.style.padding = '15px 20px';
      loadingIndicator.style.borderRadius = '6px';
      loadingIndicator.style.fontSize = '16px';
      loadingIndicator.style.zIndex = '10003';
      loadingIndicator.style.textAlign = 'center';
      loadingIndicator.textContent = '地图加载中，请稍候...';
      document.body.appendChild(loadingIndicator);
      
      // 创建地图选项，简化配置
      const mapOptions = {
        center: new qq.maps.LatLng(lat, lng),
        zoom: 15,
        mapTypeId: qq.maps.MapTypeId.ROADMAP,
        panControl: false,
        zoomControl: true,
        zoomControlOptions: {
          position: qq.maps.ControlPosition.RIGHT_BOTTOM
        },
        mapTypeControl: false,
        scaleControl: true,
        streetViewControl: false,
        maxZoom: 19,
        minZoom: 9
      };
      
      // 创建地图实例
      const map = new qq.maps.Map(document.getElementById('qqMapContainer'), mapOptions);
      
      // 监听地图加载完成事件
      qq.maps.event.addListenerOnce(map, 'tilesloaded', function() {
        console.log('地图图块加载完成');
        // 移除加载指示器
        if (document.getElementById('map-loading-indicator')) {
          document.body.removeChild(loadingIndicator);
        }
        uni.hideLoading();
      });
      
      // 创建标记
      let marker = new qq.maps.Marker({
        position: new qq.maps.LatLng(lat, lng),
        map: map,
        animation: qq.maps.MarkerAnimation.DROP // 添加动画，更醒目
      });
      
      // 添加位置名称输入框（简化版）
      let inputPanel = document.createElement('div');
      inputPanel.style.position = 'fixed';
      inputPanel.style.top = '70px';
      inputPanel.style.left = '10px';
      inputPanel.style.right = '10px';
      inputPanel.style.padding = '10px';
      inputPanel.style.backgroundColor = '#fff';
      inputPanel.style.borderRadius = '5px';
      inputPanel.style.boxShadow = '0 2px 6px rgba(0,0,0,0.1)';
      inputPanel.style.display = 'flex';
      inputPanel.style.alignItems = 'center';
      inputPanel.style.zIndex = '10001';
      
      let locationInput = document.createElement('input');
      locationInput.placeholder = '输入位置名称';
      locationInput.style.flex = '1';
      locationInput.style.padding = '8px 12px';
      locationInput.style.border = '1px solid #ddd';
      locationInput.style.borderRadius = '4px';
      locationInput.style.fontSize = '14px';
      
      inputPanel.appendChild(locationInput);
      document.body.appendChild(inputPanel);
      
      // 简化界面：添加顶部标题和按钮
      let headerPanel = document.createElement('div');
      headerPanel.style.position = 'fixed';
      headerPanel.style.top = '0';
      headerPanel.style.left = '0';
      headerPanel.style.right = '0';
      headerPanel.style.padding = '10px';
      headerPanel.style.backgroundColor = '#fff';
      headerPanel.style.boxShadow = '0 2px 6px rgba(0,0,0,0.1)';
      headerPanel.style.display = 'flex';
      headerPanel.style.alignItems = 'center';
      headerPanel.style.zIndex = '10001';
      
      let titleDiv = document.createElement('div');
      titleDiv.textContent = '选择位置';
      titleDiv.style.flex = '1';
      titleDiv.style.textAlign = 'center';
      titleDiv.style.fontWeight = 'bold';
      titleDiv.style.fontSize = '18px';
      
      let backBtn = document.createElement('button');
      backBtn.textContent = '返回';
      backBtn.style.border = 'none';
      backBtn.style.background = 'none';
      backBtn.style.fontSize = '16px';
      backBtn.style.color = '#007aff';
      backBtn.style.padding = '5px 10px';
      
      headerPanel.appendChild(backBtn);
      headerPanel.appendChild(titleDiv);
      document.body.appendChild(headerPanel);
      
      // 底部按钮面板
      let controlPanel = document.createElement('div');
      controlPanel.style.position = 'fixed';
      controlPanel.style.bottom = '20px';
      controlPanel.style.left = '0';
      controlPanel.style.right = '0';
      controlPanel.style.display = 'flex';
      controlPanel.style.justifyContent = 'center';
      controlPanel.style.padding = '0 20px';
      controlPanel.style.zIndex = '10001';
      
      let confirmBtn = document.createElement('button');
      confirmBtn.textContent = '确认位置';
      confirmBtn.style.flex = '1';
      confirmBtn.style.padding = '12px 0';
      confirmBtn.style.backgroundColor = '#007aff';
      confirmBtn.style.color = 'white';
      confirmBtn.style.border = 'none';
      confirmBtn.style.borderRadius = '5px';
      confirmBtn.style.fontSize = '16px';
      
      controlPanel.appendChild(confirmBtn);
      document.body.appendChild(controlPanel);
      
      // 地图说明提示
      let tipBox = document.createElement('div');
      tipBox.style.position = 'fixed';
      tipBox.style.top = '130px';
      tipBox.style.left = '50%';
      tipBox.style.transform = 'translateX(-50%)';
      tipBox.style.backgroundColor = 'rgba(0,0,0,0.7)';
      tipBox.style.color = 'white';
      tipBox.style.padding = '8px 12px';
      tipBox.style.borderRadius = '4px';
      tipBox.style.fontSize = '14px';
      tipBox.style.zIndex = '10001';
      tipBox.style.textAlign = 'center';
      tipBox.textContent = '点击地图选择具体位置，然后输入名称';
      document.body.appendChild(tipBox);
      
      // 3秒后自动隐藏提示
      setTimeout(() => {
        tipBox.style.display = 'none';
      }, 3000);
      
      // 创建信息显示区域的辅助函数
      function createInfoDiv() {
        let infoDiv = document.createElement('div');
        infoDiv.id = 'position-info';
        infoDiv.style.position = 'fixed';
        infoDiv.style.bottom = '80px';
        infoDiv.style.left = '50%';
        infoDiv.style.transform = 'translateX(-50%)';
        infoDiv.style.backgroundColor = 'rgba(0,0,0,0.7)';
        infoDiv.style.color = 'white';
        infoDiv.style.padding = '8px 12px';
        infoDiv.style.borderRadius = '4px';
        infoDiv.style.fontSize = '12px';
        infoDiv.style.zIndex = '10002';
        infoDiv.style.maxWidth = '90%';
        infoDiv.style.textAlign = 'center';
        document.body.appendChild(infoDiv);
        return infoDiv;
      }
      
      // 添加清理地图元素的函数
      function cleanupMapElements() {
        // 定义需要清理的元素
        const elementsToRemove = [
          document.getElementById('qqMapContainer'),
          document.getElementById('position-info'),
          document.getElementById('map-loading-indicator')
        ];
        
        // 清理DOM元素数组
        for (const el of elementsToRemove) {
          if (el && el.parentNode) {
            try {
              document.body.removeChild(el);
            } catch (e) {
              console.error('清理元素失败:', e);
            }
          }
        }
        
        // 清理面板元素
        [headerPanel, controlPanel, inputPanel, tipBox].forEach(panel => {
          if (panel && panel.parentNode) {
            try {
              document.body.removeChild(panel);
            } catch (e) {
              console.error('清理面板失败:', e);
            }
          }
        });
        
        // 查找并清理其他可能的临时元素
        document.querySelectorAll('div[style*="position: fixed"][style*="z-index: 1000"]').forEach(el => {
          if (el && el.parentNode) {
            try {
              document.body.removeChild(el);
            } catch (e) {
              console.error('清理临时元素失败:', e);
            }
          }
        });
        
        console.log('地图元素已清理');
      }
      
      // 简化的地图点击事件，不使用逆地址解析
      qq.maps.event.addListener(map, 'click', function(event) {
        marker.setPosition(event.latLng);
        
        // 保存点击位置的经纬度
        console.log('地图点击 - 选择位置:', {
          latitude: event.latLng.lat,
          longitude: event.latLng.lng
        });
        
        // 在地图上显示当前选择的经纬度
        let infoDiv = document.getElementById('position-info') || createInfoDiv();
        
        if (marker.customName) {
          // 如果有自定义名称，显示名称和经纬度
          infoDiv.textContent = `名称: ${marker.customName}\n经度: ${event.latLng.lng.toFixed(6)}, 纬度: ${event.latLng.lat.toFixed(6)}`;
        } else {
          // 否则只显示经纬度
          infoDiv.textContent = `经度: ${event.latLng.lng.toFixed(6)}, 纬度: ${event.latLng.lat.toFixed(6)}`;
        }
        infoDiv.style.display = 'block';
      });
      
      // 修改确认按钮事件，确保正确更新位置信息到Store
      confirmBtn.onclick = function() {
        const position = marker.getPosition();
        
        console.log('确认位置 - 经纬度:', {
          latitude: position.lat,
          longitude: position.lng
        });
        
        // 更新本地位置信息
        location.latitude = position.lat;
        location.longitude = position.lng;
        markers.data.latitude = position.lat;
        markers.data.longitude = position.lng;
        
        // 获取位置名称
        const nameInput = locationInput.value.trim();
        console.log('位置名称输入:', {
          nameInput: nameInput,
          hasCustomName: !!marker.customName
        });
        
        // 如果用户没有输入名称，则提示需要输入
        if (!marker.customName && !nameInput) {
          console.log('未输入位置名称，显示提示框');
          
          // 创建一个自定义的高层级提示框
          let alertBox = document.createElement('div');
          alertBox.style.position = 'fixed';
          alertBox.style.top = '50%';
          alertBox.style.left = '50%';
          alertBox.style.transform = 'translate(-50%, -50%)';
          alertBox.style.backgroundColor = 'white';
          alertBox.style.padding = '20px';
          alertBox.style.borderRadius = '8px';
          alertBox.style.boxShadow = '0 4px 12px rgba(0,0,0,0.15)';
          alertBox.style.zIndex = '20000'; // 超高z-index确保显示在最上层
          alertBox.style.textAlign = 'center';
          alertBox.style.maxWidth = '80%';
          
          let alertTitle = document.createElement('div');
          alertTitle.textContent = '请输入位置名称';
          alertTitle.style.fontSize = '18px';
          alertTitle.style.fontWeight = 'bold';
          alertTitle.style.marginBottom = '15px';
          
          let alertContent = document.createElement('div');
          alertContent.textContent = '您需要为选定的位置输入一个名称';
          alertContent.style.fontSize = '16px';
          alertContent.style.marginBottom = '20px';
          
          let alertBtn = document.createElement('button');
          alertBtn.textContent = '确定';
          alertBtn.style.backgroundColor = '#007aff';
          alertBtn.style.color = 'white';
          alertBtn.style.border = 'none';
          alertBtn.style.padding = '8px 24px';
          alertBtn.style.borderRadius = '4px';
          alertBtn.style.fontSize = '16px';
          
          alertBox.appendChild(alertTitle);
          alertBox.appendChild(alertContent);
          alertBox.appendChild(alertBtn);
          
          // 点击确定按钮关闭提示框
          alertBtn.onclick = function() {
            document.body.removeChild(alertBox);
            // 让输入框获得焦点
            locationInput.focus();
          };
          
          // 添加到body
          document.body.appendChild(alertBox);
          
          return;
        }
        
        // 使用自定义名称或输入框中的名称
        const locationName = marker.customName || nameInput;
        
        // 更新状态到 Store - 确保存储完整的位置信息
        address.value = locationName;
        store.commit('user/setLocation', {
          latitude: position.lat,
          longitude: position.lng
        });
        store.commit('user/setAddress', address.value);
        
        // 记录完整的位置信息
        console.log('更新Store - 完整位置信息:', {
          latitude: position.lat,
          longitude: position.lng,
          address: address.value,
          cityName: users.cityName,
          cityCode: users.cityCode
        });
        
        // 先清理地图元素，避免后续操作被阻塞
        try {
          cleanupMapElements();
        } catch (e) {
          console.error('清理地图元素失败:', e);
          // 尝试更简单的清理方式
          try {
            document.querySelectorAll('div[style*="position: fixed"]').forEach(el => {
              if (el.parentNode) document.body.removeChild(el);
            });
          } catch (e2) {
            console.error('备用清理也失败:', e2);
          }
        }
        
        // 检查城市是否已选择
        if (!users.cityCode) {
          uni.showToast({
            title: '请先选择服务城市',
            icon: 'none',
            duration: 2000
          });
          return;
        }
        
        // 直接保存位置信息到服务器
        uni.showLoading({
          title: '保存中...',
          mask: true,
        });
        
        const locationStr = 
          String(users.location.longitude) + ',' + String(users.location.latitude);
        
        console.log('自动保存 - 位置字符串:', locationStr);
        
        setServiceSetting({
          cityCode: users.cityCode,
          location: locationStr,
          intentionScope: users.address,
          cityName: users.cityName,
        }).then(() => {
          uni.hideLoading();
          
          // 提示成功后直接返回
          uni.showToast({
            title: '保存成功',
            icon: 'success'
          });
          
          // 直接调用页面的返回函数
          console.log('准备返回上一页...');
          setTimeout(() => {
            goBack();
          }, 1000);
        }).catch(err => {
          uni.hideLoading();
          uni.showToast({
            title: err.msg || '保存失败，请重试',
            icon: 'none'
          });
        });
      };
      
      // 返回按钮事件
      backBtn.onclick = function() {
        try {
          cleanupMapElements();
        } catch (e) {
          console.error('返回时清理地图元素失败:', e);
          // 尝试更简单的清理方式
          try {
            document.querySelectorAll('div[style*="position: fixed"]').forEach(el => {
              if (el.parentNode) document.body.removeChild(el);
            });
          } catch (e2) {
            console.error('备用清理也失败:', e2);
          }
        }
      };
    }
    
  } catch (error) {
    uni.hideLoading();
    console.error('QQ地图初始化错误:', error);
    
    // 显示错误提示并提供手动输入选项
    showMapLoadErrorDialog();
  }
  
  // 显示地图加载错误对话框
  function showMapLoadErrorDialog() {
    uni.showModal({
      title: '地图加载失败',
      content: '是否手动输入位置信息？',
      success: function(res) {
        if (res.confirm) {
          // 提供手动输入界面
          uni.showModal({
            title: '请输入位置名称',
            editable: true,
            placeholderText: '例如：xx小区、xx大厦',
            success: function(res) {
              if (res.confirm && res.content) {
                address.value = res.content;
                store.commit('user/setAddress', address.value);
                
                uni.showToast({
                  title: '位置已设置',
                  icon: 'success'
                });
              }
            }
          });
        }
      }
    });
  }
};

const handleSubmit = () => {
  // 检查必要信息
  if (!users.cityCode) {
    return uni.showToast({
      title: '请选择服务城市',
      duration: 1500,
      icon: 'none',
    });
  } else if (!users.address || users.address === '请选择') {
    return uni.showToast({
      title: '请设置意向接单范围',
      duration: 1500,
      icon: 'none',
    });
  }
  
  // 确保有位置信息
  if (!users.location || typeof users.location.latitude === 'undefined' || typeof users.location.longitude === 'undefined') {
    return uni.showToast({
      title: '位置信息不完整，请重新选择位置',
      duration: 1500,
      icon: 'none',
    });
  }
  
  uni.showLoading({
    title: '保存中...',
    mask: true,
  });
  
  // 确保使用用户最后选择的经纬度
  const locationStr = 
    String(users.location.longitude) + ',' + String(users.location.latitude);
  
  console.log('提交保存 - 最终位置字符串:', locationStr);
  console.log('提交保存 - 位置完整信息:', {
    cityCode: users.cityCode,
    cityName: users.cityName,
    address: users.address,
    location: {
      latitude: users.location.latitude,
      longitude: users.location.longitude
    }
  });
  
  setServiceSetting({
    cityCode: users.cityCode,
    location: locationStr,
    intentionScope: users.address,
    cityName: users.cityName,
  }).then(() => {
    uni.hideLoading();
    uni.showToast({
      title: '保存成功',
      duration: 1500,
      icon: 'success',
      success: () => {
        // 修改这里：保存成功后跳转到 setting 页面
        setTimeout(() => {
          uni.navigateTo({
            url: '/pages/setting/index'
          });
        }, 1000);
      },
    });
  }).catch(err => {
    uni.hideLoading();
    uni.showToast({
      title: err.msg || '保存失败，请重试',
      duration: 1500,
      icon: 'none'
    });
  });
};

onShow(() => {
  console.log('页面显示 - 开始加载位置信息');
  console.log('初始cityName值:', cityName.value);
  console.log('Store中的城市信息:', {
    cityCode: users.cityCode,
    cityName: users.cityName
  });

  // 直接使用 store 中的城市信息
  if (users.cityCode && users.cityName) {
    cityName.value = users.cityName;
    console.log('从store更新cityName:', cityName.value);
  }

  getSettingInfo()
    .then((res) => {
      console.log('获取设置信息成功:', res.data);
      console.log('当前平台:', process.env.VUE_APP_PLATFORM);
      console.log('h5Data默认数据:', data);
      
      if (!res.data.cityCode && !users.cityCode) {
        console.log('无城市信息，使用默认设置');
        
        if (process.env.VUE_APP_PLATFORM === 'h5') {
          console.log('H5环境 - 使用预设数据:', data);
          
          // 设置城市信息
          cityName.value = data.city;
          console.log('设置cityName为:', cityName.value);
          store.commit('user/setCityCode', data.cityCode);
          store.commit('user/setCityName', data.city);
          console.log('更新后的store城市信息:', {
            cityCode: users.cityCode,
            cityName: users.cityName
          });
          
          // 设置位置信息
          location.latitude = Number(data.latitude);
          location.longitude = Number(data.longitude);
          markers.data.latitude = Number(data.latitude);
          markers.data.longitude = Number(data.longitude);
          
          // 设置地址信息
          address.value = data.address || '请选择';
          store.commit('user/setAddress', address.value);
          store.commit('user/setLocation', location);
          
          // 显示城市选择提示
          uni.showModal({
            title: '提示',
            content: '是否切换到当前城市？',
            success: function(res) {
              if (res.confirm) {
                // 用户点击确定，保持当前城市设置
              } else {
                // 用户点击取消，清除城市信息
                clearStore();
              }
            }
          });
        }
      } else {
        console.log('使用已有城市信息');
        console.log('接口返回的城市信息:', {
          cityCode: res.data.cityCode,
          cityName: res.data.cityName,
          intentionScope: res.data.intentionScope
        });
        
        // 设置地址和位置信息
        address.value = res.data.intentionScope || '请选择';
        
        if (res.data.location && res.data.location.includes(',')) {
          const [lng, lat] = res.data.location.split(',');
          location.latitude = Number(lat);
          location.longitude = Number(lng);
          markers.data.latitude = Number(lat);
          markers.data.longitude = Number(lng);
        }
        
        store.commit('user/setLocation', location);
        store.commit('user/setAddress', address.value);
      }
    })
    .catch((err) => {
      console.error('获取设置信息失败:', err);
      uni.showToast({
        title: err.msg || '接口调用失败',
        duration: 1500,
        icon: 'none',
      });
    });
});

onMounted(() => {});
const clearStore = () => {
  store.commit('user/setLocation', {});
  store.commit('user/setAddress', '');
  store.commit('user/setCityCode', '');
  store.commit('user/setCityName', '请选择');
};
// 返回上一页
const goBack = () => {
  uni.navigateBack();
  // clearStore();
};

</script>
<style src="./index.scss" lang="scss" scoped></style>

