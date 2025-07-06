package io.daocloud.prometheustestdemo.util;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.stereotype.Component;

import java.io.IOException;
import java.net.http.HttpClient;
import java.net.http.HttpRequest;
import java.net.http.HttpResponse;
import java.net.URI;
import java.time.Duration;
import java.util.concurrent.CompletableFuture;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;
import java.util.concurrent.atomic.AtomicInteger;
import java.util.concurrent.atomic.AtomicLong;

@Component
public class StressTestUtil {

    private static final Logger log = LoggerFactory.getLogger(StressTestUtil.class);

    private final HttpClient httpClient = HttpClient.newBuilder()
            .connectTimeout(Duration.ofSeconds(10))
            .build();

    public void runStressTest(String url, int concurrency, int totalRequests, int requestsPerSecond) {
        log.info("开始压测: URL={}, 并发数={}, 总请求数={}, QPS={}", 
                url, concurrency, totalRequests, requestsPerSecond);

        ExecutorService executor = Executors.newFixedThreadPool(concurrency);
        AtomicInteger successCount = new AtomicInteger(0);
        AtomicInteger failureCount = new AtomicInteger(0);
        AtomicLong totalResponseTime = new AtomicLong(0);

        long startTime = System.currentTimeMillis();
        long delayBetweenRequests = 1000L / requestsPerSecond;

        for (int i = 0; i < totalRequests; i++) {
            final int requestId = i;
            CompletableFuture.runAsync(() -> {
                try {
                    long requestStartTime = System.currentTimeMillis();
                    
                    HttpRequest request = HttpRequest.newBuilder()
                            .uri(URI.create(url))
                            .GET()
                            .timeout(Duration.ofSeconds(5))
                            .build();

                    HttpResponse<String> response = httpClient.send(request, 
                            HttpResponse.BodyHandlers.ofString());

                    long responseTime = System.currentTimeMillis() - requestStartTime;
                    totalResponseTime.addAndGet(responseTime);

                    if (response.statusCode() == 200) {
                        successCount.incrementAndGet();
                        log.debug("请求成功: ID={}, 状态码={}, 响应时间={}ms", 
                                requestId, response.statusCode(), responseTime);
                    } else {
                        failureCount.incrementAndGet();
                        log.warn("请求失败: ID={}, 状态码={}, 响应时间={}ms", 
                                requestId, response.statusCode(), responseTime);
                    }

                } catch (Exception e) {
                    failureCount.incrementAndGet();
                    log.error("请求异常: ID={}, 错误={}", requestId, e.getMessage());
                }
            }, executor);

            // 控制请求频率
            if (i < totalRequests - 1) {
                try {
                    Thread.sleep(delayBetweenRequests);
                } catch (InterruptedException e) {
                    Thread.currentThread().interrupt();
                    break;
                }
            }
        }

        // 等待所有请求完成
        executor.shutdown();
        while (!executor.isTerminated()) {
            try {
                Thread.sleep(100);
            } catch (InterruptedException e) {
                Thread.currentThread().interrupt();
                break;
            }
        }

        long endTime = System.currentTimeMillis();
        long totalTime = endTime - startTime;
        int totalRequestsMade = successCount.get() + failureCount.get();
        double avgResponseTime = totalRequestsMade > 0 ? 
                (double) totalResponseTime.get() / totalRequestsMade : 0;
        double actualQPS = totalRequestsMade > 0 ? 
                (double) totalRequestsMade / (totalTime / 1000.0) : 0;

        log.info("压测结果:");
        log.info("  总请求数: {}", totalRequestsMade);
        log.info("  成功请求: {}", successCount.get());
        log.info("  失败请求: {}", failureCount.get());
        log.info("  成功率: {:.2f}%", 
                totalRequestsMade > 0 ? (double) successCount.get() / totalRequestsMade * 100 : 0);
        log.info("  总耗时: {}ms", totalTime);
        log.info("  平均响应时间: {:.2f}ms", avgResponseTime);
        log.info("  实际QPS: {:.2f}", actualQPS);
    }
} 