package com.vivek;

import org.springframework.boot.SpringApplication;
import org.springframework.boot.autoconfigure.SpringBootApplication;
import org.springframework.boot.context.properties.EnableConfigurationProperties;
import org.springframework.cache.annotation.EnableCaching;
import org.springframework.scheduling.annotation.EnableAsync;
import org.springframework.scheduling.annotation.EnableScheduling;

import lombok.extern.slf4j.Slf4j;

/**
 * Main Spring Boot Application for Database Batch Operation Performance
 * Analyzer
 * 
 * This application provides:
 * - Real-time performance testing for database batch operations
 * - Comprehensive monitoring and metrics collection
 * - Interactive web dashboard for visualization
 * - RESTful APIs for performance testing
 * 
 * @author Vivek
 * @version 1.0.0
 * @since 2025-06-14
 */
@Slf4j
@SpringBootApplication
@EnableConfigurationProperties
@EnableCaching
@EnableAsync
@EnableScheduling
public class BatchOperationVisualizationApplication {

	public static void main(String[] args) {

		// Log system information
		logSystemInfo();

		try {
			SpringApplication.run(BatchOperationVisualizationApplication.class, args);
			logStartupSuccess();
		} catch (Exception e) {
			log.error("❌ Failed to start application: {}", e.getMessage(), e);
			System.exit(1);
		}
	}

	/**
	 * Log system information at startup
	 */
	private static void logSystemInfo() {
		log.info("🚀 Starting Database Batch Performance Analyzer...");
		log.info("📊 System Information:");
		log.info("   Java Version: {}", System.getProperty("java.version"));
		log.info("   OS: {} {}", System.getProperty("os.name"), System.getProperty("os.version"));
		log.info("   Available Processors: {}", Runtime.getRuntime().availableProcessors());
		log.info("   Max Memory: {} MB", Runtime.getRuntime().maxMemory() / (1024 * 1024));
		log.info("   Free Memory: {} MB", Runtime.getRuntime().freeMemory() / (1024 * 1024));
	}

	/**
	 * Log successful startup information
	 */
	private static void logStartupSuccess() {
		log.info("✅ Database Batch Performance Analyzer started successfully!");
		log.info("🌐 Application URLs:");
		log.info("   Dashboard: http://localhost:8080");
		log.info("   Health Check: http://localhost:8080/actuator/health");
		log.info("   Metrics: http://localhost:8080/actuator/metrics");
		log.info("   Prometheus: http://localhost:8080/actuator/prometheus");
		log.info("🔧 Management URLs:");
		log.info("   Grafana: http://localhost:3000");
		log.info("   Prometheus: http://localhost:9090");
		log.info("💾 Database: PostgreSQL on localhost:5433");
	}
}