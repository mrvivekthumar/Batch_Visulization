import React from 'react';
import { AuthProvider, useAuth } from './components/auth';
import LoginForm from './components/auth/LoginForm';
import Dashboard from './components/dashboard/Dashboard';
import './App.css';

// Main App Content Component
const AppContent: React.FC = () => {
  const { state, login, logout } = useAuth();

  if (!state.isAuthenticated) {
    return (
      <LoginForm
        onLogin={login}
        isLoading={state.isLoading}
        error={state.error}
      />
    );
  }

  return <Dashboard user={state.user!} onLogout={logout} />;
};

// Main App Component
const App: React.FC = () => {
  return (
    <AuthProvider>
      <div className="App">
        <AppContent />
      </div>
    </AuthProvider>
  );
};

export default App;