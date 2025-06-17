import React from 'react';
import { SystemStats as ISystemStats, DatabaseStats } from '../../types/index';
import { format } from 'date-fns';

interface SystemStatsProps {
    systemStats: ISystemStats | null;
    databaseStats: DatabaseStats | null;
    onRefresh: () => Promise<void>;
}

const SystemStats: React.FC<SystemStatsProps> = ({
    systemStats,
    databaseStats,
    onRefresh
}) => {
    const formatUptime = (uptimeStr: string) => {
        // Parse uptime string (e.g., "2h 30m 45s")
        const match = uptimeStr.match(/(\d+)h\s*(\d+)m\s*(\d+)s/);
        if (match) {
            const [, hours, minutes, seconds] = match;
            return `${hours}h ${minutes}m ${seconds}s`;
        }
        return uptimeStr;
    };

    const getHealthStatus = () => {
        if (!systemStats || !databaseStats) return 'unknown';

        if (!systemStats.memoryInfo || !systemStats.processorInfo || !databaseStats.connectionInfo) {
            return 'unknown';
        }

        const memoryUsage = systemStats.memoryInfo.usagePercentage;
        const cpuLoad = systemStats.processorInfo.processCpuLoad * 100;
        const dbConnected = databaseStats.connectionInfo.isValid;

        if (!dbConnected) return 'error';
        if (memoryUsage > 90 || cpuLoad > 90) return 'warning';
        if (memoryUsage > 70 || cpuLoad > 70) return 'caution';
        return 'healthy';
    };

    const healthStatus = getHealthStatus();

    return (
        <div className="stats-section">
            <div className="stats-header">
                <h2 className="stats-title">📊 System Health Dashboard</h2>
                <button
                    className="refresh-button"
                    onClick={onRefresh}
                    title="Refresh Statistics"
                >
                    🔄 Refresh
                </button>
            </div>

            <div className="health-indicator">
                <div className={`health-status ${healthStatus}`}>
                    <span className="health-icon">
                        {healthStatus === 'healthy' && '✅'}
                        {healthStatus === 'caution' && '⚠️'}
                        {healthStatus === 'warning' && '🟠'}
                        {healthStatus === 'error' && '❌'}
                        {healthStatus === 'unknown' && '❓'}
                    </span>
                    <span className="health-text">
                        System Status: {healthStatus.toUpperCase()}
                    </span>
                </div>
                {systemStats && (
                    <div className="last-updated">
                        Last updated: {systemStats.timestamp
                            ? format(new Date(systemStats.timestamp), 'MMM d, HH:mm:ss')
                            : 'Unknown'
                        }
                    </div>
                )}
            </div>

            <div className="stats-grid">
                {/* JVM Information */}
                <div className="stat-card jvm-card">
                    <div className="stat-header">
                        <h3>☕ JVM Information</h3>
                    </div>
                    {systemStats ? (
                        <div className="stat-content">
                            <div className="stat-row">
                                <span className="stat-label">Version:</span>
                                <span className="stat-value">{systemStats.jvmInfo.version}</span>
                            </div>
                            <div className="stat-row">
                                <span className="stat-label">Vendor:</span>
                                <span className="stat-value">{systemStats.jvmInfo.vendor}</span>
                            </div>
                            <div className="stat-row">
                                <span className="stat-label">Runtime:</span>
                                <span className="stat-value">{systemStats.jvmInfo.runtime}</span>
                            </div>
                        </div>
                    ) : (
                        <div className="stat-loading">Loading JVM info...</div>
                    )}
                </div>

                {/* Memory Information */}
                <div className="stat-card memory-card">
                    <div className="stat-header">
                        <h3>💾 Memory Usage</h3>
                    </div>
                    {systemStats ? (
                        <div className="stat-content">
                            <div className="stat-row">
                                <span className="stat-label">Total Memory:</span>
                                <span className="stat-value">{systemStats.memoryInfo.totalMemoryMB} MB</span>
                            </div>
                            <div className="stat-row">
                                <span className="stat-label">Used Memory:</span>
                                <span className="stat-value">{systemStats.memoryInfo.usedMemoryMB} MB</span>
                            </div>
                            <div className="stat-row">
                                <span className="stat-label">Free Memory:</span>
                                <span className="stat-value">{systemStats.memoryInfo.freeMemoryMB} MB</span>
                            </div>
                            <div className="stat-row">
                                <span className="stat-label">Max Memory:</span>
                                <span className="stat-value">{systemStats.memoryInfo.maxMemoryMB} MB</span>
                            </div>
                            <div className="stat-row">
                                <span className="stat-label">Usage:</span>
                                <span className={`stat-value ${systemStats.memoryInfo.usagePercentage > 70 ? 'warning' : ''}`}>
                                    {systemStats.memoryInfo.usagePercentage.toFixed(1)}%
                                </span>
                            </div>
                        </div>
                    ) : (
                        <div className="stat-loading">Loading memory info...</div>
                    )}
                </div>

                {/* Processor Information */}
                <div className="stat-card processor-card">
                    <div className="stat-header">
                        <h3>🖥️ Processor Info</h3>
                    </div>
                    {systemStats ? (
                        <div className="stat-content">
                            <div className="stat-row">
                                <span className="stat-label">Available Processors:</span>
                                <span className="stat-value">{systemStats.processorInfo.availableProcessors}</span>
                            </div>
                            <div className="stat-row">
                                <span className="stat-label">System Load:</span>
                                <span className="stat-value">
                                    {systemStats.processorInfo.systemLoadAverage.toFixed(2)}
                                </span>
                            </div>
                            <div className="stat-row">
                                <span className="stat-label">CPU Usage:</span>
                                <span className="stat-value">
                                    {(systemStats.processorInfo.processCpuLoad * 100).toFixed(1)}%
                                </span>
                            </div>
                        </div>
                    ) : (
                        <div className="stat-loading">Loading processor info...</div>
                    )}
                </div>

                {/* Application Information */}
                <div className="stat-card app-card">
                    <div className="stat-header">
                        <h3>🚀 Application Info</h3>
                    </div>
                    {systemStats ? (
                        <div className="stat-content">
                            <div className="stat-row">
                                <span className="stat-label">Version:</span>
                                <span className="stat-value">{systemStats.applicationInfo.version || 'Unknown'}</span>
                            </div>
                            <div className="stat-row">
                                <span className="stat-label">Profile:</span>
                                <span className="stat-value stat-badge">
                                    {systemStats.applicationInfo.profile || 'default'}
                                </span>
                            </div>
                            <div className="stat-row">
                                <span className="stat-label">Port:</span>
                                <span className="stat-value">{systemStats.applicationInfo.port || '8080'}</span>
                            </div>
                            <div className="stat-row">
                                <span className="stat-label">Uptime:</span>
                                <span className="stat-value">
                                    {systemStats.applicationInfo.uptime ? formatUptime(systemStats.applicationInfo.uptime) : 'Unknown'}
                                </span>
                            </div>
                        </div>
                    ) : (
                        <div className="stat-loading">Loading app info...</div>
                    )}
                </div>

                {/* Database Information */}
                <div className="stat-card database-card">
                    <div className="stat-header">
                        <h3>🗄️ Database Status</h3>
                    </div>
                    {databaseStats ? (
                        <div className="stat-content">
                            <div className="connection-status">
                                <span className={`status-indicator ${databaseStats.connectionInfo.isValid ? 'connected' : 'disconnected'}`}>
                                    {databaseStats.connectionInfo.isValid ? '🟢 Connected' : '🔴 Disconnected'}
                                </span>
                            </div>
                            <div className="stat-row">
                                <span className="stat-label">Product:</span>
                                <span className="stat-value">
                                    {databaseStats.connectionInfo.productName} {databaseStats.connectionInfo.productVersion}
                                </span>
                            </div>
                            <div className="stat-row">
                                <span className="stat-label">Driver:</span>
                                <span className="stat-value">{databaseStats.connectionInfo.driverName}</span>
                            </div>
                            <div className="stat-row">
                                <span className="stat-label">Connections:</span>
                                <span className="stat-value">
                                    {databaseStats.connectionInfo.activeConnections} / {databaseStats.connectionInfo.maxConnections}
                                </span>
                            </div>
                        </div>
                    ) : (
                        <div className="stat-loading">Loading database info...</div>
                    )}
                </div>

                {/* Database Performance */}
                <div className="stat-card performance-card">
                    <div className="stat-header">
                        <h3>📈 Database Performance</h3>
                    </div>
                    {databaseStats ? (
                        <div className="stat-content">
                            <div className="stat-row">
                                <span className="stat-label">Table Records:</span>
                                <span className="stat-value">
                                    {databaseStats.tableInfo.totalRecords.toLocaleString()}
                                </span>
                            </div>
                            <div className="stat-row">
                                <span className="stat-label">Table Size:</span>
                                <span className="stat-value">
                                    {databaseStats.tableInfo.estimatedSizeMB.toFixed(2)} MB
                                </span>
                            </div>
                            <div className="stat-row">
                                <span className="stat-label">Avg Insert:</span>
                                <span className="stat-value">
                                    {databaseStats.performanceMetrics.avgInsertTimeMs.toFixed(2)}ms
                                </span>
                            </div>
                            <div className="stat-row">
                                <span className="stat-label">Avg Delete:</span>
                                <span className="stat-value">
                                    {databaseStats.performanceMetrics.avgDeleteTimeMs.toFixed(2)}ms
                                </span>
                            </div>
                            <div className="stat-row">
                                <span className="stat-label">Total Operations:</span>
                                <span className="stat-value">
                                    {(databaseStats.performanceMetrics.totalInsertOperations +
                                        databaseStats.performanceMetrics.totalDeleteOperations).toLocaleString()}
                                </span>
                            </div>
                        </div>
                    ) : (
                        <div className="stat-loading">Loading performance info...</div>
                    )}
                </div>
            </div>
        </div>
    );
};

export default SystemStats;