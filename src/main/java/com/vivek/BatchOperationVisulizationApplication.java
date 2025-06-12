package com.vivek;

import org.springframework.boot.SpringApplication;
import org.springframework.boot.autoconfigure.SpringBootApplication;
import org.springframework.boot.context.properties.EnableConfigurationProperties;
import org.springframework.cache.annotation.EnableCaching;
import org.springframework.scheduling.annotation.EnableAsync;
import org.springframework.scheduling.annotation.EnableScheduling;

import lombok.extern.slf4j.Slf4j;

@Slf4j
@SpringBootApplication(exclude = {
		org.springframework.boot.autoconfigure.admin.SpringApplicationAdminJmxAutoConfiguration.class,
		org.springframework.boot.autoconfigure.jmx.JmxAutoConfiguration.class
})
@EnableConfigurationProperties
@EnableCaching
@EnableAsync
@EnableScheduling
public class BatchOperationVisulizationApplication {

	public static void main(String[] args) {
		// Completely disable JMX
		System.setProperty("spring.jmx.enabled", "false");
		System.setProperty("com.sun.management.jmxremote", "false");
		System.setProperty("spring.application.admin.enabled", "false");

		log.info("Starting Database Batch Performance Analyzer (JMX-Free)...");
		log.info("Java Version: {}", System.getProperty("java.version"));
		log.info("Available Processors: {}", Runtime.getRuntime().availableProcessors());
		log.info("Max Memory: {} MB", Runtime.getRuntime().maxMemory() / (1024 * 1024));

		SpringApplication.run(BatchOperationVisulizationApplication.class, args);

		log.info("✅ Database Batch Performance Analyzer started successfully!");
		log.info("🔗 Health Check: http://localhost:8080/actuator/health");
		log.info("📊 Metrics: http://localhost:8080/actuator/metrics");
		log.info("💡 Prometheus: http://localhost:8080/actuator/prometheus");
	}
}