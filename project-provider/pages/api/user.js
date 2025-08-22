import {
  request
} from "../../utils/request.js"

// 手机号登录
export const phoneLogins = (params) =>
  request({
    url: `/user/open/login/worker`,
    method: 'post',
    data: params
  })

// 发送短信验证码
export const getsmsCode = (params) =>
  request({
    url: `/foundation/sms-code/send`,
    method: 'POST',
    data: params,
    header: {
      'Content-Type': 'application/json;charset=UTF-8'
    }
  })

// 获取当前用户信息
export const getUserInfo = (params) =>
  request({
    url: `/user/worker/serve-provider/currentUserInfo`,
    method: 'get',
    params
  })