import { useEffect, useId, useRef } from 'react';

export default function ModalDialog({ title, onClose, busy = false, children }) {
  const dialog = useRef(null);
  const heading = useId();
  useEffect(() => {
    if (dialog.current && !dialog.current.open) dialog.current.showModal();
  }, []);

  function close() {
    dialog.current?.close();
    onClose();
  }

  return (
    <dialog ref={dialog} className="app-dialog" aria-labelledby={heading}
      onCancel={event => { event.preventDefault(); if (!busy) close(); }}>
      <header className="app-dialog__header">
        <h2 id={heading}>{title}</h2>
        <button type="button" aria-label="Đóng" onClick={close} disabled={busy}>×</button>
      </header>
      <div className="app-dialog__body">{children(close)}</div>
    </dialog>
  );
}
