#!/bin/bash

# 部署脚本
# 使用方法: ./deploy.sh <namespace>

NAMESPACE=${1:-"prometheus-demo"}
IMAGE_TAG=${2:-"latest"}

echo "开始部署到Kubernetes..."
echo "命名空间: $NAMESPACE"
echo "镜像标签: $IMAGE_TAG"
echo "========================"

# 创建命名空间
kubectl create namespace $NAMESPACE --dry-run=client -o yaml | kubectl apply -f -

# 更新deployment.yaml中的镜像标签
sed -i "s/prometheus-test-demo:latest/prometheus-test-demo:$IMAGE_TAG/g" k8s/deployment.yaml

# 部署应用
echo "部署应用..."
kubectl apply -f k8s/deployment.yaml -n $NAMESPACE

# 部署服务
echo "部署服务..."
kubectl apply -f k8s/service.yaml -n $NAMESPACE

# 部署监控
echo "部署ServiceMonitor..."
kubectl apply -f k8s/servicemonitor.yaml

# 部署HPA
echo "部署HPA..."
kubectl apply -f k8s/hpa.yaml -n $NAMESPACE

# 等待Pod就绪
echo "等待Pod就绪..."
kubectl wait --for=condition=ready pod -l app=prometheus-test-demo -n $NAMESPACE --timeout=300s

# 显示部署状态
echo "========================"
echo "部署状态:"
kubectl get pods -n $NAMESPACE
kubectl get svc -n $NAMESPACE
kubectl get hpa -n $NAMESPACE

echo "========================"
echo "访问信息:"
echo "应用端口转发: kubectl port-forward svc/prometheus-test-demo-service 8080:80 -n $NAMESPACE"
echo "监控端口转发: kubectl port-forward svc/prometheus-test-demo-service 8998:8998 -n $NAMESPACE"
echo "========================"
echo "部署完成!" 