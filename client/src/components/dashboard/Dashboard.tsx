import React, { useState, useEffect, useCallback } from 'react';
import { PerformanceResult, SystemStats, DatabaseStats, TestConfig } from '../../types/index';
import apiService from '../../services/apiService';
import PerformanceCharts from './PerformanceCharts';
import SystemStatsDisplay from './SystemStats';
import './Dashboard.css';

const Dashboard: React.FC = () => {
    // State management
    const [testResults, setTestResults] = useState<PerformanceResult[]>([]);
    const [systemStats, setSystemStats] = useState<SystemStats | null>(null);
    const [databaseStats, setDatabaseStats] = useState<DatabaseStats | null>(null);
    const [isLoading, setIsLoading] = useState(false);
    const [error, setError] = useState<string | null>(null);
    const [connectionStatus, setConnectionStatus] = useState<'connected' | 'disconnected' | 'checking'>('checking');

    // Test configuration state
    const [testConfig, setTestConfig] = useState<TestConfig>({
        totalRecords: 1000,
        batchSize: 100,
    });

    // Check backend connection on component mount
    useEffect(() => {
        checkConnection();
        loadInitialData();

        // Set up periodic stats refresh
        const statsInterval = setInterval(() => {
            loadSystemStats();
            loadDatabaseStats();
        }, 5000); // Refresh every 5 seconds

        return () => clearInterval(statsInterval);
    }, []);

    const checkConnection = async () => {
        setConnectionStatus('checking');
        try {
            const result = await apiService.testConnection();
            setConnectionStatus(result.success ? 'connected' : 'disconnected');
            if (!result.success) {
                setError(`Connection failed: ${result.message}`);
            } else {
                setError(null);
                console.log(`✅ Backend connected in ${result.latency}ms`);
            }
        } catch (error) {
            setConnectionStatus('disconnected');
            setError('Unable to connect to backend service');
            console.error('❌ Connection test failed:', error);
        }
    };

    const loadInitialData = async () => {
        try {
            await Promise.all([
                loadSystemStats(),
                loadDatabaseStats()
            ]);
        } catch (error) {
            console.error('Failed to load initial data:', error);
        }
    };

    const loadSystemStats = useCallback(async () => {
        try {
            const stats = await apiService.getSystemStats();
            setSystemStats(stats);
        } catch (error) {
            console.error('Failed to load system stats:', error);
            // Don't show error to user for stats loading failures
        }
    }, []);

    const loadDatabaseStats = useCallback(async () => {
        try {
            const stats = await apiService.getDatabaseStats();
            setDatabaseStats(stats);
        } catch (error) {
            console.error('Failed to load database stats:', error);
            // Don't show error to user for stats loading failures
        }
    }, []);

    const handleTestConfigChange = (field: keyof TestConfig, value: number) => {
        setTestConfig(prev => ({
            ...prev,
            [field]: value
        }));
    };

    const validateTestConfig = (): boolean => {
        if (testConfig.totalRecords < 1 || testConfig.totalRecords > 100000) {
            setError('Total records must be between 1 and 100,000');
            return false;
        }
        if (testConfig.batchSize < 1 || testConfig.batchSize > testConfig.totalRecords) {
            setError('Batch size must be between 1 and total records');
            return false;
        }
        setError(null);
        return true;
    };

    const runInsertTest = async () => {
        if (!validateTestConfig()) return;

        setIsLoading(true);
        setError(null);

        try {
            console.log(`🚀 Starting INSERT test with ${testConfig.totalRecords} records, batch size ${testConfig.batchSize}`);
            const result = await apiService.runInsertTest(testConfig);

            setTestResults(prev => [...prev, result]);

            // Refresh stats after test
            await Promise.all([loadSystemStats(), loadDatabaseStats()]);

            console.log(`✅ INSERT test completed: ${result.throughputRecordsPerSecond.toFixed(2)} records/sec`);
        } catch (error: any) {
            const errorMessage = error?.response?.data?.message || error?.message || 'Insert test failed';
            setError(errorMessage);
            console.error('❌ INSERT test failed:', error);
        } finally {
            setIsLoading(false);
        }
    };

    const runDeleteTest = async () => {
        if (!validateTestConfig()) return;

        setIsLoading(true);
        setError(null);

        try {
            console.log(`🗑️ Starting DELETE test with ${testConfig.totalRecords} records, batch size ${testConfig.batchSize}`);
            const result = await apiService.runDeleteTest(testConfig);

            setTestResults(prev => [...prev, result]);

            // Refresh stats after test
            await Promise.all([loadSystemStats(), loadDatabaseStats()]);

            console.log(`✅ DELETE test completed: ${result.throughputRecordsPerSecond.toFixed(2)} records/sec`);
        } catch (error: any) {
            const errorMessage = error?.response?.data?.message || error?.message || 'Delete test failed';
            setError(errorMessage);
            console.error('❌ DELETE test failed:', error);
        } finally {
            setIsLoading(false);
        }
    };

    const clearResults = () => {
        setTestResults([]);
        setError(null);
        console.log('🧹 Test results cleared');
    };

    const refreshStats = async () => {
        await Promise.all([
            checkConnection(),
            loadSystemStats(),
            loadDatabaseStats()
        ]);
    };

    return (
        <div className="dashboard">
            <header className="dashboard-header">
                <div className="header-content">
                    <h1>🚀 Database Performance Analyzer</h1>
                    <div className="header-actions">
                        <div className={`connection-status ${connectionStatus}`}>
                            <span className="status-indicator"></span>
                            <span className="status-text">
                                {connectionStatus === 'connected' && '🟢 Connected'}
                                {connectionStatus === 'disconnected' && '🔴 Disconnected'}
                                {connectionStatus === 'checking' && '🟡 Checking...'}
                            </span>
                        </div>
                        <button
                            onClick={refreshStats}
                            className="refresh-button"
                            disabled={isLoading}
                        >
                            🔄 Refresh
                        </button>
                    </div>
                </div>
            </header>

            {error && (
                <div className="error-banner">
                    <span className="error-icon">⚠️</span>
                    <span className="error-message">{error}</span>
                    <button
                        onClick={() => setError(null)}
                        className="error-close"
                    >
                        ✕
                    </button>
                </div>
            )}

            <main className="dashboard-main">
                {/* System Statistics */}
                <section className="stats-section">
                    <SystemStatsDisplay
                        systemStats={systemStats}
                        databaseStats={databaseStats}
                        onRefresh={refreshStats}
                    />
                </section>

                {/* Test Controls */}
                <section className="test-controls-section">
                    <div className="test-controls">
                        <h2>⚡ Performance Testing</h2>

                        <div className="config-grid">
                            <div className="config-item">
                                <label htmlFor="totalRecords">Total Records:</label>
                                <input
                                    id="totalRecords"
                                    type="number"
                                    min="1"
                                    max="100000"
                                    value={testConfig.totalRecords}
                                    onChange={(e) => handleTestConfigChange('totalRecords', parseInt(e.target.value) || 1)}
                                    disabled={isLoading}
                                />
                                <span className="config-hint">1 - 100,000</span>
                            </div>

                            <div className="config-item">
                                <label htmlFor="batchSize">Batch Size:</label>
                                <input
                                    id="batchSize"
                                    type="number"
                                    min="1"
                                    max={testConfig.totalRecords}
                                    value={testConfig.batchSize}
                                    onChange={(e) => handleTestConfigChange('batchSize', parseInt(e.target.value) || 1)}
                                    disabled={isLoading}
                                />
                                <span className="config-hint">1 - {testConfig.totalRecords}</span>
                            </div>
                        </div>

                        <div className="test-actions">
                            <button
                                onClick={runInsertTest}
                                disabled={isLoading || connectionStatus !== 'connected'}
                                className="test-button insert-button"
                            >
                                {isLoading ? '⏳ Running...' : '📥 Run INSERT Test'}
                            </button>

                            <button
                                onClick={runDeleteTest}
                                disabled={isLoading || connectionStatus !== 'connected'}
                                className="test-button delete-button"
                            >
                                {isLoading ? '⏳ Running...' : '🗑️ Run DELETE Test'}
                            </button>

                            <button
                                onClick={clearResults}
                                disabled={isLoading || testResults.length === 0}
                                className="test-button clear-button"
                            >
                                🧹 Clear Results
                            </button>
                        </div>

                        {/* Quick Test Presets */}
                        <div className="quick-presets">
                            <h3>🎯 Quick Test Presets</h3>
                            <div className="preset-buttons">
                                <button
                                    onClick={() => setTestConfig({ totalRecords: 1000, batchSize: 100 })}
                                    className="preset-button"
                                    disabled={isLoading}
                                >
                                    Small (1K / 100)
                                </button>
                                <button
                                    onClick={() => setTestConfig({ totalRecords: 10000, batchSize: 1000 })}
                                    className="preset-button"
                                    disabled={isLoading}
                                >
                                    Medium (10K / 1K)
                                </button>
                                <button
                                    onClick={() => setTestConfig({ totalRecords: 50000, batchSize: 5000 })}
                                    className="preset-button"
                                    disabled={isLoading}
                                >
                                    Large (50K / 5K)
                                </button>
                            </div>
                        </div>
                    </div>
                </section>

                {/* Performance Charts */}
                <section className="charts-section">
                    <PerformanceCharts testResults={testResults} />
                </section>

                {/* Test Results Summary */}
                {testResults.length > 0 && (
                    <section className="results-summary-section">
                        <h2>📋 Test Results Summary</h2>
                        <div className="results-table-container">
                            <table className="results-table">
                                <thead>
                                    <tr>
                                        <th>Operation</th>
                                        <th>Records</th>
                                        <th>Batch Size</th>
                                        <th>Duration (ms)</th>
                                        <th>Throughput (rec/sec)</th>
                                        <th>Memory (MB)</th>
                                        <th>Time/Record (ms)</th>
                                        <th>Timestamp</th>
                                    </tr>
                                </thead>
                                <tbody>
                                    {testResults.map((result, index) => (
                                        <tr key={index}>
                                            <td className={`operation-type ${result.operationType.toLowerCase()}`}>
                                                {result.operationType}
                                            </td>
                                            <td>{result.recordsProcessed.toLocaleString()}</td>
                                            <td>{result.batchSize.toLocaleString()}</td>
                                            <td>{Math.round(result.durationMs).toLocaleString()}</td>
                                            <td className="throughput-cell">
                                                {Math.round(result.throughputRecordsPerSecond).toLocaleString()}
                                            </td>
                                            <td>{Math.round(result.memoryUsedMB)}</td>
                                            <td>{result.avgTimePerRecord.toFixed(3)}</td>
                                            <td className="timestamp-cell">
                                                {new Date(result.timestamp).toLocaleTimeString()}
                                            </td>
                                        </tr>
                                    ))}
                                </tbody>
                            </table>
                        </div>
                    </section>
                )}
            </main>
        </div>
    );
};

export default Dashboard;