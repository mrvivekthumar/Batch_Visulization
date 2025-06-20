// Enhanced API Service for Database Batch Performance Analyzer
// Handles all communication with Spring Boot backend with better error handling and data adaptation

import axios, { AxiosError, AxiosInstance, AxiosResponse } from 'axios';
import {
    ApiError,
    ApiResponse,
    DatabaseStats,
    LoginCredentials,
    PerformanceResult,
    SystemStats,
    TestConfig,
    User,
} from '../types/index';

class ApiService {
    private api: AxiosInstance;
    private baseURL: string;
    private retryCount = 3;
    private retryDelay = 1000;

    constructor() {
        // Enhanced URL configuration for Docker environments
        this.baseURL = this.getBaseURL();

        this.api = axios.create({
            baseURL: this.baseURL,
            timeout: 45000, // Increased timeout for performance tests
            headers: {
                'Content-Type': 'application/json',
                'Accept': 'application/json',
            },
        });

        this.setupInterceptors();
        console.log(`🔗 API Service initialized with base URL: ${this.baseURL}`);
    }

    private getBaseURL(): string {
        if (process.env.NODE_ENV === 'production') {
            // For Docker production environment
            return process.env.REACT_APP_API_URL || 'http://localhost:8080';
        } else if (process.env.NODE_ENV === 'development') {
            // Check if running in Docker (look for common Docker indicators)
            const isDocker = process.env.REACT_APP_DOCKER === 'true' ||
                window.location.hostname === 'localhost' ||
                window.location.hostname === '0.0.0.0';

            if (isDocker) {
                return 'http://localhost:8080'; // Direct connection for Docker
            }
            return ''; // Use proxy for development
        }
        return 'http://localhost:8080';
    }

    private setupInterceptors(): void {
        // Request interceptor - add auth token and logging
        this.api.interceptors.request.use(
            (config) => {
                const token = this.getAuthToken();
                if (token && config.headers) {
                    config.headers.Authorization = `Basic ${token}`;
                }

                // Log API calls in development
                if (process.env.NODE_ENV === 'development') {
                    console.log(`🔄 API Request: ${config.method?.toUpperCase()} ${config.url}`);
                }

                return config;
            },
            (error) => Promise.reject(error)
        );

        // Response interceptor with retry logic
        this.api.interceptors.response.use(
            (response) => {
                if (process.env.NODE_ENV === 'development') {
                    console.log(`✅ API Response: ${response.status} ${response.config.url}`);
                }
                return response;
            },
            async (error: AxiosError) => {
                const config = error.config as any;

                // Log errors in development
                if (process.env.NODE_ENV === 'development') {
                    console.error(`❌ API Error: ${error.response?.status} ${config?.url}`, error.message);
                }

                // Handle authentication errors
                if (error.response?.status === 401) {
                    this.clearAuth();
                    window.location.href = '/login';
                    return Promise.reject(this.formatError(error));
                }

                // Retry logic for network errors
                if (this.shouldRetry(error) && (!config._retry || config._retry < this.retryCount)) {
                    config._retry = (config._retry || 0) + 1;

                    console.log(`🔄 Retrying request... Attempt ${config._retry}/${this.retryCount}`);

                    await this.delay(this.retryDelay * config._retry);
                    return this.api.request(config);
                }

                return Promise.reject(this.formatError(error));
            }
        );
    }

    private shouldRetry(error: AxiosError): boolean {
        return !error.response ||
            error.response.status >= 500 ||
            error.code === 'NETWORK_ERROR' ||
            error.code === 'ECONNREFUSED';
    }

    private delay(ms: number): Promise<void> {
        return new Promise(resolve => setTimeout(resolve, ms));
    }

    private formatError(error: AxiosError): ApiError {
        return {
            response: error.response ? {
                status: error.response.status,
                data: error.response.data,
            } : undefined,
            request: error.request ? {
                url: error.config?.url,
                method: error.config?.method,
            } : undefined,
            message: error.message || 'An unexpected error occurred',
            code: error?.code,
        };
    }

    // ===== Authentication Methods =====
    private getAuthToken(): string | null {
        return localStorage.getItem('auth_token');
    }

    private setAuthToken(username: string, password: string): void {
        const token = btoa(`${username}:${password}`);
        localStorage.setItem('auth_token', token);
        localStorage.setItem('current_user', username);
    }

    private clearAuth(): void {
        localStorage.removeItem('auth_token');
        localStorage.removeItem('current_user');
        localStorage.removeItem('user_role');
    }

    async login(credentials: LoginCredentials): Promise<User> {
        try {
            this.setAuthToken(credentials.username, credentials.password);

            // Test authentication with a simple request
            const response = await this.api.get('/api/v1/performance/health');

            if (response.status === 200) {
                const role = credentials.username === 'admin' ? 'ADMIN' : 'VIEWER';
                localStorage.setItem('user_role', role);

                return {
                    username: credentials.username,
                    role: role,
                };
            }

            throw new Error('Invalid credentials');
        } catch (error) {
            this.clearAuth();
            throw error;
        }
    }

    logout(): void {
        this.clearAuth();
    }

    isAuthenticated(): boolean {
        return !!this.getAuthToken();
    }

    getCurrentUser(): User | null {
        const username = localStorage.getItem('current_user');
        const role = localStorage.getItem('user_role') as 'ADMIN' | 'VIEWER';

        if (username && role) {
            return { username, role };
        }
        return null;
    }

    // ===== Performance Test Methods with Enhanced Data Handling =====
    async runInsertTest(config: TestConfig): Promise<PerformanceResult> {
        try {
            const url = `/api/v1/performance/initialize?totalRecords=${config.totalRecords}&batchSize=${config.batchSize}`;
            console.log(`🚀 Starting INSERT test: ${config.totalRecords} records, batch size ${config.batchSize}`);

            const response: AxiosResponse<ApiResponse<any>> = await this.api.post(url);
            const adaptedResult = this.adaptPerformanceResult(response.data.data, 'INSERT');

            console.log(`✅ INSERT test completed: ${adaptedResult.throughputRecordsPerSecond.toFixed(2)} records/sec`);
            return adaptedResult;
        } catch (error) {
            console.error('❌ INSERT test failed:', error);
            throw error;
        }
    }

    async runDeleteTest(config: TestConfig): Promise<PerformanceResult> {
        try {
            const url = `/api/v1/performance/delete?totalRecords=${config.totalRecords}&batchSize=${config.batchSize}`;
            console.log(`🗑️ Starting DELETE test: ${config.totalRecords} records, batch size ${config.batchSize}`);

            const response: AxiosResponse<ApiResponse<any>> = await this.api.post(url);
            const adaptedResult = this.adaptPerformanceResult(response.data.data, 'DELETE');

            console.log(`✅ DELETE test completed: ${adaptedResult.throughputRecordsPerSecond.toFixed(2)} records/sec`);
            return adaptedResult;
        } catch (error) {
            console.error('❌ DELETE test failed:', error);
            throw error;
        }
    }

    // Enhanced data adapter for performance results
    private adaptPerformanceResult(backendData: any, operationType: string): PerformanceResult {
        const recordsProcessed = backendData.recordsProcessed || backendData.totalRecords || 0;
        const durationMs = backendData.durationMs || backendData.duration || backendData.executionTimeMs || 0;
        const batchSize = backendData.batchSize || 1;

        // Calculate throughput if not provided
        let throughput = backendData.throughputRecordsPerSecond || backendData.recordsPerSecond || 0;
        if (throughput === 0 && recordsProcessed > 0 && durationMs > 0) {
            throughput = recordsProcessed / (durationMs / 1000);
        }

        // Calculate missing values
        const totalBatches = Math.ceil(recordsProcessed / batchSize);
        const status = backendData.status || (recordsProcessed > 0 ? 'SUCCESS' : 'FAILED');

        // Calculate average times
        const avgTimePerRecord = recordsProcessed > 0 ? durationMs / recordsProcessed : 0;
        const avgTimePerBatch = batchSize > 0 ? durationMs / (recordsProcessed / batchSize) : 0;

        return {
            testId: backendData.testId || `test_${Date.now()}`,
            operationType: operationType,
            batchSize: batchSize,
            recordsProcessed: recordsProcessed,
            durationMs: durationMs,
            totalBatches: totalBatches,
            throughputRecordsPerSecond: throughput,
            avgTimePerRecord: avgTimePerRecord,
            avgTimePerBatch: avgTimePerBatch,
            memoryUsedMB: backendData.memoryUsedMB || backendData.memoryUsage || 0,
            cpuUsagePercent: backendData.cpuUsagePercent || backendData.cpuUsage || 0,
            timestamp: backendData.timestamp || new Date().toISOString(),
            metadata: {
                jvmInfo: backendData.jvmInfo || {},
                systemInfo: backendData.systemInfo || {},
                databaseInfo: backendData.databaseInfo || {},
                performanceProfile: this.calculatePerformanceProfile(throughput, avgTimePerRecord),
            },
            status: status as 'SUCCESS' | 'FAILED' | 'PARTIAL',
        };
    }

    private calculatePerformanceProfile(throughput: number, avgTimePerRecord: number): string {
        if (throughput > 10000) return 'EXCELLENT';
        if (throughput > 5000) return 'GOOD';
        if (throughput > 1000) return 'FAIR';
        return 'POOR';
    }

    // ===== Health Check =====
    async healthCheck(): Promise<boolean> {
        try {
            const response = await this.api.get('/api/v1/performance/health');
            return response.status === 200;
        } catch (error) {
            console.warn('⚠️ Health check failed:', error);
            return false;
        }
    }

    // ===== Statistics Methods with Better Error Handling =====
    async getSystemStats(): Promise<SystemStats> {
        try {
            const response: AxiosResponse<ApiResponse<any>> = await this.api.get('/api/v1/performance/stats/system');
            return this.adaptSystemStats(response.data.data);
        } catch (error) {
            console.error('❌ Failed to fetch system stats:', error);
            // Return default stats if request fails
            return this.getDefaultSystemStats();
        }
    }

    async getDatabaseStats(): Promise<DatabaseStats> {
        try {
            const response: AxiosResponse<ApiResponse<any>> = await this.api.get('/api/v1/performance/stats/database');
            return this.adaptDatabaseStats(response.data.data);
        } catch (error) {
            console.error('❌ Failed to fetch database stats:', error);
            // Return default stats if request fails
            return this.getDefaultDatabaseStats();
        }
    }

    private getDefaultSystemStats(): SystemStats {
        return {
            timestamp: new Date().toISOString(),
            jvmInfo: {
                version: 'Unknown',
                vendor: 'Unknown',
                runtime: 'Unknown',
            },
            memoryInfo: {
                totalMemoryMB: 0,
                usedMemoryMB: 0,
                freeMemoryMB: 0,
                maxMemoryMB: 0,
                usagePercentage: 0,
            },
            processorInfo: {
                availableProcessors: 1,
                systemLoadAverage: 0,
                processCpuLoad: 0,
            },
            applicationInfo: {
                uptime: 'Unknown',
                profile: 'unknown',
                version: '1.0.0',
                port: 8080,
            },
        };
    }

    private getDefaultDatabaseStats(): DatabaseStats {
        return {
            timestamp: new Date().toISOString(),
            connectionInfo: {
                url: 'Unknown',
                driverName: 'Unknown',
                productName: 'Unknown',
                productVersion: 'Unknown',
                isValid: false,
                activeConnections: 0,
                idleConnections: 0,
                maxConnections: 0,
            },
            tableInfo: {
                tableName: 'Unknown',
                totalRecords: 0,
                estimatedSizeMB: 0,
                lastUpdated: new Date().toISOString(),
            },
            performanceMetrics: {
                avgInsertTimeMs: 0,
                avgDeleteTimeMs: 0,
                totalInsertOperations: 0,
                totalDeleteOperations: 0,
                lastTestTimestamp: new Date().toISOString(),
            },
        };
    }

    // ===== Enhanced Data Adapters =====
    private adaptSystemStats(backendData: any): SystemStats {
        return {
            timestamp: backendData.timestamp || new Date().toISOString(),
            jvmInfo: {
                version: backendData.jvm?.version || backendData.jvmInfo?.version || 'Unknown',
                vendor: backendData.jvm?.vendor || backendData.jvmInfo?.vendor || 'Unknown',
                runtime: `${backendData.jvm?.vendor || 'Unknown'} ${backendData.jvm?.version || ''}`,
            },
            memoryInfo: {
                totalMemoryMB: backendData.jvm?.totalMemoryMB || backendData.memory?.total || 0,
                usedMemoryMB: backendData.jvm?.usedMemoryMB || backendData.memory?.used || 0,
                freeMemoryMB: backendData.jvm?.freeMemoryMB || backendData.memory?.free || 0,
                maxMemoryMB: backendData.jvm?.maxMemoryMB || backendData.memory?.max || 0,
                usagePercentage: backendData.jvm?.memoryUsagePercent || backendData.memory?.usagePercent || 0,
            },
            processorInfo: {
                availableProcessors: backendData.jvm?.availableProcessors || backendData.cpu?.processors || 1,
                systemLoadAverage: backendData.system?.loadAverage || 0,
                processCpuLoad: backendData.system?.cpuLoad || 0,
            },
            applicationInfo: {
                uptime: backendData.application?.uptime || 'Unknown',
                profile: backendData.application?.profile || 'default',
                version: backendData.application?.version || '1.0.0',
                port: backendData.application?.port || 8080,
            },
        };
    }

    private adaptDatabaseStats(backendData: any): DatabaseStats {
        const totalRecords = backendData.totalRecords || backendData.table?.totalRecords || 0;
        const tableSize = backendData.tableSize || backendData.table?.size || '0 MB';

        return {
            timestamp: backendData.timestamp || new Date().toISOString(),
            connectionInfo: {
                url: backendData.connectionInfo?.url || 'jdbc:postgresql://postgres:5432/performance_db',
                driverName: backendData.connectionInfo?.driverName || 'PostgreSQL JDBC Driver',
                productName: backendData.connectionInfo?.productName || 'PostgreSQL',
                productVersion: backendData.connectionInfo?.productVersion || '16',
                isValid: backendData.connectionInfo?.isValid !== undefined ? backendData.connectionInfo.isValid : true,
                activeConnections: backendData.connectionInfo?.activeConnections || 1,
                idleConnections: backendData.connectionInfo?.idleConnections || 0,
                maxConnections: backendData.connectionInfo?.maxConnections || 20,
            },
            tableInfo: {
                tableName: backendData.tableName || backendData.table?.name || 'performance_data',
                totalRecords: totalRecords,
                estimatedSizeMB: this.parseSizeToMB(tableSize),
                lastUpdated: backendData.lastUpdated || backendData.timestamp || new Date().toISOString(),
            },
            performanceMetrics: {
                avgInsertTimeMs: backendData.performanceMetrics?.avgInsertTimeMs || 0,
                avgDeleteTimeMs: backendData.performanceMetrics?.avgDeleteTimeMs || 0,
                totalInsertOperations: backendData.performanceMetrics?.totalInsertOps || 0,
                totalDeleteOperations: backendData.performanceMetrics?.totalDeleteOps || 0,
                lastTestTimestamp: backendData.performanceMetrics?.lastTestTimestamp || new Date().toISOString(),
            },
        };
    }

    private parseSizeToMB(sizeString: string): number {
        if (!sizeString) return 0;

        // Parse strings like "48 kB", "1.2 MB", "1.5 GB"
        const match = sizeString.toString().match(/^([\d.]+)\s*([kMG]?B)$/i);
        if (!match) return 0;

        const value = parseFloat(match[1]);
        const unit = match[2].toUpperCase();

        switch (unit) {
            case 'KB': return value / 1024;
            case 'MB': return value;
            case 'GB': return value * 1024;
            default: return value / (1024 * 1024); // Assume bytes
        }
    }

    // ===== Rate Limit Info =====
    getRateLimitInfo(): { remaining?: string; resetTime?: string } {
        const response = this.api.defaults.headers;
        return {
            remaining: response?.['X-Rate-Limit-Remaining'] as string,
            resetTime: response?.['X-Rate-Limit-Reset'] as string,
        };
    }

    // ===== Connection Test Utility =====
    async testConnection(): Promise<{ success: boolean; latency: number; message: string }> {
        const startTime = Date.now();
        try {
            await this.healthCheck();
            const latency = Date.now() - startTime;
            return {
                success: true,
                latency,
                message: `Connection successful (${latency}ms)`
            };
        } catch (error) {
            const latency = Date.now() - startTime;
            return {
                success: false,
                latency,
                message: `Connection failed: ${error instanceof Error ? error.message : 'Unknown error'}`
            };
        }
    }
}

// Export singleton instance
export const apiService = new ApiService();
export default apiService;