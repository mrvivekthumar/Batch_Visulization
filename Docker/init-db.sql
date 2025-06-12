-- Database Initialization Script for Performance Testing

-- Create simple test table
CREATE TABLE IF NOT EXISTS connection_test (
    id SERIAL PRIMARY KEY,
    test_message VARCHAR(255),
    created_at TIMESTAMP DEFAULT CURRENT_TIMESTAMP
);

-- Insert test data
INSERT INTO connection_test (test_message) 
VALUES ('Database initialized successfully for performance testing');

-- Verify the table was created
SELECT 'Init script executed successfully' as status, count(*) as records FROM connection_test;