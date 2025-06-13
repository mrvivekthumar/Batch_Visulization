-- Enhanced Database Initialization Script for Performance Testing
-- This script will be executed after the basic init-db.sql

-- Create performance test records table if not exists
CREATE TABLE IF NOT EXISTS performance_test_records (
    id BIGSERIAL PRIMARY KEY,
    test_id VARCHAR(255) NOT NULL,
    category VARCHAR(50) NOT NULL,
    description VARCHAR(500),
    numeric_value BIGINT,
    string_value VARCHAR(255),
    json_data TEXT,
    is_active BOOLEAN DEFAULT TRUE,
    priority INTEGER DEFAULT 1,
    tags VARCHAR(1000),
    created_at TIMESTAMP DEFAULT CURRENT_TIMESTAMP,
    updated_at TIMESTAMP DEFAULT CURRENT_TIMESTAMP,
    version BIGINT DEFAULT 0
);

-- Create indexes for better performance
CREATE INDEX IF NOT EXISTS idx_test_id ON performance_test_records(test_id);
CREATE INDEX IF NOT EXISTS idx_category ON performance_test_records(category);
CREATE INDEX IF NOT EXISTS idx_created_at ON performance_test_records(created_at);
CREATE INDEX IF NOT EXISTS idx_composite ON performance_test_records(category, test_id);
CREATE INDEX IF NOT EXISTS idx_is_active ON performance_test_records(is_active);

-- Create trigger for updated_at timestamp
CREATE OR REPLACE FUNCTION update_updated_at_column()
RETURNS TRIGGER AS $$
BEGIN
    NEW.updated_at = CURRENT_TIMESTAMP;
    RETURN NEW;
END;
$$ language 'plpgsql';

-- Apply trigger to performance_test_records table
DROP TRIGGER IF EXISTS update_performance_test_records_updated_at ON performance_test_records;
CREATE TRIGGER update_performance_test_records_updated_at
    BEFORE UPDATE ON performance_test_records
    FOR EACH ROW
    EXECUTE FUNCTION update_updated_at_column();

-- Create a table for performance metrics history
CREATE TABLE IF NOT EXISTS performance_metrics_history (
    id BIGSERIAL PRIMARY KEY,
    test_type VARCHAR(50) NOT NULL,
    batch_size INTEGER NOT NULL,
    records_processed INTEGER NOT NULL,
    duration_ms BIGINT NOT NULL,
    average_time_per_record DECIMAL(10,4) NOT NULL,
    memory_used_mb BIGINT NOT NULL,
    records_per_second DECIMAL(10,2) NOT NULL,
    batch_count INTEGER NOT NULL,
    start_time TIMESTAMP NOT NULL,
    end_time TIMESTAMP NOT NULL,
    created_at TIMESTAMP DEFAULT CURRENT_TIMESTAMP
);

-- Create indexes for metrics history
CREATE INDEX IF NOT EXISTS idx_metrics_test_type ON performance_metrics_history(test_type);
CREATE INDEX IF NOT EXISTS idx_metrics_created_at ON performance_metrics_history(created_at);
CREATE INDEX IF NOT EXISTS idx_metrics_batch_size ON performance_metrics_history(batch_size);

-- Create a view for latest performance metrics
CREATE OR REPLACE VIEW latest_performance_metrics AS
SELECT 
    test_type,
    batch_size,
    AVG(duration_ms) as avg_duration_ms,
    AVG(records_per_second) as avg_records_per_second,
    AVG(memory_used_mb) as avg_memory_used_mb,
    COUNT(*) as test_count,
    MAX(created_at) as last_test_time
FROM performance_metrics_history 
WHERE created_at >= CURRENT_DATE - INTERVAL '7 days'
GROUP BY test_type, batch_size
ORDER BY test_type, batch_size;

-- Insert some initial test data for verification
INSERT INTO performance_test_records (test_id, category, description, numeric_value, string_value, json_data, tags) 
VALUES 
    ('INIT-001', 'STARTUP_TEST', 'Initial test record created during database setup', 1000, 'StartupData', '{"type":"initialization","success":true}', 'startup,init,test'),
    ('INIT-002', 'HEALTH_CHECK', 'Health check test record', 2000, 'HealthData', '{"type":"health","status":"ok"}', 'health,check,test'),
    ('INIT-003', 'CONNECTIVITY', 'Database connectivity verification', 3000, 'ConnectivityData', '{"type":"connectivity","database":"postgresql"}', 'connectivity,db,test');

-- Create function to clean old test data (for maintenance)
CREATE OR REPLACE FUNCTION cleanup_old_test_data(days_to_keep INTEGER DEFAULT 30)
RETURNS INTEGER AS $$
DECLARE
    deleted_count INTEGER;
BEGIN
    DELETE FROM performance_test_records 
    WHERE created_at < CURRENT_DATE - INTERVAL '%s days' % days_to_keep
    AND category LIKE '%TEST%';
    
    GET DIAGNOSTICS deleted_count = ROW_COUNT;
    
    -- Also clean old metrics history
    DELETE FROM performance_metrics_history 
    WHERE created_at < CURRENT_DATE - INTERVAL '%s days' % (days_to_keep * 2);
    
    RETURN deleted_count;
END;
$$ LANGUAGE plpgsql;

-- Create function to get database statistics
CREATE OR REPLACE FUNCTION get_database_stats()
RETURNS TABLE (
    table_name TEXT,
    row_count BIGINT,
    table_size TEXT,
    index_size TEXT
) AS $$
BEGIN
    RETURN QUERY
    SELECT 
        schemaname||'.'||tablename as table_name,
        n_tup_ins - n_tup_del as row_count,
        pg_size_pretty(pg_total_relation_size(schemaname||'.'||tablename)) as table_size,
        pg_size_pretty(pg_indexes_size(schemaname||'.'||tablename)) as index_size
    FROM pg_stat_user_tables 
    WHERE schemaname = 'public'
    ORDER BY pg_total_relation_size(schemaname||'.'||tablename) DESC;
END;
$$ LANGUAGE plpgsql;

-- Verify the setup
SELECT 'Database initialization completed successfully' as status, 
       count(*) as initial_records 
FROM performance_test_records;

-- Show table information
SELECT table_name, column_name, data_type 
FROM information_schema.columns 
WHERE table_name = 'performance_test_records' 
ORDER BY ordinal_position;