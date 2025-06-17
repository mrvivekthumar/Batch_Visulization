import axios, { AxiosInstance, AxiosResponse } from 'axios';
import {
    ApiResponse,
    LoginCredentials,
    PerformanceResult,
    DatabaseStats,
    SystemStats,
    TestConfig
} from '../types';

class ApiService {
    private api: AxiosInstance;
    private baseURL: string;

    constructor() {
        this.baseURL = process.env.REACT_APP_API_BASE_URL || 'http://localhost:8080';

        this.api = axios.create({
            baseURL: this.baseURL,
            timeout: 30000, // 30 seconds timeout
            headers: {
                'Content-Type': 'application/json',
            },
        });

        // Add request interceptor to include auth token
        this.api.interceptors.request.use(
            (config) => {
                const token = localStorage.getItem('auth_token');
                if (token) {
                    config.headers.Authorization = token;
                }
                return config;
            },
            (error) => Promise.reject(error)
        );

        // Add response interceptor for error handling
        this.api.interceptors.response.use(
            (response) => response,
            (error) => {
                if (error.response?.status === 401) {
                    // Clear auth on 401
                    this.clearAuth();
                    window.location.reload();
                }
                return Promise.reject(error);
            }
        );
    }

    // Authentication Methods
    async login(credentials: LoginCredentials): Promise<boolean> {
        try {
            const token = `Basic ${btoa(`${credentials.username}:${credentials.password}`)}`;

            // Test authentication with health endpoint
            const response = await axios.get(`${this.baseURL}/api/v1/performance/health`, {
                headers: { Authorization: token }
            });

            if (response.status === 200) {
                localStorage.setItem('auth_token', token);
                localStorage.setItem('current_user', credentials.username);
                localStorage.setItem('user_role', credentials.username === 'admin' ? 'ADMIN' : 'VIEWER');
                return true;
            }
            return false;
        } catch (error) {
            console.error('Login failed:', error);
            return false;
        }
    }

    logout(): void {
        this.clearAuth();
    }

    private clearAuth(): void {
        localStorage.removeItem('auth_token');
        localStorage.removeItem('current_user');
        localStorage.removeItem('user_role');
    }

    isAuthenticated(): boolean {
        return !!localStorage.getItem('auth_token');
    }

    getCurrentUser(): { username: string; role: 'ADMIN' | 'VIEWER' } | null {
        const username = localStorage.getItem('current_user');
        const role = localStorage.getItem('user_role') as 'ADMIN' | 'VIEWER';

        if (username && role) {
            return { username, role };
        }
        return null;
    }

    // Performance Test Methods
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

    // Statistics Methods
    async getSystemStats(): Promise<SystemStats> {
        const response: AxiosResponse<ApiResponse<SystemStats>> = await this.api.get('/api/v1/performance/stats/system');
        return response.data.data;
    }

    async getDatabaseStats(): Promise<DatabaseStats> {
        const response: AxiosResponse<ApiResponse<DatabaseStats>> = await this.api.get('/api/v1/performance/stats/database');
        return response.data.data;
    }

    // Health Check
    async healthCheck(): Promise<boolean> {
        try {
            const response = await this.api.get('/api/v1/performance/health');
            return response.status === 200;
        } catch (error) {
            return false;
        }
    }
}

// Export singleton instance
export const apiService = new ApiService();
export default apiService;