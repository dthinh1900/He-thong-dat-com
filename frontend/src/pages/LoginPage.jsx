import { useState } from 'react';
import { useLocation, useNavigate } from 'react-router-dom';
import MealIcon from '../components/MealIcon';
import { ROUTES } from '../constants/app';
import { useAuth } from '../hooks/useAuth';
import { useToast } from '../hooks/useToast';

export default function LoginPage() {
  const { signIn } = useAuth();
  const { notify } = useToast();
  const navigate = useNavigate();
  const location = useLocation();
  const [username, setUsername] = useState('');
  const [password, setPassword] = useState('');
  const [remember, setRemember] = useState(false);
  const [submitting, setSubmitting] = useState(false);

  async function handleSubmit(event) {
    event.preventDefault();
    setSubmitting(true);
    try {
      await signIn({ username, password, remember });
      notify('Đăng nhập thành công.', 'success');
      const nextPath = location.state?.from;
      navigate(nextPath && nextPath !== ROUTES.LOGIN ? nextPath : ROUTES.REGISTRATION, { replace: true });
    } catch (exception) {
      notify(exception.message || 'Đăng nhập thất bại.', 'error');
    } finally {
      setSubmitting(false);
    }
  }

  return (
    <div className="login-page">
      <div className="login-page__identity">
        <MealIcon />
        <h1>Hệ Thống Đặt Cơm</h1>
      </div>
      <section className="login-card" aria-labelledby="login-title">
        <h2 className="panel-heading" id="login-title">Đăng nhập</h2>
        <form className="login-card__form" onSubmit={handleSubmit}>
          <div className="login-card__field">
            <label htmlFor="username">Tên đăng nhập</label>
            <input id="username" name="username" type="text" autoComplete="username" autoFocus value={username} onChange={(event) => setUsername(event.target.value)} required />
          </div>
          <div className="login-card__field">
            <label htmlFor="password">Mật khẩu</label>
            <input id="password" name="password" type="password" autoComplete="current-password" value={password} onChange={(event) => setPassword(event.target.value)} required />
          </div>
          <label className="login-card__remember">
            <input type="checkbox" checked={remember} onChange={(event) => setRemember(event.target.checked)} />
            <span>Ghi nhớ đăng nhập</span>
          </label>
          <div className="login-card__submit">
            <button type="submit" className="button button--blue" disabled={submitting}>{submitting ? 'Đang đăng nhập...' : 'Đăng nhập'}</button>
          </div>
        </form>
      </section>
    </div>
  );
}
