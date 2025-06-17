import React, { useState, useEffect } from 'react';
import { useApp } from '../../context/AppContext';
import { TestConfig, PerformanceResult } from '../../types/index';
import { apiService } from '../../services/apiService';
import PerformanceCharts from './PerformanceCharts';
import TestControls from './TestControls';
import ResultsList from './ResultsList';
import SystemStats from './SystemStats';
import './Dashboard.css';

interface DashboardProps {
    user: any;
    onLogout: () => void;
}

const Dashboard: React.FC<DashboardProps> = ({ user, onLogout }) => {
    const { state, dispatch, showNotification, setLoading, refreshStats } = useApp();
    const [testConfig, setTestConfig] = useState<TestConfig>({
        totalRecords: 1000,
        batchSize: 100,
    });

    // Load initial data
    useEffect(() => {
        const loadInitialData = async () => {
            await refreshStats();
        };

        loadInitialData();

        // Set up periodic refresh every 30 seconds
        const interval = setInterval(refreshStats, 30000);
        return () => clearInterval(interval);
    }, [refreshStats]);

    const runInsertTest = async () => {
        if (state.user?.role !== 'ADMIN') {
            showNotification('INSERT operations require ADMIN role', 'warning');
            return;
        }

        setLoading(true, `Running INSERT test: ${testConfig.totalRecords} records with batch size ${testConfig.batchSize}...`);

        try {
            const result = await apiService.runInsertTest(testConfig);
            dispatch({ type: 'ADD_TEST_RESULT', payload: result });

            const operation = testConfig.batchSize === 1 ? "one by one" : `in batches of ${testConfig.batchSize}`;
            showNotification(
                `✅ INSERT completed: ${result.recordsProcessed} records ${operation} in ${result.durationMs}ms`,
                'success'
            );

            // Refresh stats after test
            await refreshStats();
        } catch (error: any) {
            console.error('INSERT test failed:', error);
            const message = error.response?.data?.message || error.message || 'INSERT test failed';
            showNotification(`❌ INSERT test failed: ${message}`, 'error');
        } finally {
            setLoading(false);
        }
    };

    const runDeleteTest = async () => {
        if (state.user?.role !== 'ADMIN') {
            showNotification('DELETE operations require ADMIN role', 'warning');
            return;
        }

        setLoading(true, `Running DELETE test: ${testConfig.totalRecords} records with batch size ${testConfig.batchSize}...`);

        try {
            const result = await apiService.runDeleteTest(testConfig);
            dispatch({ type: 'ADD_TEST_RESULT', payload: result });

            const operation = testConfig.batchSize === 1 ? "one by one" : `in batches of ${testConfig.batchSize}`;
            showNotification(
                `✅ DELETE completed: ${result.recordsProcessed} records ${operation} in ${result.durationMs}ms`,
                'success'
            );

            // Refresh stats after test
            await refreshStats();
        } catch (error: any) {
            console.error('DELETE test failed:', error);
            const message = error.response?.data?.message || error.message || 'DELETE test failed';
            showNotification(`❌ DELETE test failed: ${message}`, 'error');
        } finally {
            setLoading(false);
        }
    };

    return (
        <div className="dashboard">
            <div className="dashboard-header">
                <h1>🚀 Database Batch Performance Analyzer</h1>
                <p>Real-time Performance Testing & Visualization Dashboard</p>
                <div className="user-info">
                    <span>👤 {state.user?.username} ({state.user?.role})</span>
                    <button className="logout-btn" onClick={onLogout}>
                        Logout
                    </button>
                </div>
            </div>

            <div className="dashboard-content">
                {/* System Statistics */}
                <SystemStats
                    systemStats={state.systemStats}
                    databaseStats={state.databaseStats}
                    onRefresh={refreshStats}
                />

                {/* Test Controls */}
                <TestControls
                    testConfig={testConfig}
                    onConfigChange={setTestConfig}
                    onRunInsert={runInsertTest}
                    onRunDelete={runDeleteTest}
                    isLoading={state.loading.isLoading}
                    userRole={state.user?.role}
                />

                {/* Performance Charts */}
                {state.testResults.length > 0 && (
                    <PerformanceCharts testResults={state.testResults} />
                )}

                {/* Results List */}
                <ResultsList testResults={state.testResults} />
            </div>

            {/* Loading Overlay */}
            {state.loading.isLoading && (
                <div className="loading-overlay">
                    <div className="loading-spinner"></div>
                    <p>{state.loading.message || 'Processing...'}</p>
                </div>
            )}
        </div>
    );
};

export default Dashboard;