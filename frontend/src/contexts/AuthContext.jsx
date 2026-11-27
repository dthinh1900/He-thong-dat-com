import { createContext, useCallback, useEffect, useMemo, useState } from 'react';
import { getStoredSession, SESSION_EXPIRED } from '../services/apiClient';
import { getCurrentUser, signIn as signInRequest, signOut as signOutRequest, updateCurrentUser } from '../services/authService';

export const AuthContext = createContext(null);

export function AuthProvider({ children }) {
  const [user, setUser] = useState(null);
  const [loading, setLoading] = useState(true);

  useEffect(() => {
    const expire = () => setUser(null);
    window.addEventListener(SESSION_EXPIRED, expire);
    return () => window.removeEventListener(SESSION_EXPIRED, expire);
  }, []);

  useEffect(() => {
    let active = true;
    if (!getStoredSession()?.token) {
      setLoading(false);
      return undefined;
    }
    getCurrentUser()
      .then((profile) => { if (active) setUser(profile); })
      .catch(() => { if (active) setUser(null); })
      .finally(() => { if (active) setLoading(false); });
    return () => { active = false; };
  }, []);

  const signIn = useCallback(async (credentials) => {
    const nextUser = await signInRequest(credentials);
    setUser(nextUser);
    return nextUser;
  }, []);

  const signOut = useCallback(() => {
    signOutRequest();
    setUser(null);
  }, []);

  const updateProfile = useCallback(async (profile) => {
    const updated = await updateCurrentUser(profile);
    setUser(updated);
    return updated;
  }, []);

  const value = useMemo(
    () => ({ user, loading, signIn, signOut, updateProfile }),
    [user, loading, signIn, signOut, updateProfile],
  );

  return <AuthContext.Provider value={value}>{children}</AuthContext.Provider>;
}
