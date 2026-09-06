# 邻车（社区车辆共享）后端接口文档

本文档面向 SpringBoot 后端实现，列出前端 `front-app` 需要的全部功能、接口路由、请求/响应格式与业务规则，以及建议的数据模型。实现时请**逐条核对，避免遗漏**。

---

## 1. 通用约定

### 1.1 服务地址

- 后端默认运行在 `http://localhost:8080`。
- 前端通过环境变量 `NEXT_PUBLIC_API_BASE` 指定后端地址（`app/lib/api.ts`）。

### 1.2 统一响应格式

所有接口返回 JSON，结构固定为：

```json
{
  "code": 0,
  "message": "ok",
  "data": { }
}
```

- `code`：`0` 表示成功，非 0 表示失败。
- `message`：提示信息；失败时是错误描述。
- `data`：业务数据（成功时），失败时为 `null`。

前端（`app/lib/api.ts`）在 `res.ok === false` 或 `code !== 0` 时抛出 `Error(message)`。

### 1.3 认证

- 采用 JWT。登录/注册成功后返回 `token`，前端存 `localStorage`，后续请求带请求头 `Authorization: Bearer <token>`。
- 除 `POST /api/auth/login`、`POST /api/auth/register` 外，其余接口都需要登录态（未带有效 token 返回 `401`）。

### 1.4 CORS

需要配置 CORS，允许前端来源（开发环境如 `http://localhost:3000`）访问，允许的方法至少包含 `GET / POST / PUT / DELETE / OPTIONS`，允许的请求头至少包含 `Content-Type` 与 `Authorization`。

### 1.5 图片静态资源

- 上传的车辆图片保存到服务器目录，通过静态资源路径暴露访问（例如 `/uploads/xxx.jpg`）。
- 接口返回的 `imageUrl` 建议为**绝对路径**（`http://localhost:8080/uploads/xxx.jpg`）；若返回相对路径（`/uploads/xxx.jpg`），前端会自动拼接 `API_BASE` 前缀（`app/rent/page.tsx`、`app/garage/page.tsx` 的 `imageSrc()`）。

---

## 2. 接口列表

### 2.1 认证

#### POST /api/auth/register — 注册

- 说明：手机号 + 密码注册。**手机号唯一**，已注册的手机号返回错误。
- 请求体（JSON）：

```json
{ "phone": "13800000000", "password": "123456" }
```

- 校验：手机号 11 位（`1` 开头）、密码至少 6 位。
- 成功响应 `data`：

```json
{
  "token": "jwt-token-string",
  "user": { "id": "1", "phone": "13800000000", "nickname": null }
}
```

- 失败示例：`{"code": 40001, "message": "该手机号已注册", "data": null}`

#### POST /api/auth/login — 登录

- 说明：手机号 + 密码登录。
- 请求体（JSON）：

```json
{ "phone": "13800000000", "password": "123456" }
```

- 成功响应 `data`：同注册（返回 `token` + `user`）。
- 失败示例：手机号不存在 / 密码错误，返回 `code != 0` + 对应 `message`。

> 密码使用 BCrypt 哈希存储，**不要明文存储**。

### 2.2 车辆（可借的闲置车辆）

#### GET /api/vehicles — 闲置车辆列表

- 说明：返回当前可借的闲置车辆（不含自己已租出的车）。前端用于 `/rent` 页，支持分类筛选。
- 可选 query 参数：`category`（`bike` / `ebike` / `car`），不传返回全部。
- 需要登录。
- 成功响应 `data`（数组）：

```json
[
  {
    "id": "BIKE-001",
    "name": "城市通勤车",
    "category": "bike",
    "imageUrl": "http://localhost:8080/uploads/bike001.jpg"
  }
]
```

- 字段说明：`category` 取值 `bike`（自行车）/ `ebike`（电动车）/ `car`（小轿车）；`imageUrl` 可为 `null`（前端显示占位图）。

#### POST /api/borrow — 借车

- 说明：当前用户开始借用某辆车，返回提示信息（前端展示“开始借用编号 xxx 的车辆”）。
- 请求体（JSON）：

```json
{ "vehicleId": "BIKE-001" }
```

- 需要登录。
- 业务校验：
  - 车辆存在且状态可用；
  - 同一用户不能重复借用同一辆未归还的车；
  - 根据计价规则与用户额度判断是否可借（见第 3 节）。
- 成功响应 `data`：

```json
{ "message": "开始借用编号 BIKE-001 的车辆" }
```

> 建议同时生成一条 `borrow_record`，并把车辆状态置为「租出中」。

### 2.3 我的车辆（车主管理）

#### GET /api/me/vehicles — 我的车辆列表

- 说明：返回当前用户发布的车辆（含「已租出 / 待出租」状态）。前端用于 `/garage` 页。
- 需要登录。
- 成功响应 `data`（数组）：

```json
[
  {
    "id": "M-001",
    "name": "折叠自行车",
    "desc": "车况良好，楼下 3 号车棚。",
    "imageUrl": "http://localhost:8080/uploads/m001.jpg",
    "status": "rented_out"
  }
]
```

- 字段说明：`status` 取值 `rented_out`（已租出）/ `available`（待出租）。

#### POST /api/me/vehicles — 新建车辆（含图片上传）

- 说明：发布自己的车辆卡片，`multipart/form-data` 上传图片 + 名称 + 介绍。
- 需要登录。
- 表单字段：
  - `file`：车辆图片（必填，`image/*`）
  - `name`：车辆名称（选填，前端默认「我的车辆」）
  - `desc`：车辆介绍（必填）
- 成功响应 `data`：新建的车辆对象（结构同 `GET /api/me/vehicles` 的数组元素，`status` 初始为 `available`）。

> 注意：该接口是 `multipart/form-data`，**不要**设置 `Content-Type: application/json`（前端 `apiUpload()` 未手动设置 Content-Type）。

### 2.4 额度和订阅

#### GET /api/me/quota — 当前用户免费额度

- 说明：返回当前用户剩余免费次数（前端在 `/rent` 页展示）。
- 需要登录。
- 成功响应 `data`：

```json
{ "freeCount": 18 }
```

- `freeCount`：剩余免费次数（按月卡/季卡规则计算，见第 3 节；无任何卡时为 `0`）。

#### POST /api/subscriptions — 购买月卡 / 季卡

- 说明：购买月卡或季卡。前端价格卡「开通月卡 / 开通季卡」按钮调用。
- 请求体（JSON）：

```json
{ "plan": "monthly" }
```

- `plan` 取值：`monthly`（月卡）/ `quarterly`（季卡）。
- 需要登录。
- 成功响应 `data`：

```json
{ "message": "月卡购买成功" }
```

---

## 3. 计价业务规则（后端必须实现）

### 3.1 按小时计费

- 起始价：`2 元`。
- 每多半小时：`+1 元`。
- 即：0～1 小时 2 元，1～1.5 小时 3 元，1.5～2 小时 4 元，以此类推。

### 3.2 月卡

- 增加 `30 次`免费额度即可。
- 每次使用：`1 小时内免费`；超出部分每半小时 `+1 元`。

### 3.3 季卡

- 增加 `90 次`免费额度即可。
- 每次使用：`1 小时内免费`；超出部分规则与月卡一致（每半小时 `+1 元`）。

### 3.4 额度扣减与结算建议

- 借车时（`POST /api/borrow`）优先消耗免费额度（月卡/季卡次数）。
- 还车时按实际时长结算费用：免费额度内免收，超出部分按「每半小时 +1 元」计费。
- `GET /api/me/quota` 返回的 `freeCount` 应反映当前周期内的剩余免费次数。

> 前端目前只做了「借车」动作；「还车 / 结算」接口作为后端预留（见 4.3 可选接口），前端暂未调用，但业务上建议实现。

---

## 4. 建议补充的接口（前端暂未调用，业务完整性建议实现）

### 4.1 还车 / 结算

- `POST /api/return`：请求体 `{ borrowId }`，结束一次借用，计算费用并扣减额度。

### 4.2 车辆管理

- `DELETE /api/me/vehicles/{id}`：删除自己的车辆。
- `PUT /api/me/vehicles/{id}`：更新自己的车辆信息。

### 4.3 车辆详情

- `GET /api/vehicles/{id}`：单辆车详情（当前前端用列表数据直接渲染弹窗，未单独调用）。

---

## 5. 数据模型建议（供建表参考）

### user（用户）

| 字段 | 类型 | 说明 |
|------|------|------|
| id | bigint | 主键 |
| phone | varchar(11) | 手机号，唯一 |
| password_hash | varchar | BCrypt 哈希 |
| nickname | varchar | 昵称（可空） |
| created_at | datetime | 注册时间 |

### vehicle（车辆）

| 字段 | 类型 | 说明 |
|------|------|------|
| id | varchar | 车辆编号（如 M-001） |
| owner_id | bigint | 车主（user.id） |
| name | varchar | 名称 |
| category | varchar | bike / ebike / car |
| desc | varchar | 介绍 |
| image_url | varchar | 图片路径（可空） |
| status | varchar | available / rented_out |
| created_at | datetime | 创建时间 |

### borrow_record（借车记录）

| 字段 | 类型 | 说明 |
|------|------|------|
| id | bigint | 主键 |
| user_id | bigint | 借车人 |
| vehicle_id | varchar | 车辆编号 |
| started_at | datetime | 开始时间 |
| ended_at | datetime | 结束时间（可空，未还车） |
| cost | decimal | 结算费用 |

### subscription（订阅 / 卡）

| 字段 | 类型 | 说明 |
|------|------|------|
| id | bigint | 主键 |
| user_id | bigint | 用户 |
| plan | varchar | monthly / quarterly |
| quota_total | int | 总免费额度 |
| quota_used | int | 已用额度 |
| started_at | datetime | 生效时间 |

---

## 6. 错误码约定（示例）

| code | 含义 |
|------|------|
| 0 | 成功 |
| 40000 | 参数错误 |
| 40001 | 手机号已注册 |
| 40002 | 手机号或密码错误 |
| 40100 | 未登录 / token 无效 |
| 40400 | 资源不存在 |
| 40900 | 状态冲突（如重复借车、车辆不可用） |

（具体 code 值可自行调整，但需保持 `code === 0` 为成功、其余为失败。）

---

## 7. 前端对接要点小结

- 前端统一通过 `app/lib/api.ts` 的 `apiGet / apiPost / apiUpload` 调用后端，自动携带 JWT、解析 `{ code, message, data }`。
- 登录态由 `app/ui/auth.tsx` 的 `AuthProvider` 全局管理，`token` 和 `user` 存 `localStorage`。
- 未登录点击「立即借车 / 立即租车 / 开通月卡 / 开通季卡」会先弹登录窗，登录成功后自动继续原动作（跳转或购买）。
