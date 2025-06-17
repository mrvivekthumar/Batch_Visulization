import React, { useState } from 'react';
import { LoginCredentials } from '../../types';
import './LoginForm.css';

interface LoginFormProps {
    onLogin: (credentials: LoginCredentials) => Promise<void>;
    isLoading: boolean;
    error: string | null;
}

const LoginForm: React.FC<LoginFormProps> = ({ onLogin, isLoading, error }) => {
    const [credentials, setCredentials] = useState<LoginCredentials>({
        username: '',
        password: ''
    });

    const handleSubmit = async (e: React.FormEvent) => {
        e.preventDefault();
        e.stopPropagation();
        await onLogin(credentials);
    };

    const handleInputChange = (e: React.ChangeEvent<HTMLInputElement>) => {
        const { name, value } = e.target;
        setCredentials(prev => ({
            ...prev,
            [name]: value
        }));
    };

    return (
        <div className="auth-modal">
            <div className="auth-form">
                <h2 className="auth-title">🔐 Login Required</h2>

                {error && (
                    <div className="auth-error">
                        {error}
                    </div>
                )}

                <form onSubmit={handleSubmit}>
                    <div className="form-group">
                        <label htmlFor="username">Username</label>
                        <input
                            type="text"
                            id="username"
                            name="username"
                            value={credentials.username}
                            onChange={handleInputChange}
                            required
                            placeholder="Enter username (admin/viewer)"
                            autoComplete="username"
                            disabled={isLoading}
                        />
                    </div>

                    <div className="form-group">
                        <label htmlFor="password">Password</label>
                        <input
                            type="password"
                            id="password"
                            name="password"
                            value={credentials.password}
                            onChange={handleInputChange}
                            required
                            placeholder="Enter password"
                            autoComplete="current-password"
                            disabled={isLoading}
                        />
                    </div>

                    <button
                        type="submit"
                        className={`auth-button ${isLoading ? 'button-loading' : ''}`}
                        disabled={isLoading}
                    >
                        {isLoading ? 'Signing in...' : 'Sign In'}
                    </button>
                </form>

                <div className="demo-credentials">
                    <p><strong>Demo Credentials:</strong></p>
                    <p>Admin: admin / admin123!</p>
                    <p>Viewer: viewer / viewer123!</p>
                </div>
            </div>
        </div>
    );
};

export default LoginForm;