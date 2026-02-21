# smartDoc Docker 部署说明

本文档说明如何使用 Docker 和 Docker Compose 在本地开发环境以及服务器上部署 smartDoc（包含后端 Java、前端 React、MySQL、MinIO、Milvus）。

> 说明：假设你已经在本机/服务器上安装好了 Docker 和 docker compose（Docker Desktop 或 docker compose 插件）。

---

## 1. 项目与编排文件概览

项目根目录下与部署相关的主要文件：

- `docker-compose.yml`：**开发/构建用** compose，负责在本机构建 backend/frontend 镜像并启动全套服务。
- `docker-compose.runtime.yml`：**运行时用** compose，适合在服务器上只使用已构建好的镜像部署（不需要 Maven/Node 环境）。
- `Dockerfile.backend`：后端 Spring Boot 镜像构建文件。
- `frontend/Dockerfile.frontend`：前端 React + Nginx 镜像构建文件。
- `frontend/nginx.conf`：前端 Nginx 配置（支持 SPA 前端路由）。
- `.env` / `.env.docker`：环境变量配置文件（下面详细说明）。

后端源代码位于根目录 `src/main/java`，前端位于 `frontend/` 目录。

---

## 2. 环境变量与多环境配置

### 2.1 后端环境变量

后端主要通过环境变量配置数据库、MinIO、Milvus、JWT 等，来源有两类：

1. **容器环境（Docker 内）**：通过 `env_file` 加载 `.env`（或 `.env.docker`），再由 `docker-compose` 中的 `environment` 覆盖部分关键字段（如 `DB_HOST`）。
2. **本地开发环境**：你可以在 IDE 或命令行中自行导出环境变量，或使用单独的 `.env.localdev` 文件。

#### 推荐文件划分

- `.env.docker`（示例，供 `docker-compose.yml` 使用，容器内部互联）：

   ```bash
   # 数据库配置（容器内访问 mysql 服务）
   DB_HOST=mysql
   DB_PORT=3306
   DB_NAME=smart_doc
   DB_USERNAME=root
   DB_PASSWORD=123456

   # JWT 配置
   JWT_SECRET=请替换为你自己的随机密钥
   JWT_ISSUER_URI=http://backend:8088

   # GitHub OAuth 配置（如不需要可留空或改为占位）
   GITHUB_CLIENT_ID=your_github_client_id
   GITHUB_CLIENT_SECRET=your_github_client_secret
   GITHUB_REDIRECT_URI=http://localhost:8088/api/auth/callback/github

   # AI 模型配置
   GLM_API_KEY=your_glm_api_key
   OPENAI_API_KEY=your_openai_api_key
   GEMINI_API_KEY=your_gemini_api_key

   # 嵌入模型配置
   EMBEDDING_API_KEY=notnull
   EMBEDDING_BASE_URL=http://your-embedding-service:9997/v1/
   EMBEDDING_MODEL_NAME=bge-m3

   # Milvus 配置（容器内部访问 milvus-standalone 服务）
   MILVUS_HOST=milvus-standalone
   MILVUS_PORT=19530

   # MinIO 配置（容器内部访问 minio 服务）
   MINIO_ENDPOINT=http://minio:9000
   MINIO_ACCESS_KEY=root
   MINIO_SECRET_KEY=12345678
   ```

   在 `docker-compose.yml` 中：

   ```yaml
   backend:
      env_file:
         - .env.docker
   ```

- `.env`（示例，供服务器 runtime 使用，可以是你的生产配置）：

   ```bash
   DB_HOST=mysql
   DB_PORT=3306
   DB_NAME=smart_doc
   DB_USERNAME=root
   DB_PASSWORD=123456

   JWT_SECRET=请替换为你自己的随机密钥
   JWT_ISSUER_URI=http://backend:8080

   # 其余如 AI、嵌入、Milvus、MinIO 配置同上
   ```

   在 `docker-compose.runtime.yml` 中通过：

   ```yaml
   backend:
      env_file:
         - .env
   ```

> 安全提示：实际生产环境请不要使用示例中的弱密码和固定密钥，并确保 `.env*` 不被提交到公开仓库。

### 2.2 前端环境变量

前端是 Create React App（CRA）风格，规则：

- 只有以 `REACT_APP_` 开头的变量才会被注入到前端代码中。
- 开发和生产可以使用不同的 env 文件。

推荐用法：

- 本地开发：在 `frontend/.env.development.local` 中配置：

   ```bash
   REACT_APP_API_BASE=http://localhost:8088
   ```

   这样 `npm start` 时，前端会调用本机 `8088` 的后端。

- Docker 构建 / 服务器运行：通过 compose 注入（注意端口要与后端映射一致）：

   ```yaml
   frontend:
      environment:
         - REACT_APP_API_BASE=http://localhost:8088
   ```

---

## 3. docker-compose.yml（本地构建 + 运行）

`docker-compose.yml` 适用于你在**本机**开发或构建镜像，包含：MySQL、MinIO、Milvus（etcd+MinIO+standalone）、后端、前端。

关键点：

1. **mysql**
    - 镜像：`mysql:8.0`
    - 端口映射：`13306:3306`
    - 卷：`mysql_data:/var/lib/mysql`

2. **minio**（业务文件存储）
    - 镜像：`minio/minio:RELEASE.2024-01-05T22-17-24Z`
    - 端口：`9000`（API）、`9001`（控制台）
    - 卷：`minio_data:/data`

3. **milvus-etcd / milvus-minio / milvus-standalone**（向量数据库）
    - `milvus-etcd`：自定义 `command`，监听 `0.0.0.0:2379`，单节点模式。
    - `milvus-minio`：Milvus 内部使用的 MinIO。
    - `milvus-standalone`：Milvus 主服务，端口 `19530`（gRPC）和 `9091`（监控）。

4. **backend**（smartDoc 后端，Spring Boot）
    - 使用 `Dockerfile.backend` 构建：

       ```yaml
       backend:
          build:
             context: .
             dockerfile: Dockerfile.backend
          env_file:
             - .env.docker
          environment:
             DB_HOST: mysql
             DB_PORT: 3306
             MILVUS_HOST: milvus-standalone
             MILVUS_PORT: 19530
             MINIO_ENDPOINT: http://minio:9000
             JWT_ISSUER_URI: http://backend:8088
          ports:
             - "8088:8088"
       ```

    - 外部访问：`http://localhost:8088`

5. **frontend**（smartDoc 前端）
    - 使用 `frontend/Dockerfile.frontend` 构建。
    - 端口映射：`3000:80`，即 `http://localhost:3000`。
    - `REACT_APP_API_BASE` 在 compose 中设为 `http://localhost:8088`，指向后端。

所有服务都在 `smartdoc-net` 网络中，容器之间通过服务名互相访问（如 `mysql`、`minio`、`milvus-standalone` 等）。

---

## 4. docker-compose.runtime.yml（服务器运行）

`docker-compose.runtime.yml` 假设：

- 你已经在服务器上构建或加载好了镜像：`smartdoc-backend`、`smartdoc-frontend`；
- 服务器上**不需要** Maven 和 Node，仅用 Docker 运行。

差异要点：

- `backend` 和 `frontend` 使用 `image:` 字段，不再包含 `build`：

   ```yaml
   backend:
      image: smartdoc-backend
      env_file:
         - .env
      ports:
         - "8080:8080"

   frontend:
      image: smartdoc-frontend
      environment:
         - REACT_APP_API_BASE=http://localhost:8080
      ports:
         - "3000:80"
   ```

- 其它服务（MySQL/MinIO/Milvus）配置与 `docker-compose.yml` 基本一致，只是去掉了 `build`。

服务器上只需要：

1. 一个 `.env`（生产配置）。
2. `docker-compose.runtime.yml`。
3. 已加载的镜像（通过 `docker load` 导入）。

---

## 5. 后端 Dockerfile.backend（已在仓库中）

当前 `Dockerfile.backend` 内容如下（已适配 JDK17、Jammy 基础镜像）：

```dockerfile
# 构建阶段
FROM maven:3.9.9-eclipse-temurin-17 AS build
WORKDIR /app

COPY pom.xml .
RUN mvn -B dependency:go-offline

COPY src ./src
RUN mvn -B package -DskipTests

# 运行阶段
FROM eclipse-temurin:17-jdk-jammy
WORKDIR /app

COPY --from=build /app/target/smartDoc-0.0.1-SNAPSHOT.jar app.jar

EXPOSE 8080

ENTRYPOINT ["java","-jar","/app/app.jar"]
```

> 注意：如果将来修改了项目版本或 jar 名称，请同步更新 `COPY --from=build` 这一行。

---

## 6. 前端 Dockerfile.frontend 与 Nginx 配置

`frontend/Dockerfile.frontend`：

```dockerfile
FROM node:20-bullseye AS build
WORKDIR /app

COPY package.json package-lock.json ./
RUN npm install

COPY . .
RUN npm run build

FROM nginx:1.27
COPY --from=build /app/build /usr/share/nginx/html
COPY nginx.conf /etc/nginx/conf.d/default.conf

EXPOSE 80
CMD ["nginx","-g","daemon off;"]
```

`frontend/nginx.conf`（支持前端路由的 SPA）：

```nginx
server {
   listen 80;
   server_name _;

   root /usr/share/nginx/html;
   index index.html;

   location / {
      try_files $uri $uri/ /index.html;
   }
}
```

---

## 7. 在本机构建与启动（开发/构建环境）

### 7.1 准备

1. 在项目根目录准备 `.env.docker`（参见上文示例）。
2. 确保本机可以访问 Maven 和 npm 源。

### 7.2 一键启动

在项目根目录执行：

```bash
# 构建并启动所有服务
docker compose up -d --build

# 查看容器状态
docker compose ps
```

启动成功后：

- 后端 API：<http://localhost:8088>
- 前端页面：<http://localhost:3000>
- MinIO 控制台：<http://localhost:9001>

---

## 8. 构建镜像并在服务器部署（推荐流程）

### 8.1 在本机构建镜像

在开发机（如 macOS）执行：

```bash
# 仅构建后端、前端镜像
docker compose build backend frontend

# 导出镜像（注意这里名称与你 compose 中的 image 一致）
docker save -o smartdoc-images.tar smartdoc-backend smartdoc-frontend
```

> 如果开发机是 Apple Silicon（arm64），而服务器是 x86（amd64），**不建议**直接在本机构建后导出给服务器使用，请在服务器上重新 `docker compose build backend frontend` 构建 amd64 镜像。

### 8.2 在服务器上构建/加载镜像

两种方式：

1. **在服务器上重新构建（推荐）**：

    - 将整个项目目录拷贝到服务器（如 `/root/smartdoc`）。
    - 在服务器执行：

       ```bash
       cd /root/smartdoc
       docker compose build backend frontend
       ```

2. **在服务器上加载导出的镜像**（只适用于架构一致的情况）：

    ```bash
    docker load -i smartdoc-images.tar
    ```

### 8.3 使用 runtime compose 一键启动

在服务器项目目录中：

```bash
cd /root/smartdoc

# 确认 .env 已根据服务器环境配置好
ls .env

# 使用 runtime 版本启动
docker compose -f docker-compose.runtime.yml up -d

# 查看运行状态
docker compose -f docker-compose.runtime.yml ps
```

服务器上访问：

- 前端：`http://<服务器IP>:3000`
- 后端：`http://<服务器IP>:8080`

---

## 9. 数据持久化与备份

在两个 compose 文件中都为以下组件配置了命名卷：

- `mysql_data`：MySQL 数据
- `minio_data`：业务 MinIO 对象数据
- `milvus_etcd` / `milvus_minio` / `milvus_data`：Milvus 相关数据
- `backend_doc_data` / `backend_processed_docs`：后端文档数据与处理结果

这些卷由 Docker 管理，备份示例（以 `mysql_data` 为例）：

```bash
docker run --rm \
   -v mysql_data:/data \
   -v $(pwd):/backup \
   alpine \
   sh -c "cd /data && tar czf /backup/mysql_data.tar.gz ."
```

恢复时可将 tar 包解压回对应卷中（注意在服务停止状态下操作）。

---

## 10. 常见问题

1. **前端能打开，但接口请求失败 / 跨域 / 404**
    - 检查前端的 `REACT_APP_API_BASE` 是否与后端对外地址一致（例如服务器上是 `http://<IP>:8080`）。
    - 本地开发时建议在 `frontend/.env.development.local` 中设置；Docker 中通过 compose 的 `environment` 注入。

2. **后端连不上 MySQL / MinIO / Milvus**
    - 容器内部访问请使用服务名：`mysql`、`minio`、`milvus-standalone`。
    - 用 `docker compose exec backend sh` 进入后端容器，尝试：

       ```bash
       ping mysql
       ping minio
       ping milvus-standalone
       ```

3. **Milvus 报 etcd 连接错误**
    - 确认 `milvus-etcd` 的 `command` 中已监听 `0.0.0.0:2379`，且 `--name default` 与 `--initial-cluster default=...` 名称一致。
    - 如修改过 etcd 配置，建议删除 `milvus_etcd` 卷后重新启动：

       ```bash
       docker compose down
       docker volume rm smartdoc_milvus_etcd  # 实际卷名以 docker volume ls 为准
       docker compose up -d
       ```

4. **端口冲突**
    - 修改 compose 中的宿主机端口映射，例如：

       ```yaml
       backend:
          ports:
             - "18080:8080"
       frontend:
          ports:
             - "13000:80"
       ```

5. **构建很慢或失败**
    - 确认网络可访问 Maven 中央仓库和 npm registry。
    - 可先在本机运行：

       ```bash
       mvn -B package -DskipTests
       cd frontend && npm install && npm run build
       ```

    - 确认通过后再进行 Docker 构建。

---

## 11. 停止与清理

```bash
# 停止服务，保留数据卷
docker compose down

# 使用 runtime 文件时
docker compose -f docker-compose.runtime.yml down

# 停止并删除数据卷（谨慎操作，会丢失所有数据）
docker compose down -v
docker compose -f docker-compose.runtime.yml down -v
```
