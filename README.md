# Cleaner
家政上门项目



## 业务架构

项目包括三种客户端：用户端(小程序)、服务端（app）、平台管理端(PC)。每个客户端对应了使用平台的一类不同的角色。

- 用户端（小程序）:  消费和通过用户端小程序完成在线预约下单、支付、退款等操作。
- 服务端（app）:  家政服务人员通过服务端APP完成在线接单。 
- 平台管理端(PC)：通过管理端完成服务管理，服务人员管理、订单管理等操作。



## 后端代码

### 工程结构

```java
duolai-clean/                     
├── foundation/                     # 基础功能服务
│
├── framework/                      # 框架公共依赖
│   ├── canal-sync/                 # canal数据同步
│   ├── common/                     # 公共代码
│   ├── mq/                         # 消息队列
│   ├── mvc/                        # MVC增强(处理请求头，拦截，封装公共响应，统一异常处理)
│   ├── mybatis-plus/               # MyBatis
│   └── xxl-job/                    # 分布式任务调度
│
├── gateway/                        # API网关
│
├── housekeeping/                   # 家政服务
│   ├── api/
│   └── service/
│
├── market/                         # 优惠卷服务
│   ├── market-api/
│   ├── market-service/
│
├── order/                          # 订单服务
│   ├── order-api/
│   ├── order-service/
│
├── order-dispatch/                 # 订单调度服务
│   ├── order-dispatch-api/
│   ├── order-dispatch-service/
│
├── pay/                            # 支付服务
│   ├── pay-api/
│   ├── pay-service/
│
└── user/                           # 用户服务
    ├── user-api/
    ├── user-service/
```

### 代码包

```java
xxx/                            # xx服务
├── api/                     
│   ├── dto                      # 其他服务也需要是使用的公共DTO类
│
├── service/                      # 服务实现
|   ├── client                    # 调用其他服务的OpenFeign接口
│   ├── config                    # 配置类
│   ├── constants                 # 常量类
│   ├── consumer                  # 消息消费者
│   ├── controller                # controller
│   │ 	├── inner                 # 接收服务调用请求的controller
│   │ 	├── operation             # 接收后台请求的controller
│   │ 	├── user                  # 接收普通用户请求的controller
│   │ 	└── worker                # 接收处理服务人员端请求的controller
│   ├── converter                 # 转化器
│   ├── dao                       # 数据访问层
│   │	├── entity                # 数据库映射实体类
│	│   ├── mapper                # mapper
│	│   └── repository            # 访问elasticsearch的repository
│   ├── dto                       # 服务自己使用的dto类
│   ├── enums                     # 枚举类
│   ├── job                       # 定时任务
│   ├── handler                   # 数据同步代码
│   ├── properties                # 绑定配置文件的properties
│   ├── request                   # 封装请求参数的类
│   └── service                   # 业务接口定义
│   	├── impl                  # 接口实现类
└── 启动类
```

### foundation服务

foundation服务及我们的基础设施服务，该服务的功能主要为三个:

- 获取地图定位信息
- 获取验证码信息
- 获取微信登录信息
- 图片上传功能(使用OSS)

### 平台端启动（后台）

提供了一个jar包，我们只需执行如下命令即可启动，该后台占用端口3000，启动后通过http://localhost:3000即可访问

```
java -jar  jar包名称(jar包名称带.jar后缀)
```

需要注意的是，后台网关监听本机的11500端口

### Controller方法返回值

在我们的上门家政微服务项目中，Controller方法不在需要将数据封装到Result对象中，因为我们基于Filter来封装Controller方法的返回值

```java
/**
 *  用于包装外网访问结果
 */
@Component
@Slf4j
public class PackResultFilter implements Filter {
    @Override
    public void doFilter(ServletRequest servletRequest, ServletResponse servletResponse, FilterChain filterChain) throws IOException, ServletException {
        // 1.无需包装，放过拦截
        String requestURI = ((HttpServletRequest) servletRequest).getRequestURI();
        if (requestURI.contains(".") ||
                requestURI.contains("/swagger") ||
                requestURI.contains("/api-docs") ||
                requestURI.contains("/inner")) {
            // 注意这里对服务调用时直接放行的(以inner开头的url)
            filterChain.doFilter(servletRequest, servletResponse);
            return;
        }
        // 2.包装响应值
        // 2.1.处理业务，获取响应值
        HttpServletResponse response = (HttpServletResponse) servletResponse;
        ResponseWrapper responseWrapper = new ResponseWrapper(response);
        filterChain.doFilter(servletRequest, responseWrapper);

        // 无需包装
        if (response.containsHeader(BODY_PROCESSED) && response.getHeader(BODY_PROCESSED).equals("1")) {
            IoUtils.write(response.getOutputStream(), false, responseWrapper.getResponseData());
            return;
        }

        // 2.2.包装，将Controller方法的返回结果封装为Json字符串，添加code和message，就像返回了Result对象
        byte[] bytes = Result.plainOk(responseWrapper.getResponseData());
        log.info("result : {}", new String(bytes));
        // 2.3.写入
        response.setContentType("applicaton/json;charset=UTF-8");
        response.setContentLength(bytes.length);
        IoUtils.write(response.getOutputStream(), false, bytes);
    }
}
```

