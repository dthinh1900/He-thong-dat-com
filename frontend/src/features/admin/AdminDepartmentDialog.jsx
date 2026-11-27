export default function AdminDepartmentDialog({ draft, setDraft, saving, onClose, onSave }) {
  return (
    <div className="admin-modal-backdrop" onClick={onClose}>
      <div className="admin-modal admin-modal--small" onClick={(event) => event.stopPropagation()}>
        <div className="admin-modal__heading"><h3>{draft.id ? 'Sửa phòng ban' : 'Thêm phòng ban'}</h3><button type="button" onClick={onClose}>×</button></div>
        <label className="admin-field">Tên phòng ban<input value={draft.name} onChange={(event) => setDraft((item) => ({ ...item, name: event.target.value }))} /></label>
        <label className="admin-field">Mô tả<input value={draft.description || ''} onChange={(event) => setDraft((item) => ({ ...item, description: event.target.value }))} /></label>
        <label className="admin-checkbox-field"><input type="checkbox" checked={Boolean(draft.active)} onChange={(event) => setDraft((item) => ({ ...item, active: event.target.checked }))} /> Phòng ban đang hoạt động</label>
        <div className="admin-modal__actions"><button className="admin-button admin-button--light" onClick={onClose} disabled={saving}>Hủy</button><button className="admin-button admin-button--primary" onClick={onSave} disabled={saving}>{saving ? 'Đang lưu...' : 'Lưu'}</button></div>
      </div>
    </div>
  );
}
