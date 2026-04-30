# tu-backend

Java + Spring Boot backend for `tu-web-ts`.

## Stack

- Java 25
- Spring Boot 4
- Maven
- MySQL 8.4
- Docker Compose

## Run with Docker

```bash
docker compose up -d --build
```

Backend:

- `http://localhost:18080`

MySQL:

- `localhost:3306`

## Local development

Build the service:

```bash
mvn -q -DskipTests package
```

## Auth API

Spring Security is enabled for password hashing and login verification, but every endpoint is still allowed by default. There is no user state restriction, session requirement, or token gate on existing features.

Register:

```bash
curl -X POST http://localhost:18080/api/auth/register \
  -H "Content-Type: application/json" \
  -d "{\"username\":\"demo\",\"email\":\"demo@example.com\",\"password\":\"demo123456\",\"displayName\":\"Demo\"}"
```

Login:

```bash
curl -X POST http://localhost:18080/api/auth/login \
  -H "Content-Type: application/json" \
  -d "{\"account\":\"demo\",\"password\":\"demo123456\"}"
```

Main application source remains under:

- `src/main/java`
- `src/main/resources`


# 运行笔记
docker compose down

如果只想停服务但保留容器：

docker compose stop
之后再启动可用：

docker compose start
暂时停下开发
建议用：

docker compose stop
下次继续时用：

docker compose start
这适合：

你没有改 Dockerfile
你没有改 docker-compose.yml
你只是暂停一下，之后还想继续用原来的容器
新机器第一次启动
不能直接用 start。

因为 docker compose start 的前提是：

这些容器之前已经被 up 创建过
新机器上第一次没有现成容器，所以必须先：

docker compose up -d --build
或者如果镜像已经有了，也至少要：

docker compose up -d
start 只能启动“已经存在但当前停止的容器”，不能负责：

创建容器
创建网络
构建镜像
首次拉镜像
你可以这样记：

第一次启动：docker compose up -d --build
暂停：docker compose stop
恢复：docker compose start
完全收掉：docker compose down
什么时候还要再用 up -d --build
如果你改了这些内容之一，就不要只用 start：

Dockerfile
docker-compose.yml
后端 Java 代码
pom.xml
只有 docker compose down -v 才会把这个卷一起删掉
