#!/bin/bash

# 压测脚本
# 使用方法: ./stress-test.sh <URL> <并发数> <总请求数> <QPS>

URL=${1:-"http://localhost:8080/api/hello"}
CONCURRENCY=${2:-10}
TOTAL_REQUESTS=${3:-1000}
REQUESTS_PER_SECOND=${4:-100}

echo "开始压测..."
echo "URL: $URL"
echo "并发数: $CONCURRENCY"
echo "总请求数: $TOTAL_REQUESTS"
echo "QPS: $REQUESTS_PER_SECOND"
echo "========================"

# 使用curl进行压测
start_time=$(date +%s)

# 创建临时文件存储结果
temp_file=$(mktemp)

# 并发请求
for i in $(seq 1 $TOTAL_REQUESTS); do
    (
        response=$(curl -s -w "%{http_code},%{time_total}" -o /dev/null "$URL")
        echo "$response" >> "$temp_file"
    ) &
    
    # 控制并发数
    if (( i % CONCURRENCY == 0 )); then
        wait
    fi
    
    # 控制QPS
    if (( i % REQUESTS_PER_SECOND == 0 )); then
        sleep 1
    fi
done

wait

end_time=$(date +%s)
total_time=$((end_time - start_time))

# 分析结果
success_count=0
failure_count=0
total_response_time=0
status_429_count=0

while IFS=',' read -r status_code response_time; do
    if [[ "$status_code" == "200" ]]; then
        ((success_count++))
    elif [[ "$status_code" == "429" ]]; then
        ((status_429_count++))
        ((failure_count++))
    else
        ((failure_count++))
    fi
    total_response_time=$(echo "$total_response_time + $response_time" | bc -l)
done < "$temp_file"

# 计算统计信息
total_requests=$((success_count + failure_count))
success_rate=$(echo "scale=2; $success_count * 100 / $total_requests" | bc -l)
avg_response_time=$(echo "scale=3; $total_response_time / $total_requests" | bc -l)
actual_qps=$(echo "scale=2; $total_requests / $total_time" | bc -l)

echo "========================"
echo "压测结果:"
echo "总请求数: $total_requests"
echo "成功请求: $success_count"
echo "失败请求: $failure_count"
echo "429状态码: $status_429_count"
echo "成功率: ${success_rate}%"
echo "总耗时: ${total_time}秒"
echo "平均响应时间: ${avg_response_time}秒"
echo "实际QPS: ${actual_qps}"

# 清理临时文件
rm "$temp_file"

echo "压测完成!" 