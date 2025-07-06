package io.daocloud.prometheustestdemo.controller;

import io.daocloud.prometheustestdemo.util.StressTestUtil;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.web.bind.annotation.*;

import java.util.HashMap;
import java.util.Map;
import java.util.concurrent.CompletableFuture;

@RestController
@RequestMapping("/api/stress")
public class StressTestController {

    private static final Logger log = LoggerFactory.getLogger(StressTestController.class);

    @Autowired
    private StressTestUtil stressTestUtil;

    @PostMapping("/test")
    public Map<String, Object> runStressTest(
            @RequestParam(defaultValue = "http://localhost:8080/api/hello") String url,
            @RequestParam(defaultValue = "10") int concurrency,
            @RequestParam(defaultValue = "100") int totalRequests,
            @RequestParam(defaultValue = "50") int requestsPerSecond) {
        
        log.info("收到压测请求: URL={}, 并发数={}, 总请求数={}, QPS={}", 
                url, concurrency, totalRequests, requestsPerSecond);

        Map<String, Object> response = new HashMap<>();
        response.put("message", "压测已启动");
        response.put("url", url);
        response.put("concurrency", concurrency);
        response.put("totalRequests", totalRequests);
        response.put("requestsPerSecond", requestsPerSecond);

        // 异步执行压测
        CompletableFuture.runAsync(() -> {
            try {
                stressTestUtil.runStressTest(url, concurrency, totalRequests, requestsPerSecond);
            } catch (Exception e) {
                log.error("压测执行失败", e);
            }
        });

        return response;
    }

    @GetMapping("/status")
    public Map<String, String> getStatus() {
        Map<String, String> response = new HashMap<>();
        response.put("status", "running");
        response.put("timestamp", String.valueOf(System.currentTimeMillis()));
        return response;
    }
} 