import React, { useState, useEffect } from 'react';
import { SystemStats as SystemStatsType, DatabaseStats } from '../../types';
import { apiService } from '../../services/api';

const SystemStats: React.FC = () => {
    const [systemStats, setSystemStats] = useState<SystemStatsType | null>(null);
    const [dbStats, setDbStats] = useState<DatabaseStats | null>(null);
    const [loading, setLoading] = useState(true);

    useEffect(() => {
        loadStats();
        // Refresh every 30 seconds
        const interval = setInterval(loadStats, 30000);
        return () => clearInterval(interval);
    }, []);

    const loadStats = async () => {
        try {
            const [sysStats, databaseStats] = await Promise.all([
                apiService.getSystemStats(),
                apiService.getDatabaseStats()
            ]);
            setSystemStats(sysStats);
            setDbStats(databaseStats);
        } catch (error) {
            console.error('Failed to load stats:', error);
        } finally {
            setLoading(false);
        }
    };

    if (loading) {
        return <div className="loading">Loading system statistics...</div>;
    }

    return (
        <div className="stats-grid">
            <div className="stat-card">
                <div className="stat-value">{systemStats?.jvm.availableProcessors || '-'}</div>
                <div className="stat-label">CPU Cores</div>
            </div>

            <div className="stat-card">
                <div className="stat-value">{systemStats?.jvm.totalMemoryMB || '-'}</div>
                <div className="stat-label">Total Memory (MB)</div>
            </div>

            <div className="stat-card">
                <div className="stat-value">{systemStats?.jvm.usedMemoryMB || '-'}</div>
                <div className="stat-label">Used Memory (MB)</div>
                <div className={`stat-trend ${getMemoryTrendClass(systemStats?.jvm.memoryUsagePercent)}`}>
                    {systemStats?.jvm.memoryUsagePercent ? `${systemStats.jvm.memoryUsagePercent}%` : ''}
                </div>
            </div>

            <div className="stat-card">
                <div className="stat-value">{dbStats?.totalRecords.toLocaleString() || '-'}</div>
                <div className="stat-label">Database Records</div>
                <div className="stat-trend trend-up">
                    {dbStats?.activeOperations ? `${dbStats.activeOperations} active ops` : 'Ready'}
                </div>
            </div>
        </div>
    );
};

const getMemoryTrendClass = (memoryPercent?: number): string => {
    if (!memoryPercent) return '';
    if (memoryPercent > 80) return 'trend-down';
    if (memoryPercent > 60) return '';
    return 'trend-up';
};

export default SystemStats;