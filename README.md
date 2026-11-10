# come-often

> 面向周边 3 公里熟客的私域复购小程序，把集章、积分、优惠券、订座与自提下单串成闭环。

## 📋 目录

- [项目简介](#项目简介)
- [技术栈](#技术栈)
- [项目结构](#项目结构)
- [环境要求](#环境要求)
- [快速开始](#快速开始)
- [配置说明](#配置说明)
- [部署指南](#部署指南)
- [API 文档](#api-文档)
- [常见问题](#常见问题)

## 项目简介

按 [docs/development-plan.md](docs/development-plan.md) 分阶段实施，单次交付、每阶段独立验收。M0~M11 全部完成，M12 登录体系、M13 首页改版与排队叫号已交付：骨架与环境、三端认证与权限、门店与菜品、购物车与自提订单（含 MQ 可靠性三件套）、支付与退款（Mock 网关与退款审核）、优惠券（动态券码与核销服务）、积分与等级（积分抵现与年度过期）、预约订座（时段容量与超时释放）、评价/搜索/首页运营、商家端小程序（统一扫码与订阅提醒降级）、数据看板与报表（Excel 导出）、联调与交付（`@JobLock` 兜底任务、死信告警可查、三条端到端脚本、四份交付文档）。M13 未完项（桌码服务端绑定、PC 后台排队页、实时叫号推送等）按 [docs/milestone-m14.md](docs/milestone-m14.md) 分六个批次补齐。

十二份验收脚本 `deploy/verify-m1.ps1` ~ `verify-m11.ps1`、`deploy/verify-m13.ps1` 串行执行（M11 一次 108 条断言、0 失败），单测 186 个用例全绿。与开发计划的逐条差异见 [deploy/init.md](deploy/init.md)。

### 核心功能

- [x] 四个工程骨架与 JDK8 构建适配脚本
- [x] 统一响应、异常、日志与 Knife4j 在线文档
- [x] 43 张表 DDL（含 M13 排队号表）、M1 种子数据与中间件隔离初始化脚本
- [x] RabbitMQ 延迟/业务/死信拓扑、库存 Lua 与并发不超发验证
- [x] MinIO 后端转存、三端独立视觉 token
- [x] 三端登录与 RBAC：code2session、员工 BCrypt 登录、微信绑定与免密登录、三套 JWT、按权限渲染菜单与宫格（M1）
- [x] 门店/营业时间/桌台、6 分类 33 道菜、规格加料、每日售罄、预定菜规则、Banner/公告（M2）
- [x] 33 张菜品图处理管线（列表图 4:3 且 ≤80KB）、PC 后台 CRUD、顾客端分类页与详情页（M2）
- [x] 购物车、确认订单、订单状态机与 PICKUP / DINE_IN 双分支、订单日志、取餐码与到店核销码（M3）
- [x] 预定菜提前 24 小时、强制 DINE_IN、200 元定金整单抵扣的硬校验（M3）
- [x] MQ 可靠性三件套：本地消息表、未确认消息定时重发、死信落库告警；延迟消息超时取消并回补库存（M3）
- [x] PC 后台订单看板 / 消息可靠性面板，顾客端购物车、确认订单与订单分包页（M3）
- [x] PayGateway 抽象、Mock 支付回调与 `payment_record` 幂等、退款单与制作中审核态、PC 后台退款审核 Tab（M4）
- [x] 支付成功 `PAY_SUCCEEDED` 事件经本地消息表投递，供 M5/M6 消费（M4）
- [x] 5 类券模板、领券与后台定向发券、卡包三 Tab、30 秒时间窗动态券码（M5）
- [x] `CouponVerifyService` 单份核销规则、PC 输码/扫码枪核销台、核销记录与券 ROI（M5）
- [x] 线上下单券抵扣与取消退回、首单支付后自动发新人券、券过期提醒延迟消息（M5）
- [x] 积分账户与流水、签到 / 分享 / 完善资料发放、订单完成发积分（M6）
- [x] 积分抵现「下单冻结 → 支付成功实扣 → 取消/超时回滚」，每日 00:05 过期批处理（M6）
- [x] 会员等级按年度消费额与累计积分自动升级，PC 后台等级配置与会员积分查询（M6）
- [x] 预约时段库存预生成（capacity 由 store_table 聚合）、Redis Lua + DB 双层防超订（M7）
- [x] 预约创建 / 审核 / 取消（2 小时限制）/ 改期（双桶回补）/ 到店签到（R 前缀动态码）（M7）
- [x] 延迟消息触发超时 15 分钟 NO_SHOW 并回补容量、提前 1 小时提醒落库（M7）
- [x] PC 后台预约管理（日历视图 + 审核 + 桌台看板）、小程序门店列表 / 详情与预约链路（M7）
- [x] 一单一评 + 可选单道菜品、评价图上传落 `review_image`、评价发积分与重复提交拦截（M8）
- [x] 菜品评分聚合与评价分页、菜品详情页评价区、订单 `canReview` 入口（M8）
- [x] `DishSearchService` 右模糊搜索（菜名 / 副标题 / 描述 / 标签）与稳定排序，接口预留换 ES（M8）
- [x] 历史订单推荐（最近 3 单、去重、排除下架售罄、仅登录可见）与首页「最近吃过」横滑（M8）
- [x] 搜索页与菜品评价列表页、首页搜索入口；Banner 权重与时间窗验收（M8）
- [x] 商家端小程序 11 页：工作台、订单看板与详情、扫码、核销结果、核销记录、预约管理、桌台看板、菜品售罄、我的（M9）
- [x] `POST /api/merchant/scan/resolve` 统一扫码：券码 / 预约签到码 / 取餐码按码串分流，失败返回可展示原因枚举（M9）
- [x] 前台轮询 + 震动的新单提醒、商家端订阅消息埋点与模板缺失时的 `message_record` 降级（M9）
- [x] PC 后台经营看板：今日净营收 / 订单数 / 客单价、实时预约看板、今日热销 Top 5（M10）
- [x] 数据报表：营收与订单趋势（按日 / 周 / 月）、热销排行、券 ROI、复购率与次日留存、高价值用户（M10）
- [x] 四类报表 Excel 导出（EasyExcel 4.0.3，行数上限 300，超限返回 30056）（M10）
- [x] `@JobLock` 注解 + AOP 切面接管 7 个兜底任务，抢不到锁直接跳过（M11）
- [x] 补齐库存键按 DB 重建（每日 00:10）、订单自动完成、券过期小时级兜底（M11）
- [x] 通知与告警收口到 `NotifySender` 接口，死信与投递失败告警可经接口分页查询（M11）
- [x] 三条端到端脚本、RabbitMQ 停机恢复不丢消息、重投不重复发积分（M11）
- [x] 交付文档四件：README、部署说明、接口清单、Mock 能力替换清单（M11）
- [x] 首页改版：按星期营业时间、取号 / 预约入口、团餐券板块、领券中心、最近吃过（M13）
- [x] tabBar 改为首页 / 点餐 / 领券 / 我的；自提预约时间、购物车改为从下弹出的面板（M13）。堂食扫码只把桌码暂存在本地，服务端绑定与营业状态联动见 M14 B2
- [x] 排队叫号：顾客取号 / 查号 / 取消、商家端叫号 / 入座（M13）。PC 后台 3 个排队接口已提供但页面未接入，见 M14 B2

## 技术栈

| 分类 | 技术 | 版本 |
| --- | --- | --- |
| 后端 | JDK + Spring Boot | 1.8.0_431 + 2.7.18 |
| ORM | MyBatis-Plus | 3.5.3.2 |
| 数据库 | MySQL | 8.0.37 |
| 缓存 | Redis | 6.2.6 |
| 消息队列 | RabbitMQ | 3.8.19（含延迟消息插件） |
| 对象存储 | MinIO Java SDK | 8.5.7 |
| 接口文档 | Knife4j（OpenAPI3） | 4.5.0 |
| 构建工具 | Maven | 3.9.8 |
| 报表导出 | EasyExcel（自带 POI 5.2.5） | 4.0.3 |
| PC 后台 | Vue 3 + Vite 5 + TypeScript + Element Plus + Pinia + ECharts | 3.4.x / 5.4.x / 5.5.x / 2.8.x / 2.2.x / 5.5.x |
| 顾客端小程序 | 微信原生框架（WXML/WXSS/ES2020） | 最低基础库 2.11.1 |
| 商家端小程序 | 微信原生框架 | 最低基础库 2.11.1 |
| Node.js | 构建 PC 后台 | 22.22.2 |

## 项目结构

```text
d:\MyProducts\come-often\
├── .vscode/                  # IDE 配置，语言服务器 JDK 与项目编译 JDK 分离
├── deploy/                   # 中间件初始化、图片管线、M1～M10 验收脚本与说明
│   └── image-pipeline/       # 33 张菜品图裁切调色、SQL 生成、MinIO 上传
├── docs/                     # 文档统一目录：需求、开发计划、视觉基线、菜品素材、交付文档、数据库脚本
│   ├── db/                   # 数据库脚本：DDL、种子数据、增量迁移
│   │   ├── schema.sql        # 43 张表 DDL
│   │   ├── seed-m1.sql       # M1 账号、角色、菜单、权限点种子数据
│   │   ├── seed-m2.sql       # M2 分类、菜品、图片、库存、运营位、桌台种子数据
│   │   └── migration-m13.sql # M13 增量迁移（排队号表 + 团餐券种子）
│   └── 菜品/                 # 菜品主数据 Excel 与图片素材
├── come-often-server/        # Spring Boot 后端（单 Maven 模块）
│   └── src/
│       ├── main/java/com/comeoften/
│       │   ├── common/       # 统一响应、错误码、全局异常；common/job 下是 @JobLock / JobResult / 切面
│       │   ├── config/       # MyBatis-Plus / Redis / RabbitMQ / MinIO / 通知通道 / Knife4j / 拦截器注册
│       │   ├── integration/  # 支付网关、微信 code2session、文件存储、通知通道（LOG + 订阅消息骨架）
│       │   ├── mq/           # 拓扑常量、生产者、发布确认、监听器、本地消息表、死信与告警
│       │   ├── job/          # 7 个兜底任务（MQ 重发 / 积分过期 / 预约未到店 / 时段滚动生成 / 库存重建 / 订单自动完成 / 券过期兜底）+ cron 接线 + 统一触发入口
│       │   ├── security/     # 三端 JWT、认证/权限拦截器、登录上下文
│       │   ├── module/       # auth / system / store / dish / home / cart / order / payment / coupon / points / reservation / review / stats / merchant / ops，域内按端拆 Controller
│       │   └── service/      # 跨域公共服务（库存等）
│       ├── main/resources/   # application*.yml、logback-spring.xml、lua/
│       └── test/             # 库存并发集成测试，状态机/金额/取餐码/SKU/券码/积分换算与等级判定纯逻辑测试
├── come-often-admin/         # Vue3 + Element Plus 商家 PC 后台
├── come-often-miniapp/       # 微信原生小程序（顾客端）
└── come-often-merchant/      # 微信原生小程序（商家端：工作台、订单看板、扫码核销、预约 / 桌台 / 售罄）
```

## 环境要求

- Windows 11，JDK 1.8.0_431（`C:\Program Files\jdk`），Maven 3.9.8
- Docker 27.1.1，已运行的 `mysql-8.0`、`redis-6.2.6`、`rabbitmq`、`minio` 四个容器
- RabbitMQ 已启用 `rabbitmq_delayed_message_exchange` 插件
- Node.js 22.22.2，微信开发者工具（开发期可用游客模式 `touristappid`）
- VSCode Java 扩展的语言服务器需 JDK 25，Spring Boot Tools 需 JDK 21，均已写入 `.vscode/settings.json`

## 快速开始

### 初始化中间件

只新建项目专用的 `come_often` / `come_often_test` 库、`/come-often` / `/come-often-test` vhost，不改动其他项目数据：

```powershell
.\deploy\init.cmd
```

`init.cmd` 的第 5 步会对两个库各导入一次 `docs/db/migration-m13.sql`：给 `queue_ticket` 补 `is_deleted`，并播种 3 张团餐券模板。脚本用 `CREATE TABLE IF NOT EXISTS`、`INSERT IGNORE` 和加列前的 `information_schema` 判断，重复执行不会重复插入或报重复列。已建库的机器直接重跑上一条 `.\deploy\init.cmd` 即可，**顺序必须是先补列再启动后端**：旧库没有 `queue_ticket.is_deleted` 时，排队查询会报 `Unknown column 'is_deleted'`。

只在单独排查迁移脚本时才手工导入，两库都要执行：

```powershell
docker cp docs\db\migration-m13.sql mysql-8.0:/tmp/migration-m13.sql
docker exec mysql-8.0 sh -c "mysql -uroot -p123456 come_often < /tmp/migration-m13.sql"
docker exec mysql-8.0 sh -c "mysql -uroot -p123456 come_often_test < /tmp/migration-m13.sql"
```

脚本说明与验证命令见 [deploy/init.md](deploy/init.md)。MinIO 私有桶由后端首次上传时自动创建。

`init.cmd` 会一并导入 `seed-m2.sql`（6 分类 33 道菜、99 条图片记录、当日库存、Banner/公告、营业时间与桌台）。这份 SQL 从 `docs/菜品/菜品.xlsx` 生成，改了 Excel 要先重新生成再初始化：

```powershell
python deploy\image-pipeline\generate_seed.py
python deploy\image-pipeline\process_images.py
```

`process_images.py` 产出 99 张 webp（列表 750×563、详情 750×500、缩略图 320×240）、`output/manifest.json`，以及 `output/preview/contact_sheet.jpg` 拼版。裁切和调色后要看一遍拼版：近方形原图裁成 4:3 会切掉上下约 9%，奶汤类白菜不能被暖调拉黄。管线依赖见 `deploy/image-pipeline/requirements.txt`。

### 后端启动

```powershell
.\build.cmd
.\run.cmd
```

`build.cmd` 只在当前进程内把 `JAVA_HOME` 指向 JDK 8，不改系统环境变量，执行时会打印 `mvn -v` 供核对。

### 登录账号与 M1 验收

`.\deploy\init.cmd` 会一并导入 `seed-m1.sql`。三个初始账号密码均为 `Come@2026`：

| 账号 | 角色 | PC 菜单 | 商家端宫格 |
| --- | --- | --- | --- |
| `admin` | 超级管理员 | 9 个（含系统设置） | 6 个全可见 |
| `manager` | 店长 | 8 个（无系统设置） | 6 个全可见 |
| `waiter` | 服务员 | 1 个（经营看板） | 3 个（接单、扫码、核销记录） |

后端运行时执行验收脚本，覆盖三端 401、跨端 token 拒绝、角色菜单差异、`@RequirePerm`、清空 `wx_openid` 立即踢登、退出登录：

```powershell
powershell -ExecutionPolicy Bypass -File .\deploy\verify-m1.ps1
```

把 token 有效期临时调成 10 秒验证静默续登：用 `JWT_APP_TTL_SECONDS=10`、`JWT_MERCHANT_TTL_SECONDS=10` 环境变量重启后端，再执行 `powershell -ExecutionPolicy Bypass -File .\deploy\verify-m1.ps1 -OnlyShortTtl`。

### M2 门店与菜品验收

后端运行时执行脚本，覆盖第七章的 5 条验收项：

```powershell
powershell -ExecutionPolicy Bypass -File .\deploy\verify-m2.ps1
```

脚本先把 99 张 webp 上传到 MinIO，再自建一道「两规格一加料」的菜验证金额试算，检查售罄拒单、下架不可见、预定菜角标与 200 元定金说明、图片管线产物体积与比例，最后删掉测试菜。图片已上传过时加 `-SkipUpload` 可跳过上传。上传脚本自己会登录一次 admin，M1 的 token 存储是「同一用户只保留最新 jti」，所以上传必须排在验收脚本取 token 之前。

### M3 购物车与订单验收

后端运行时执行，覆盖第七章的 5 条标准与 MQ 三件套：

```powershell
powershell -ExecutionPolicy Bypass -File .\deploy\verify-m3.ps1
powershell -ExecutionPolicy Bypass -File .\deploy\verify-m3.ps1 -SkipCompensation
```

脚本会先按 DB 重建当日 Redis 库存键，再验证购物车增删改、确认页金额取自服务端、取餐码同店同日不重复、CREATED→COMPLETED 全流程、`READY→CREATED` 返回 30002、预定菜 24 小时 / 强制 DINE_IN / 200 元定金整单抵扣、延迟消息到点取消并同时回补 DB 与 Redis、本地消息表 PENDING→SENT 与通知落库、重复投递幂等。默认模式还会把一条 SENT 消息改回 PENDING 等每分钟补偿任务重发（最多 75 秒），`-SkipCompensation` 跳过这段等待。脚本注释与输出全 ASCII，全部通过输出 `M3 VERIFY PASSED`。

### M4 支付与退款验收

后端运行时执行，覆盖第七章的 3 条标准与定金/尾款口径：

```powershell
powershell -ExecutionPolicy Bypass -File .\deploy\verify-m4.ps1
powershell -ExecutionPolicy Bypass -File .\deploy\verify-m4.ps1 -SkipWait
```

脚本登录 `admin` 与独立 `mockKey` 的顾客账号，先把 1002 当日备货设为 50 保证多次回归有库存，再验证：Mock 回调把订单转 PAID 并写入 1 条 `payment_record`、同一回调重放 3 次流水仍为 1 条且 `PAY_SUCCEEDED` 消息不增加、制作中（ACCEPTED）发起退款进入 `REFUNDING` 且退款单待审核、后台审核通过后转 `REFUNDED`、制作前（PAID）退款自动审核通过、定金单顾客端退款返回 30021 而商家主动发起只退已收的 200 元（不取整单 238）、Mock 网关下外部 `/api/pay/notify` 验签失败返回 30013。默认模式等 3 秒确认支付成功事件被消费者落库，`-SkipWait` 跳过。脚本注释与输出全 ASCII，全部通过输出 `M4 VERIFY PASSED`。

### M5 优惠券验收

后端运行时执行，覆盖第七章优惠券的五条标准：

```powershell
powershell -ExecutionPolicy Bypass -File .\deploy\verify-m5.ps1
powershell -ExecutionPolicy Bypass -File .\deploy\verify-m5.ps1 -SkipWindowWait
```

脚本登录 `admin` 与独立 `mockKey` 的顾客账号，先把 1002 当日备货设为 50，再验证：5 类券模板各建一张、5 张券各领一次并各核销一次成功、`coupon_use_record.operator_user_id` 等于 admin、同一券再次核销返回 30026、跨 1 个窗口的旧码仍可核销而跨 2 个窗口返回 30028、券 ROI 可查、线上下单券抵扣与取消退回、首单支付后自动发新人券、券过期提醒延迟链路。窗口边界需要真实等待（约 20 秒 + 60 秒），`-SkipWindowWait` 跳过这两段做快速冒烟。脚本注释与输出全 ASCII，全部通过输出 `M5 VERIFY PASSED`。

### M6 积分与等级验收

后端运行时执行，覆盖第七章「积分与等级」的四条标准：

```powershell
powershell -ExecutionPolicy Bypass -File .\deploy\verify-m6.ps1
powershell -ExecutionPolicy Bypass -File .\deploy\verify-m6.ps1 -SkipWait
```

脚本每次用新的 `mockKey` 注册会员，先把 1002 当日备货设为 50，再验证：账户初始为零值、签到 / 分享 / 完善资料各发一次且重复调用返回 30036 / 30037 / 30038、订单完成发积分且按实付金额与配置比例计算、临时把 SILVER 门槛改到 `0.01 / 0` 后完成订单自动升级（`finally` 恢复种子门槛）、删掉 Redis 幂等键后重投同一完成消息 3 次 `points_record` 仍只有 1 条、积分抵现下单写 FREEZE 并扣余额、延迟超时取消后写 ROLLBACK 并回滚余额、过期批处理扣减且重复执行不重复扣、后台四个查询接口可用。默认模式等延迟消息到点（约 6 秒），`-SkipWait` 改用显式取消走同一条回滚链路。脚本注释与输出全 ASCII，全部通过输出 `M6 VERIFY PASSED`。

### M7 预约订座验收

后端运行时执行，覆盖第七章「预约订座」的三条标准：

```powershell
powershell -ExecutionPolicy Bypass -File .\deploy\verify-m7.ps1
powershell -ExecutionPolicy Bypass -File .\deploy\verify-m7.ps1 -SkipWait
```

脚本先调后台生成接口把未来 7 天 × 13 个时段 × 3 类桌型的容量桶幂等建好（capacity 为 `store_table` 按桌型聚合的 14 / 4 / 4），再验证：重复生成不改变行数与容量、创建预约后 `reservation_slot.booked` 与 `comeoften:slot:cap:{slotId}` 同步、把某时段容量改成 5 后 20 路真并发（`Start-Job`）恰好成功 5 单且 DB 与 Redis 余量一致、距预约 30 分钟取消返回 30045、正常取消与改期后新旧时段双向回补、延迟消息到点置 NO_SHOW 并回补容量且重投不重复释放、提前提醒落 `message_record`、R 前缀签到码校验与重复签到返回 30046、后台分页 / 日历 / 桌台看板 / 容量键重建可用。默认模式等两段延迟消息（约 12 秒），`-SkipWait` 跳过等待做快速冒烟。脚本注释与输出全 ASCII，全部通过输出 `M7 VERIFY PASSED`。

回归 M1 / M2 / M3 / M4 / M5 / M6 / M7 时要串行执行：七份脚本都会登录 admin，token 存储只保留同一用户最新 jti。脚本会留下测试订单、退款单、券模板、核销记录、积分流水、预约单与消息记录供复查，不自动清理。与开发计划的逐条差异见 [deploy/init.md](deploy/init.md)。

### M8 评价 / 搜索 / 首页运营验收

后端运行时执行，覆盖第七章「评价 / 搜索 / 首页运营」的三条标准：

```powershell
powershell -ExecutionPolicy Bypass -File .\deploy\verify-m8.ps1
```

脚本每次用新的 `mockKey` 注册会员，真实走完 4 笔 COMPLETED 订单后验证：3 张评价图上传（复用图片管线的 320 宽缩略图）并落 `review_image`、评价发 10 积分且 `biz_id` 为评价 ID、重复评价返回 30051 且不重复发分、未完成订单 30052、非本单菜品 30053、非 `review/` 前缀图片 30054；搜索分别命中菜名 / 副标题 / 标签并验证空结果与分页稳定性；推荐只返回最近 3 单菜品、排除售罄、无 token 返回 401；Banner 按 weight 30/20/10 排序，过期与未生效不出现。脚本会临时软删种子轮播并在结束时恢复，创建的轮播会删掉。全部通过输出 `M8 VERIFY PASSED`。

M8 起八份脚本都要串行执行（都登录 admin）；与开发计划的逐条差异见 [deploy/init.md](deploy/init.md) 第 99～111 条。

### M9 商家端小程序验收

后端运行时执行，覆盖第七章「商家端小程序」的五条标准与权限边界：

```powershell
powershell -ExecutionPolicy Bypass -File .\deploy\verify-m9.ps1
powershell -ExecutionPolicy Bypass -File .\deploy\verify-m9.ps1 -SkipWindowWait
```

脚本登录 `admin`、`manager` 与一个全新会员，验证：工作台三个数字与订单看板计数一致；接单 / 出餐 / 确认取餐三步走通，`PAID→READY` 与商家端取消返回 30002；READY 订单扫取餐码转 COMPLETED、二次扫码提示已完成、制作中扫码提示未出餐；券扫码核销后 `coupon_use_record.operator_user_id` 为 manager、`verify_channel=MERCHANT_SCAN`，PC 后台核销记录页能查到同一条并带核销人，二次扫码返回 30026；跨 1 个窗口的旧码仍成功、跨 2 个窗口与篡改签名返回 30028；R 码签到写 `CHECKED_IN` / `checkin_user_id` / `checkin_time` 且重复签到失败；商家端售罄后顾客端下单被拒（20005），开售后恢复；支付成功自动落 `message_record(MERCHANT_NEW_ORDER)`，补发接口在模板缺失时返回 `sent=false` / `channel=LOG`；桌台看板可改状态并还原；`waiter` 可读核销记录，售罄 / 预约 / 桌台均 403。窗口校验需真实等待（约 32 秒 + 60 秒），`-SkipWindowWait` 跳过。全部通过输出 `M9 VERIFY PASSED`，退出码 0。

九份脚本串行执行（M9 只用 merchant token，admin 仍是同一份 jti）；M9 的新增差异见 [deploy/init.md](deploy/init.md) 第 112 条起。

### M10 数据看板与报表验收

后端运行时执行，覆盖第七章「数据看板与报表」的两条标准：

```powershell
powershell -ExecutionPolicy Bypass -File .\deploy\verify-m10.ps1
```

脚本先把一批真实订单走完接口链路（购物车 → 下单 → Mock 支付 → 后台推进状态），覆盖 PICKUP / DINE_IN、含券单、积分抵现单与一张已取消单；再用 SQL 在固定历史窗口 `2020-03-02 ~ 2020-03-08` 造 100 单（4 张已取消、4 张整单已退款、4 张退款待审核、4 张退款被拒、5 笔部分退款），订单号统一 `M10nnn`、会员统一 `m10-verify-*`，**重复执行会先清掉上一轮再重建**，因此数值稳定。断言分七段：看板今日净营收 / 退款 / 订单数 / 客单价 / 热销 / 预约数对齐 SQL；区间报表的汇总、7 个日桶、热销 Top、复购率、次日留存、高价值用户逐项对齐 SQL，并专门断言「有已退款单的那天」净营收 = 毛额 − 退款；券 ROI 按新建模板核对领取 / 核销 / 带动金额 / 折扣成本 / 投产比；`waiter` 只能看看板（报表与导出 403），临时收回菜单 100 后看板也 403、恢复后可用；导出核对 HTTP 200、ZIP 头、行数 = 接口行数 + 1、表头列数、金额单元格、空数据只出表头、366 天趋势导出返回 30056；最后校验报错码 30055 / 10001 与「报表不改任何订单数据」。全部通过输出 `M10 VERIFY PASSED`，退出码 0。

十份脚本串行执行（都登录 admin，token 存储只保留同一用户最新 jti）；M10 的新增差异见 [deploy/init.md](deploy/init.md) 第 137 条起。

### M11 联调与交付验收

后端运行时执行，覆盖第七章 M11 行的全部内容（三条端到端流程 + MQ 停机恢复 + 重复投递 + 死信告警 + 兜底任务）：

```powershell
powershell -ExecutionPolicy Bypass -File .\deploy\verify-m11.ps1
powershell -ExecutionPolicy Bypass -File .\deploy\verify-m11.ps1 -SkipMqOutage      # 不停 RabbitMQ
powershell -ExecutionPolicy Bypass -File .\deploy\verify-m11.ps1 -SkipWindowWait    # 不等 30 秒券码窗口
```

脚本登录 `admin`、`manager`（商家端）与一个全新 `mockKey` 的会员，把小程序侧的点击用接口扮演（UI 由接口代替，真机截图不在自动化范围内），然后验证：

1. **流程 A**：新客领券 → 自提下单（券抵扣 10 元）→ 商家端接单 / 出餐 → 扫取餐码 `A###` 完成 → 订单完成事件发积分，`points_record.biz_id={orderNo}#EARN` 只有 1 条且金额等于实付金额向下取整。
2. **流程 B**：预约 → 自动确认（`auto-confirm=false` 时走后台审核）→ 商家端扫 `R` 签到码置 `CHECKED_IN` 且记签到人 → 另造一张已过预约时刻的单，由 `reservation-no-show` 兜底任务置 `NO_SHOW` 并释放容量。
3. **流程 C**：领券 → 取券码（`C` 前缀）→ 等一个时间窗后重新取码，码串发生变化 → 商家端扫码核销成功、`coupon_use_record` 带核销人与 `MERCHANT_SCAN` → 二次扫码返回 30026。
4. **`@JobLock`**：脚本手动占住 `comeoften:job:lock:stock-rebuild`，调补跑接口返回 `executed=false` 且 Redis 库存键没被动过；放开锁后同一接口 `executed=true` 并把注入的漂移值改回 DB 值。真正的并发互斥（8 路并发只有 1 次执行）由单测 `JobLockAspectIntegrationTest` 覆盖——HTTP 层的并发窗口无法稳定制造。
5. **RabbitMQ 停机恢复**：`docker stop rabbitmq` 期间下单并支付，业务照常成功，该单 4 条本地消息全为 `PENDING`、无一条 `SENT`；`docker start rabbitmq` 后由补偿任务重发至全部 `SENT`、无 `FAILED`/`DEAD`，订单通知与商家新单提醒在恢复后被消费。**这一步会短暂影响共用该容器的其他项目，加 `-SkipMqOutage` 可跳过。**
6. **重复投递**：清掉订单完成消息的 Redis 幂等键后连投 3 次，`points_record` 仍只有 1 条、余额不变（判据是 `uk_points_idempotent(member_id, biz_type, biz_id)` 唯一索引）。
7. **死信告警**：往 `comeoften.order.notify.queue` 投一条无法反序列化的报文，重试耗尽后进死信，`message_record(MQ_ALARM)` 落一条，`GET /api/admin/mq/message/alarm` 能分页查到（类型 `MQ_ALARM`、通道 `LOG`）。
8. **兜底任务**：库存键重建、`READY` 超 24 小时自动完成并触发积分、券过期兜底首次提醒且重跑不重复、预约未到店扫描、积分过期补跑（与 M6 端点共用一把锁）。

造数可重复：合成的 `M11AUTO001` 订单、过期预约、未使用券在插入前先删除，真实订单用每轮新的 `mockKey`。全部通过输出 `M11 VERIFY PASSED`，退出码 0（本次 108 条断言、0 失败）。十二份脚本串行执行（含 `verify-m13.ps1`）；M11 的新增差异见 [deploy/init.md](deploy/init.md) 第 152 条起，M14 B0 的收口差异见第 172 条起。

### M13 首页改版与 M14 B0 收口验收

后端运行时执行，排在其余脚本之后（每次登录 admin 都会刷新 token）：

```powershell
powershell -ExecutionPolicy Bypass -File .\deploy\verify-m13.ps1
```

脚本用两个不同 `phoneCode` 的全新会员走排队链路（M12 按手机号唯一，只换 `mockKey` 会落到同一会员），验三件事：首页 8 条（按星期营业时间与门店状态、旧 `sections` 已删、取号 / 预约入口、团餐券板块、最近吃过、四项 tabBar、购物车底部弹层）；9 个排队接口的状态机（取号 / 查号 / 取消 / 分页 / 叫号 / 入座，重复取号 30059、取消后再叫号 30058、无 token 401）；两库 43 张表、`queue_ticket.is_deleted` 列、3 张团餐券种子，以及逻辑删除后分页不再返回该号、对该号叫号返回 30057（过滤前后做正向对照）。

造数日期取后端返回的 `bizDate`，软删与恢复按本轮票的主键定位，所有失败路径与 `finally` 都按 `member_id + biz_date` 清理，脚本可重复跑。全部通过输出 `M13 VERIFY PASSED`，退出码 0。

### 交付文档

| 文档 | 内容 |
| --- | --- |
| [README.md](README.md) | 项目全貌、验收方式、配置项、常见问题 |
| [docs/deployment.md](docs/deployment.md) | 部署说明：中间件准备、构建启动、配置与凭据、日常运维、升级回滚、故障排查 |
| [docs/api-list.md](docs/api-list.md) | 接口清单：163 个接口按端与分组列出，附权限点对照与业务码分段（M13 新增 10 个见文末） |
| [docs/mock-replacement.md](docs/mock-replacement.md) | Mock 能力替换清单：支付网关、微信登录、订阅消息、短信、上传，逐项写替换点与要改的配置 |
| [docs/milestone-m14.md](docs/milestone-m14.md) | M14 里程碑：2.md 未实现项与 M13 交付欠账，六批次范围、DDL 清单、验收口径与状态跟踪 |

### 显式运行测试

`pom.xml` 默认跳过测试编译与执行以加快打包，需要验证时显式开启（当前 186 个用例）：

```powershell
cd come-often-server
mvn test "-Dmaven.test.skip=false"
```

库存并发测试与 `@JobLock` 互斥测试直连本机 Redis 6.2.6 容器，使用 `comeoften:test:` / 独立任务名，结束后自行删除键。

### PC 后台启动

```powershell
cd come-often-admin
npm install
npm run dev
```

Vite dev server 在 `http://localhost:5173`，`/api` 代理到后端 8080。

### 两个小程序

用微信开发者工具分别导入 `come-often-miniapp` 与 `come-often-merchant` 目录，在 `project.config.json` 中把 `appid` 换成已有测试 AppID，勾选 ES6 转 ES5 后编译预览。开发期两端可共用一个测试 AppID；正式上线商家端需单独申请 AppID。

## 配置说明

| 配置项 | 说明 | 默认值 |
| --- | --- | --- |
| `spring.profiles.active` | 激活的 Profile | `dev` |
| `spring.datasource.url` | MySQL 连接 | `jdbc:mysql://127.0.0.1:3306/come_often` |
| `spring.redis.host` / `spring.redis.port` | Redis 连接 | `127.0.0.1` / `6379` |
| `spring.rabbitmq.virtual-host` | 项目专用 vhost | `/come-often` |
| `spring.rabbitmq.publisher-confirm-type` | 发布确认 | `correlated` |
| `spring.rabbitmq.listener.simple.acknowledge-mode` | 消费确认；`rabbitmq-reliability` 禁止 manual | `auto` |
| `spring.rabbitmq.listener.simple.retry.stateless` | 事务消费者每次重试开新事务 | `false` |
| `comeoften.order.pay-timeout-seconds` | 待支付超时秒数，下单发延迟消息；`ORDER_PAY_TIMEOUT_SECONDS` 覆盖 | `900` |
| `comeoften.mq.retry-interval-ms` | 未确认消息补偿扫描间隔（毫秒）；`MQ_RETRY_INTERVAL_MS` 覆盖 | `60000` |
| `comeoften.mq.pending-timeout-seconds` | PENDING 超过该秒数进入重发；`MQ_PENDING_TIMEOUT_SECONDS` 覆盖 | `60` |
| `comeoften.mq.max-retry` | 最大重发次数，超过转 FAILED + 告警；`MQ_MAX_RETRY` 覆盖 | `3` |
| `mybatis-plus.global-config.db-config.id-type` | 主键策略 | `ASSIGN_ID` |
| `comeoften.minio.*` | MinIO 端点、凭据、桶 | 见 `deploy/init.md` 环境变量表 |
| `comeoften.jwt.*-secret` / `*-ttl-seconds` | 三端独立 JWT 密钥与有效期（秒） | `86400` / `86400` / `7200` |
| `comeoften.wx.mock-enabled` | 是否走 Mock code2session，dev 无需 AppSecret | `true` |
| `comeoften.wx.appid` / `secret` | 真实小程序凭据，关闭 Mock 后必填 | `touristappid` / 空 |
| `comeoften.pay.gateway` | 支付网关；`mock` 本地默认，`wxpay` 需商户号（M4 只写骨架不启用） | `mock` |
| `comeoften.pay.wxpay.*` | 商户号、证书序列号、APIv3 密钥、私钥路径；由 `WXPAY_*` 环境变量注入 | 空 |
| `comeoften.coupon.code-secret` | 动态券码 HMAC-SHA256 密钥；`COUPON_CODE_SECRET` 覆盖，生产必须注入 | dev 内置长字符串 |
| `comeoften.coupon.expire-remind-ahead-hours` | 券过期提醒提前量（小时）；`COUPON_EXPIRE_REMIND_AHEAD_HOURS` 覆盖 | `24` |
| `comeoften.coupon.newcomer-template-id` / `newcomer-template-name` | 新人券模板：优先按 ID 匹配，留空按名称匹配发放中的同名模板 | 空 / `新人无门槛券` |
| `comeoften.points.order-earn-rate` | 每消费 1 元获得积分；`POINTS_ORDER_EARN_RATE` 覆盖 | `1` |
| `comeoften.points.exchange-rate` | 多少积分抵 1 元；抵现后订单至少保留 0.01 元；`POINTS_EXCHANGE_RATE` 覆盖 | `100` |
| `comeoften.points.sign-in-points` / `share-points` / `profile-points` / `review-points` | 签到、分享、完善资料、评价的发放额度；评价额度在 M8 生效，`biz_id` 为评价 ID | `5` / `5` / `20` / `10` |
| `comeoften.points.valid-days` | 积分有效期（天），写入 `points_record.expire_at`；`POINTS_VALID_DAYS` 覆盖 | `365` |
| `comeoften.points.expire-cron` / `expire-batch-size` | 每日过期批处理 cron 与单批条数；`@JobLock` 做单实例互斥 | `0 5 0 * * ?` / `200` |
| `comeoften.order.auto-complete-hours` / `auto-complete-batch-size` | `READY` 超过该小时数未取餐自动完成，单批条数；`ORDER_AUTO_COMPLETE_HOURS` 覆盖 | `24` / `200` |
| `comeoften.job.stock-rebuild-cron` | 三类库存键按 DB 重建的 cron | `0 10 0 * * ?` |
| `comeoften.job.order-auto-complete-cron` | 订单自动完成任务的 cron | `0 5 0 * * ?` |
| `comeoften.job.coupon-expire-cron` | 券过期提醒小时级兜底的 cron | `0 0 * * * ?` |
| `comeoften.coupon.expire-fallback-batch-size` | 券过期兜底的单批条数 | `200` |
| `comeoften.notify.channel` | 通知通道：`log` 落库 + 日志；`wx-subscribe` 为订阅消息骨架（同样降级落库）；`NOTIFY_CHANNEL` 覆盖 | `log` |
| `comeoften.notify.wx-subscribe-template-id` | 订阅消息模板 ID，只影响日志里的 `templateConfigured` | 空 |
| `comeoften.reservation.days-ahead` | 可预约未来天数（含当天），时段库存按 天数 × 时段 × 桌型 预生成；`RESERVATION_DAYS_AHEAD` 覆盖 | `7` |
| `comeoften.reservation.time-slots` | 开放预约的时段，逗号分隔 HH:mm；`RESERVATION_TIME_SLOTS` 覆盖 | `11:00`～`20:00` 共 13 个 |
| `comeoften.reservation.table-types` | 开放预约的桌型；`RESERVATION_TABLE_TYPES` 覆盖 | `HALL_CARD,PRIVATE_SMALL,PRIVATE_LARGE` |
| `comeoften.reservation.cancel-limit-hours` | 距预约不足该小时数不可取消，返回 30045 | `2` |
| `comeoften.reservation.no-show-minutes` | 预约时刻过后多少分钟未签到置 NO_SHOW，延迟消息按此投递 | `15` |
| `comeoften.reservation.remind-ahead-minutes` | 预约前多少分钟发提醒延迟消息 | `60` |
| `comeoften.reservation.auto-confirm` | 下单后是否自动确认；`false` 时停在 PENDING 等后台审核 | `true` |
| `comeoften.reservation.no-show-scan-cron` / `no-show-scan-batch-size` | 未到店兜底扫描 cron 与单批条数（实时仍走延迟消息） | `0 */5 * * * ?` / `200` |
| `comeoften.merchant.subscribe-template-id` | 商家端「新订单提醒」订阅消息模板；留空时降级为落 `message_record` + 日志 | 空 |
| `comeoften.stats.export-max-rows` | 报表 Excel 单次导出行数上限，超过返回 30056；`STATS_EXPORT_MAX_ROWS` 覆盖 | `300` |
| `comeoften.snowflake.worker-id` / `datacenter-id` | 多实例雪花节点号 | `0` / `0` |
| `spring.servlet.multipart.max-file-size` / `max-request-size` | 上传体积上限，容纳菜品原图 | `20MB` |
| `config/env.js` 的 `CURRENT_ENV` | 小程序接口环境 | `dev` |
| `config/env.js` 的 `FILE_BASE_URL` | 小程序图片只读地址 | `http://127.0.0.1:8080/api/public/file` |

凭据全部支持环境变量覆盖，生产环境不要沿用 dev 默认值。

## 部署指南

```powershell
cd come-often-server
mvn clean package -Dfile.encoding=UTF-8
java -Dfile.encoding=UTF-8 -jar target/come-often-server-0.0.1-SNAPSHOT.jar --spring.profiles.active=prod
```

运行日志在 `come-often-server/logs/app.log`，按时间与大小滚动。生产环境还需 HTTPS 与已备案域名、独立部署的中间件、微信支付 V3 证书与每日对账、短信与订阅消息资质，详见开发计划第十章。

中间件准备、环境变量清单、启动自检、日常运维（消息面板 / 兜底任务补跑 / 库存重建）、升级回滚与故障排查见 [docs/deployment.md](docs/deployment.md)。

## API 文档

启动后访问 `http://localhost:8080/doc.html`（Knife4j）。生产环境必须设置 `knife4j.production=true` 或加访问控制，避免文档裸奔。

按端与分组整理的全量接口清单（含权限点对照与业务码分段）见 [docs/api-list.md](docs/api-list.md)。

M0 已提供 `POST /api/admin/file/upload` 与 `GET /api/admin/file`，用于验证 MinIO 上传后能取回。

M1 起，登录接口无需 token，其余 `/api/app/**`、`/api/merchant/**`、`/api/admin/**` 都要带对应端的 Bearer Token：

| 端 | 接口 |
| --- | --- |
| 顾客端 | `POST /api/app/auth/login`、`GET /api/app/auth/info` |
| 商家端 | `POST /api/merchant/auth/login`、`POST /api/merchant/auth/auto-login`、`GET /api/merchant/auth/info`、`POST /api/merchant/auth/logout` |
| PC 后台 | `POST /api/admin/auth/login`、`GET /api/admin/auth/info`、`GET /api/admin/auth/menus`、`POST /api/admin/auth/refresh`、`POST /api/admin/auth/logout`、`GET /api/admin/auth/permission-probe` |

未登录返回 HTTP 401 + 业务码 40100（过期为 40101；跨端 token 因三套密钥不同，验签失败返回 40102），无权限返回 403 + 40301，员工未绑定门店返回 403 + 40302。

M2 新增分组：`/api/app/{home,dish,store}`、`/api/admin/{dish,store,home,file}`、`/api/public/file`。后台菜品/分类/库存/运营位接口需要 `dish:manage`，门店接口需要 `store:manage`，指定键上传需要 `file:upload`。`/api/public/file` 是匿名只读图片通道，只放行 `dish/`、`banner/`、`review/` 前缀，DB 只存 objectKey，不存完整 URL。

M2 业务码：20001 门店不存在、20002 菜品不存在、20003 分类不存在、20004 已下架、20005 今日已售罄、20006 规格选项不合法、20007 加料不合法、20008 预定菜仅限到店用餐、20009 对象键不合法、20010 分类名重复。

M3 新增分组：`/api/app/{cart,order}`、`/api/admin/order`、`/api/admin/mq/message`。后台订单与消息接口需要 `order:manage`，顾客端订单接口只需登录态。主要接口：

| 端 | 接口 |
| --- | --- |
| 购物车 | `GET/POST/DELETE /api/app/cart`、`PUT/DELETE /api/app/cart/{itemId}` |
| 顾客端订单 | `POST /api/app/order/preview`、`POST /api/app/order`、`GET /api/app/order`、`GET /api/app/order/{orderNo}`、`POST /api/app/order/{orderNo}/pay`、`POST /api/app/order/{orderNo}/cancel` |
| PC 后台 | `GET /api/admin/order/page`、`GET /api/admin/order/{orderNo}`、`POST /api/admin/order/{orderNo}/transition`、`POST /api/admin/order/{orderNo}/resend-timeout` |
| 消息可靠性 | `GET /api/admin/mq/message`、`POST /api/admin/mq/message/{messageId}/republish` |

金额一律由服务端计算：确认页取 `POST /api/app/order/preview`（内部复用 M2 的 `DishOrderService.check`），单品展示取 `/api/app/dish/quote`，页面不做业务计算。`POST /api/app/order/{orderNo}/pay` 自 M4 起改为调用 `PayGateway.prepay`，Mock 网关同步确认收款后由同一入账回调写流水，返回体仍是 `OrderVO`，M3 的验收脚本无需改动。

M3 业务码：30001 库存不足、30002 订单状态流转非法、30003 购物车项不存在、30004 购物车为空、30005 订单不存在、30006 无权操作该订单、30007 预定菜需提前 24 小时、30008 请选择到店用餐时间、30009 自提时间不合法、30010 支付金额不合法、30011 本地消息不存在。含预定菜却传自提时段仍返回 20008。

M4 新增分组：`/api/mock/pay`、`/api/pay/notify`、`/api/app/order/{orderNo}/refund`、`/api/admin/refund`。后台退款接口复用 `order:manage` 权限点，未新增菜单。主要接口：

| 端 | 接口 |
| --- | --- |
| Mock 回调 | `POST /api/mock/pay/success?orderNo=` |
| 渠道回调 | `POST /api/pay/notify`（V3 骨架；Mock 网关下验签失败返回 30013） |
| 顾客端退款 | `POST /api/app/order/{orderNo}/refund`、`GET /api/app/order/{orderNo}/refund` |
| PC 后台 | `GET /api/admin/refund`、`POST /api/admin/refund/{refundNo}/audit`、`POST /api/admin/refund/order/{orderNo}` |

回调按 `out_trade_no` 先查 `payment_record`，命中成功即返回，绝不二次改订单状态；`uk_out_trade_no` 唯一索引兜住并发插入。支付成功在同一事务内经本地消息表发 `STATUS_CHANGED(PAID)`（供订阅消息）与 `PAY_SUCCEEDED`（路由 `order.paid`，供 M5/M6 消费）两条消息。制作前（PAID）退款在同一事务内自动审核通过，制作中（ACCEPTED / READY）保持 `REFUNDING` 等后台审核；退款金额只取已收流水，定金单不把到店未付的尾款退出去。

M4 业务码：30012 支付回调报文不合法、30013 支付回调验签失败、30014 支付网关未启用、30015 支付流水不存在、30016 当前订单状态不支持退款、30017 退款单不存在、30018 已有处理中的退款申请、30019 退款单状态不允许审核、30020 退款金额不合法、30021 定金订单请联系商家发起退款。

M5 新增分组：`/api/app/coupon`、`/api/admin/coupon`。后台模板与发券需要 `coupon:manage`，核销需要 `coupon:verify`，核销记录需要 `coupon:record`（三个权限点 M1 种子已有）。主要接口：

| 端 | 接口 |
| --- | --- |
| 顾客端卡包 | `POST /api/app/coupon/{templateId}/receive`、`GET /api/app/coupon/page?tab=`、`GET /api/app/coupon/{userCouponId}/code` |
| PC 后台模板 | `GET /api/admin/coupon/template/page`、`GET/PUT /api/admin/coupon/template/{templateId}`、`POST /api/admin/coupon/template`、`POST /api/admin/coupon/template/{templateId}/status` |
| PC 后台发券与核销 | `POST /api/admin/coupon/grant`、`POST /api/admin/coupon/verify`、`GET /api/admin/coupon/record/page`、`GET /api/admin/coupon/roi`、`POST /api/admin/coupon/expire-probe` |
| 顾客端订单 | `POST /api/app/order/preview` 与 `POST /api/app/order` 新增 `userCouponId`，返回体新增 `discountAmount` / `couponId` / `availableCoupons` |

动态券码按计划 3.6：`6 位短序号 + HMAC-SHA256(短序号|时间窗序号) 截 10 位`，时间窗 = `epochSecond / 30`，校验当前窗口与前 1 个窗口，密钥走 `comeoften.coupon.code-secret`。核销只走 `CouponVerifyService` 一份规则，PC 入口标记为 `ADMIN_INPUT`，M9 商家端扫码复用同接口传 `MERCHANT_SCAN`。线上下单用券只锁 `user_coupon` 并写 `trade_order.coupon_id/discount_amount`，现场核销才写 `coupon_use_record`（带 `operator_user_id`）。

M5 业务码：30022 券模板不存在、30023 券未发放或已停用、30024 券已领完、30025 券不存在、30026 券已使用、30027 券已过期、30028 券码已失效、30029 未达到券使用门槛、30030 券不适用于当前门店、30031 券不适用于当前订单菜品、30032 券不在可领取时间内。

M6 新增分组：`/api/app/points`、`/api/admin/points`。后台积分与等级接口复用 M1 已有的 `member:manage`（菜单 105 用户管理），未新增菜单与权限点。主要接口：

| 端 | 接口 |
| --- | --- |
| 顾客端账户 | `GET /api/app/points/account`、`GET /api/app/points/record/page`、`GET /api/app/points/rule` |
| 顾客端发放 | `POST /api/app/points/sign-in`、`POST /api/app/points/share`、`POST /api/app/points/profile` |
| 顾客端等级 | `GET /api/app/points/level` |
| PC 后台 | `GET /api/admin/points/rule`、`GET /api/admin/points/level`、`PUT /api/admin/points/level/{levelId}` |
| PC 后台查询 | `GET /api/admin/points/member/page`、`GET /api/admin/points/record/page`、`POST /api/admin/points/expire/run` |
| 顾客端订单 | `POST /api/app/order/preview` 与 `POST /api/app/order` 新增 `usePoints`，返回体新增 `pointsAmount` / `pointsUsed` / `pointsBalance` / `maxUsablePoints` / `pointsExchangeRate` |

积分抵现按计划 3.7 走三段式：下单写 `FREEZE` 流水并扣余额（`UPDATE ... WHERE balance >= n`），支付成功把同一行 CAS 改成 `USE` 并累加 `total_used`，取消或超时插入 `ROLLBACK` 流水并加回余额。流水幂等由 `points_record.uk_points_idempotent` 兜底，`biz_id` 按用途加后缀（`{orderNo}#EARN` / `#REDEEM` / `#ROLLBACK`）。订单完成事件是新的 `TYPE_ORDER_COMPLETED`，经 `comeoften.member.exchange` 投递到 `comeoften.points.completed.queue`，消费者同时负责发积分与重算等级。

M6 业务码：30033 积分余额不足、30034 使用的积分不合法、30035 积分账户不存在、30036 今日已签到、30037 今日分享积分已领取、30038 完善资料积分已领取、30039 会员等级不存在、30040 等级门槛配置不合法。

M7 新增分组：`/api/app/reservation`、`/api/admin/reservation`。后台接口复用 M1 已有的 `reservation:manage`（菜单 103 预约管理），`reservation:audit` / `table:manage`（菜单 203 / 204）留给 M9 商家端，未新增菜单与权限点。主要接口：

| 端 | 接口 |
| --- | --- |
| 顾客端预约 | `GET /api/app/reservation/meta`、`GET /api/app/reservation/slot?date=`、`POST /api/app/reservation` |
| 顾客端我的预约 | `GET /api/app/reservation/page?tab=`、`GET /api/app/reservation/{reserveNo}`、`POST /api/app/reservation/{reserveNo}/cancel`、`POST /api/app/reservation/{reserveNo}/reschedule`、`GET /api/app/reservation/{reserveNo}/code` |
| PC 后台审核 | `GET /api/admin/reservation/page`、`GET /api/admin/reservation/{reserveNo}`、`POST /api/admin/reservation/{reserveNo}/confirm`、`POST /api/admin/reservation/{reserveNo}/reject` |
| PC 后台签到 | `POST /api/admin/reservation/{reserveNo}/checkin`、`POST /api/admin/reservation/checkin-by-code` |
| PC 后台看板 | `GET /api/admin/reservation/calendar`、`GET /api/admin/reservation/table-board`、`GET /api/admin/reservation/slot` |
| 运维 | `POST /api/admin/reservation/slot/generate`、`POST /api/admin/reservation/slot/rebuild`、`POST /api/admin/reservation/{reserveNo}/probe` |

时段容量按「日期 × 时段 × 桌型」预生成，`capacity` 取该桌型在 `store_table` 下的桌数（大厅 14 / 小包 4 / 大包 4），不是常量；`uk_slot` 兜底幂等，只有 `booked = 0` 的桶才会随桌台变化刷新容量。扣减固定顺序：Redis `tryOccupy(comeoften:slot:cap:{slotId}, 1, capacity)` → DB `UPDATE reservation_slot SET booked = booked + 1 WHERE id = ? AND booked + 1 <= capacity` → 影响行数为 0 立即回补 Redis 并返回 30041。签到码复用券码的时间窗 + HMAC 公共件（`DynamicCodeSigner`，scope 隔离），格式为 `R` + 预约单号 + 10 位签名，30 秒一个窗口，兼容前 1 个窗口。

M7 业务码：30041 时段已满、30042 时段不存在或未开放、30043 预约单不存在、30044 就餐人数与桌型不匹配、30045 距预约不足 2 小时无法取消、30046 预约状态不允许该操作、30047 无权操作该预约、30048 签到码已失效、30049 预约时间不合法、30050 该门店未开放所选桌型。

M8 新增分组：`/api/app/review`、`/api/app/file`、`/api/app/dish/search` 与 `/api/app/dish/recommend`。全部只需登录态，不新增权限点、不新增后台菜单。主要接口：

| 端 | 接口 |
| --- | --- |
| 顾客端评价 | `POST /api/app/review`、`GET /api/app/review/dish/{dishId}`、`GET /api/app/review/dish/{dishId}/rating` |
| 顾客端上传 | `POST /api/app/file/upload?prefix=review`（只接受 review 前缀与 jpg/jpeg/png/webp） |
| 顾客端搜索 | `GET /api/app/dish/search?keyword=&current=&size=`（默认 10 条、上限 20 条） |
| 顾客端推荐 | `GET /api/app/dish/recommend`（最近 3 单，未登录 401） |

评价一单一评由 `review.uk_order_member` 兜底，`dish_id` 可空且必须是该订单里的菜；评价图只收 `review/` 前缀的 objectKey（最多 3 张、单张 3MB、对象必须已上传），服务端不做压缩，端上 `wx.compressImage` 后上传。评价成功按 `comeoften.points.review-points` 发分，幂等键是 `biz_type=REVIEW` + `biz_id=评价 ID`，与 M6 共用同一套积分流水。搜索只用右模糊（菜名 / 副标题 / 描述 / 标签，`%` `_` 已转义），排序为菜名 > 标签 > 副标题 > 描述、同档招牌优先，`DishSearchService` 是后续换 ES 的替换点。菜品详情新增 `ratingAverage` / `reviewCount`（一条聚合 SQL，条数与平均分同源），订单详情与列表新增 `canReview` / `reviewed`，均由服务端计算。

M8 业务码：30051 该订单已评价、30052 订单完成后才能评价、30053 评价菜品不在该订单内、30054 评价图片数量或对象键不合法。

M9 新增分组：`/api/merchant/{workbench,order,scan,coupon,reservation,table,dish,notify}`，全部复用 M1 已有的商家端权限点（200~207），不新增菜单与权限点。主要接口：

| 端 | 接口 |
| --- | --- |
| 工作台 | `GET /api/merchant/workbench/summary`（`workbench:view`） |
| 订单看板 | `GET /api/merchant/order/page`、`GET /api/merchant/order/{orderNo}`、`POST /api/merchant/order/{orderNo}/transition`、`POST /api/merchant/order/{orderNo}/refund`（`order:accept`） |
| 统一扫码 | `POST /api/merchant/scan/resolve`（`scan:use`） |
| 核销记录 | `GET /api/merchant/coupon/record/page`、`GET /api/merchant/coupon/record/operators`（`coupon:record`） |
| 预约管理 | `GET /api/merchant/reservation/page`、`POST .../{reserveNo}/confirm`、`POST .../reject`、`POST .../checkin`（`reservation:audit`） |
| 桌台看板 | `GET /api/merchant/table/board`、`PUT /api/merchant/table/{tableId}/status`（`table:manage`） |
| 菜品售罄 | `GET /api/merchant/dish/soldout`、`PUT /api/merchant/dish/soldout`（`dish:soldout`） |
| 新订单提醒 | `POST /api/merchant/notify/new-order?orderNo=`（`order:accept`） |

扫码只分结果不分功能：前端只有一个扫码按钮，`scan/resolve` 按码串分流——券码 `C` + 6 位短序号 + 10 位签名、预约签到码 `R` + 预约单号 + 10 位签名、取餐 / 到店核销码 `^[A-D]\d{3}$`（`A001` 起，自提 A 段、到店 D 段）。计划 3.12 写的 `P` 前缀不采用：M3 已按 3.3 落地 `A###`/`D###`，实际码串里不会出现 `P`。失败不抛业务异常，统一返回 `success=false` + `failReason` / `failCode` / 可直接展示的 `resultMessage`（已使用 30026、已过期 30027、非本店 30030、码失效 30028、未出餐 `ORDER_NOT_READY`、已完成 `ORDER_COMPLETED`、无法识别 `CODE_UNRECOGNIZED`）。订单看板只放开 `PAID→ACCEPTED→READY→COMPLETED` 三步，退单走 `RefundService.applyByAdmin`（制作前自动通过，制作中 / 待取餐等 PC 审核）。

工作台三个订单数字是**当前积压**（含昨日未处理的单，与订单看板 `total` 同源），营收与预约数是当日口径：营收取当日 `paid_time` 落在区间、状态非 `CANCELLED`/`REFUNDED` 的 `pay_amount` 汇总。售罄复用 `DishStockService`，开售时未配置库存按默认 50 份补齐。发起退单额外要求 `SUPER_ADMIN` / `STORE_MANAGER` 角色（`waiter` 有 `order:accept` 但退款在制作前会自动通过，不能只靠权限点）。新订单提醒由既有 `order.notify` 消费者在 `PAID` 时落 `message_record(MERCHANT_NEW_ORDER)`；`comeoften.merchant.subscribe-template-id` 只影响 `templateConfigured` 字段，M9 的实际通道恒为 `LOG`，真实订阅消息发送留 M11。M9 未新增表、未改表结构。

M10 新增分组：`/api/admin/stats`。经营看板走 M1 已有的 `dashboard:view`（菜单 100），报表与导出走 `stats:view`（菜单 106），**不新增菜单与权限点**。主要接口：

| 端 | 接口 |
| --- | --- |
| 经营看板 | `GET /api/admin/stats/dashboard`（`dashboard:view`） |
| 数据报表 | `GET /api/admin/stats/report?startDate=&endDate=&granularity=&topN=`（`stats:view`） |
| 报表导出 | `GET /api/admin/stats/export?type=trend|hot-dish|coupon-roi|high-value-member&...`（`stats:view`） |

口径按 4.9 与第六章的两页分工：看板回答「今天怎么样」（今日净营收、订单数、客单价、预约看板、热销 Top 5，60 秒轮询），报表回答「一段时间怎么样」（区间汇总 + 趋势 + 热销 + 券 ROI + 复购留存 + 高价值用户，一个聚合接口返回整页）。净营收 = 区间内已支付实付金额 − 已退款金额，退款按**原订单 `paid_time`** 冲减；有效订单数排除 `CANCELLED` 与 `REFUNDED`；`REFUNDING` 与已拒绝（status=2）的退款不扣。趋势按 `paid_time` 自然日分桶，按周为周一起、按月为当月 1 日起，三种粒度之和恒等于区间汇总；默认近 7 天、按日、Top 10，区间上限 366 天。导出数据直接取 `/report` 的同一份结果，行数超过 300 返回 30056，空结果仍输出表头。

M10 业务码：30055 统计区间不合法（起止倒置或超 366 天）、30056 导出行数超过上限。

M11 没有新增业务分组，只在既有面板上加两个运维入口，复用 `order:manage`，**不新增权限点与菜单**：

| 端 | 接口 | 说明 |
| --- | --- | --- |
| 兜底任务 | `GET /api/admin/job` | 返回 7 个任务名：`mq-retry` / `points-expire` / `reservation-no-show` / `reservation-slot-generate` / `stock-rebuild` / `order-auto-complete` / `coupon-expire-fallback` |
| 兜底任务 | `POST /api/admin/job/{jobName}/run` | 手动补跑一次，返回 `{jobName, executed, count, detail}`；`executed=false` 表示锁被其他实例持有、本次跳过；任务名不存在返回 10001 |
| 告警查询 | `GET /api/admin/mq/message/alarm` | 只查 `message_record` 的 `MQ_ALARM`（死信与重发耗尽都落这个类型）；刻意不支持按任意类型查，避免门店级权限读到别店业务通知 |

调度入口在 `JobScheduler`（cron 只负责触发），执行体是被 `@JobLock` 代理的任务 Bean，手动补跑与 cron 共享同一把锁，因此同一个任务不会并发执行两次。M11 未新增表、未改表结构、未新增业务码（只有 10001 复用）。

## 常见问题

**Q: 终端里 `mvn -v` 显示 JDK 25，会不会编译出错？**
A: 项目编译只看 `JAVA_HOME`。`build.cmd` / `run.cmd` 已固定为 `C:\Program Files\jdk`，实测输出 `Java version: 1.8.0_431`。`.vscode/settings.json` 里的 JDK 25 只给 Java 语言服务器用，二者是不同进程。

**Q: 启动日志报 RabbitMQ vhost 不存在，或连不上 `come_often` 库？**
A: 说明还没执行 `.\deploy\init.cmd`。该脚本只新建项目专用资源，可重复执行。

**Q: 直接把 `schema.sql` 通过 PowerShell 管道喂给 `mysql` 出现中文乱码？**
A: 用 `deploy\init.cmd`，它把文件 `docker cp` 进容器后用容器内重定向导入，绕开宿主终端编码。

**Q: 商家端小程序和顾客端能共用一个 AppID 吗？**
A: 开发期可以，openid 相同不影响功能验证。上线前商家端必须换成独立 AppID（企业主体，内部管理工具类目）。

**Q: 没有微信 AppSecret，M1 的登录怎么跑通？**
A: dev 默认 `comeoften.wx.mock-enabled=true`，走 `MockMiniAppClient`：小程序端持久化的 `mockKey` 映射成稳定 openid，同一设备多次 `wx.login` 仍是同一个用户。拿到真实 AppID/AppSecret 后把该开关设为 `false` 并配置 `WX_APPID` / `WX_APPSECRET`，改用 `WxJavaMiniAppClient` 调 code2session，业务代码不用改。

**Q: 清空 `wx_openid` 后，还没过期的商家端 token 为什么立刻失效？**
A: `MerchantAuthInterceptor` 每次请求都复核 `sys_user` 的状态与微信绑定，不依赖 JWT 自然过期；`TokenStoreService` 另用 Redis 存当前有效 jti，退出登录或后台删 key 也能立即踢登。

**Q: 小程序里菜品图为什么不显示？**
A: 图片走 `GET /api/public/file?objectKey=...` 匿名只读通道。`/api/public/**` 不在三端鉴权拦截器的注册范围内，但服务端只放行 `dish/`、`banner/`、`review/` 前缀并拒绝路径穿越。先执行 `python deploy\image-pipeline\upload_images.py` 把产物传进 MinIO 桶；换域名时改 `come-often-miniapp/config/env.js` 的 `fileBaseUrl`，DB 里的 objectKey 不用动。

**Q: 后台改了库存，顾客端多久生效？**
A: 立即。设置备货或一键售罄在 DB 事务提交后重置 `comeoften:dish:stock:{storeId}:{dishId}:{yyyyMMdd}`，key 带 48 小时 TTL；DB 的 `remain` 是最终事实源，下单前校验直接读 DB。每日 00:10 由 `stock-rebuild` 任务按 DB 把三类库存键（菜品余量 / 券模板余量 / 时段占用）全量拉回，Redis 重启或漂移最多影响一天；要立刻纠正可调 `POST /api/admin/job/stock-rebuild/run`。

**Q: 后台新建菜品时没填当日备货会怎样？**
A: 默认按 50 份初始化。M3 下单前若发现 Redis 库存键不存在，会按 DB 的 `remain` 重建（带 48 小时 TTL），不会误判售罄；没有当日库存记录的菜品按「不限量」处理，与 M2 的下单校验一致。

**Q: 含预定菜的订单，200 元定金到底怎么算？**
A: 定金 = `min(200, 整单金额)`，是整单金额的一部分，不额外加收。`payAmount` 仍是整单应付，小程序先付定金即转 PAID，尾款 `payAmount − depositAmount` 到店付。含预定菜的订单由服务端强制为 `DINE_IN`，到店时间必须晚于当前 24 小时。M4 起 `/pay` 走 `PayGateway` 预下单，Mock 回调入账只记已收的 200 元；退款时同样只退这 200 元，未收的 38 元尾款不退，且按计划 4.7 只能由商家后台主动发起（顾客端申请返回 30021）。

**Q: 待支付订单多久自动取消？验收脚本为什么几秒就能看到？**
A: 默认 900 秒，可用 `ORDER_PAY_TIMEOUT_SECONDS` 覆盖。延迟消息到点触发取消，同时回补 DB 与 Redis 库存。`verify-m3.ps1` 不改全局配置，而是调 `POST /api/admin/order/{orderNo}/resend-timeout?delaySeconds=3` 补发一条短延迟消息走同一消费链路；消费端按订单幂等，自动消息与补发消息不会重复回补。

**Q: 下单用积分抵现，钱从哪一步扣？订单超时或取消会退回来吗？**
A: 下单时写一条 `FREEZE` 流水并用带余额条件的 `UPDATE` 扣减 `points_account.balance`，同时落 `trade_order.points_amount / points_used` 快照；支付成功后这条流水 CAS 改成 `USE` 并累加 `total_used`，余额不再变动；取消或超时写一条 `ROLLBACK` 流水把积分加回余额。`balance = Σ change_amount` 恒成立，可直接对账。抵扣比例在 `comeoften.points.exchange-rate`（默认 100 分抵 1 元），且抵扣后订单至少保留 0.01 元，避免 0 元支付。

**Q: 等级什么时候变？为什么后台把门槛调高了会员等级没掉？**
A: 等级在「订单完成」消息的消费者里算，按本自然年已支付且未取消 / 未退款的实付总额与 `points_account.total_earned` 双条件判定，取同时达标的最高档。规则只升不降：降级会牵涉已发权益，计划未定义，所以调高门槛后已有等级不会回退。等级折扣率当前只作权益展示，未参与订单金额计算（见 `deploy/init.md` 差异 67）。

**Q: 手动重投同一条订单完成消息，会不会重复发积分？**
A: 不会。消费端先按 `messageId` 走 Redis 去重，最终判据是 `points_record.uk_points_idempotent(member_id, biz_type, biz_id)` 唯一索引，`biz_id` 为 `{orderNo}#EARN`。`verify-m6.ps1` 每次重投前会先删掉 Redis 幂等键，专门验证数据库这一层，重投 3 次流水仍是 1 条。

**Q: 小程序里预约时段全是空的怎么办？**
A: 时段是运行时数据，`init.cmd` 不会生成。现在由 `reservation-slot-generate` 兜底任务每日 00:00 按未来 N 天滚动预生成（cron 可用 `JOB_RESERVATION_SLOT_GENERATE_CRON` 覆盖），不需要人工点按钮。新增门店、调整桌型或任务漏跑时可手动补跑：`POST /api/admin/job/reservation-slot-generate/run`；只补单店用 PC 后台「预约管理 → 生成时段库存」（`POST /api/admin/reservation/slot/generate`）。生成走 `uk_slot` 幂等 upsert，重复执行不会重置已占用容量。

**Q: 并发抢同一个时段会不会超订？Redis 与 DB 不一致时以谁为准？**
A: 不会。Lua `tryOccupy` 先原子判 `booked + n <= capacity`，DB 再用 `booked + 1 <= capacity` 的条件更新兜底，任一失败立即回补 Redis 并返回 30041。DB 的 `reservation_slot.booked` 是最终事实源；Redis 键缺失（重启 / 过期）时会按 DB booked 重建，`POST /api/admin/reservation/slot/rebuild` 可全量对齐。

**Q: 预约改期后，旧的超时消息会不会把新预约置成未到店？**
A: 不会。延迟消息里带投递时的 `reserveAtMillis`，消费者比对当前预约时刻，不一致就直接丢弃；改期会同时投新的超时与提醒消息。延迟插件不支持撤回消息，所以靠这个字段而不是撤回。

**Q: 搜索为什么搜不到菜名中间的字？为什么不直接上 MySQL FULLTEXT？**
A: 按 `db-spec` 与已定口径，搜索只做右模糊（`LIKE 'kw%'`），命中菜名、副标题、描述与标签，禁止 `LIKE '%kw%'` 全模糊——那类查询用不上索引，数据量一大就拖垮库。所以输入「烧肉」搜不到「红烧肉」，要搜词中或词尾只能等换 Elasticsearch。查询条件全部收在 `DishSearchService` 实现里，Controller 与页面只依赖接口和 `PageResult<DishListVO>`，换实现时不动契约。

**Q: 评价图为什么只有 3 张？宽 320 是服务端裁的吗？**
A: 接口与页面都限制 3 张、单张 3MB。服务端**不做图片处理**，只校验 `review/` 前缀、图片类型与对象是否存在；端上先用 `wx.compressImage`（quality 80）压一次再 `wx.uploadFile`，所以桶里的图不一定精确 320 宽。要把压缩强制放到服务端，需另接图片处理管线，M8 刻意没做。

**Q: 商家端扫码要选「券核销」还是「签到」吗？三码怎么区分？**
A: 不用选。商家端只有一个扫码按钮，`POST /api/merchant/scan/resolve` 按码串分流：`C` + 6 位短序号 + 10 位签名是券码、`R` + 预约单号 + 10 位签名是签到码、`A001`~`D999` 是取餐 / 到店核销码。计划 3.12 写的 `P` 前缀没有采用——M3 已按 3.3 生成 `A###`（自提）与 `D###`（到店），再造一个 `P` 等于多一种不存在的码。失败也是 HTTP 200 + `success=false`，附 `failReason` 与可直接展示的文案。

**Q: 真机上商家扫顾客的券码扫不出来？**
A: 顾客端券码页目前只显示分组文本码，没有渲染二维码（M5 差异 52 留到 M9，M9 也未补：二维码需要二维码库或后端出图，属新增依赖）。验收走接口直传码串，PC 后台「核销台」手输仍是可用主路径。要给真机演示补二维码，加渲染即可，不用改后端接口。

**Q: 商家端退到后台后收不到新订单提醒？**
A: 订阅消息是**一次性订阅**，必须员工在点「接单」时授权一次；没有模板 ID 时后端只落 `message_record(MERCHANT_NEW_ORDER)` 并打日志（`comeoften.merchant.subscribe-template-id` 留空即此路径）。前台则靠 10 秒轮询 + 震动 + 顶部提示条，退到后台只能靠订阅消息，这是平台硬约束，M11 再接真实发送。

**Q: 兜底任务会不会在两个实例上同时跑？停掉 RabbitMQ 期间下的单会丢吗？**
A: 都不会。任务方法标 `@JobLock`，锁是 Redis `SETNX + TTL`（键 `comeoften:job:lock:{任务名}`），抢不到锁**直接跳过**、不排队；cron 与手动补跑共用同一把锁，并发触发时只有一个真正执行（单测 `JobLockAspectIntegrationTest` 用 8 路并发验证）。Redis 整个不可用时按 fail-open 处理——任务照常执行，因为它们都按 DB 条件幂等（状态 CAS、唯一索引、比对后写入），一次缓存抖动不该让兜底能力整体停摆。

MQ 停机期间下单：业务事务照常提交，本地消息表里的记录停在 `PENDING`（付款接口也不会失败），`publisher-returns` / confirm 拿不到确认就交给每分钟的补偿任务；broker 恢复后重发至 `SENT`。`verify-m11.ps1` 用真实的 `docker stop/start rabbitmq` 跑这条链路：停机期间 4 条消息全 `PENDING` 且业务成功，恢复后全部 `SENT`、无 `FAILED`/`DEAD`。

**Q: 死信和投递失败的告警在哪儿看？**
A: 落 `message_record(MQ_ALARM)`，`GET /api/admin/mq/message/alarm`（`order:manage`）分页查，只暴露告警类型；消息本身的状态在 `GET /api/admin/mq/message?status=DEAD|FAILED`，可对 `messageId` 人工重放。M11 修了两个会让告警失真的问题：死信消费者此前用的是 JSON 容器工厂，无法反序列化的毒消息在调用监听器之前就抛异常，告警根本没落库；延迟交换机在消息到期前的 returned 回调（replyCode 312）被当成投递失败，导致每条延迟消息都多一条假 FAILED。现在死信走 `SimpleMessageConverter` 原始字节通道，延迟消息的 returned 只记 warn、不改状态（差异 155、157、171）。

**Q: 报表净营收和 M6 的年度消费额、M9 工作台的今日营收为什么对不上？**
A: 三套口径是刻意分开的。报表现行口径是**净额**（已支付 − 已退款，退款按原订单 `paid_time` 冲减），M6 年度消费额与 M9 工作台营收仍按 `pay_amount` 全额汇总、不扣部分退款（见 `deploy/init.md` 差异 67 与 M6 已知局限）。改动营收口径会牵动等级判定与已经验收的商家端数字，所以只在报表侧收敛。

**Q: 为什么报表按 `paid_time` 的自然日分桶，而不是取餐码用的 `biz_date`？**
A: 取餐码的 `biz_date` 是**营业日**，用来保证「同店同营业日取餐码不重复」，跨零点仍算前一天的生意；报表是**资金口径**，一笔 23:59 支付的单应该记在支付当天，否则跨零点当天的营收会忽然变负。两者服务不同目的，刻意不统一，差异见 `deploy/init.md` 第 138 条。
