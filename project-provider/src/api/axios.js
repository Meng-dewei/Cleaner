// 添加请求拦截器
axios.interceptors.request.use(
  config => {
    console.log('发送请求:', config.method, config.url, config.baseURL);
    console.log('完整请求 URL:', config.baseURL + config.url);
    return config;
  },
  error => {
    console.error('请求错误:', error);
    return Promise.reject(error);
  }
); 