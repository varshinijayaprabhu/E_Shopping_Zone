import React, { useEffect, useState } from 'react';
import { useNavigate, useSearchParams } from 'react-router-dom';
import { useAuth } from '../context/AuthContext';
import { useNotification } from '../context/NotificationContext';
import { setAccessToken, axiosClient } from '../api/axiosClient';
import { LoadingSpinner } from '../components/common/LoadingSpinner';

export const OAuthRedirectHandler: React.FC = () => {
  const [searchParams] = useSearchParams();
  const navigate = useNavigate();
  const { setAuthSession, refreshUser } = useAuth();
  const { showSuccess, showError } = useNotification();
  const [statusMessage, setStatusMessage] = useState('Finalizing authentication...');

  useEffect(() => {
    const handleRedirect = async () => {
      const code = searchParams.get('code');
      const token = searchParams.get('token');
      const error = searchParams.get('error');

      if (error) {
        showError(`OAuth login failed: ${error}`);
        navigate('/login', { replace: true });
        return;
      }

      if (code) {
        try {
          setStatusMessage('Exchanging secure authorization code...');
          const exchangeRes = await axiosClient.post<any>('/api/auth/oauth2/exchange', { code });
          const authData = exchangeRes.data?.data || exchangeRes.data;
          if (authData?.accessToken && authData?.user) {
            setAuthSession(authData.user, authData.accessToken);
            window.history.replaceState({}, document.title, window.location.pathname);
            showSuccess('Successfully signed in with GitHub!');
            navigate('/', { replace: true });
            return;
          }
        } catch {
          showError('Failed to exchange authorization code.');
          navigate('/login', { replace: true });
          return;
        }
      } else if (token) {
        // Fallback for direct token parameter
        setAccessToken(token);
        await refreshUser();
        showSuccess('Successfully signed in with GitHub!');
        navigate('/', { replace: true });
        return;
      }

      // Sanitize URL history so credentials never remain in address bar
      window.history.replaceState({}, document.title, window.location.pathname);

      try {
        setStatusMessage('Syncing user profile...');
        await refreshUser();
        showSuccess('Successfully signed in with GitHub!');
        navigate('/', { replace: true });
      } catch (err: unknown) {
        const errorResponse = err as { message?: string };
        showError(errorResponse.message || 'Authentication session verification failed.');
        navigate('/login', { replace: true });
      }
    };

    handleRedirect();
  }, [searchParams, navigate, setAuthSession, refreshUser, showSuccess, showError]);

  return (
    <div className="min-h-[60vh] flex items-center justify-center">
      <LoadingSpinner size="lg" message={statusMessage} />
    </div>
  );
};

