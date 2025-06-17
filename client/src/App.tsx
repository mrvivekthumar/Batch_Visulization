import { AppProvider } from './context/AppContext';
import { useApp } from './context/AppContext';
import LoginForm from './components/auth/LoginForm';
import Dashboard from './components/dashboard/Dashboard';
import './App.css';
import { LoginCredentials } from './types';

const AppContent: React.FC = () => {
  const { state, login, logout } = useApp();

  const handleLogin = async (credentials: LoginCredentials) => {
    await login(credentials.username, credentials.password); // Convert object to parameters
  };

  if (!state.isAuthenticated) {
    return (
      <LoginForm
        onLogin={handleLogin} // Use wrapper function
        isLoading={state.loading.isLoading}
        error={state.notification.open ? state.notification.message : null}
      />
    );
  }

  return <Dashboard user={state.user!} onLogout={logout} />;
};

const App: React.FC = () => {
  return (
    <AppProvider> {/* Changed from AuthProvider */}
      <div className="App">
        <AppContent />
      </div>
    </AppProvider>
  );
};

export default App;