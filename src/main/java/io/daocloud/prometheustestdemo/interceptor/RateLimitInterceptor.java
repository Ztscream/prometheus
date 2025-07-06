package io.daocloud.prometheustestdemo.interceptor;

import io.daocloud.prometheustestdemo.annotation.RateLimit;
import io.github.bucket4j.Bandwidth;
import io.github.bucket4j.Bucket;
import io.github.bucket4j.BucketConfiguration;
import io.github.bucket4j.Refill;
import io.github.bucket4j.distributed.proxy.ProxyManager;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.http.HttpStatus;
import org.springframework.stereotype.Component;
import org.springframework.web.method.HandlerMethod;
import org.springframework.web.servlet.HandlerInterceptor;

import javax.servlet.http.HttpServletRequest;
import javax.servlet.http.HttpServletResponse;
import java.time.Duration;
import java.util.concurrent.ConcurrentHashMap;

@Component
public class RateLimitInterceptor implements HandlerInterceptor {

    private static final Logger log = LoggerFactory.getLogger(RateLimitInterceptor.class);

    @Autowired
    private ProxyManager<String> proxyManager;

    private final ConcurrentHashMap<String, Bucket> bucketCache = new ConcurrentHashMap<>();

    @Override
    public boolean preHandle(HttpServletRequest request, HttpServletResponse response, Object handler) throws Exception {
        if (!(handler instanceof HandlerMethod)) {
            return true;
        }

        HandlerMethod handlerMethod = (HandlerMethod) handler;
        RateLimit rateLimit = handlerMethod.getMethodAnnotation(RateLimit.class);
        
        if (rateLimit == null) {
            return true;
        }

        String key = rateLimit.key().isEmpty() ? 
            request.getRequestURI() : 
            rateLimit.key() + ":" + request.getRequestURI();

        Bucket bucket = bucketCache.computeIfAbsent(key, k -> {
            Bandwidth limit = Bandwidth.classic(rateLimit.limit(), 
                Refill.greedy(rateLimit.limit(), Duration.ofSeconds(rateLimit.time())));
            BucketConfiguration configuration = BucketConfiguration.builder()
                .addLimit(limit)
                .build();
            return proxyManager.builder().build(key, () -> configuration);
        });

        if (bucket.tryConsume(1)) {
            log.info("请求通过限流检查: {}", request.getRequestURI());
            return true;
        } else {
            log.warn("请求被限流: {}", request.getRequestURI());
            response.setStatus(HttpStatus.TOO_MANY_REQUESTS.value());
            response.setContentType("application/json;charset=UTF-8");
            response.getWriter().write("{\"error\":\"Too Many Requests\",\"message\":\"请求频率过高，请稍后重试\"}");
            return false;
        }
    }
} 