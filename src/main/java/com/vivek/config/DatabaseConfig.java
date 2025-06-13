package com.vivek.config;

import com.zaxxer.hikari.HikariConfig;
import com.zaxxer.hikari.HikariDataSource;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.context.annotation.Primary;
import org.springframework.dao.annotation.PersistenceExceptionTranslationPostProcessor;
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
@Slf4j
@Configuration
@EnableTransactionManagement
public class DatabaseConfig {

    @Value("${spring.datasource.url}")
    private String jdbcUrl;

    @Value("${spring.datasource.username}")
    private String username;

    @Value("${spring.datasource.password}")
    private String password;

    @Value("${spring.datasource.driver-class-name}")
    private String driverClassName;

    @Value("${spring.datasource.hikari.maximum-pool-size:20}")
    private int maximumPoolSize;

    @Value("${spring.datasource.hikari.minimum-idle:5}")
    private int minimumIdle;

    @Value("${spring.datasource.hikari.connection-timeout:30000}")
    private long connectionTimeout;

    @Value("${spring.datasource.hikari.idle-timeout:600000}")
    private long idleTimeout;

    @Value("${spring.datasource.hikari.max-lifetime:1800000}")
    private long maxLifetime;

    @Value("${spring.datasource.hikari.leak-detection-threshold:60000}")
    private long leakDetectionThreshold;

    /**
     * Primary DataSource with optimized HikariCP configuration
     */
    @Bean
    @Primary
    public DataSource dataSource() {
        log.info("🔧 Configuring optimized HikariCP DataSource...");

        HikariConfig config = new HikariConfig();

        // Basic connection settings
        config.setJdbcUrl(jdbcUrl);
        config.setUsername(username);
        config.setPassword(password);
        config.setDriverClassName(driverClassName);

        // Pool configuration for performance
        config.setMaximumPoolSize(maximumPoolSize);
        config.setMinimumIdle(minimumIdle);
        config.setConnectionTimeout(connectionTimeout);
        config.setIdleTimeout(idleTimeout);
        config.setMaxLifetime(maxLifetime);
        config.setLeakDetectionThreshold(leakDetectionThreshold);

        // Pool name for monitoring
        config.setPoolName("PerformanceTestHikariCP");

        // Performance optimizations
        config.addDataSourceProperty("cachePrepStmts", "true");
        config.addDataSourceProperty("prepStmtCacheSize", "250");
        config.addDataSourceProperty("prepStmtCacheSqlLimit", "2048");
        config.addDataSourceProperty("useServerPrepStmts", "true");
        config.addDataSourceProperty("useLocalSessionState", "true");
        config.addDataSourceProperty("rewriteBatchedStatements", "true");
        config.addDataSourceProperty("cacheResultSetMetadata", "true");
        config.addDataSourceProperty("cacheServerConfiguration", "true");
        config.addDataSourceProperty("elideSetAutoCommits", "true");
        config.addDataSourceProperty("maintainTimeStats", "false");

        // PostgreSQL specific optimizations
        config.addDataSourceProperty("tcpKeepAlive", "true");
        config.addDataSourceProperty("socketTimeout", "30");
        config.addDataSourceProperty("loginTimeout", "10");
        config.addDataSourceProperty("connectTimeout", "10");
        config.addDataSourceProperty("ApplicationName", "BatchPerformanceAnalyzer");

        // Connection validation
        config.setConnectionTestQuery("SELECT 1");
        config.setValidationTimeout(3000);

        // Health check and monitoring
        config.setRegisterMbeans(false); // We disabled JMX
        config.setMetricRegistry(null);
        config.setHealthCheckRegistry(null);

        // Initialize the pool
        HikariDataSource dataSource = new HikariDataSource(config);

        log.info("✅ HikariCP DataSource configured successfully");
        log.info("   Max Pool Size: {}", maximumPoolSize);
        log.info("   Min Idle: {}", minimumIdle);
        log.info("   Connection Timeout: {}ms", connectionTimeout);
        log.info("   Leak Detection Threshold: {}ms", leakDetectionThreshold);

        return dataSource;
    }

    /**
     * Enhanced EntityManagerFactory with Hibernate optimizations
     */
    @Bean
    @Primary
    public LocalContainerEntityManagerFactoryBean entityManagerFactory(DataSource dataSource) {
        log.info("🔧 Configuring optimized EntityManagerFactory...");

        LocalContainerEntityManagerFactoryBean em = new LocalContainerEntityManagerFactoryBean();
        em.setDataSource(dataSource);
        em.setPackagesToScan("com.vivek.model");

        HibernateJpaVendorAdapter vendorAdapter = new HibernateJpaVendorAdapter();
        vendorAdapter.setGenerateDdl(true);
        vendorAdapter.setShowSql(false); // Controlled by application.yml
        em.setJpaVendorAdapter(vendorAdapter);

        em.setJpaProperties(hibernateProperties());

        log.info("✅ EntityManagerFactory configured with Hibernate optimizations");
        return em;
    }

    /**
     * Optimized Hibernate properties for batch performance
     */
    private Properties hibernateProperties() {
        Properties properties = new Properties();

        // Basic Hibernate settings
        properties.setProperty("hibernate.dialect", "org.hibernate.dialect.PostgreSQLDialect");
        properties.setProperty("hibernate.hbm2ddl.auto", "create-drop");
        properties.setProperty("hibernate.show_sql", "false");
        properties.setProperty("hibernate.format_sql", "false");

        // Batch processing optimizations
        properties.setProperty("hibernate.jdbc.batch_size", "1000");
        properties.setProperty("hibernate.jdbc.batch_versioned_data", "true");
        properties.setProperty("hibernate.jdbc.fetch_size", "1000");
        properties.setProperty("hibernate.order_inserts", "true");
        properties.setProperty("hibernate.order_updates", "true");
        properties.setProperty("hibernate.batch_fetch_style", "PADDED");

        // Connection and transaction settings
        properties.setProperty("hibernate.connection.provider_disables_autocommit", "true");
        properties.setProperty("hibernate.connection.autocommit", "false");
        properties.setProperty("hibernate.transaction.jta.platform", "");

        // Performance optimizations
        properties.setProperty("hibernate.cache.use_second_level_cache", "false");
        properties.setProperty("hibernate.cache.use_query_cache", "false");
        properties.setProperty("hibernate.generate_statistics", "true");
        properties.setProperty("hibernate.session.events.log.LOG_QUERIES_SLOWER_THAN_MS", "1000");

        // Memory and garbage collection optimizations
        properties.setProperty("hibernate.jdbc.use_streams_for_binary", "true");
        properties.setProperty("hibernate.jdbc.use_get_generated_keys", "true");
        properties.setProperty("hibernate.connection.handling_mode",
                "DELAYED_ACQUISITION_AND_RELEASE_AFTER_TRANSACTION");

        // Query optimization
        properties.setProperty("hibernate.query.plan_cache_max_size", "2048");
        properties.setProperty("hibernate.query.plan_parameter_metadata_max_size", "128");

        log.info("✅ Hibernate properties configured for optimal batch performance");
        return properties;
    }

    /**
     * Transaction Manager with enhanced configuration
     */
    @Bean
    @Primary
    public PlatformTransactionManager transactionManager(EntityManagerFactory entityManagerFactory) {
        log.info("🔧 Configuring enhanced JPA Transaction Manager...");

        JpaTransactionManager transactionManager = new JpaTransactionManager();
        transactionManager.setEntityManagerFactory(entityManagerFactory);

        // Transaction timeout settings
        transactionManager.setDefaultTimeout(300); // 5 minutes default
        transactionManager.setRollbackOnCommitFailure(true);

        // Enhanced transaction settings
        transactionManager.setValidateExistingTransaction(true);
        transactionManager.setGlobalRollbackOnParticipationFailure(true);
        transactionManager.setFailEarlyOnGlobalRollbackOnly(true);

        log.info("✅ JPA Transaction Manager configured with enhanced settings");
        return transactionManager;
    }

    /**
     * Exception translation for better error handling
     */
    @Bean
    public PersistenceExceptionTranslationPostProcessor exceptionTranslation() {
        return new PersistenceExceptionTranslationPostProcessor();
    }

    /**
     * Database health check bean
     */
    @Bean
    public DatabaseHealthChecker databaseHealthChecker(DataSource dataSource) {
        return new DatabaseHealthChecker(dataSource);
    }

    /**
     * Custom database health checker
     */
    public static class DatabaseHealthChecker {
        private final DataSource dataSource;

        public DatabaseHealthChecker(DataSource dataSource) {
            this.dataSource = dataSource;
        }

        public boolean isHealthy() {
            try (var connection = dataSource.getConnection()) {
                return connection.isValid(5);
            } catch (Exception e) {
                log.error("Database health check failed", e);
                return false;
            }
        }

        public String getConnectionInfo() {
            if (dataSource instanceof HikariDataSource hikariDS) {
                return String.format(
                        "Pool: %s, Active: %d, Idle: %d, Total: %d, Waiting: %d",
                        hikariDS.getPoolName(),
                        hikariDS.getHikariPoolMXBean().getActiveConnections(),
                        hikariDS.getHikariPoolMXBean().getIdleConnections(),
                        hikariDS.getHikariPoolMXBean().getTotalConnections(),
                        hikariDS.getHikariPoolMXBean().getThreadsAwaitingConnection());
            }
            return "Connection pool information not available";
        }
    }
}