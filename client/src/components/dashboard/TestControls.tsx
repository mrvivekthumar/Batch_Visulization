import React, { useState } from 'react';
import { TestConfig, TEST_LIMITS } from '../../types/index';

interface TestControlsProps {
    testConfig: TestConfig;
    onConfigChange: (config: TestConfig) => void;
    onRunInsert: () => void;
    onRunDelete: () => void;
    isLoading: boolean;
    userRole?: 'ADMIN' | 'VIEWER';
}

const TestControls: React.FC<TestControlsProps> = ({
    testConfig,
    onConfigChange,
    onRunInsert,
    onRunDelete,
    isLoading,
    userRole,
}) => {
    const [errors, setErrors] = useState<{ totalRecords?: string; batchSize?: string }>({});

    const validateInput = (name: string, value: number) => {
        const newErrors = { ...errors };

        if (name === 'totalRecords') {
            if (value < TEST_LIMITS.MIN_RECORDS) {
                newErrors.totalRecords = `Minimum ${TEST_LIMITS.MIN_RECORDS} records required`;
            } else if (value > TEST_LIMITS.MAX_RECORDS) {
                newErrors.totalRecords = `Maximum ${TEST_LIMITS.MAX_RECORDS} records allowed`;
            } else {
                delete newErrors.totalRecords;
            }
        }

        if (name === 'batchSize') {
            if (value < TEST_LIMITS.MIN_BATCH_SIZE) {
                newErrors.batchSize = `Minimum batch size is ${TEST_LIMITS.MIN_BATCH_SIZE}`;
            } else if (value > TEST_LIMITS.MAX_BATCH_SIZE) {
                newErrors.batchSize = `Maximum batch size is ${TEST_LIMITS.MAX_BATCH_SIZE}`;
            } else if (value > testConfig.totalRecords) {
                newErrors.batchSize = 'Batch size cannot exceed total records';
            } else {
                delete newErrors.batchSize;
            }
        }

        setErrors(newErrors);
        return Object.keys(newErrors).length === 0;
    };

    const handleInputChange = (e: React.ChangeEvent<HTMLInputElement>) => {
        const { name, value } = e.target;
        const numValue = parseInt(value) || 0;

        // Update config immediately for responsiveness
        const newConfig = {
            ...testConfig,
            [name]: numValue,
        };
        onConfigChange(newConfig);

        // Validate after a short delay to avoid excessive validation
        setTimeout(() => validateInput(name, numValue), 300);
    };

    const isFormValid = () => {
        return (
            Object.keys(errors).length === 0 &&
            testConfig.totalRecords >= TEST_LIMITS.MIN_RECORDS &&
            testConfig.totalRecords <= TEST_LIMITS.MAX_RECORDS &&
            testConfig.batchSize >= TEST_LIMITS.MIN_BATCH_SIZE &&
            testConfig.batchSize <= TEST_LIMITS.MAX_BATCH_SIZE &&
            testConfig.batchSize <= testConfig.totalRecords
        );
    };

    const getOperationDescription = () => {
        if (testConfig.batchSize === 1) {
            return `Insert/Delete ${testConfig.totalRecords} records one by one`;
        }
        const batches = Math.ceil(testConfig.totalRecords / testConfig.batchSize);
        return `Insert/Delete ${testConfig.totalRecords} records in ${batches} batches of ${testConfig.batchSize}`;
    };

    const isAdminUser = userRole === 'ADMIN';

    return (
        <div className="controls">
            <h2 className="controls-title">🎯 Performance Test Configuration</h2>

            <div className="controls-content">
                <div className="control-row">
                    <div className="control-group">
                        <label htmlFor="totalRecords">Total Records</label>
                        <input
                            type="number"
                            id="totalRecords"
                            name="totalRecords"
                            value={testConfig.totalRecords}
                            onChange={handleInputChange}
                            min={TEST_LIMITS.MIN_RECORDS}
                            max={TEST_LIMITS.MAX_RECORDS}
                            disabled={isLoading}
                            className={errors.totalRecords ? 'error' : ''}
                        />
                        {errors.totalRecords && (
                            <span className="error-message">{errors.totalRecords}</span>
                        )}
                        <span className="input-hint">
                            Range: {TEST_LIMITS.MIN_RECORDS.toLocaleString()} - {TEST_LIMITS.MAX_RECORDS.toLocaleString()}
                        </span>
                    </div>

                    <div className="control-group">
                        <label htmlFor="batchSize">Batch Size</label>
                        <input
                            type="number"
                            id="batchSize"
                            name="batchSize"
                            value={testConfig.batchSize}
                            onChange={handleInputChange}
                            min={TEST_LIMITS.MIN_BATCH_SIZE}
                            max={TEST_LIMITS.MAX_BATCH_SIZE}
                            disabled={isLoading}
                            className={errors.batchSize ? 'error' : ''}
                        />
                        {errors.batchSize && (
                            <span className="error-message">{errors.batchSize}</span>
                        )}
                        <span className="input-hint">
                            Range: {TEST_LIMITS.MIN_BATCH_SIZE} - {TEST_LIMITS.MAX_BATCH_SIZE.toLocaleString()}
                            {testConfig.batchSize === 1 && ' (One by one)'}
                        </span>
                    </div>
                </div>

                <div className="operation-description">
                    <p>📋 <strong>Operation:</strong> {getOperationDescription()}</p>
                    {testConfig.batchSize === 1 && (
                        <p className="warning-text">⚠️ Individual inserts will be slower than batch operations</p>
                    )}
                </div>

                <div className="button-row">
                    <button
                        className="test-button insert-button"
                        onClick={onRunInsert}
                        disabled={isLoading || !isFormValid() || !isAdminUser}
                        title={!isAdminUser ? 'Admin role required for INSERT operations' : ''}
                    >
                        {isLoading ? '⏳' : '📥'} Run INSERT Test
                    </button>

                    <button
                        className="test-button delete-button"
                        onClick={onRunDelete}
                        disabled={isLoading || !isFormValid() || !isAdminUser}
                        title={!isAdminUser ? 'Admin role required for DELETE operations' : ''}
                    >
                        {isLoading ? '⏳' : '🗑️'} Run DELETE Test
                    </button>
                </div>

                {!isAdminUser && (
                    <div className="role-warning">
                        <p>ℹ️ You are logged in as a VIEWER. Only ADMIN users can run performance tests.</p>
                    </div>
                )}
            </div>
        </div>
    );
};

export default TestControls;