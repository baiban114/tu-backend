# tu-backend

面向 `tu-web-ts` 的后端工程骨架。

当前阶段先完成三件事：

1. 定义后端技术栈和容器运行方式
2. 输出与现有前端行为对齐的接口文档
3. 为后续“逐个接口实现”预留 Spring Boot 4 项目结构

## 技术选型

- Java: Eclipse Temurin 25
- Framework: Spring Boot 4
- Build: Maven
- Database: MySQL 8.4
- Runtime: Docker Compose

## 目录

- `docs/api-spec.md`: 首版接口文档
- `docker-compose.yml`: 本地开发编排
- `Dockerfile`: 应用镜像构建
- `src/main/...`: Spring Boot 启动骨架

## 启动说明

当前仓库先落骨架和文档，接口代码将在后续逐步补充。

开发阶段建议顺序：

1. 先实现知识库接口
2. 再实现页面树与页面内容接口
3. 再实现块引用与块同步接口

