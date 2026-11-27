import { useEffect, useState } from 'react';
import { getMenu, getMenuImage } from '../../services/menuService';

export function MenuPopup({ onClose, weekDate }) {
  const [image, setImage] = useState('');
  const [error, setError] = useState('');
  const [loading, setLoading] = useState(true);

  useEffect(() => {
    let active = true;
    let url = '';
    (async () => {
      try {
        const menu = await getMenu(weekDate);
        if (!active || !menu?.imageUrl) return;
        const result = await getMenuImage(menu.imageUrl);
        if (!active) return;
        if (!result.blob?.type.startsWith('image/')) throw Error('Dữ liệu nhận được không phải ảnh thực đơn.');
        url = URL.createObjectURL(result.blob);
        setImage(url);
      } catch (exception) {
        if (active) setError(exception.message);
      } finally {
        if (active) setLoading(false);
      }
    })();
    return () => { active = false; if (url) URL.revokeObjectURL(url); };
  }, [weekDate]);

  useEffect(() => {
    const key = event => { if (event.key === 'Escape') onClose(); };
    document.addEventListener('keydown', key);
    return () => document.removeEventListener('keydown', key);
  }, [onClose]);

  return <div className="preview-modal-backdrop" onClick={onClose} role="presentation">
    <div className="preview-modal" role="dialog" aria-modal="true" aria-label="Thực đơn tuần" onClick={event => event.stopPropagation()}>
      <header className="preview-modal__header"><strong>Thực đơn tuần</strong><button aria-label="Đóng thực đơn" onClick={onClose}>×</button></header>
      <div className="preview-modal__body">
        {error && <p className="form-message form-message--error" role="alert">{error}</p>}
        {loading && <p role="status">Đang tải thực đơn...</p>}
        {image && <img src={image} alt="Ảnh thực đơn tuần" className="preview-menu-image" />}
        {!error && !loading && !image && <p>Tuần này chưa có ảnh thực đơn.</p>}
      </div><footer><button className="button button--blue" onClick={onClose}>Đóng</button></footer>
    </div>
  </div>;
}
