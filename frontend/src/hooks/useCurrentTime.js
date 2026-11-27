import { useEffect, useState } from 'react';

// Cập nhật để lịch tự khóa khi qua 14:45 hoặc sang ngày mới, kể cả mở tab lâu.
export function useCurrentTime() {
  const [now, setNow] = useState(() => new Date());

  useEffect(() => {
    const refresh = () => setNow(new Date());
    const interval = window.setInterval(refresh, 15_000);
    window.addEventListener('focus', refresh);
    document.addEventListener('visibilitychange', refresh);
    return () => {
      window.clearInterval(interval);
      window.removeEventListener('focus', refresh);
      document.removeEventListener('visibilitychange', refresh);
    };
  }, []);

  return now;
}
