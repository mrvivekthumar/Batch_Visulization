import React from 'react';
import { PerformanceResult } from '../../types';

interface TestResultsProps {
    results: PerformanceResult[];
}

const TestResults: React.FC<TestResultsProps> = ({ results }) => {
    if (results.length === 0) {
        return (
            <div className="results-section">
                <div className="results-title">📈 Test Results</div>
                <div className="results-container">
                    <p style={{ textAlign: 'center', color: '#666', fontStyle: 'italic' }}>
                        Run a performance test to see detailed results here.
                    </p>
                </div>
            </div>
        );
    }

    return (
        <div className="results-section">
            <div className="results-title">📈 Test Results</div>
            <div className="results-container">
                {results.slice(-5).reverse().map((result, index) => (
                    <ResultItem key={`${result.operationId}-${index}`} result={result} />
                ))}
            </div>
        </div>
    );
};

interface ResultItemProps {
    result: PerformanceResult;
}

const ResultItem: React.FC<ResultItemProps> = ({ result }) => {
    const formatTestType = (testType: string): string => {
        return testType.replace('_', ' ');
    };

    const formatDateTime = (dateTime: string): string => {
        return new Date(dateTime).toLocaleString();
    };

    const metrics = [
        {
            label: 'Records Processed',
            value: result.recordsProcessed.toLocaleString()
        },
        {
            label: 'Duration (ms)',
            value: result.durationMs.toLocaleString()
        },
        {
            label: 'Avg Time/Record (ms)',
            value: result.averageTimePerRecord.toFixed(3)
        },
        {
            label: 'Throughput (rec/sec)',
            value: Math.round(result.recordsPerSecond).toLocaleString()
        },
        {
            label: 'Memory Used (MB)',
            value: result.memoryUsedMB.toString()
        },
        {
            label: 'Batch Count',
            value: result.batchCount.toString()
        }
    ];

    const getPerformanceIndicator = (result: PerformanceResult): string => {
        const recordsPerSecond = result.recordsPerSecond;
        if (recordsPerSecond > 10000) return '🚀 Excellent';
        if (recordsPerSecond > 5000) return '⚡ Very Good';
        if (recordsPerSecond > 1000) return '✅ Good';
        if (recordsPerSecond > 100) return '⚠️ Fair';
        return '🐌 Slow';
    };

    return (
        <div className="result-item">
            <div className="result-header">
                <div className="result-type">
                    {formatTestType(result.testType)} - Batch Size: {result.batchSize}
                </div>
                <div className="result-meta">
                    <div className="result-time">{formatDateTime(result.endTime)}</div>
                    <div className="performance-indicator">
                        {getPerformanceIndicator(result)}
                    </div>
                </div>
            </div>

            <div className="result-metrics">
                {metrics.map((metric, index) => (
                    <div key={index} className="metric">
                        <div className="metric-value">{metric.value}</div>
                        <div className="metric-label">{metric.label}</div>
                    </div>
                ))}
            </div>

            {result.operationId && (
                <div className="operation-id">
                    <small>Operation ID: {result.operationId}</small>
                </div>
            )}
        </div>
    );
};

export default TestResults;