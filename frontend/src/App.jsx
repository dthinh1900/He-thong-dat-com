import { BrowserRouter } from 'react-router-dom';
import { AuthProvider } from './contexts/AuthContext';
import AppHeader from './components/AppHeader';
import AppRoutes from './routes/AppRoutes';
import ToastViewport from './components/ToastViewport';
import { ToastProvider } from './contexts/ToastContext';

export default function App() {
  return (
    <BrowserRouter>
      <ToastProvider>
        <AuthProvider>
          <AppHeader />
          <main className="app-content">
            <AppRoutes />
          </main>
          <ToastViewport />
        </AuthProvider>
      </ToastProvider>
    </BrowserRouter>
  );
}
