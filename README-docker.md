# smartRAG Docker 部署完整指南

本文档说明如何使用 Docker 和 Docker Compose 在本地开发环境以及生产服务器上部署 smartRAG 系统（包含后端 Java、前端 React、MySQL、MinIO、Milvus）。

> **前置要求**：已在本机/服务器上安装了 Docker 和 Docker Compose（Docker Desktop 或 docker compose 插件）。

---

## 📋 快速导航

- [1. 文件结构概览](#1-文件结构概览)
- [2. 端口配置说明](#2-端口配置说明)
- [3. 环境变量配置](#3-环境变量配置)
- [4. 本地开发部署](#4-本地开发部署)
- [5. 生产服务器部署](#5-生产服务器部署)
- [6. Docker 镜像构建](#6-docker-镜像构建)
- [7. 常见问题排查](#7-常见问题排查)
- [8. 数据备份与恢复](#8-数据备份与恢复)

---

## 1. 文件结构概览

项目根目录下与 Docker 部署相关的文件：

```
smartRAG/
├── docker-compose.yml                 # 开发/构建用 compose（本地使用）
├── docker-compose.runtime.yml         # 生产/运行时 compose（服务器使用）
├── Dockerfile.backend                 # 后端 Java 镜像构建文件
├── frontend/
│   ├── Dockerfile.frontend            # 前端 React + Nginx 镜像构建文件
│   └── nginx.conf                     # Nginx 反向代理配置（包含 API 代理）
├── .env.example                       # 环境变量参考示例（仅供参考）
└── src/                               # 后端 Java 源代码
    └── main/java/
```

### 文件用途

| 文件                           | 用途                | 说明                                                     |
| ------------------------------ | ------------------- | -------------------------------------------------------- |
| `docker-compose.yml`         | 开发/构建用 compose | 自动构建后端/前端镜像，所有配置直接定义在文件中          |
| `docker-compose.runtime.yml` | 生产/运行时 compose | 只使用预构建镜像，所有配置直接定义在文件中               |
| `Dockerfile.backend`         | 后端镜像构建        | 多阶段构建：Maven → OpenJDK 运行时                      |
| `Dockerfile.frontend`        | 前端镜像构建        | 多阶段构建：Node.js → Nginx                             |
| `nginx.conf`                 | Nginx 反向代理配置  | API 反向代理和 SSE 流式连接支持                          |
| `.env.example`               | 环境变量参考        | 仅供参考，不参与部署，所有配置已在 docker-compose 中定义 |

---

## 2. 端口配置说明

### 标准端口映射

| 服务                      | 容器内端口 | 宿主机端口 | 访问地址              | 说明                 |
| ------------------------- | ---------- | ---------- | --------------------- | -------------------- |
| **Frontend**        | 80         | 3000       | http://localhost:3000 | 前端 Web 应用        |
| **Backend**         | 8080       | 8080       | http://localhost:8080 | 后端 API             |
| **MySQL**           | 3306       | 13306      | localhost:13306       | 关系型数据库         |
| **MinIO (API)**     | 9000       | 9000       | http://localhost:9000 | 对象存储 API         |
| **MinIO (Console)** | 9001       | 9001       | http://localhost:9001 | MinIO 管理控制台     |
| **Milvus**          | 19530      | 19530      | localhost:19530       | 向量数据库 gRPC 服务 |
| **Milvus Metrics**  | 9091       | 9091       | http://localhost:9091 | Milvus 监控指标      |

### 重要说明

- **后端端口**：开发和生产均使用 `8080`
- **容器间通信**：容器内部使用服务名（如 `mysql`、`minio`、`backend`）而不是 `localhost`
- **容器外访问**：在宿主机上使用 `localhost:宿主机端口` 访问

---

## 3. 环境变量配置

### 3.1 配置方式说明

**所有环境变量现在都直接定义在 docker-compose 文件中的 `environment` 字段中**，无需额外的 `.env` 或 `.env.docker` 文件。这样做的优势：

✅ 配置更加清晰，所有设置都在 compose 文件中可见
✅ 避免了环境变量文件的复杂性
✅ 与 Kubernetes 等容器编排工具更兼容
✅ 易于通过环境变量覆盖（`export VAR=value` 或 `docker compose -e VAR=value`）

### 3.2 后端环境变量详解

#### 数据库配置

```yaml
DB_HOST: mysql              # MySQL 服务名或 IP（Docker 中使用服务名）
DB_PORT: 3306              # MySQL 端口
DB_NAME: smart_rag_db         # 数据库名
DB_USERNAME: root          # 数据库用户
DB_PASSWORD: 123456        # 数据库密码（生产环境请修改）
```

#### JWT 认证配置

```yaml
JWT_SECRET: 404E635266556A586E3272357538782F413F4428472B4B6250645367566B5970
# 用于签名 JWT Token，生产环境建议修改为强随机密钥
# 生成方法: openssl rand -base64 32

JWT_ISSUER_URI: http://backend:8080
# 令牌颁发者 URI，用于验证令牌有效性
# 在 Docker Compose 中使用服务名 backend
```

#### GitHub OAuth 配置（可选）

```yaml
GITHUB_CLIENT_ID: your_github_client_id
GITHUB_CLIENT_SECRET: your_github_client_secret
GITHUB_REDIRECT_URI: http://localhost:8080/api/auth/callback/github
# 如不使用 GitHub OAuth，可保留默认值
```

#### AI 模型配置

```yaml
# 至少需要配置一个 AI 模型

# 智谱 GLM（推荐）
GLM_API_KEY: your_glm_api_key_here

# OpenAI（可选）
OPENAI_API_KEY: your_openai_api_key_here

# Google Gemini（可选）
GEMINI_API_KEY: your_gemini_api_key_here
```

#### 嵌入模型配置

```yaml
EMBEDDING_API_KEY: notnull
EMBEDDING_BASE_URL: http://embedding-service:9997/v1/
EMBEDDING_MODEL_NAME: bge-m3
# 用于将文档分块转换为向量表示
# 可使用本地 Xinference 或 API 服务
```

#### 向量数据库配置（Milvus）

```yaml
MILVUS_HOST: milvus-standalone
MILVUS_PORT: 19530
# 在 Docker Compose 中使用服务名
# 在生产环境中可改为 Milvus 集群地址
```

#### 对象存储配置（MinIO）

```yaml
MINIO_ENDPOINT: http://minio:9000
MINIO_ACCESS_KEY: root
MINIO_SECRET_KEY: 12345678
# 用于存储用户上传的文档
# 在 Docker Compose 中使用服务名
# 在生产环境中可改为 AWS S3 或其他兼容服务
```

### 3.3 修改环境变量的方法

#### 方法 1：修改 docker-compose 文件（推荐）

直接编辑 `docker-compose.yml` 或 `docker-compose.runtime.yml` 中的 `environment` 部分：

```yaml
backend:
  environment:
    DB_PASSWORD: your_new_password
    GLM_API_KEY: your_actual_glm_key
```

然后重新启动服务：

```bash
docker compose down
docker compose up -d
```

#### 方法 2：使用环境变量覆盖（临时）

在启动前导出环境变量：

```bash
export DB_PASSWORD=your_new_password
export GLM_API_KEY=your_actual_glm_key
docker compose up -d
```

#### 方法 3：使用 -e 参数（临时）

```bash
docker compose up -d -e DB_PASSWORD=your_new_password -e GLM_API_KEY=your_actual_glm_key
```

### 3.4 前端环境变量

前端是 Create React App（CRA），环境变量规则：

- **只有 `REACT_APP_` 前缀**的变量会被注入到前端代码
- **必须在构建时设置**，运行时修改无效
- 在 `docker-compose.yml` 中通过 `build.args` 传递

```yaml
# docker-compose.yml 中的配置
frontend:
  build:
    context: ./frontend
    dockerfile: Dockerfile.frontend
    args:
      - REACT_APP_API_BASE=http://localhost:8080
```

**重要**：修改 `REACT_APP_API_BASE` 需要重新构建镜像：

```bash
docker compose build frontend
docker compose up -d frontend
```

---

## 4. 本地开发部署

### 4.1 前置准备

1. **确保文件存在**：

   ```bash
   ls -la docker-compose.yml Dockerfile.backend
   ls -la frontend/Dockerfile.frontend frontend/nginx.conf
   ```
2. **检查 Docker 安装**：

   ```bash
   docker --version
   docker compose version
   ```
3. **可选：调整资源限制**

   如果 Docker 内存/CPU 不足，可以在 Docker Desktop 设置中增加分配的资源。

### 4.2 一键启动所有服务

在项目根目录执行：

```bash
# 构建镜像并启动所有服务
docker compose up -d --build

# 查看容器运行状态
docker compose ps

# 查看详细日志
docker compose logs -f
```

### 4.3 验证部署成功

等待约 30-60 秒，服务完全启动。然后验证：

```bash
# 1. 检查后端健康状态
curl http://localhost:8080/api/health

# 2. 检查前端页面
curl http://localhost:3000

# 3. 检查 MySQL 连接
docker compose exec mysql mysql -uroot -p123456 -e "SELECT 1;"

# 4. 检查 Milvus 连接
docker compose exec backend curl http://milvus-standalone:19530/
```

### 4.4 访问应用

| 组件         | 访问地址              | 用途           |
| ------------ | --------------------- | -------------- |
| 前端页面     | http://localhost:3000 | 用户交互界面   |
| 后端 API     | http://localhost:8080 | REST API 端点  |
| MinIO 控制台 | http://localhost:9001 | 查看上传的文件 |
| Milvus 监控  | http://localhost:9091 | 向量库监控指标 |

### 4.5 实时开发工作流

```bash
# 修改代码后重新构建镜像
docker compose build backend    # 仅构建后端
docker compose build frontend   # 仅构建前端

# 重启服务
docker compose up -d backend frontend

# 查看新构建的容器日志
docker compose logs -f backend
```

---

## 5. 生产服务器部署

### 5.1 前置要求

1. **服务器环境**：

   - Linux 服务器（CentOS 7.x 或 Ubuntu 18.04+）
   - Docker >= 20.10
   - Docker Compose >= 2.0
   - 至少 4GB 内存、20GB 磁盘空间
2. **网络配置**：

   - 开放对外端口：3000（前端）、8080（后端 API）、9001（MinIO 控制台，可选）
   - 建议使用防火墙隔离内部通信端口

### 5.2 镜像构建与传输

#### 方案 A：在服务器上直接构建（推荐）

```bash
# 登录服务器
ssh user@your-server-ip

# 克隆项目（或上传代码）
cd /root
git clone https://github.com/your-org/smartRAG.git
# 或
scp -r ./smartRAG user@your-server-ip:/root/

# 进入项目目录
cd /root/smartRAG

# 构建后端和前端镜像
docker compose build backend frontend

# 确认镜像已构建
docker images | grep smartrag
```

#### 方案 B：在开发机构建并导出

```bash
# 在开发机上构建镜像
docker compose build backend frontend

# 导出镜像为 tar 文件
docker save -o smartrag-images.tar smartrag-backend:latest smartrag-frontend:latest

# 上传到服务器
scp smartrag-images.tar user@your-server-ip:/root/

# 在服务器上加载镜像
ssh user@your-server-ip
docker load -i smartrag-images.tar

# 验证镜像已加载
docker images | grep smartrag
```

**注意**：如果开发机和服务器架构不同（如 ARM64 vs AMD64），不建议使用方案 B，应该在服务器上重新构建。

### 5.3 配置生产环境

编辑 `docker-compose.runtime.yml` 中的 `environment` 部分，修改关键配置：

```bash
# 打开编辑器
vi docker-compose.runtime.yml
```

**必须修改的内容**：

```yaml
backend:
  environment:
    # 1. 数据库密码（使用强密码）
    DB_PASSWORD: your-strong-password-here

    # 2. JWT 密钥（使用强随机密钥）
    JWT_SECRET: $(openssl rand -base64 32)

    # 3. MinIO 凭证
    MINIO_ACCESS_KEY: your-minio-access-key
    MINIO_SECRET_KEY: your-strong-secret-key

    # 4. AI 模型配置
    GLM_API_KEY: your-production-glm-key
    OPENAI_API_KEY: your-production-openai-key
    # 其他模型密钥...

    # 5. 数据库连接（如使用外部数据库）
    DB_HOST: your-mysql-server-ip-or-domain
    MILVUS_HOST: your-milvus-server-ip-or-domain
    MINIO_ENDPOINT: http://your-minio-server-ip:9000
```

### 5.4 启动生产环境

```bash
cd /root/smartRAG

# 使用 runtime compose 启动（不再构建）
docker compose -f docker-compose.runtime.yml up -d

# 检查所有容器状态
docker compose -f docker-compose.runtime.yml ps

# 查看后端日志（检查启动是否有错误）
docker compose -f docker-compose.runtime.yml logs -f backend
```

### 5.5 健康检查和验证

```bash
# 检查容器健康状态
docker compose -f docker-compose.runtime.yml ps
# Status 应该显示 "Up" 且没有 "(unhealthy)"

# 检查后端 API 是否响应
curl -I http://localhost:8080/api/health

# 检查前端是否加载
curl -I http://localhost:3000

# 检查数据库是否运行
docker compose -f docker-compose.runtime.yml exec mysql mysql -uroot -pYOUR_PASSWORD -e "SELECT 1;"
```

### 5.6 反向代理配置（可选但推荐）

使用 Nginx 或 Apache 作为反向代理，提升安全性和性能：

```nginx
# /etc/nginx/sites-available/smartrag
server {
    listen 80;
    server_name your-domain.com;

    # 重定向到 HTTPS（可选但推荐）
    return 301 https://$server_name$request_uri;
}

server {
    listen 443 ssl http2;
    server_name your-domain.com;

    ssl_certificate /etc/ssl/certs/your-cert.crt;
    ssl_certificate_key /etc/ssl/private/your-key.key;

    # 前端服务
    location / {
        proxy_pass http://localhost:3000;
        proxy_set_header Host $host;
        proxy_set_header X-Real-IP $remote_addr;
        proxy_set_header X-Forwarded-For $proxy_add_x_forwarded_for;
        proxy_set_header X-Forwarded-Proto $scheme;
    }

    # API 服务
    location /api/ {
        proxy_pass http://localhost:8080;
        proxy_set_header Host $host;
        proxy_set_header X-Real-IP $remote_addr;
        proxy_set_header X-Forwarded-For $proxy_add_x_forwarded_for;
        proxy_set_header X-Forwarded-Proto $scheme;
    }
}
```

---

## 6. Docker 镜像构建

### 6.1 后端镜像构建（Dockerfile.backend）

```dockerfile
# 构建阶段：Maven 编译
FROM maven:3.9.9-eclipse-temurin-17 AS build
WORKDIR /app
COPY pom.xml .
RUN mvn -B dependency:go-offline    # 预下载依赖
COPY src ./src
RUN mvn -B package -DskipTests      # 编译打包

# 运行阶段：OpenJDK 17
FROM eclipse-temurin:17-jdk-jammy
WORKDIR /app
COPY --from=build /app/target/smartrag-3.0.jar app.jar
EXPOSE 8080
ENTRYPOINT ["java", "-jar", "/app/app.jar"]
```

**构建特点**：

- 多阶段构建：减小最终镜像大小
- 第一阶段使用 Maven 编译，第二阶段只保留 JDK 和 jar
- jar 文件在构建时被复制，容器内部不需要编译工具

### 6.2 前端镜像构建（Dockerfile.frontend）

```dockerfile
# 构建阶段：Node.js + npm
FROM node:20-bullseye AS build
WORKDIR /app
ARG REACT_APP_API_BASE=http://localhost:8080  # 接收构建参数
ENV REACT_APP_API_BASE=${REACT_APP_API_BASE}  # 设置环境变量
COPY package.json package-lock.json ./
RUN npm install --prefer-offline --no-audit
COPY . .
RUN npm run build                              # 构建 React 应用

# 运行阶段：Nginx
FROM nginx:1.27
COPY --from=build /app/build /usr/share/nginx/html
COPY nginx.conf /etc/nginx/conf.d/default.conf
EXPOSE 80
CMD ["nginx", "-g", "daemon off;"]
```

**构建特点**：

- 多阶段构建：第一阶段构建，第二阶段只保留静态文件和 Nginx
- 构建参数 `REACT_APP_API_BASE` 在构建时注入
- Nginx 配置支持 SPA 前端路由和 API 代理

### 6.3 手动构建镜像

```bash
# 仅构建后端
docker build -f Dockerfile.backend -t smartrag-backend:latest .

# 仅构建前端（可以指定不同的 API 地址）
docker build \
  --build-arg REACT_APP_API_BASE=http://api.your-domain.com:8080 \
  -f frontend/Dockerfile.frontend \
  -t smartrag-frontend:latest ./frontend

# 验证镜像已创建
docker images | grep smartrag
```

---

## 7. 常见问题排查

### 7.1 前端无法连接后端（API 请求失败）

**现象**：浏览器控制台显示 `404` 或 `ERR_CONNECTION_REFUSED`

**排查步骤**：

```bash
# 1. 检查后端容器是否运行
docker compose ps | grep backend
# 应该显示 "Up" 状态

# 2. 检查后端服务是否响应
curl http://localhost:8080/api/health

# 3. 检查前端配置的 API 地址
# 在 docker-compose.yml 中查看 REACT_APP_API_BASE 是否正确
grep -A5 "REACT_APP_API_BASE" docker-compose.yml

# 4. 重新构建前端镜像（如果修改过 API 地址）
docker compose build --no-cache frontend
docker compose up -d frontend

# 5. 检查 Nginx 反向代理日志
docker compose logs frontend | grep -i proxy
```

**解决方案**：

- 本地开发：确保 `REACT_APP_API_BASE=http://localhost:8080`
- 生产环境：使用实际的域名或 IP，如 `http://api.your-domain.com` 或 `http://your-server-ip:8080`

### 7.2 后端连接数据库失败

**现象**：后端日志显示 `Connection refused` 或 `Authentication failed`

**排查步骤**：

```bash
# 1. 检查 MySQL 容器是否运行
docker compose ps | grep mysql

# 2. 进入后端容器测试 MySQL 连接
docker compose exec backend sh
ping mysql        # 检查网络连接
telnet mysql 3306 # 检查端口是否开放

# 3. 验证 MySQL 用户和密码
docker compose exec mysql mysql -uroot -p123456 -e "SELECT 1;"

# 4. 检查环境变量是否正确
docker compose exec backend env | grep DB_
```

**解决方案**：

- 确保 `docker-compose.yml` 中的数据库凭证正确
- 确保 `DB_HOST=mysql`（服务名，不是 localhost）
- 重新启动 MySQL：`docker compose restart mysql`

### 7.3 Milvus 连接失败

**现象**：后端日志显示 `Milvus connection timeout` 或 `etcd connection error`

**排查步骤**：

```bash
# 1. 检查 Milvus 所有组件是否运行
docker compose ps | grep milvus

# 2. 检查 etcd 健康状态
docker compose logs milvus-etcd | head -20

# 3. 检查 Milvus 日志
docker compose logs milvus-standalone | grep -i error

# 4. 清理并重启 Milvus 及其依赖
docker compose down
docker volume rm smartrag_milvus_etcd smartrag_milvus_minio smartrag_milvus_data
docker compose up -d milvus-etcd milvus-minio milvus-standalone
```

**解决方案**：

- 给 Milvus 足够的启动时间（通常需要 30-60 秒）
- 确保有足够的磁盘空间
- 检查 Docker 日志查看具体错误：`docker compose logs milvus-standalone`

### 7.4 MinIO 连接失败或文件上传失败

**现象**：上传文件时返回 `403 Forbidden` 或 `Connection refused`

**排查步骤**：

```bash
# 1. 检查 MinIO 容器运行状态
docker compose ps | grep minio

# 2. 访问 MinIO 控制台
# 浏览器打开 http://localhost:9001
# 使用默认凭证登录：root / 12345678

# 3. 检查 MinIO API 响应
curl http://localhost:9000/

# 4. 验证环境变量
docker compose exec backend env | grep MINIO_
```

**解决方案**：

- 确保 `MINIO_ENDPOINT=http://minio:9000`（容器内服务名）
- 确保凭证与 docker-compose 中的配置一致
- 重启 MinIO：`docker compose restart minio`

### 7.5 磁盘空间不足

**现象**：Docker 容器启动失败或构建中止

**解决方案**：

```bash
# 1. 检查磁盘使用
df -h

# 2. 清理 Docker 未使用的资源
docker system prune -a

# 3. 查看各卷的大小
docker volume ls
du -sh /var/lib/docker/volumes/*

# 4. 删除不需要的卷
docker volume rm smartrag_mysql_data  # 谨慎操作，会丢失数据
```

### 7.6 端口已被占用

**现象**：启动 Docker 服务时报错 `Address already in use`

**解决方案**：

```bash
# 1. 查找占用端口的进程
sudo lsof -i :8080   # 查找占用 8080 的进程
sudo lsof -i :3000

# 2. 修改 docker-compose.yml 中的端口映射
# 例如，改为 8081:8080 和 3001:80
ports:
  - "8081:8080"
  - "3001:80"

# 3. 重启服务
docker compose down
docker compose up -d
```

---

## 8. 数据备份与恢复

### 8.1 备份数据卷

#### 备份 MySQL 数据

```bash
# 方式 1：使用 mysqldump（推荐，可读性强）
docker compose exec -T mysql mysqldump -uroot -p123456 smart_rag_db > smart_rag_db_backup.sql

# 方式 2：备份整个卷
docker run --rm \
  -v smartrag_mysql_data:/data \
  -v $(pwd):/backup \
  alpine \
  sh -c "tar czf /backup/mysql_data.tar.gz -C /data ."
```

#### 备份 MinIO 数据

```bash
docker run --rm \
  -v smartrag_minio_data:/data \
  -v $(pwd):/backup \
  alpine \
  sh -c "tar czf /backup/minio_data.tar.gz -C /data ."
```

#### 备份 Milvus 数据

```bash
docker run --rm \
  -v smartrag_milvus_data:/data \
  -v $(pwd):/backup \
  alpine \
  sh -c "tar czf /backup/milvus_data.tar.gz -C /data ."
```

### 8.2 恢复数据

#### 恢复 MySQL 数据

```bash
# 前置：确保 MySQL 容器已启动
docker compose up -d mysql

# 方式 1：从 SQL 文件恢复
docker compose exec -T mysql mysql -uroot -p123456 smart_rag_db < smart_rag_db_backup.sql

# 方式 2：从压缩包恢复
docker run --rm \
  -v smartrag_mysql_data:/data \
  -v $(pwd):/backup \
  alpine \
  sh -c "tar xzf /backup/mysql_data.tar.gz -C /data"
```

#### 恢复 MinIO 数据

```bash
# 停止 MinIO 容器（重要：防止数据冲突）
docker compose stop minio

# 恢复数据卷
docker run --rm \
  -v smartrag_minio_data:/data \
  -v $(pwd):/backup \
  alpine \
  sh -c "rm -rf /data/* && tar xzf /backup/minio_data.tar.gz -C /data"

# 启动 MinIO
docker compose up -d minio
```

### 8.3 定期备份脚本

创建 `backup.sh`：

```bash
#!/bin/bash
set -e

BACKUP_DIR="/mnt/backups"
DATE=$(date +%Y%m%d_%H%M%S)

echo "Starting backup at $(date)"

# 备份 MySQL
echo "Backing up MySQL..."
docker compose exec -T mysql mysqldump -uroot -p123456 smart_rag_db | gzip > "$BACKUP_DIR/mysql_$DATE.sql.gz"

# 备份 MinIO
echo "Backing up MinIO..."
docker run --rm \
  -v smartrag_minio_data:/data \
  -v $BACKUP_DIR:/backup \
  alpine \
  sh -c "tar czf /backup/minio_$DATE.tar.gz -C /data ."

# 删除 7 天前的备份
find $BACKUP_DIR -type f -mtime +7 -delete

echo "Backup completed at $(date)"
```

运行脚本：

```bash
chmod +x backup.sh
./backup.sh

# 使用 cron 实现定期备份（每天凌晨 2 点）
echo "0 2 * * * /root/smartRAG/backup.sh" | crontab -
```

---

## 9. 关键配置检查清单

部署前完整检查：

- [ ] `docker-compose.yml` 存在且包含所有必需的 environment 配置
- [ ] `docker-compose.runtime.yml` 存在且包含所有必需的 environment 配置
- [ ] `Dockerfile.backend` 和 `frontend/Dockerfile.frontend` 存在
- [ ] `frontend/nginx.conf` 存在且包含 API 代理配置
- [ ] Docker 和 Docker Compose 已安装
- [ ] 宿主机磁盘空间 >= 20GB
- [ ] 宿主机内存 >= 4GB（或 Docker 分配 >= 2GB）
- [ ] 所有必需的端口未被占用
- [ ] 生产环境中的关键密码和 API Key 已修改

---

## 10. 常用命令速查

```bash
# 启动服务
docker compose up -d
docker compose -f docker-compose.runtime.yml up -d

# 停止服务（保留数据）
docker compose down
docker compose -f docker-compose.runtime.yml down

# 停止服务（删除数据卷，谨慎）
docker compose down -v

# 查看容器状态
docker compose ps

# 查看日志
docker compose logs -f                    # 所有容器
docker compose logs -f backend            # 仅后端
docker compose logs backend --tail 100    # 最后 100 行

# 进入容器
docker compose exec backend sh
docker compose exec mysql bash

# 重建镜像
docker compose build --no-cache backend frontend

# 查看卷和磁盘用量
docker volume ls
docker volume inspect smartrag_mysql_data

# 清理资源
docker system prune -a        # 删除所有未使用的镜像和容器
docker volume prune           # 删除未使用的卷
```

---

## 11. 获取帮助

### 查看详细日志

```bash
# 查看所有日志
docker compose logs

# 查看特定服务的日志
docker compose logs backend --follow

# 查看特定时间范围的日志
docker compose logs --since "2024-01-01" --until "2024-01-02"
```

### 检查系统资源

```bash
# 查看容器资源使用
docker stats

# 查看镜像大小
docker images --format "table {{.Repository}}\t{{.Size}}"

# 查看网络连接
docker network ls
docker network inspect smartrag-net
```

### 联系与反馈

如遇到问题，请：

1. 查看 [常见问题排查](#7-常见问题排查) 部分
2. 查看详细日志找到错误信息
3. 确认所有环境变量配置正确
4. 尝试重新构建镜像并重启服务

---

**最后更新**：2025 年
**版本**：v2.1（采用方案 A - 所有配置直接在 docker-compose 中定义）
