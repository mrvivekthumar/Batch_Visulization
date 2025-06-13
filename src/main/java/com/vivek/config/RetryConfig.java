package com.vivek.config;

import lombok.extern.slf4j.Slf4j;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.retry.RetryCallback;
import org.springframework.retry.RetryContext;
import org.springframework.retry.RetryListener;
import org.springframework.retry.annotation.EnableRetry;
import org.springframework.retry.backoff.ExponentialBackOffPolicy;
import org.springframework.retry.policy.SimpleRetryPolicy;
import org.springframework.retry.support.RetryTemplate;

import java.util.HashMap;
import java.util.Map;

/**
 * Retry Configuration for resilient operations
 * 
 * Provides:
 * - Exponential backoff retry strategy
 * - Configurable retry policies
 * - Retry listeners for monitoring
 * - Exception-specific retry rules
 * 
 * @author Vivek
 * @version 1.0.0
 */
@Slf4j
@Configuration
@EnableRetry
public class RetryConfig {

    /**
     * Retry template for database operations
     */
    @Bean
    public RetryTemplate retryTemplate() {
        RetryTemplate retryTemplate = new RetryTemplate();

        // Configure retry policy
        Map<Class<? extends Throwable>, Boolean> retryableExceptions = new HashMap<>();
        retryableExceptions.put(org.springframework.dao.DataAccessResourceFailureException.class, true);
        retryableExceptions.put(org.springframework.dao.TransientDataAccessException.class, true);
        retryableExceptions.put(org.springframework.transaction.CannotCreateTransactionException.class, true);
        retryableExceptions.put(java.sql.SQLTransientException.class, true);
        retryableExceptions.put(java.net.SocketTimeoutException.class, true);

        SimpleRetryPolicy retryPolicy = new SimpleRetryPolicy(3, retryableExceptions);
        retryTemplate.setRetryPolicy(retryPolicy);

        // Configure backoff policy
        ExponentialBackOffPolicy backOffPolicy = new ExponentialBackOffPolicy();
        backOffPolicy.setInitialInterval(1000L); // Start with 1 second
        backOffPolicy.setMultiplier(2.0); // Double each time
        backOffPolicy.setMaxInterval(10000L); // Maximum 10 seconds
        retryTemplate.setBackOffPolicy(backOffPolicy);

        // Add retry listener for monitoring
        retryTemplate.registerListener(new RetryListener() {
            @Override
            public <T, E extends Throwable> boolean open(RetryContext context, RetryCallback<T, E> callback) {
                log.debug("🔄 Starting retry operation: {}", context.getAttribute("context.name"));
                return true;
            }

            @Override
            public <T, E extends Throwable> void onError(RetryContext context, RetryCallback<T, E> callback,
                    Throwable throwable) {
                log.warn("⚠️ Retry attempt {} failed: {}",
                        context.getRetryCount(), throwable.getMessage());
            }

            @Override
            public <T, E extends Throwable> void close(RetryContext context, RetryCallback<T, E> callback,
                    Throwable throwable) {
                if (throwable == null) {
                    log.debug("✅ Retry operation succeeded after {} attempts", context.getRetryCount());
                } else {
                    log.error("❌ Retry operation failed after {} attempts: {}",
                            context.getRetryCount(), throwable.getMessage());
                }
            }
        });

        return retryTemplate;
    }
}