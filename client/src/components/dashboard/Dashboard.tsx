import React, { useState, useCallback } from 'react';
import { User, PerformanceResult, NotificationType } from '../../types';
import SystemStats from './SystemStats';
import PerformanceControls from './PerformanceControls';
import TestResults from './TestResults';
import NotificationContainer from './NotificationContainer';
import './Dashboard.css';

interface DashboardProps {
    user: User;
    onLogout: () => void;
}

const Dashboard: React.FC<DashboardProps> = ({ user, onLogout }) => {
    const [testResults, setTestResults] = useState<PerformanceResult[]>([]);
    const [addNotification, setAddNotification] = useState<((message: string, type: NotificationType) => void) | null>(null);

    const handleTestComplete = useCallback((result: PerformanceResult) => {
        setTestResults(prev => [...prev, result]);
    }, []);

    const handleNotification = useCallback((message: string, type: NotificationType) => {
        if (addNotification) {
            addNotification(message, type);
        }
    }, [addNotification]);

    const handleAddNotificationFunction = useCallback((addFn: (message: string, type: NotificationType) => void) => {
        setAddNotification(() => addFn);
    }, []);

    return (
        <div className="dashboard">
            <header className="dashboard-header">
                <h1>🚀 Database Batch Performance Analyzer</h1>
                <p>Real-time Performance Testing & Visualization Dashboard</p>
                <div className="user-info">
                    <span>{user.username} ({user.role})</span>
                    <button onClick={onLogout} className="logout-btn">
                        Logout
                    </button>
                </div>
            </header>

            <div className="dashboard-content">
                {/* System Statistics */}
                <SystemStats />

                {/* Performance Controls */}
                <PerformanceControls
                    user={user}
                    onTestComplete={handleTestComplete}
                    onNotification={handleNotification}
                />

                {/* Test Results */}
                <TestResults results={testResults} />
            </div>

            {/* Notification System */}
            <NotificationContainer onAddNotification={handleAddNotificationFunction} />
        </div>
    );
};

export default Dashboard;