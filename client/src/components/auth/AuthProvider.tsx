import React, { createContext, useContext, useReducer, useEffect, ReactNode } from 'react';
import { User, AuthState, LoginCredentials } from '../../types';
import { apiService } from '../../services/api';

// Auth Actions
type AuthAction =
    | { type: 'LOGIN_START' }
    | { type: 'LOGIN_SUCCESS'; payload: User }
    | { type: 'LOGIN_FAILURE'; payload: string }
    | { type: 'LOGOUT' }
    | { type: 'RESTORE_SESSION'; payload: User };

// Auth Context
interface AuthContextType {
    state: AuthState;
    login: (credentials: LoginCredentials) => Promise<void>;
    logout: () => void;
}

const AuthContext = createContext<AuthContextType | undefined>(undefined);

// Auth Reducer
const authReducer = (state: AuthState, action: AuthAction): AuthState => {
    switch (action.type) {
        case 'LOGIN_START':
            return {
                ...state,
                isLoading: true,
                error: null
            };

        case 'LOGIN_SUCCESS':
            return {
                ...state,
                isLoading: false,
                isAuthenticated: true,
                user: action.payload,
                token: localStorage.getItem('auth_token'),
                error: null
            };

        case 'LOGIN_FAILURE':
            return {
                ...state,
                isLoading: false,
                isAuthenticated: false,
                user: null,
                token: null,
                error: action.payload
            };

        case 'LOGOUT':
            return {
                ...state,
                isAuthenticated: false,
                user: null,
                token: null,
                isLoading: false,
                error: null
            };

        case 'RESTORE_SESSION':
            return {
                ...state,
                isAuthenticated: true,
                user: action.payload,
                token: localStorage.getItem('auth_token'),
                isLoading: false
            };

        default:
            return state;
    }
};

// Initial State
const initialState: AuthState = {
    user: null,
    token: null,
    isAuthenticated: false,
    isLoading: false,
    error: null
};

// Auth Provider Component
interface AuthProviderProps {
    children: ReactNode;
}

export const AuthProvider: React.FC<AuthProviderProps> = ({ children }) => {
    const [state, dispatch] = useReducer(authReducer, initialState);

    // Restore session on app load
    useEffect(() => {
        const restoreSession = async () => {
            const currentUser = apiService.getCurrentUser();

            if (currentUser && apiService.isAuthenticated()) {
                try {
                    // Verify the session is still valid
                    const isHealthy = await apiService.healthCheck();
                    if (isHealthy) {
                        dispatch({
                            type: 'RESTORE_SESSION',
                            payload: currentUser
                        });
                    } else {
                        // Session invalid, clear it
                        apiService.logout();
                    }
                } catch (error) {
                    console.error('Session restore failed:', error);
                    apiService.logout();
                }
            }
        };

        restoreSession();
    }, []);

    // Login function
    const login = async (credentials: LoginCredentials): Promise<void> => {
        dispatch({ type: 'LOGIN_START' });

        try {
            const success = await apiService.login(credentials);

            if (success) {
                const user: User = {
                    username: credentials.username,
                    role: credentials.username === 'admin' ? 'ADMIN' : 'VIEWER'
                };

                dispatch({ type: 'LOGIN_SUCCESS', payload: user });
            } else {
                dispatch({
                    type: 'LOGIN_FAILURE',
                    payload: 'Invalid username or password. Please try again.'
                });
            }
        } catch (error: any) {
            let errorMessage = 'Login failed. Please try again.';

            if (error.response?.status === 401) {
                errorMessage = 'Invalid username or password.';
            } else if (error.response?.status === 429) {
                errorMessage = 'Too many login attempts. Please wait and try again.';
            } else if (!error.response) {
                errorMessage = 'Network error. Please check your connection.';
            }

            dispatch({ type: 'LOGIN_FAILURE', payload: errorMessage });
        }
    };

    // Logout function
    const logout = (): void => {
        apiService.logout();
        dispatch({ type: 'LOGOUT' });
    };

    const contextValue: AuthContextType = {
        state,
        login,
        logout
    };

    return (
        <AuthContext.Provider value={contextValue}>
            {children}
        </AuthContext.Provider>
    );
};

// Custom hook to use auth context
export const useAuth = (): AuthContextType => {
    const context = useContext(AuthContext);
    if (context === undefined) {
        throw new Error('useAuth must be used within an AuthProvider');
    }
    return context;
};

export default AuthProvider;