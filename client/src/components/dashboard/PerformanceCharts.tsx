import React, { useMemo } from 'react';
import {
    LineChart,
    Line,
    BarChart,
    Bar,
    XAxis,
    YAxis,
    CartesianGrid,
    Tooltip,
    Legend,
    ResponsiveContainer,
    ScatterChart,
    Scatter,
    AreaChart,
    Area,
} from 'recharts';
import { PerformanceResult } from '../../types/index';
import { format } from 'date-fns';

interface PerformanceChartsProps {
    testResults: PerformanceResult[];
}

const PerformanceCharts: React.FC<PerformanceChartsProps> = ({ testResults }) => {
    // Prepare chart data with better data handling
    const chartData = useMemo(() => {
        if (!testResults || testResults.length === 0) {
            return [];
        }

        return testResults.map((result, index) => {
            // Handle different property names from backend
            const recordsProcessed = result.recordsProcessed || result.totalRecords || 0;
            const durationMs = result.durationMs || result.duration || 0;
            const throughput = result.throughputRecordsPerSecond ||
                result.recordsPerSecond ||
                (recordsProcessed > 0 && durationMs > 0 ? (recordsProcessed / (durationMs / 1000)) : 0);

            return {
                index: index + 1,
                name: `${result.operationType || result.testType || 'TEST'} #${index + 1}`,
                operation: result.operationType || result.testType || 'UNKNOWN',
                records: recordsProcessed,
                batchSize: result.batchSize || 1,
                duration: Math.round(durationMs),
                throughput: Math.round(throughput),
                memory: Math.round(result.memoryUsedMB || result.memoryUsage || 0),
                cpu: Math.round(result.cpuUsagePercent || result.cpuUsage || 0),
                avgTimePerRecord: Number((result.avgTimePerRecord || result.averageTimePerRecord ||
                    (durationMs / recordsProcessed) || 0).toFixed(3)),
                avgTimePerBatch: Math.round(result.avgTimePerBatch || (durationMs / (recordsProcessed / (result.batchSize || 1))) || 0),
                timestamp: result.timestamp
                    ? format(new Date(result.timestamp), 'HH:mm:ss')
                    : `Test ${index + 1}`,
                fullTimestamp: result.timestamp || new Date().toISOString(),
                // Additional calculated metrics
                efficiency: throughput > 0 ? Math.round((throughput / (result.batchSize || 1)) * 100) / 100 : 0,
                memoryPerRecord: recordsProcessed > 0 ? Math.round((result.memoryUsedMB || 0) / recordsProcessed * 1000) / 1000 : 0,
            };
        }).filter(item => item.records > 0); // Filter out invalid data
    }, [testResults]);

    // Separate data by operation type with better filtering
    const insertData = chartData.filter(d =>
        d.operation && (d.operation.toUpperCase().includes('INSERT') || d.operation.toUpperCase().includes('BATCH_INSERT'))
    );
    const deleteData = chartData.filter(d =>
        d.operation && (d.operation.toUpperCase().includes('DELETE') || d.operation.toUpperCase().includes('BATCH_DELETE'))
    );

    // Enhanced tooltip formatter
    const formatTooltip = (value: any, name: string, props: any) => {
        if (value === null || value === undefined || isNaN(value)) {
            return ['N/A', name];
        }

        const { payload } = props;

        switch (name) {
            case 'throughput':
                return [`${Math.round(value).toLocaleString()} records/sec`, 'Throughput'];
            case 'duration':
                return [`${Math.round(value).toLocaleString()}ms`, 'Duration'];
            case 'memory':
                return [`${Math.round(value)}MB`, 'Memory Used'];
            case 'cpu':
                return [`${Math.round(value)}%`, 'CPU Usage'];
            case 'records':
                return [`${Math.round(value).toLocaleString()}`, 'Records Processed'];
            case 'avgTimePerRecord':
                return [`${Number(value).toFixed(3)}ms`, 'Avg Time/Record'];
            case 'avgTimePerBatch':
                return [`${Math.round(value)}ms`, 'Avg Time/Batch'];
            case 'efficiency':
                return [`${value}`, 'Efficiency Score'];
            case 'memoryPerRecord':
                return [`${value}MB`, 'Memory/Record'];
            default:
                return [value, name];
        }
    };

    // Custom label formatter for X-axis
    const formatXAxisLabel = (value: any, index: number) => {
        if (chartData[index]) {
            return chartData[index].timestamp;
        }
        return `Test ${index + 1}`;
    };

    if (!testResults || testResults.length === 0 || chartData.length === 0) {
        return (
            <div className="charts-container">
                <div className="no-data">
                    <h3>📊 No Performance Data Yet</h3>
                    <p>Run some performance tests to see visualizations here!</p>
                    <div style={{ marginTop: '20px', fontSize: '14px', color: '#666' }}>
                        <p>💡 Tips for better results:</p>
                        <ul style={{ textAlign: 'left', display: 'inline-block' }}>
                            <li>Try different batch sizes (100, 1000, 5000)</li>
                            <li>Test both INSERT and DELETE operations</li>
                            <li>Monitor memory usage patterns</li>
                        </ul>
                    </div>
                </div>
            </div>
        );
    }

    return (
        <div className="charts-container">
            <h2 className="charts-title">📊 Performance Analytics Dashboard</h2>

            <div className="charts-grid">
                {/* Enhanced Throughput Comparison Chart */}
                <div className="chart-card">
                    <h3>⚡ Throughput Performance (Records/Second)</h3>
                    <ResponsiveContainer width="100%" height={350}>
                        <AreaChart data={chartData}>
                            <defs>
                                <linearGradient id="throughputGradient" x1="0" y1="0" x2="0" y2="1">
                                    <stop offset="5%" stopColor="#667eea" stopOpacity={0.8} />
                                    <stop offset="95%" stopColor="#667eea" stopOpacity={0.1} />
                                </linearGradient>
                            </defs>
                            <CartesianGrid strokeDasharray="3 3" stroke="#f0f0f0" />
                            <XAxis
                                dataKey="timestamp"
                                tick={{ fontSize: 11 }}
                                angle={-45}
                                textAnchor="end"
                                height={80}
                            />
                            <YAxis
                                tick={{ fontSize: 11 }}
                                label={{ value: 'Records/Second', angle: -90, position: 'insideLeft' }}
                            />
                            <Tooltip
                                formatter={formatTooltip}
                                labelFormatter={(label) => `Time: ${label}`}
                                contentStyle={{
                                    backgroundColor: 'rgba(255, 255, 255, 0.95)',
                                    border: '1px solid #ccc',
                                    borderRadius: '8px',
                                    boxShadow: '0 4px 6px rgba(0, 0, 0, 0.1)'
                                }}
                            />
                            <Legend />
                            <Area
                                type="monotone"
                                dataKey="throughput"
                                stroke="#667eea"
                                strokeWidth={2}
                                fill="url(#throughputGradient)"
                                name="Throughput"
                            />
                        </AreaChart>
                    </ResponsiveContainer>
                </div>

                {/* Duration vs Batch Size Analysis */}
                <div className="chart-card">
                    <h3>⏱️ Performance vs Batch Size</h3>
                    <ResponsiveContainer width="100%" height={350}>
                        <ScatterChart data={chartData}>
                            <CartesianGrid strokeDasharray="3 3" stroke="#f0f0f0" />
                            <XAxis
                                dataKey="batchSize"
                                type="number"
                                domain={['dataMin', 'dataMax']}
                                name="Batch Size"
                                tick={{ fontSize: 11 }}
                                label={{ value: 'Batch Size', position: 'insideBottom', offset: -10 }}
                            />
                            <YAxis
                                dataKey="throughput"
                                type="number"
                                name="Throughput"
                                tick={{ fontSize: 11 }}
                                label={{ value: 'Throughput (Records/Sec)', angle: -90, position: 'insideLeft' }}
                            />
                            <Tooltip
                                formatter={formatTooltip}
                                cursor={{ strokeDasharray: '3 3' }}
                                contentStyle={{
                                    backgroundColor: 'rgba(255, 255, 255, 0.95)',
                                    border: '1px solid #ccc',
                                    borderRadius: '8px'
                                }}
                            />
                            <Legend />
                            {insertData.length > 0 && (
                                <Scatter
                                    data={insertData}
                                    fill="#28a745"
                                    name="INSERT Operations"
                                />
                            )}
                            {deleteData.length > 0 && (
                                <Scatter
                                    data={deleteData}
                                    fill="#dc3545"
                                    name="DELETE Operations"
                                />
                            )}
                        </ScatterChart>
                    </ResponsiveContainer>
                </div>

                {/* Memory Usage Trends */}
                <div className="chart-card">
                    <h3>💾 Memory Usage Analysis</h3>
                    <ResponsiveContainer width="100%" height={350}>
                        <LineChart data={chartData}>
                            <CartesianGrid strokeDasharray="3 3" stroke="#f0f0f0" />
                            <XAxis
                                dataKey="timestamp"
                                tick={{ fontSize: 11 }}
                                angle={-45}
                                textAnchor="end"
                                height={80}
                            />
                            <YAxis tick={{ fontSize: 11 }} />
                            <Tooltip formatter={formatTooltip} />
                            <Legend />
                            <Line
                                type="monotone"
                                dataKey="memory"
                                stroke="#20c997"
                                strokeWidth={2}
                                dot={{ r: 4 }}
                                name="Total Memory (MB)"
                            />
                            <Line
                                type="monotone"
                                dataKey="memoryPerRecord"
                                stroke="#ffc107"
                                strokeWidth={2}
                                dot={{ r: 4 }}
                                name="Memory per Record (MB)"
                            />
                        </LineChart>
                    </ResponsiveContainer>
                </div>

                {/* Execution Time Breakdown */}
                <div className="chart-card">
                    <h3>📈 Execution Time Breakdown</h3>
                    <ResponsiveContainer width="100%" height={350}>
                        <BarChart data={chartData}>
                            <CartesianGrid strokeDasharray="3 3" stroke="#f0f0f0" />
                            <XAxis
                                dataKey="name"
                                tick={{ fontSize: 10 }}
                                angle={-45}
                                textAnchor="end"
                                height={100}
                            />
                            <YAxis tick={{ fontSize: 11 }} />
                            <Tooltip formatter={formatTooltip} />
                            <Legend />
                            <Bar dataKey="duration" fill="#667eea" name="Total Duration (ms)" />
                            <Bar dataKey="avgTimePerRecord" fill="#ffc107" name="Avg Time/Record (ms)" />
                        </BarChart>
                    </ResponsiveContainer>
                </div>
            </div>

            {/* Enhanced Performance Summary */}
            <div className="performance-summary">
                <h3>📈 Performance Summary</h3>
                <div className="summary-grid">
                    <div className="summary-item">
                        <span className="summary-label">Peak Throughput:</span>
                        <span className="summary-value">
                            {chartData.length > 0
                                ? `${Math.max(...chartData.map(d => d.throughput || 0)).toLocaleString()} records/sec`
                                : '0 records/sec'}
                        </span>
                    </div>
                    <div className="summary-item">
                        <span className="summary-label">Optimal Batch Size:</span>
                        <span className="summary-value">
                            {chartData.length > 0
                                ? chartData.reduce((best, current) =>
                                    current.throughput > best.throughput ? current : best
                                ).batchSize.toLocaleString()
                                : 'N/A'}
                        </span>
                    </div>
                    <div className="summary-item">
                        <span className="summary-label">Fastest Execution:</span>
                        <span className="summary-value">
                            {chartData.length > 0
                                ? `${Math.min(...chartData.map(d => d.duration || Infinity))}ms`
                                : '0ms'}
                        </span>
                    </div>
                    <div className="summary-item">
                        <span className="summary-label">Total Records Processed:</span>
                        <span className="summary-value">
                            {testResults.reduce((sum, r) => sum + (r.recordsProcessed || r.totalRecords || 0), 0).toLocaleString()}
                        </span>
                    </div>
                    <div className="summary-item">
                        <span className="summary-label">Avg Memory Efficiency:</span>
                        <span className="summary-value">
                            {chartData.length > 0
                                ? `${(chartData.reduce((sum, d) => sum + (d.memoryPerRecord || 0), 0) / chartData.length).toFixed(3)}MB/record`
                                : '0MB/record'}
                        </span>
                    </div>
                    <div className="summary-item">
                        <span className="summary-label">Test Operations:</span>
                        <span className="summary-value">
                            {insertData.length} INSERT, {deleteData.length} DELETE
                        </span>
                    </div>
                </div>
            </div>
        </div>
    );
};

export default PerformanceCharts;