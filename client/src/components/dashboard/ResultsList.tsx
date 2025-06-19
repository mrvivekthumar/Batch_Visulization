import React, { useState, useMemo } from 'react';
import { PerformanceResult } from '../../types/index';
import { format } from 'date-fns';

interface ResultsListProps {
    testResults: PerformanceResult[];
}

const ResultsList: React.FC<ResultsListProps> = ({ testResults }) => {
    const [filter, setFilter] = useState<'ALL' | 'INSERT' | 'DELETE'>('ALL');
    const [sortBy, setSortBy] = useState<'timestamp' | 'duration' | 'throughput'>('timestamp');
    const [sortOrder, setSortOrder] = useState<'asc' | 'desc'>('desc');
    const [showDetails, setShowDetails] = useState<string | null>(null);

    // Filter and sort results with null checks
    const filteredAndSortedResults = useMemo(() => {
        if (!testResults || testResults.length === 0) {
            return [];
        }

        let filtered = testResults;

        // Apply filter with null check
        if (filter !== 'ALL') {
            filtered = testResults.filter(result =>
                result && result.operationType === filter
            );
        }

        // Apply sorting with null checks
        const sorted = [...filtered].sort((a, b) => {
            if (!a || !b) return 0;

            let comparison = 0;

            switch (sortBy) {
                case 'timestamp':
                    const dateA = a.timestamp ? new Date(a.timestamp).getTime() : 0;
                    const dateB = b.timestamp ? new Date(b.timestamp).getTime() : 0;
                    comparison = dateA - dateB;
                    break;
                case 'duration':
                    comparison = (a.durationMs || 0) - (b.durationMs || 0);
                    break;
                case 'throughput':
                    comparison = (a.throughputRecordsPerSecond || 0) - (b.throughputRecordsPerSecond || 0);
                    break;
            }

            return sortOrder === 'asc' ? comparison : -comparison;
        });

        return sorted;
    }, [testResults, filter, sortBy, sortOrder]);

    const formatDuration = (ms: number | undefined) => {
        if (!ms || ms === 0) return '0ms';
        if (ms < 1000) return `${ms}ms`;
        if (ms < 60000) return `${(ms / 1000).toFixed(1)}s`;
        return `${(ms / 60000).toFixed(1)}m`;
    };

    const formatThroughput = (recordsPerSec: number | undefined) => {
        if (!recordsPerSec || recordsPerSec === 0) return '0 rec/s';
        if (recordsPerSec < 1000) return `${recordsPerSec.toFixed(0)} rec/s`;
        if (recordsPerSec < 1000000) return `${(recordsPerSec / 1000).toFixed(1)}k rec/s`;
        return `${(recordsPerSec / 1000000).toFixed(1)}M rec/s`;
    };

    const getPerformanceRating = (result: PerformanceResult) => {
        const throughput = result.throughputRecordsPerSecond || 0;
        if (throughput > 10000) return { rating: 'excellent', color: '#28a745', icon: '🚀' };
        if (throughput > 5000) return { rating: 'good', color: '#20c997', icon: '⚡' };
        if (throughput > 1000) return { rating: 'average', color: '#ffc107', icon: '⭐' };
        return { rating: 'slow', color: '#dc3545', icon: '🐌' };
    };

    const toggleDetails = (testId: string | undefined) => {
        if (!testId) return;
        setShowDetails(showDetails === testId ? null : testId);
    };

    if (!testResults || testResults.length === 0) {
        return (
            <div className="results-section">
                <h2 className="results-title">📋 Test Results</h2>
                <div className="no-results">
                    <div className="no-results-content">
                        <h3>🎯 No Test Results Yet</h3>
                        <p>Run some performance tests to see detailed results here!</p>
                        <div className="results-hint">
                            <p>💡 <strong>Tip:</strong> Start with a small batch test (1000 records) to see how it works.</p>
                        </div>
                    </div>
                </div>
            </div>
        );
    }

    return (
        <div className="results-section">
            <div className="results-header">
                <h2 className="results-title">📋 Test Results History</h2>
                <div className="results-count">
                    {filteredAndSortedResults.length} of {testResults.length} results
                </div>
            </div>

            {/* Controls */}
            <div className="results-controls">
                <div className="filter-controls">
                    <label>Filter by Operation:</label>
                    <select
                        value={filter}
                        onChange={(e) => setFilter(e.target.value as 'ALL' | 'INSERT' | 'DELETE')}
                        className="filter-select"
                    >
                        <option value="ALL">All Operations</option>
                        <option value="INSERT">INSERT Only</option>
                        <option value="DELETE">DELETE Only</option>
                    </select>
                </div>

                <div className="sort-controls">
                    <label>Sort by:</label>
                    <select
                        value={sortBy}
                        onChange={(e) => setSortBy(e.target.value as 'timestamp' | 'duration' | 'throughput')}
                        className="sort-select"
                    >
                        <option value="timestamp">Time</option>
                        <option value="duration">Duration</option>
                        <option value="throughput">Throughput</option>
                    </select>
                    <button
                        className={`sort-order-btn ${sortOrder}`}
                        onClick={() => setSortOrder(sortOrder === 'asc' ? 'desc' : 'asc')}
                        title={`Sort ${sortOrder === 'asc' ? 'Descending' : 'Ascending'}`}
                    >
                        {sortOrder === 'asc' ? '↑' : '↓'}
                    </button>
                </div>
            </div>

            {/* Results List */}
            <div className="results-container">
                {filteredAndSortedResults.map((result, index) => {
                    if (!result) return null;

                    const performance = getPerformanceRating(result);
                    const isExpanded = showDetails === result.testId;

                    return (
                        <div key={result.testId || `result-${index}`} className="result-item">
                            <div className="result-summary" onClick={() => toggleDetails(result.testId)}>
                                <div className="result-header">
                                    <div className="result-operation">
                                        <span className={`operation-badge ${(result.operationType || 'unknown').toLowerCase()}`}>
                                            {result.operationType === 'INSERT' ? '📥' : result.operationType === 'DELETE' ? '🗑️' : '❓'} {result.operationType || 'UNKNOWN'}
                                        </span>
                                        <span className="result-index">#{index + 1}</span>
                                    </div>

                                    <div className="result-key-metrics">
                                        <div className="metric-item">
                                            <span className="metric-label">Records:</span>
                                            <span className="metric-value">{(result.recordsProcessed || 0).toLocaleString()}</span>
                                        </div>
                                        <div className="metric-item">
                                            <span className="metric-label">Duration:</span>
                                            <span className="metric-value">{formatDuration(result.durationMs)}</span>
                                        </div>
                                        <div className="metric-item">
                                            <span className="metric-label">Throughput:</span>
                                            <span className="metric-value">{formatThroughput(result.throughputRecordsPerSecond)}</span>
                                        </div>
                                    </div>

                                    <div className="result-meta">
                                        <div className="performance-rating" style={{ color: performance.color }}>
                                            {performance.icon} {performance.rating.toUpperCase()}
                                        </div>
                                        <div className="result-time">
                                            {result.timestamp
                                                ? format(new Date(result.timestamp), 'MMM dd, HH:mm:ss')
                                                : 'Unknown time'
                                            }
                                        </div>
                                        <div className="expand-indicator">
                                            {isExpanded ? '▼' : '▶'}
                                        </div>
                                    </div>
                                </div>
                            </div>

                            {/* Expanded Details */}
                            {isExpanded && (
                                <div className="result-details">
                                    <div className="details-grid">
                                        <div className="detail-section">
                                            <h4>📊 Performance Metrics</h4>
                                            <div className="detail-items">
                                                <div className="detail-item">
                                                    <span className="detail-label">Batch Size:</span>
                                                    <span className="detail-value">{result.batchSize || 'N/A'}</span>
                                                </div>
                                                <div className="detail-item">
                                                    <span className="detail-label">Total Batches:</span>
                                                    <span className="detail-value">{result.totalBatches || 'N/A'}</span>
                                                </div>
                                                <div className="detail-item">
                                                    <span className="detail-label">Avg Time/Record:</span>
                                                    <span className="detail-value">
                                                        {result.avgTimePerRecord ? `${result.avgTimePerRecord.toFixed(3)}ms` : 'N/A'}
                                                    </span>
                                                </div>
                                                <div className="detail-item">
                                                    <span className="detail-label">Avg Time/Batch:</span>
                                                    <span className="detail-value">
                                                        {result.avgTimePerBatch ? `${Math.round(result.avgTimePerBatch)}ms` : 'N/A'}
                                                    </span>
                                                </div>
                                            </div>
                                        </div>

                                        <div className="detail-section">
                                            <h4>💾 Resource Usage</h4>
                                            <div className="detail-items">
                                                <div className="detail-item">
                                                    <span className="detail-label">Memory Used:</span>
                                                    <span className="detail-value">
                                                        {result.memoryUsedMB ? `${Math.round(result.memoryUsedMB)} MB` : 'N/A'}
                                                    </span>
                                                </div>
                                                <div className="detail-item">
                                                    <span className="detail-label">CPU Usage:</span>
                                                    <span className="detail-value">
                                                        {result.cpuUsagePercent ? `${Math.round(result.cpuUsagePercent)}%` : 'N/A'}
                                                    </span>
                                                </div>
                                                <div className="detail-item">
                                                    <span className="detail-label">Status:</span>
                                                    <span className={`detail-value status-${(result.status || 'unknown').toLowerCase()}`}>
                                                        {result.status || 'UNKNOWN'}
                                                    </span>
                                                </div>
                                                <div className="detail-item">
                                                    <span className="detail-label">Test ID:</span>
                                                    <span className="detail-value test-id">
                                                        {result.testId || 'N/A'}
                                                    </span>
                                                </div>
                                            </div>
                                        </div>

                                        <div className="detail-section full-width">
                                            <h4>🔍 Performance Analysis</h4>
                                            {result.batchSize === 1 ? (
                                                <p className="analysis-text">
                                                    <strong>Individual Processing:</strong> Each record was processed separately.
                                                    This provides maximum reliability but lower throughput compared to batch operations.
                                                </p>
                                            ) : (
                                                <p className="analysis-text">
                                                    <strong>Batch Processing:</strong> Records were processed in batches of {result.batchSize || 'unknown'}.
                                                    {(result.throughputRecordsPerSecond || 0) > 5000
                                                        ? ' Excellent throughput achieved with this batch size!'
                                                        : (result.throughputRecordsPerSecond || 0) > 1000
                                                            ? ' Good performance - consider testing larger batch sizes for better throughput.'
                                                            : ' Consider optimizing batch size or checking system resources.'
                                                    }
                                                </p>
                                            )}
                                        </div>
                                    </div>
                                </div>
                            )}
                        </div>
                    );
                })}
            </div>
        </div>
    );
};

export default ResultsList;