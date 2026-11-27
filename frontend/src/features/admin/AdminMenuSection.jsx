import { useCallback, useEffect, useRef, useState } from 'react';
import { useToast } from '../../hooks/useToast';
import { getAdminMenu, getMenuImage, uploadAdminMenu } from '../../services/adminService';
import { dateLabel } from './adminFormatting';
import AdminMealPrices from './AdminMealPrices';

const isMissingMenu = (error) => /chưa có thực đơn tuần này|không tìm thấy ảnh thực đơn/i.test(error?.message ?? '');

export default function AdminMenuSection({ active, loading, setLoading, saving, setSaving }) {
  const { notify } = useToast();
  const [menu, setMenu] = useState(null);
  const [menuImage, setMenuImage] = useState('');
  const [menuZoom, setMenuZoom] = useState(false);
  const [menuFile, setMenuFile] = useState(null);
  const uploadRef = useRef(null);
  const menuObjectUrlRef = useRef(null);

  const replaceMenuImage = useCallback((blob) => {
    if (menuObjectUrlRef.current) URL.revokeObjectURL(menuObjectUrlRef.current);
    const nextUrl = blob ? URL.createObjectURL(blob) : '';
    menuObjectUrlRef.current = nextUrl || null;
    setMenuImage(nextUrl);
  }, []);

  useEffect(() => () => {
    if (menuObjectUrlRef.current) URL.revokeObjectURL(menuObjectUrlRef.current);
  }, []);
  const loadMenu = useCallback(async () => {
    setLoading(true);
    setMenuFile(null);
    try {
      const menuRow = await getAdminMenu();
      setMenu(menuRow);
      const imageResult = await getMenuImage(menuRow.imageUrl);
      replaceMenuImage(imageResult.blob);
    } catch (error) {
      setMenu(null);
      replaceMenuImage(null);
      if (!isMissingMenu(error)) notify(error.message || 'Không tải được ảnh thực đơn.', 'error');
    } finally {
      setLoading(false);
    }
  }, [notify, replaceMenuImage, setLoading]);

  useEffect(() => {
    if (active) loadMenu();
  }, [active, loadMenu]);

  function handleMenuFile(file) {
    if (!file) return;
    const allowed = ['image/png', 'image/jpeg'];
    if (!allowed.includes(file.type)) {
      notify('Chỉ chấp nhận ảnh PNG hoặc JPEG.', 'error');
      return;
    }
    if (file.size > 5 * 1024 * 1024) {
      notify('Ảnh thực đơn không được vượt quá 5 MB.', 'error');
      return;
    }
    setMenuFile(file);
    replaceMenuImage(file);
  }

  async function saveMenu() {
    if (!menuFile) return;
    setSaving(true);
    try {
      const savedMenu = await uploadAdminMenu(menuFile);
      setMenu(savedMenu);
      setMenuFile(null);
      if (uploadRef.current) uploadRef.current.value = '';
      notify('Đã lưu ảnh thực đơn.', 'success');
    } catch (error) {
      notify(error.message || 'Không tải được ảnh thực đơn lên máy chủ.', 'error');
    } finally {
      setSaving(false);
    }
  }

  return (
    <>
      {active && <>
    <div className="admin-section-heading"><div><h2>Ảnh thực đơn tuần</h2><p>Tải lên một ảnh PNG/JPEG tối đa 5 MB chứa bảng thực đơn các ngày trong tuần.</p></div></div>
    <div className="admin-menu-card">
      <input ref={uploadRef} type="file" accept="image/png,image/jpeg" hidden onChange={(event) => handleMenuFile(event.target.files?.[0])} />
      {menuImage ? <button className="preview-image-zoom" type="button" onClick={() => setMenuZoom(true)} aria-label="Phóng to ảnh thực đơn"><img className="admin-menu-preview" src={menuImage} alt="Ảnh thực đơn tuần" /></button> : <div className="admin-menu-placeholder"><span>▧</span><b>{loading ? 'Đang tải thực đơn...' : 'Chưa có ảnh thực đơn'}</b><small>Chọn ảnh thực đơn tuần để xem trước tại đây</small></div>}
      {menu && <p className="admin-menu-meta">Tuần {dateLabel(menu.weekStartDate)} – {dateLabel(menu.weekEndDate)} · Cập nhật {menu.uploadedAt ? new Date(menu.uploadedAt).toLocaleString('vi-VN') : '—'}</p>}
      <div className="admin-menu-actions">
        <button className="admin-button admin-button--light" onClick={() => uploadRef.current?.click()}>⇧ Tải ảnh lên</button>
        <button className="admin-button admin-button--primary" disabled={!menuFile || saving} onClick={saveMenu}>{saving ? 'Đang lưu...' : 'Lưu ảnh'}</button>
      </div>
    </div>
    {menuZoom && menuImage && <div className="preview-modal-backdrop" onClick={() => setMenuZoom(false)} role="presentation"><div className="preview-modal" role="dialog" aria-modal="true" aria-label="Ảnh thực đơn phóng to" onClick={event => event.stopPropagation()}><header className="preview-modal__header"><strong>Ảnh thực đơn</strong><button type="button" aria-label="Đóng" onClick={() => setMenuZoom(false)}>×</button></header><div className="preview-modal__body"><img className="preview-menu-image" src={menuImage} alt="Thực đơn tuần" /></div></div></div>}
      </>}
      <AdminMealPrices active={active} loading={loading} />
    </>
  );
}
