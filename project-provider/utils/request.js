import { baseUrl } from './env'
// 参数： url:请求地址  param：请求参数  method：请求方式 callBack：回调函数
export function request({ url = "", params = {}, data = {}, method = "GET", header = {} }) {
  // 合并默认header和传入的header
  const defaultHeader = {
    "Content-Type": "application/json;charset=UTF-8",
    "Access-Control-Allow-Origin": "*"
  };
  
  const token = uni.getStorageSync("token");
  if (token) {
    defaultHeader.Authorization = token;
  }
  
  const finalHeader = { ...defaultHeader, ...header };
  
  // 打印请求信息
  console.log('Request:', {
    url: baseUrl + url,
    method,
    data: method.toUpperCase() === 'GET' ? params : data,
    header: finalHeader
  });

  return new Promise((resolve, reject) => {
    uni.request({
      url: baseUrl + url,
      data: method.toUpperCase() === 'GET' ? params : data,
      header: finalHeader,
      method: method.toUpperCase(),
      success: (res) => {
        console.log('Response:', res);
        if (res.statusCode === 200) {
          if (res.data.code === 200 || res.data.code === 0) {
            resolve(res.data);
          } else {
            reject(res.data);
          }
        } else if (res.statusCode === 401) {
          uni.showToast({
            title: '登录已过期，请重新登录',
            icon: 'none',
            duration: 2000
          });
          uni.redirectTo({
            url: '/pages/login/user'
          });
          reject({ code: 401, msg: '登录已过期' });
        } else {
          reject({
            code: res.statusCode,
            msg: `请求失败：${res.statusCode}`
          });
        }
      },
      fail: (err) => {
        console.error('Request failed:', err);
        reject({
          code: -1,
          msg: err.errMsg || '网络请求失败'
        });
      }
    });
  });
}
