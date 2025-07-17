package com.vivek.config;

import com.zaxxer.hikari.HikariConfig;
import com.zaxxer.hikari.HikariDataSource;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.context.annotation.Primary;
import org.springframework.dao.annotation.PersistenceExceptionTranslationPostProcessor;
import org.springframework.data.jpa.repository.config.EnableJpaRepositories;
import org.springframework.orm.jpa.JpaTransactionManager;
import org.springframework.orm.jpa.LocalContainerEntityManagerFactoryBean;
import org.springframework.orm.jpa.vendor.HibernateJpaVendorAdapter;
import org.springframework.transaction.PlatformTransactionManager;
import org.springframework.transaction.annotation.EnableTransactionManagement;

import jakarta.persistence.EntityManagerFactory;
import javax.sql.DataSource;
import java.util.Properties;

/**
 * Enhanced Database Configuration for Production Performance
 * 
 * Provides:
 * - Optimized HikariCP connection pooling
 * - Transaction management with proper timeouts
 * - Hibernate optimization for batch operations
 * - Connection monitoring and health checks
 * - Proper exception handling
 * 
 * @author Vivek
 * @version 1.0.0
 */

// Fixed DatabaseConfig.java - Remove conflicting autocommit settings

@Configuration
@EnableJpaRepositories(basePackages = "com.vivek.repository")
@EnableTransactionManagement
@Slf4j
public class DatabaseConfig {

    @Value("${spring.datasource.url}")
    private String datasourceUrl;

    @Value("${spring.datasource.username}")
    private String datasourceUsername;

    @Value("${spring.datasource.password}")
    private String datasourcePassword;

    @Bean
    @Primary
    public DataSource dataSource() {
        log.info("🔧 Configuring HikariCP DataSource...");

        HikariConfig config = new HikariConfig();
        config.setJdbcUrl(datasourceUrl);
        config.setUsername(datasourceUsername);
        config.setPassword(datasourcePassword);
        config.setDriverClassName("org.postgresql.Driver");

        // Pool settings
        config.setMaximumPoolSize(20);
        config.setMinimumIdle(5);
        config.setConnectionTimeout(30000);
        config.setIdleTimeout(600000);
        config.setMaxLifetime(1800000);
        config.setLeakDetectionThreshold(60000);

        // CRITICAL FIX: Remove autoCommit setting - let Spring manage it
        // DO NOT SET: config.setAutoCommit(false);

        // Pool name for monitoring
        config.setPoolName("PerformanceTestPool");

        // Connection validation
        config.setConnectionTestQuery("SELECT 1");
        config.setValidationTimeout(5000);

        log.info("✅ HikariCP DataSource configured successfully");
        return new HikariDataSource(config);
    }

    @Bean
    @Primary
    public LocalContainerEntityManagerFactoryBean entityManagerFactory(DataSource dataSource) {
        log.info("🔧 Configuring JPA EntityManagerFactory...");

        LocalContainerEntityManagerFactoryBean factory = new LocalContainerEntityManagerFactoryBean();
        factory.setDataSource(dataSource);
        factory.setPackagesToScan("com.vivek.model");
        factory.setJpaVendorAdapter(new HibernateJpaVendorAdapter());
        factory.setJpaProperties(hibernateProperties());

        log.info("✅ JPA EntityManagerFactory configured");
        return factory;
    }

    private Properties hibernateProperties() {
        Properties properties = new Properties();

        // Basic Hibernate settings
        properties.setProperty("hibernate.dialect", "org.hibernate.dialect.PostgreSQLDialect");
        properties.setProperty("hibernate.hbm2ddl.auto", "create-drop");
        properties.setProperty("hibernate.show_sql", "false");
        properties.setProperty("hibernate.format_sql", "true");

        // CRITICAL FIX: Remove conflicting connection settings
        // DO NOT SET: properties.setProperty("hibernate.connection.autocommit",
        // "false");

        // Batch processing settings
        properties.setProperty("hibernate.jdbc.batch_size", "100");
        properties.setProperty("hibernate.jdbc.batch_versioned_data", "true");
        properties.setProperty("hibernate.jdbc.fetch_size", "1000");
        properties.setProperty("hibernate.order_inserts", "true");
        properties.setProperty("hibernate.order_updates", "true");
        properties.setProperty("hibernate.batch_fetch_style", "PADDED");

        // Performance optimizations
        properties.setProperty("hibernate.cache.use_second_level_cache", "false");
        properties.setProperty("hibernate.cache.use_query_cache", "false");
        properties.setProperty("hibernate.generate_statistics", "true");

        // Memory optimizations
        properties.setProperty("hibernate.jdbc.use_streams_for_binary", "true");
        properties.setProperty("hibernate.jdbc.use_get_generated_keys", "true");

        log.info("✅ Hibernate properties configured for optimal batch performance");
        return properties;
    }

    @Bean
    @Primary
    public PlatformTransactionManager transactionManager(EntityManagerFactory entityManagerFactory) {
        log.info("🔧 Configuring JPA Transaction Manager...");

        JpaTransactionManager transactionManager = new JpaTransactionManager();
        transactionManager.setEntityManagerFactory(entityManagerFactory);

        // Transaction timeout settings
        transactionManager.setDefaultTimeout(300); // 5 minutes
        transactionManager.setRollbackOnCommitFailure(true);

        log.info("✅ JPA Transaction Manager configured");
        return transactionManager;
    }
}