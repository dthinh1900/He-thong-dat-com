import { useToast } from '../hooks/useToast';

const ICONS = { success: '✓', error: '!', info: 'i' };
const TITLES = { success: 'Thành công', error: 'Không thể thực hiện', info: 'Thông báo' };

export default function ToastViewport() {
  const { toast, dismiss } = useToast();
  if (!toast) return null;

  return (
    <div className="toast-region" aria-live={toast.type === 'error' ? 'assertive' : 'polite'}>
      <div key={toast.id} className={`app-toast app-toast--${toast.type}`} role={toast.type === 'error' ? 'alert' : 'status'}>
        <span className="app-toast__icon" aria-hidden="true">{ICONS[toast.type] ?? ICONS.info}</span>
        <div className="app-toast__content">
          <strong>{TITLES[toast.type] ?? TITLES.info}</strong>
          <span>{toast.message}</span>
        </div>
        <button type="button" className="app-toast__close" onClick={dismiss} aria-label="Đóng thông báo">×</button>
      </div>
    </div>
  );
}
