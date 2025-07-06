# Prometheus Test Demo - 云原生技术实践项目

## 项目概述

本项目是一个基于Spring Boot的REST应用，结合云原生技术栈（Docker、Kubernetes、Jenkins、Prometheus、Grafana）完成限流控制、持续集成部署、指标采集与扩容验证的全流程实践。

## 功能特性

### 1. REST API接口
- `/api/hello` - 返回固定JSON数据：`{"msg": "hello"}`
- `/api/health` - 健康检查接口
- `/api/stress/test` - 压测接口
- `/api/stress/status` - 压测状态接口

### 2. 限流控制
- 使用Bucket4j + Redis实现分布式限流
- 支持每秒100次请求的限流策略
- 超过限流阈值返回HTTP 429状态码
- 支持多实例统一限流（加分项）

### 3. 监控指标
- 暴露Prometheus指标：`/actuator/prometheus`
- 监控端口：8998
- 主要指标：
  - `http_server_requests_seconds_count` - 请求次数
  - `http_server_requests_seconds_sum` - 响应时间总和

### 4. 压测功能
- 内置压测工具
- 支持自定义并发数、总请求数、QPS
- 实时统计成功率、响应时间等指标

## 技术栈

- **后端框架**: Spring Boot 2.1.13
- **限流**: Bucket4j + Redis
- **监控**: Micrometer + Prometheus
- **容器化**: Docker
- **编排**: Kubernetes
- **CI/CD**: Jenkins
- **可视化**: Grafana

## 快速开始

### 1. 本地开发

#### 环境要求
- Java 8+
- Maven 3.6+
- Redis 6.0+

#### 启动步骤
```bash
# 1. 启动Redis
redis-server

# 2. 构建项目
mvn clean package

# 3. 运行应用
java -jar target/prometheus-test-demo-0.0.1-SNAPSHOT.jar
```

#### 测试接口
```bash
# 测试hello接口
curl http://localhost:8080/api/hello

# 测试健康检查
curl http://localhost:8080/api/health

# 查看监控指标
curl http://localhost:8998/actuator/prometheus

# 执行压测
curl -X POST "http://localhost:8080/api/stress/test?totalRequests=1000&requestsPerSecond=200"
```

### 2. Docker部署

#### 构建镜像
```bash
docker build -t prometheus-test-demo:latest .
```

#### 运行容器
```bash
docker run -d \
  --name prometheus-test-demo \
  -p 8080:8080 \
  -p 8998:8998 \
  --link redis:redis \
  prometheus-test-demo:latest
```

### 3. Kubernetes部署

#### 部署应用
```bash
# 创建命名空间
kubectl create namespace prometheus-demo

# 部署应用
kubectl apply -f k8s/deployment.yaml
kubectl apply -f k8s/service.yaml

# 部署监控
kubectl apply -f k8s/servicemonitor.yaml

# 部署自动扩缩容
kubectl apply -f k8s/hpa.yaml
```

#### 验证部署
```bash
# 查看Pod状态
kubectl get pods -n prometheus-demo

# 查看服务
kubectl get svc -n prometheus-demo

# 端口转发
kubectl port-forward svc/prometheus-test-demo-service 8080:80 -n prometheus-demo
```

## Jenkins CI/CD流水线

### 流水线功能
1. **代码拉取**: 从Git仓库拉取最新代码
2. **构建测试**: Maven构建并运行单元测试
3. **镜像构建**: 多阶段Docker构建
4. **镜像推送**: 推送到Harbor镜像仓库
5. **K8s部署**: 自动部署到Kubernetes集群
6. **健康检查**: 验证部署状态

### 触发方式
- 手动触发
- Git Webhook自动触发（加分项）

## 监控配置

### Prometheus配置
- 监控端点: `/actuator/prometheus`
- 采集间隔: 15秒
- 超时时间: 10秒

### Grafana仪表板
包含以下监控面板：
- CPU使用率
- 内存使用率
- JVM指标（堆内存、线程数、GC次数）
- 请求QPS
- 请求平均响应时间

## 压测验证

### 压测场景
1. **正常负载测试**: QPS < 100
2. **限流验证测试**: QPS > 100
3. **高并发测试**: 验证自动扩缩容
4. **长时间稳定性测试**: 验证系统稳定性

### 压测命令示例
```bash
# 正常负载测试
curl -X POST "http://localhost:8080/api/stress/test?totalRequests=500&requestsPerSecond=50"

# 限流验证测试
curl -X POST "http://localhost:8080/api/stress/test?totalRequests=1000&requestsPerSecond=200"

# 高并发测试
curl -X POST "http://localhost:8080/api/stress/test?concurrency=50&totalRequests=2000&requestsPerSecond=300"
```

## 项目结构

```
prometheus-test-demo/
├── src/
│   ├── main/
│   │   ├── java/io/daocloud/prometheustestdemo/
│   │   │   ├── controller/          # REST控制器
│   │   │   ├── config/             # 配置类
│   │   │   ├── interceptor/        # 拦截器
│   │   │   ├── annotation/         # 自定义注解
│   │   │   └── util/               # 工具类
│   │   └── resources/
│   │       └── application.properties
│   └── test/                       # 单元测试
├── k8s/                           # Kubernetes配置
│   ├── deployment.yaml
│   ├── service.yaml
│   ├── servicemonitor.yaml
│   └── hpa.yaml
├── jenkins/                       # Jenkins配置
│   └── scripts/
├── Dockerfile                     # Docker构建文件
├── Jenkinsfile                    # Jenkins流水线
└── pom.xml                       # Maven配置
```

## 评分标准实现

### 功能开发（20分 + 5分加分项）
- ✅ REST接口实现（5分）
- ✅ 限流控制（10分）
- ✅ Prometheus指标暴露（5分）
- ✅ 统一限流机制（+5分加分项）

### DevOps流水线（20分 + 5分加分项）
- ✅ Dockerfile多阶段构建（5分）
- ✅ Kubernetes YAML配置（5分）
- ✅ Jenkins CI流水线（5分）
- ✅ Jenkins CD流水线（5分）
- ⚠️ Git Webhook自动触发（+5分加分项）

### 监控与弹性扩展（15分 + 10分加分项）
- ✅ Prometheus指标采集（5分）
- ✅ Grafana监控面板（5分）
- ✅ 压测验证（5分）
- ✅ HPA自动扩缩容（+10分加分项）

## 注意事项

1. **Redis依赖**: 应用需要Redis服务，确保Redis可用
2. **资源限制**: Kubernetes部署包含资源限制配置
3. **监控端口**: 应用暴露8998端口用于监控指标
4. **健康检查**: 配置了liveness和readiness探针
5. **日志级别**: 可通过application.properties调整日志级别

## 故障排除

### 常见问题
1. **Redis连接失败**: 检查Redis服务状态和连接配置
2. **限流不生效**: 检查Redis连接和Bucket4j配置
3. **监控指标缺失**: 检查Actuator配置和Prometheus采集
4. **压测失败**: 检查网络连接和并发配置

### 日志查看
```bash
# 查看应用日志
kubectl logs -f deployment/prometheus-test-demo -n prometheus-demo

# 查看多个Pod日志
kubectl logs -f -l app=prometheus-test-demo -n prometheus-demo
```

## 贡献指南

欢迎提交Issue和Pull Request来改进项目。

## 许可证

本项目采用MIT许可证。
