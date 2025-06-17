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

    // ===== Statistics Methods =====
    async getSystemStats(): Promise<SystemStats> {
        const response: AxiosResponse<ApiResponse<SystemStats>> = await this.api.get('/api/v1/performance/stats/system');
        return response.data.data;
    }

    async getDatabaseStats(): Promise<DatabaseStats> {
        const response: AxiosResponse<ApiResponse<DatabaseStats>> = await this.api.get('/api/v1/performance/stats/database');
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