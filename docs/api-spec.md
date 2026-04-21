# tu-backend 接口文档 v0.1

本文档根据当前前端项目 `tu-web-ts` 的实际调用与交互整理，用于后续按接口逐个实现。

当前阶段只覆盖单机版知识库，不包含：

- 登录鉴权
- 多用户隔离
- 审计日志
- 文件上传

## 1. 目标范围

后端需要支撑以下能力：

- 知识库管理
- 页面树管理
- 页面内容读写
- 块引用查询
- 被引用块内容回写
- 整页块同步

## 2. 基础约定

### 2.1 Base URL

`/api`

### 2.2 响应格式

成功：

```json
{
  "code": 0,
  "message": "ok",
  "data": {}
}
```

失败：

```json
{
  "code": 40001,
  "message": "page not found",
  "data": null
}
```

### 2.3 通用错误码

| code | 含义 |
| --- | --- |
| 0 | 成功 |
| 40000 | 请求参数错误 |
| 40001 | 资源不存在 |
| 40009 | 数据冲突 |
| 50000 | 服务器内部错误 |

## 3. 核心数据模型

### 3.1 KnowledgeBase

```json
{
  "id": "kb-1",
  "name": "个人笔记",
  "icon": "📘",
  "description": "日常学习与思考记录"
}
```

### 3.2 PageItem

```json
{
  "id": "p-1",
  "kbId": "kb-1",
  "parentId": null,
  "title": "快速入门",
  "order": 0,
  "children": []
}
```

### 3.3 Block

```json
{
  "id": "block-1001",
  "type": "richtext",
  "title": "示例",
  "content": "# 标题",
  "refId": null,
  "graphData": null,
  "timelineData": null,
  "layout": null,
  "children": []
}
```

支持的 `type`：

- `richtext`
- `line`
- `x6`
- `ref`
- `container`

### 3.4 BlockWithMeta

```json
{
  "block": {
    "id": "block-1001",
    "type": "richtext",
    "content": "可被引用的内容"
  },
  "pageId": "p-1",
  "pageTitle": "快速入门"
}
```

## 4. 接口清单

### 4.1 知识库接口

#### 4.1.1 获取知识库列表

- Method: `GET`
- Path: `/api/kbs`

响应：

```json
{
  "code": 0,
  "message": "ok",
  "data": [
    {
      "id": "kb-1",
      "name": "个人笔记",
      "icon": "📘",
      "description": "日常学习与思考记录"
    }
  ]
}
```

#### 4.1.2 新建知识库

- Method: `POST`
- Path: `/api/kbs`

请求：

```json
{
  "name": "技术文档",
  "icon": "📗",
  "description": "开发规范与技术方案"
}
```

#### 4.1.3 重命名知识库

- Method: `PATCH`
- Path: `/api/kbs/{id}`

请求：

```json
{
  "name": "项目规划"
}
```

#### 4.1.4 删除知识库

- Method: `DELETE`
- Path: `/api/kbs/{id}`

说明：

- 级联删除知识库下所有页面与块内容

### 4.2 页面接口

#### 4.2.1 获取页面树

- Method: `GET`
- Path: `/api/kbs/{kbId}/pages/tree`

#### 4.2.2 新建页面

- Method: `POST`
- Path: `/api/pages`

请求：

```json
{
  "kbId": "kb-1",
  "parentId": "p-2",
  "title": "新页面"
}
```

#### 4.2.3 更新页面

- Method: `PATCH`
- Path: `/api/pages/{id}`

说明：

- 同一个接口同时支持“重命名”和“移动排序”

请求示例一，重命名：

```json
{
  "title": "算法基础"
}
```

请求示例二，移动页面：

```json
{
  "parentId": "p-10",
  "order": 0
}
```

#### 4.2.4 删除页面

- Method: `DELETE`
- Path: `/api/pages/{id}`

说明：

- 级联删除该页面及其全部子页面

#### 4.2.5 获取页面内容

- Method: `GET`
- Path: `/api/pages/{id}/content`

响应：

```json
{
  "code": 0,
  "message": "ok",
  "data": {
    "pageId": "p-1",
    "blocks": [
      {
        "id": "block-1001",
        "type": "richtext",
        "content": "# 快速入门"
      }
    ]
  }
}
```

#### 4.2.6 保存页面内容

- Method: `PUT`
- Path: `/api/pages/{id}/content`

请求：

```json
{
  "blocks": [
    {
      "id": "block-1001",
      "type": "richtext",
      "content": "# 快速入门"
    },
    {
      "id": "block-1002",
      "type": "x6",
      "title": "默认画板",
      "graphData": {
        "nodes": [],
        "edges": []
      }
    }
  ]
}
```

### 4.3 块引用接口

#### 4.3.1 获取所有可引用块

- Method: `GET`
- Path: `/api/blocks`

说明：

- 返回全部非 `ref` 类型块
- 用于前端引用块选择器

#### 4.3.2 更新指定块富文本内容

- Method: `PATCH`
- Path: `/api/blocks/{id}/content`

请求：

```json
{
  "pageId": "p-1",
  "content": "更新后的文本"
}
```

说明：

- 用于引用块回写原块内容
- 仅适用于文本类块

#### 4.3.3 更新指定块画板内容

- Method: `PATCH`
- Path: `/api/blocks/{id}/graph`

请求：

```json
{
  "pageId": "p-1",
  "graphData": {
    "nodes": [],
    "edges": []
  }
}
```

### 4.4 块同步接口

#### 4.4.1 整页块同步

- Method: `POST`
- Path: `/api/blocks/sync`

说明：

- 当前前端已有统一同步入口
- 后端实现时建议兼容“整页 blocks 覆盖更新”

请求：

```json
{
  "pageId": "p-1",
  "blocks": [
    {
      "id": "block-1001",
      "type": "richtext",
      "content": "同步后的内容"
    }
  ]
}
```

## 5. 数据库建议表结构

### 5.1 knowledge_base

| 字段 | 类型 | 说明 |
| --- | --- | --- |
| id | varchar(64) | 主键 |
| name | varchar(128) | 名称 |
| icon | varchar(32) | 图标 |
| description | varchar(255) | 描述 |
| created_at | datetime | 创建时间 |
| updated_at | datetime | 更新时间 |

### 5.2 page

| 字段 | 类型 | 说明 |
| --- | --- | --- |
| id | varchar(64) | 主键 |
| kb_id | varchar(64) | 知识库 ID |
| parent_id | varchar(64) | 父页面 ID |
| title | varchar(128) | 页面标题 |
| sort_order | int | 排序号 |
| created_at | datetime | 创建时间 |
| updated_at | datetime | 更新时间 |

### 5.3 page_content

| 字段 | 类型 | 说明 |
| --- | --- | --- |
| page_id | varchar(64) | 页面 ID |
| blocks_json | longtext | 整页块 JSON |
| created_at | datetime | 创建时间 |
| updated_at | datetime | 更新时间 |

## 6. 推荐实施顺序

### 第一批

- `GET /api/kbs`
- `POST /api/kbs`
- `DELETE /api/kbs/{id}`
- `PATCH /api/kbs/{id}`

### 第二批

- `GET /api/kbs/{kbId}/pages/tree`
- `POST /api/pages`
- `PATCH /api/pages/{id}`
- `DELETE /api/pages/{id}`

### 第三批

- `GET /api/pages/{id}/content`
- `PUT /api/pages/{id}/content`

### 第四批

- `GET /api/blocks`
- `PATCH /api/blocks/{id}/content`
- `PATCH /api/blocks/{id}/graph`
- `POST /api/blocks/sync`

## 7. 当前结论

后续接口实现应严格以当前前端行为为准，优先保证：

- 页面树与页面内容能完整跑通
- 引用块能回写源块
- `x6` 和 `line` 类型以 JSON 原样存储
