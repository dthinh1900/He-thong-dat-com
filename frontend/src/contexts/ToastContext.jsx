import { createContext, useCallback, useEffect, useMemo, useRef, useState } from 'react';

export const ToastContext = createContext(null);

export function ToastProvider({ children }) {
  const [toast, setToast] = useState(null);
  const timeoutRef = useRef(null);
  const idRef = useRef(0);

  const dismiss = useCallback(() => {
    window.clearTimeout(timeoutRef.current);
    setToast(null);
  }, []);

  const notify = useCallback((message, type = 'success') => {
    window.clearTimeout(timeoutRef.current);
    const id = ++idRef.current;
    setToast({ id, message, type });
    timeoutRef.current = window.setTimeout(() => setToast(null), 4000);
  }, []);

  useEffect(() => () => window.clearTimeout(timeoutRef.current), []);

  const value = useMemo(() => ({ toast, notify, dismiss }), [toast, notify, dismiss]);
  return <ToastContext.Provider value={value}>{children}</ToastContext.Provider>;
}
