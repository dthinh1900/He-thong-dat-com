import { useEffect, useState } from 'react';
import { useAuth } from '../hooks/useAuth';
import { useToast } from '../hooks/useToast';

export default function ProfilePage() {
  const { user, updateProfile } = useAuth();
  const { notify } = useToast();
  const [fullName, setFullName] = useState(user.fullName ?? '');
  const [email, setEmail] = useState(user.email ?? '');
  const [phone, setPhone] = useState(user.phone ?? '');
  const [password, setPassword] = useState('');
  const [confirmPassword, setConfirmPassword] = useState('');
  const [submitting, setSubmitting] = useState(false);

  useEffect(() => {
    setFullName(user.fullName ?? '');
    setEmail(user.email ?? '');
    setPhone(user.phone ?? '');
  }, [user]);

  async function handleSubmit(event) {
    event.preventDefault();
    if (password && password !== confirmPassword) {
      notify('Mật khẩu nhập lại chưa trùng khớp.', 'error');
      return;
    }
    if (password && password.length < 8) {
      notify('Mật khẩu mới phải có ít nhất 8 ký tự.', 'error');
      return;
    }

    setSubmitting(true);
    try {
      await updateProfile({
        fullName: fullName.trim(),
        email: email.trim() || null,
        phone: phone.trim() || null,
        newPassword: password || null,
      });
      notify('Đã cập nhật thông tin cá nhân.', 'success');
      setPassword('');
      setConfirmPassword('');
    } catch (exception) {
      notify(exception.message || 'Cập nhật thông tin thất bại.', 'error');
    } finally {
      setSubmitting(false);
    }
  }

  return (
    <div className="profile-page">
      <section className="profile-panel" id="profile-information" aria-labelledby="profile-title">
        <h1 id="profile-title" className="panel-heading">Thông tin cá nhân</h1>
        <form className="profile-form" onSubmit={handleSubmit}>
          <div className="profile-form__field">
            <label htmlFor="employee-code">Mã nhân viên</label>
            <input id="employee-code" value={user.employeeCode ?? ''} readOnly />
          </div>
          <div className="profile-form__field">
            <label htmlFor="username">Tên đăng nhập</label>
            <input id="username" value={user.username ?? ''} readOnly />
          </div>
          <div className="profile-form__field">
            <label htmlFor="full-name">Họ và tên</label>
            <input id="full-name" value={fullName} onChange={(event) => setFullName(event.target.value)} required maxLength={160} />
          </div>
          <div className="profile-form__field">
            <label htmlFor="email">Email</label>
            <input id="email" type="email" value={email} onChange={(event) => setEmail(event.target.value)} maxLength={160} />
          </div>
          <div className="profile-form__field">
            <label htmlFor="phone">Số điện thoại</label>
            <input id="phone" type="tel" value={phone} onChange={(event) => setPhone(event.target.value)} maxLength={30} />
          </div>
          <div className="profile-form__field">
            <label htmlFor="department">Phòng ban</label>
            <input id="department" value={user.departmentName ?? ''} readOnly />
          </div>
          <div className="profile-form__field">
            <label htmlFor="role">Vai trò</label>
            <input id="role" value={user.role ?? ''} readOnly />
          </div>
          <div className="profile-form__field">
            <label htmlFor="account-status">Trạng thái tài khoản</label>
            <input id="account-status" value={user.active ? 'Đang hoạt động' : 'Ngừng hoạt động'} readOnly />
          </div>
          <div className="profile-form__field">
            <label htmlFor="new-password">Mật khẩu mới</label>
            <input id="new-password" type="password" autoComplete="new-password" placeholder="Để trống nếu không đổi" value={password} onChange={(event) => setPassword(event.target.value)} />
          </div>
          <div className="profile-form__field">
            <label htmlFor="confirm-password">Nhập lại mật khẩu</label>
            <input id="confirm-password" type="password" autoComplete="new-password" placeholder="Nhập lại mật khẩu mới" value={confirmPassword} onChange={(event) => setConfirmPassword(event.target.value)} />
          </div>
          <button type="submit" className="button button--blue" disabled={submitting}>{submitting ? 'Đang lưu...' : 'Lưu'}</button>
        </form>
      </section>
    </div>
  );
}
