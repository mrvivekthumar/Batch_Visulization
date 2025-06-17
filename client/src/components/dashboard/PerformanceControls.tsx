import React, { useState } from 'react';
import { TestConfig, PerformanceResult, User } from '../../types';
import { apiService } from '../../services/api';

interface PerformanceControlsProps {
    user: User;
    onTestComplete: (result: PerformanceResult) => void;
    onNotification: (message: string, type: 'success' | 'error' | 'warning') => void;
}

const PerformanceControls: React.FC<PerformanceControlsProps> = ({
    user,
    onTestComplete,
    onNotification
}) => {
    const [config, setConfig] = useState<TestConfig>({
        totalRecords: 1000,
        batchSize: 100
    });
    const [isRunning, setIsRunning] = useState(false);
    const [currentOperation, setCurrentOperation] = useState<string>('');

    const handleInputChange = (e: React.ChangeEvent<HTMLInputElement | HTMLSelectElement>) => {
        const { name, value } = e.target;
        setConfig(prev => ({
            ...prev,
            [name]: name === 'totalRecords' ? parseInt(value) : parseInt(value)
        }));
    };

    const validateInputs = (): boolean => {
        if (config.totalRecords < 100 || config.totalRecords > 100000) {
            onNotification('Total records must be between 100 and 100,000.', 'warning');
            return false;
        }

        if (config.batchSize < 1 || config.batchSize > 10000) {
            onNotification('Batch size must be between 1 and 10,000.', 'warning');
            return false;
        }

        if (config.batchSize > config.totalRecords) {
            onNotification('Batch size cannot be greater than total records.', 'warning');
            return false;
        }

        return true;
    };

    const runInsertTest = async () => {
        if (user.role !== 'ADMIN') {
            onNotification('INSERT operations require ADMIN role.', 'warning');
            return;
        }

        if (!validateInputs()) return;

        setIsRunning(true);
        setCurrentOperation(`Running INSERT test: ${config.totalRecords} records with batch size ${config.batchSize}...`);

        try {
            const result = await apiService.runInsertTest(config);
            onTestComplete(result);

            const operation = config.batchSize === 1 ? "one by one" : `in batches of ${config.batchSize}`;
            onNotification(
                `✅ INSERT completed: ${result.recordsProcessed} records ${operation} in ${result.durationMs}ms`,
                'success'
            );
        } catch (error: any) {
            console.error('INSERT test failed:', error);
            onNotification(`❌ INSERT test failed: ${getErrorMessage(error)}`, 'error');
        } finally {
            setIsRunning(false);
            setCurrentOperation('');
        }
    };

    const runDeleteTest = async () => {
        if (user.role !== 'ADMIN') {
            onNotification('DELETE operations require ADMIN role.', 'warning');
            return;
        }

        if (!validateInputs()) return;

        setIsRunning(true);
        setCurrentOperation(`Running DELETE test: ${config.totalRecords} records with batch size ${config.batchSize}...`);

        try {
            const result = await apiService.runDeleteTest(config);
            onTestComplete(result);

            const operation = config.batchSize === 1 ? "one by one" : `in batches of ${config.batchSize}`;
            onNotification(
                `✅ DELETE completed: ${result.recordsProcessed} records ${operation} in ${result.durationMs}ms`,
                'success'
            );
        } catch (error: any) {
            console.error('DELETE test failed:', error);
            onNotification(`❌ DELETE test failed: ${getErrorMessage(error)}`, 'error');
        } finally {
            setIsRunning(false);
            setCurrentOperation('');
        }
    };

    const getErrorMessage = (error: any): string => {
        if (error.response?.data?.message) {
            return error.response.data.message;
        }

        switch (error.response?.status) {
            case 401:
                return 'Authentication failed. Please login again.';
            case 403:
                return 'Access denied. Insufficient privileges.';
            case 429:
                return 'Rate limit exceeded. Please wait before trying again.';
            case 500:
                return 'Server error. Please try again later.';
            default:
                return error.message || 'An unexpected error occurred.';
        }
    };

    const isAdminUser = user.role === 'ADMIN';

    return (
        <div className="controls">
            <div className="control-row">
                <div className="control-group">
                    <label htmlFor="totalRecords">Total Records</label>
                    <input
                        type="number"
                        id="totalRecords"
                        name="totalRecords"
                        value={config.totalRecords}
                        onChange={handleInputChange}
                        min="100"
                        max="100000"
                        disabled={isRunning}
                    />
                    <small>Between 100 and 100,000 records</small>
                </div>

                <div className="control-group">
                    <label htmlFor="batchSize">Batch Size</label>
                    <select
                        id="batchSize"
                        name="batchSize"
                        value={config.batchSize}
                        onChange={handleInputChange}
                        disabled={isRunning}
                    >
                        <option value={1}>1 (One by One)</option>
                        <option value={10}>10</option>
                        <option value={100}>100</option>
                        <option value={1000}>1000</option>
                        <option value={5000}>5000</option>
                    </select>
                    <small>Larger batches are usually faster</small>
                </div>

                <button
                    onClick={runInsertTest}
                    disabled={!isAdminUser || isRunning}
                    className={`btn btn-primary ${isRunning ? 'button-loading' : ''}`}
                    title={isAdminUser ? 'Run INSERT performance test' : 'ADMIN role required for INSERT operations'}
                >
                    📝 INSERT Test
                </button>

                <button
                    onClick={runDeleteTest}
                    disabled={!isAdminUser || isRunning}
                    className={`btn btn-primary ${isRunning ? 'button-loading' : ''}`}
                    title={isAdminUser ? 'Run DELETE performance test' : 'ADMIN role required for DELETE operations'}
                >
                    🗑️ DELETE Test
                </button>
            </div>

            {isRunning && (
                <div className="loading-indicator">
                    <div className="spinner"></div>
                    <div>{currentOperation}</div>
                </div>
            )}

            <div className="help-text">
                <small>Requires ADMIN role. Inserts/deletes test records into database.</small>
            </div>
        </div>
    );
};

export default PerformanceControls;