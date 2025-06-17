// API Service for Database Batch Performance Analyzer
// Handles all communication with Spring Boot backend

import axios, { AxiosInstance, AxiosResponse, AxiosError } from 'axios';
import {
    ApiResponse,
    LoginCredentials,
    User,
    TestConfig,
    PerformanceResult,
    SystemStats,
    DatabaseStats,
    ApiError,
} from '../types/index';

class ApiService {
    private api: AxiosInstance;
    private baseURL: string;

    constructor() {
        // Use proxy in development, direct URL in production
        this.baseURL = process.env.NODE_ENV === 'production'
            ? process.env.REACT_APP_API_URL || 'http://localhost:8080'
            : '';

        this.api = axios.create({
            baseURL: this.baseURL,
            timeout: 30000, // 30 seconds timeout for performance tests
            headers: {
                'Content-Type': 'application/json',
                'Accept': 'application/json',
            },
        });

        this.setupInterceptors();
    }

    private setupInterceptors(): void {
        // Request interceptor - add auth token
        this.api.interceptors.request.use(
            (config) => {
                const token = this.getAuthToken();
                if (token && config.headers) {
                    config.headers.Authorization = `Basic ${token}`;
                }
                return config;
            },
            (error) => Promise.reject(error)
        );

        // Response interceptor - handle common errors
        this.api.interceptors.response.use(
            (response) => response,
            (error: AxiosError) => {
                if (error.response?.status === 401) {
                    this.clearAuth();
                    window.location.href = '/login';
                }
                return Promise.reject(this.formatError(error));
            }
        );
    }

    private formatError(error: AxiosError): ApiError {
        return {
            response: error.response ? {
                status: error.response.status,
                data: error.response.data
            } : undefined,
            request: error.request,
            message: error.message,
        };
    }

    private getAuthToken(): string | null {
        return localStorage.getItem('auth_token');
    }

    private setAuthToken(username: string, password: string): string {
        const token = btoa(`${username}:${password}`);
        localStorage.setItem('auth_token', token);
        localStorage.setItem('current_user', username);
        return token;
    }

    private clearAuth(): void {
        localStorage.removeItem('auth_token');
        localStorage.removeItem('current_user');
        localStorage.removeItem('user_role');
    }

    // ===== Authentication Methods =====
    async login(credentials: LoginCredentials): Promise<User> {
        try {
            const token = this.setAuthToken(credentials.username, credentials.password);

            // Test the credentials by calling a protected endpoint
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

    // ===== Performance Test Methods =====
    async runInsertTest(config: TestConfig): Promise<PerformanceResult> {
        const url = `/api/v1/performance/initialize?totalRecords=${config.totalRecords}&batchSize=${config.batchSize}`;
        const response: AxiosResponse<ApiResponse<PerformanceResult>> = await this.api.post(url);
        return response.data.data;
    }

    async runDeleteTest(config: TestConfig): Promise<PerformanceResult> {
        const url = `/api/v1/performance/delete?totalRecords=${config.totalRecords}&batchSize=${config.batchSize}`;
        const response: AxiosResponse<ApiResponse<PerformanceResult>> = await this.api.post(url);
        return response.data.data;
    }

    // ===== Health Check =====
    async healthCheck(): Promise<boolean> {
        try {
            const response = await this.api.get('/api/v1/performance/health');
            return response.status === 200;
        } catch (error) {
            return false;
        }
    }
    // ===== Data Adapters =====
    private adaptSystemStats(backendData: any): SystemStats {
        return {
            timestamp: backendData.application?.timestamp || new Date().toISOString(),
            jvmInfo: {
                version: backendData.jvm?.version || 'Unknown',
                vendor: backendData.jvm?.vendor || 'Unknown',
                runtime: `${backendData.jvm?.vendor || 'Unknown'} ${backendData.jvm?.version || ''}`,
            },
            memoryInfo: {
                totalMemoryMB: backendData.jvm?.totalMemoryMB || 0,
                usedMemoryMB: backendData.jvm?.usedMemoryMB || 0,
                freeMemoryMB: backendData.jvm?.freeMemoryMB || 0,
                maxMemoryMB: backendData.jvm?.maxMemoryMB || 0,
                usagePercentage: backendData.jvm?.memoryUsagePercent || 0,
            },
            processorInfo: {
                availableProcessors: backendData.jvm?.availableProcessors || 1,
                systemLoadAverage: 0, // Not provided by backend
                processCpuLoad: 0, // Not provided by backend
            },
            applicationInfo: {
                uptime: backendData.application?.uptime || 'Unknown',
                profile: 'docker', // Default profile
                version: backendData.application?.version || '1.0.0',
                port: 8080, // Default port
            },
        };
    }

    private adaptDatabaseStats(backendData: any): DatabaseStats {
        return {
            timestamp: backendData.timestamp || new Date().toISOString(),
            connectionInfo: {
                url: 'jdbc:postgresql://postgres:5432/performance_db', // Default URL
                driverName: 'PostgreSQL JDBC Driver', // Default driver
                productName: 'PostgreSQL', // Default product
                productVersion: '16', // Default version
                isValid: true, // Assume valid if we get data
                activeConnections: 1, // Parse from connectionInfo string or default
                idleConnections: 0, // Not provided by backend
                maxConnections: 20, // Default max connections
            },
            tableInfo: {
                tableName: 'performance_data', // Default table name
                totalRecords: backendData.totalRecords || 0,
                estimatedSizeMB: this.parseSizeToMB(backendData.tableSize),
                lastUpdated: backendData.timestamp || new Date().toISOString(),
            },
            performanceMetrics: {
                avgInsertTimeMs: 0, // Not provided by backend currently
                avgDeleteTimeMs: 0, // Not provided by backend currently  
                totalInsertOperations: 0, // Not provided by backend currently
                totalDeleteOperations: 0, // Not provided by backend currently
                lastTestTimestamp: backendData.timestamp || new Date().toISOString(),
            },
        };
    }

    private parseSizeToMB(sizeString: string): number {
        if (!sizeString) return 0;

        // Parse strings like "48 kB", "1.2 MB", "1.5 GB"
        const match = sizeString.match(/^([\d.]+)\s*([kMG]?B)$/i);
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

    // ===== Statistics Methods =====
    async getSystemStats(): Promise<SystemStats> {
        const response: AxiosResponse<ApiResponse<any>> = await this.api.get('/api/v1/performance/stats/system');
        const adaptedData = this.adaptSystemStats(response.data.data);
        return adaptedData;
    }

    async getDatabaseStats(): Promise<DatabaseStats> {
        const response: AxiosResponse<ApiResponse<any>> = await this.api.get('/api/v1/performance/stats/database');
        const adaptedData = this.adaptDatabaseStats(response.data.data);
        return adaptedData;
    }
    // ===== Rate Limit Info =====
    getRateLimitInfo(): { remaining?: string; resetTime?: string } {
        const response = this.api.defaults.headers;
        return {
            remaining: response?.['X-Rate-Limit-Remaining'] as string,
            resetTime: response?.['X-Rate-Limit-Reset'] as string,
        };
    }
}

// Export singleton instance
export const apiService = new ApiService();
export default apiService;