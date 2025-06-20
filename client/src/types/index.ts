// TypeScript Type Definitions for Batch Performance Analyzer
// These types match exactly with your Spring Boot backend API

// ===== API Response Types =====
export interface ApiResponse<T> {
    success: boolean;
    message: string;
    data: T;
    timestamp: string;
    errorDetails?: string;
}

// ===== Authentication Types =====
export interface LoginCredentials {
    username: string;
    password: string;
}

export interface User {
    username: string;
    role: 'ADMIN' | 'VIEWER';
}

export interface AuthResponse {
    token: string;
    user: User;
}

// ===== Performance Test Types =====
export interface TestConfig {
    totalRecords: number;
    batchSize: number;
}

export interface PerformanceResult {
    operationType: "INSERT" | "DELETE" | "BATCH_INSERT" | "BATCH_DELETE" | "UPDATE" | "SELECT" | string;
    recordsProcessed: number;
    batchSize: number;
    durationMs: number;
    throughputRecordsPerSecond: number;
    avgTimePerBatch: number;
    memoryUsage?: number;
    totalRecords?: number;
    duration?: number;
    recordsPerSecond?: number;
    avgTimePerRecord: number;
    totalBatches: number;
    memoryUsedMB: number;
    cpuUsagePercent: number;
    metadata?: any;
    testType?: string;
    cpuUsage?: number;
    averageTimePerRecord?: number;
    timestamp: string;
    testId: string;
    status: 'SUCCESS' | 'FAILED' | 'PARTIAL';
}

// ===== Statistics Types =====
export interface SystemStats {
    timestamp: string;
    jvmInfo: {
        version: string;
        vendor: string;
        runtime: string;
    };
    memoryInfo: {
        totalMemoryMB: number;
        usedMemoryMB: number;
        freeMemoryMB: number;
        maxMemoryMB: number;
        usagePercentage: number;
    };
    processorInfo: {
        availableProcessors: number;
        systemLoadAverage: number;
        processCpuLoad: number;
    };
    applicationInfo: {
        uptime: string;
        profile: string;
        version: string;
        port: number;
    };
}

export interface DatabaseStats {
    timestamp: string;
    connectionInfo: {
        url: string;
        driverName: string;
        productName: string;
        productVersion: string;
        isValid: boolean;
        activeConnections: number;
        idleConnections: number;
        maxConnections: number;
    };
    tableInfo: {
        tableName: string;
        totalRecords: number;
        estimatedSizeMB: number;
        lastUpdated: string;
    };
    performanceMetrics: {
        avgInsertTimeMs: number;
        avgDeleteTimeMs: number;
        totalInsertOperations: number;
        totalDeleteOperations: number;
        lastTestTimestamp: string;
    };
}

// ===== Chart Data Types =====
export interface ChartDataPoint {
    name: string;
    insert?: number;
    delete?: number;
    throughput?: number;
    memory?: number;
    records?: number;
    duration?: number;
    timestamp?: string;
}

export interface PerformanceChartData {
    throughputData: ChartDataPoint[];
    durationData: ChartDataPoint[];
    memoryData: ChartDataPoint[];
    comparisonData: ChartDataPoint[];
}

// ===== UI State Types =====
export interface LoadingState {
    isLoading: boolean;
    message?: string;
}

export interface NotificationState {
    open: boolean;
    message: string;
    severity: 'success' | 'error' | 'warning' | 'info';
}

export interface AppState {
    user: User | null;
    isAuthenticated: boolean;
    loading: LoadingState;
    notification: NotificationState;
    testResults: PerformanceResult[];
    systemStats: SystemStats | null;
    databaseStats: DatabaseStats | null;
}

export interface AuthState {
    user: User | null;
    token: string | null;
    isAuthenticated: boolean;
    isLoading: boolean;
    error: string | null;
}

// ===== Form Types =====
export interface TestFormData {
    totalRecords: string;
    batchSize: string;
}

export interface TestFormErrors {
    totalRecords?: string;
    batchSize?: string;
    general?: string;
}

// ===== API Error Types =====
export interface ApiError {
    response?: {
        status: number;
        data?: any; // Made more flexible for error responses
    };
    request?: any;
    message: string;
    code?: string
}

// ===== Constants =====
export const TEST_LIMITS = {
    MIN_RECORDS: 100,
    MAX_RECORDS: 100000,
    MIN_BATCH_SIZE: 1,
    MAX_BATCH_SIZE: 10000,
} as const;

export const USER_ROLES = {
    ADMIN: 'ADMIN',
    VIEWER: 'VIEWER',
} as const;

export const OPERATION_TYPES = {
    INSERT: 'INSERT',
    DELETE: 'DELETE',
} as const;

// ===== Utility Types =====
export type Theme = 'light' | 'dark';
export type OperationType = keyof typeof OPERATION_TYPES;
export type UserRole = keyof typeof USER_ROLES;


export const safeFormatDate = (timestamp: string | undefined) => {
    if (!timestamp) return 'Unknown';
    try {
        return new Date(timestamp).toLocaleString();
    } catch {
        return 'Invalid date';
    }
};