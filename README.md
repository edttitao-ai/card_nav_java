# 涛的导航站 - 后端

卡片式导航站点后端，基于 Spring Boot 4 + MyBatis + MySQL 构建，提供 RESTful API 供前端调用。

## 技术栈

- **框架** - Spring Boot 4.0.0
- **ORM** - MyBatis 4.0.1
- **数据库** - MySQL 8
- **日志切面** - Spring AOP
- **构建工具** - Maven

## 功能特点

- **栏目管理** - 增删改查导航栏目
- **卡片管理** - 增删改查链接卡片
- **收藏夹** - 收藏/取消收藏链接
- **点击统计** - 记录卡片点击次数，支持排行
- **访客日志** - 记录每个访问者的访问时间
- **数据统计** - 访问量、栏目数量、分类统计等
- **统一响应** - 所有接口返回统一 JSON 格式

## 目录结构

```
card-nav-java/
├── card_nav/
│   ├── src/main/java/com/tao/card_nav/
│   │   ├── CardNavApplication.java    # 启动类
│   │   ├── aspect/                    # 切面
│   │   │   ├── CardLogAspect.java     # 卡片操作日志切面
│   │   │   └── VisitorInterceptor.java # 访客拦截器
│   │   ├── config/                    # 配置类
│   │   │   ├── CorsConfig.java        # 跨域配置
│   │   │   └── WebMvcConfig.java      # Web MVC 配置
│   │   ├── controller/                # 控制器
│   │   │   ├── CardsController.java   # 卡片接口
│   │   │   ├── CategoryController.java
│   │   │   ├── ClicksController.java
│   │   │   ├── FavoritesController.java
│   │   │   ├── SidebarController.java
│   │   │   ├── StatsController.java
│   │   │   └── VisitorsController.java
│   │   ├── domain/                    # 视图对象
│   │   ├── entity/                    # 实体类
│   │   ├── exception/                 # 异常处理
│   │   ├── mapper/                    # MyBatis Mapper 接口
│   │   ├── result/                    # 统一响应封装
│   │   └── service/                   # 业务逻辑
│   ├── src/main/resources/
│   │   ├── application.yml            # 主配置文件
│   │   ├── mapper/                    # MyBatis XML 映射文件
│   │   └── generatorConfig.xml        # MyBatis Generator 配置
│   ├── sql/                           # 数据库脚本
│   │   ├── card_nav.sql               # 建表语句
│   │   └── init_data.sql              # 初始化数据
│   ├── env.example                    # 环境变量模板
│   └── pom.xml
```

## 环境配置

后端使用环境变量管理敏感配置，参考 `env.example` 创建 `.env` 文件：

```bash
# 数据库配置（必须）
DB_HOST=your-mysql-host
DB_USER=your-mysql-user
DB_PASSWORD=your-mysql-password

# 数据库配置（可选，有默认值）
DB_PORT=3306
DB_NAME=card_nav

# 服务端口（可选，默认 3002）
SERVER_PORT=3002
```

> **重要**：`.env` 文件包含敏感信息，已加入 `.gitignore`，请勿上传到 GitHub。

## 数据库初始化

1. 创建数据库：
```sql
CREATE DATABASE card_nav DEFAULT CHARACTER SET utf8mb4 COLLATE utf8mb4_unicode_ci;
```

2. 执行建表脚本（`sql/card_nav.sql`）

3. 可选：导入初始数据（`sql/init_data.sql`）

## 本地开发

```bash
cd card_nav

# 使用 Maven 启动
./mvnw spring-boot:run

# 或打包后运行
./mvnw clean package
java -jar target/card_nav-0.0.1-SNAPSHOT.jar
```

启动后访问 http://localhost:3002

## API 接口

| 接口 | 方法 | 说明 |
|------|------|------|
| `/api/cards/{sidebarId}` | GET | 获取栏目下所有卡片 |
| `/api/cards` | POST | 新增卡片 |
| `/api/cards/{id}` | PUT | 更新卡片 |
| `/api/cards/{id}` | DELETE | 删除卡片 |
| `/api/cards/{id}/pin` | PUT | 切换置顶状态 |
| `/api/sidebars` | GET | 获取所有栏目 |
| `/api/sidebars` | POST | 新增栏目 |
| `/api/sidebars/{id}` | PUT | 更新栏目 |
| `/api/sidebars/{id}` | DELETE | 删除栏目 |
| `/api/favorites` | GET | 获取收藏列表 |
| `/api/favorites` | POST | 添加收藏 |
| `/api/favorites/{cardId}` | DELETE | 取消收藏 |
| `/api/clicks` | GET | 获取点击排行 |
| `/api/clicks` | POST | 记录点击 |
| `/api/stats` | GET | 获取统计数据 |
| `/api/visitors` | GET | 获取访客日志 |
| `/api/categories` | GET | 获取分类统计 |
| `/api/chat` | POST | AI 流式聊天（SSE，返回 `rate_limited` 事件表示触发限流） |
| `/api/chat/stop` | POST | 中断指定 sessionId 的 AI 流式会话 |

## AI 聊天限流

`POST /api/chat` 默认按客户端 IP 限流，**每 IP 每分钟 10 次**。

- **算法**：Resilience4j RateLimiter（令牌桶），由自造注解 `@RateLimitedByIp` + AOP 切面触发
- **客户端 IP 来源**：`X-Forwarded-For` → `X-Real-IP` → `request.getRemoteAddr()`
- **触发限流**：返回 SSE 流，第一个事件为 `event: rate_limited`（payload 含 `retryAfterSeconds`），第二个事件为 `event: done`
- **多实例限制**：当前是 JVM 内存版，**N 个实例部署时单 IP 实际配额 = 10 × N / 分钟**。严格跨实例共享需要后续切换到 Bucket4j + Redis
- **反代要求**：必须由反代正确写入 `X-Forwarded-For`（或 `X-Real-IP`），否则所有请求会拿到反代 IP

### 用法：在 controller 方法上贴注解

```java
@PostMapping(produces = MediaType.TEXT_EVENT_STREAM_VALUE)
@RateLimitedByIp(fallbackMethod = "rateLimitFallback")
public Flux<ServerSentEvent<String>> chat(
        @RequestBody Map<String, String> payload,
        HttpServletRequest request,
        @RateLimitKey String clientIp) {     // IP 由切面自动注入，业务无需解析
    // 业务逻辑...
}

// fallback 签名：原方法参数列表 + 末尾追加 RateLimitException
private Flux<ServerSentEvent<String>> rateLimitFallback(
        Map<String, String> payload, HttpServletRequest request,
        String clientIp, RateLimitException ex) {
    return Flux.just(/* rate_limited 事件 + done 事件 */);
}
```

### 为什么不用 Resilience4j 标准 `@RateLimiter`？

`@RateLimiter` 的 `name` 是静态配置，所有调用共享一个 RateLimiter 实例，**无法按 IP 各自一个 bucket**。本项目用自造注解 `@RateLimitedByIp` + 切面在 AOP 层动态按 IP 取 RateLimiter（复用底层 `RateLimitService` 的 `limiterCache`），兼顾注解式写法的清爽与"每 IP 隔离"的语义。

### 调整配额

通过环境变量覆盖，无需改代码：

```bash
# 把 AI 聊天限流调到每分钟 60 次
AI_CHAT_RATE_LIMIT_PER_MINUTE=60
```

完整配置在 `application.yml` 的 `resilience4j.ratelimiter.instances.aiChatByIp` 块。

### Prometheus 指标

应用通过 Spring Boot Actuator + Micrometer 暴露限流指标：

- **端点**：`GET /actuator/prometheus`（文本格式）
- **核心指标**：`rate_limit_requests_total`（Counter）
  - `method`（tag）：被拦截的 controller 方法名，如 `chat`
  - `outcome`（tag）：`allowed` / `limited`
- **示例查询**：
  - 过去 5 分钟限流命中数（PromQL）：
    ```promql
    sum by (method) (rate(rate_limit_requests_total{outcome="limited"}[5m]))
    ```
  - 限流命中率：
    ```promql
    sum(rate(rate_limit_requests_total{outcome="limited"}[5m]))
      / sum(rate(rate_limit_requests_total[5m]))
    ```
- **生产建议**：actuator 端点直接暴露在公网有信息泄露风险（jvm、tomcat 等内部指标），务必通过反代做访问控制（内网 IP 白名单 / 鉴权网关），不要让 `/actuator/**` 直接对外可访问。

## 统一响应格式

```json
{
  "code": 200,
  "message": "success",
  "data": { ... }
}
```

错误响应：
```json
{
  "code": 400,
  "message": "错误信息",
  "data": null
}
```

## 常见问题

**Q: 启动报错 `CannotGetJdbcConnectionException`？**
A: 检查 `.env` 文件是否创建，以及数据库配置是否正确。

**Q: 跨域问题？**
A: 后端已配置 CORS，允许前端开发服务器访问。如需修改，编辑 `CorsConfig.java`。

**Q: 如何查看日志？**
A: 启动后控制台会输出日志，也可在 `application.yml` 中配置日志输出到文件。

**Q: AI 聊天触发限流，前端怎么收到提示？**
A: 不要用 fetch 的 status code 判断（限流返回的也是 200）。监听 SSE 事件的 `event` 字段，看到 `rate_limited` 即触发限流，payload 里 `retryAfterSeconds` 提示客户端等多少秒再试。详见上文"AI 聊天限流"章节。