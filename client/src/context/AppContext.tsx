// React Context for Global State Management
// Using React 19 features with TypeScript

import React, { createContext, useContext, useReducer, useEffect, ReactNode } from 'react';
import { User, PerformanceResult, SystemStats, DatabaseStats, AppState, NotificationState } from '../types/index';
import { apiService } from '../services/apiService';

// ===== Action Types =====
type AppAction =
    | { type: 'SET_USER'; payload: User | null }
    | { type: 'SET_LOADING'; payload: { isLoading: boolean; message?: string } }
    | { type: 'SET_NOTIFICATION'; payload: NotificationState }
    | { type: 'CLEAR_NOTIFICATION' }
    | { type: 'ADD_TEST_RESULT'; payload: PerformanceResult }
    | { type: 'SET_TEST_RESULTS'; payload: PerformanceResult[] }
    | { type: 'SET_SYSTEM_STATS'; payload: SystemStats }
    | { type: 'SET_DATABASE_STATS'; payload: DatabaseStats }
    | { type: 'RESET_STATE' };

// ===== Initial State =====
const initialState: AppState = {
    user: null,
    isAuthenticated: false,
    loading: { isLoading: false },
    notification: { open: false, message: '', severity: 'info' },
    testResults: [],
    systemStats: null,
    databaseStats: null,
};

// ===== Reducer =====
function appReducer(state: AppState, action: AppAction): AppState {
    switch (action.type) {
        case 'SET_USER':
            return {
                ...state,
                user: action.payload,
                isAuthenticated: !!action.payload,
            };

        case 'SET_LOADING':
            return {
                ...state,
                loading: action.payload,
            };

        case 'SET_NOTIFICATION':
            return {
                ...state,
                notification: action.payload,
            };

        case 'CLEAR_NOTIFICATION':
            return {
                ...state,
                notification: { open: false, message: '', severity: 'info' },
            };

        case 'ADD_TEST_RESULT':
            return {
                ...state,
                testResults: [...state.testResults, action.payload],
            };

        case 'SET_TEST_RESULTS':
            return {
                ...state,
                testResults: action.payload,
            };

        case 'SET_SYSTEM_STATS':
            return {
                ...state,
                systemStats: action.payload,
            };

        case 'SET_DATABASE_STATS':
            return {
                ...state,
                databaseStats: action.payload,
            };

        case 'RESET_STATE':
            return initialState;

        default:
            return state;
    }
}

// ===== Context Type =====
interface AppContextType {
    state: AppState;
    dispatch: React.Dispatch<AppAction>;
    // Helper functions
    showNotification: (message: string, severity?: 'success' | 'error' | 'warning' | 'info') => void;
    setLoading: (isLoading: boolean, message?: string) => void;
    login: (username: string, password: string) => Promise<boolean>;
    logout: () => void;
    refreshStats: () => Promise<void>;
}

// ===== Create Context =====
const AppContext = createContext<AppContextType | undefined>(undefined);

// ===== Provider Component =====
interface AppProviderProps {
    children: ReactNode;
}

export const AppProvider: React.FC<AppProviderProps> = ({ children }) => {
    const [state, dispatch] = useReducer(appReducer, initialState);

    // Initialize user from localStorage on app start
    useEffect(() => {
        const currentUser = apiService.getCurrentUser();
        if (currentUser && apiService.isAuthenticated()) {
            dispatch({ type: 'SET_USER', payload: currentUser });
        }
    }, []);

    // Helper function to show notifications
    const showNotification = (
        message: string,
        severity: 'success' | 'error' | 'warning' | 'info' = 'info'
    ) => {
        dispatch({
            type: 'SET_NOTIFICATION',
            payload: { open: true, message, severity },
        });
    };

    // Helper function to set loading state
    const setLoading = (isLoading: boolean, message?: string) => {
        dispatch({
            type: 'SET_LOADING',
            payload: { isLoading, message },
        });
    };

    // Login function
    const login = async (username: string, password: string): Promise<boolean> => {
        setLoading(true, 'Authenticating...');
        try {
            const user = await apiService.login({ username, password });
            dispatch({ type: 'SET_USER', payload: user });
            showNotification(`Welcome, ${user.username}!`, 'success');
            return true;
        } catch (error: any) {
            const message = error.response?.data?.message || 'Invalid credentials';
            showNotification(`Login failed: ${message}`, 'error');
            return false;
        } finally {
            setLoading(false);
        }
    };

    // Logout function
    const logout = () => {
        apiService.logout();
        dispatch({ type: 'RESET_STATE' });
        showNotification('Logged out successfully', 'info');
    };

    // Refresh system and database stats
    const refreshStats = async () => {
        try {
            const [systemStats, databaseStats] = await Promise.all([
                apiService.getSystemStats(),
                apiService.getDatabaseStats(),
            ]);

            dispatch({ type: 'SET_SYSTEM_STATS', payload: systemStats });
            dispatch({ type: 'SET_DATABASE_STATS', payload: databaseStats });
        } catch (error: any) {
            showNotification('Failed to load statistics', 'warning');
        }
    };

    const contextValue: AppContextType = {
        state,
        dispatch,
        showNotification,
        setLoading,
        login,
        logout,
        refreshStats,
    };

    return (
        <AppContext.Provider value={contextValue}>
            {children}
        </AppContext.Provider>
    );
};

// ===== Custom Hook =====
export const useApp = (): AppContextType => {
    const context = useContext(AppContext);
    if (context === undefined) {
        throw new Error('useApp must be used within an AppProvider');
    }
    return context;
};

export default AppContext;