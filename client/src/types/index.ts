// User and Authentication Types
export interface User {
    username: string;
    role: 'ADMIN' | 'VIEWER';
}

export interface LoginCredentials {
    username: string;
    password: string;
}

export interface AuthState {
    user: User | null;
    token: string | null;
    isAuthenticated: boolean;
    isLoading: boolean;
    error: string | null;
}

// Performance Test Types (matching your Spring Boot DTOs)
export interface PerformanceResult {
    testType: string;
    batchSize: number;
    recordsProcessed: number;
    durationMs: number;
    averageTimePerRecord: number;
    memoryUsedMB: number;
    recordsPerSecond: number;
    batchCount: number;
    startTime: string;
    endTime: string;
    operationId?: string;
}

export interface DatabaseStats {
    totalRecords: number;
    tableSize: string;
    connectionInfo: string;
    timestamp: string;
    activeOperations: number;
}

export interface SystemStats {
    jvm: {
        availableProcessors: number;
        totalMemoryMB: number;
        usedMemoryMB: number;
        freeMemoryMB: number;
        maxMemoryMB: number;
        memoryUsagePercent: number;
    };
    os: {
        name: string;
        version: string;
        architecture: string;
    };
    application: {
        name: string;
        version: string;
        uptime: string;
        timestamp: string;
    };
}

// API Response Types
export interface ApiResponse<T> {
    success: boolean;
    message?: string;
    data: T;
    timestamp: string;
    error?: string;
}

// Rate Limiting Types
export interface RateLimitInfo {
    general: number | null;
    performance: number | null;
}

// Chart Data Types
export interface ChartDataPoint {
    label: string;
    value: number;
}

// Test Configuration
export interface TestConfig {
    totalRecords: number;
    batchSize: number;
}

// Notification Types
export type NotificationType = 'success' | 'error' | 'warning' | 'info';

export interface Notification {
    id: string;
    message: string;
    type: NotificationType;
    timestamp: number;
}