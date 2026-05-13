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
- RAG 检索与重建索引

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

## 6. External Resource Management

The external resource module is self-developed metadata management. No standalone open-source resource manager is introduced.

### 6.1 Data Models

`ResourceType`

```json
{
  "id": "rt-1",
  "code": "book",
  "name": "Book",
  "icon": "📚",
  "description": "External books",
  "identityFieldKey": "isbn",
  "identityFieldLabel": "ISBN"
}
```

`ResourceWork`

```json
{
  "id": "rw-1",
  "typeId": "rt-1",
  "typeName": "Book",
  "title": "Example Work",
  "subtitle": "Optional subtitle",
  "description": "Abstract grouping for editions or releases"
}
```

`ResourceItem`

```json
{
  "id": "ri-1",
  "typeId": "rt-1",
  "typeName": "Book",
  "identityFieldKey": "isbn",
  "identityFieldLabel": "ISBN",
  "workId": "rw-1",
  "workTitle": "Example Work",
  "title": "Example Work, First Edition",
  "identityValue": "9780000000000",
  "sourceUrl": "https://example.com/book",
  "edition": "First Edition",
  "note": "Manually entered metadata"
}
```

### 6.2 Resource Type APIs

- `GET /api/resource-types`
- `POST /api/resource-types`
- `PATCH /api/resource-types/{id}`
- `DELETE /api/resource-types/{id}`

Create request:

```json
{
  "code": "book",
  "name": "Book",
  "icon": "📚",
  "description": "External books",
  "identityFieldKey": "isbn",
  "identityFieldLabel": "ISBN"
}
```

Rules:

- `code` is immutable after creation.
- `identityFieldKey` and `identityFieldLabel` are required.
- Delete returns `40009` when the type is used by works or items.

### 6.3 Resource Work APIs

- `GET /api/resource-works`
- `GET /api/resource-works?typeId=rt-1`
- `POST /api/resource-works`
- `PATCH /api/resource-works/{id}`
- `DELETE /api/resource-works/{id}`

Create request:

```json
{
  "typeId": "rt-1",
  "title": "Example Work",
  "subtitle": "Optional subtitle",
  "description": "Same resource across editions, releases, or sources"
}
```

Rules:

- Work must belong to an existing type.
- Work type cannot be changed while it has items.
- Delete returns `40009` when the work is used by items.

### 6.4 Resource Item APIs

- `GET /api/resource-items`
- `GET /api/resource-items?typeId=rt-1`
- `GET /api/resource-items?workId=rw-1`
- `GET /api/resource-items?typeId=rt-1&identityValue=9780000000000`
- `POST /api/resource-items`
- `PATCH /api/resource-items/{id}`
- `DELETE /api/resource-items/{id}`

Create request:

```json
{
  "typeId": "rt-1",
  "workId": "rw-1",
  "title": "Example Work, First Edition",
  "identityValue": "9780000000000",
  "sourceUrl": "https://example.com/book",
  "edition": "First Edition",
  "note": "Manually entered metadata"
}
```

Rules:

- Work must belong to the selected type.
- `identityValue` is unique within the same `typeId`.
- The first version stores metadata only and does not download or host external files.

## 7. 推荐实施顺序

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

## 8. 当前结论

后续接口实现应严格以当前前端行为为准，优先保证：

- 页面树与页面内容能完整跑通
- 引用块能回写源块
- `x6` 和 `line` 类型以 JSON 原样存储

### 4.5 RAG 接口

#### 4.5.1 知识库问答

- Method: `POST`
- Path: `/api/rag/query`

请求：

```json
{
  "kbId": "kb-demo-1",
  "query": "引用块如何加载？",
  "topK": 5
}
```

响应：

```json
{
  "code": 0,
  "message": "ok",
  "data": {
    "answer": "根据当前知识库中检索到的内容...",
    "sources": [
      {
        "kbId": "kb-demo-1",
        "pageId": "p-demo-1",
        "blockId": "b-demo-1",
        "title": "基础概念",
        "content": "页面、块内容和引用块...",
        "blockType": "richtext",
        "score": 0.82
      }
    ]
  }
}
```

#### 4.5.2 重建页面索引

- Method: `POST`
- Path: `/api/rag/reindex/page/{pageId}`

说明：

- 从 Java 后端读取页面内容，抽取 `richtext`、`table`、`container` 子块文本。
- `x6` 和 `line` 第一版只索引标题或可提取文本。
- 调用 FastAPI 内部 `/internal/rag/index`。

#### 4.5.3 重建知识库索引

- Method: `POST`
- Path: `/api/rag/reindex/kb/{kbId}`

说明：

- 遍历知识库下所有页面并逐页重建索引。
- 普通页面保存、块同步、块内容更新会自动触发单页 best-effort 重建索引。
