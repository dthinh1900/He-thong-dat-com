import { useEffect, useId, useRef, useState } from 'react';

export function NavigationIcon() {
  return (
    <svg width="22" height="22" viewBox="0 0 24 24" aria-hidden="true" fill="none"
      stroke="currentColor" strokeWidth="2" strokeLinecap="round">
      <path d="M4 6h16M4 12h16M4 18h16" />
    </svg>
  );
}

export default function CompactNavigation({ label, currentLabel, className, itemsClassName, children }) {
  const [open, setOpen] = useState(false);
  const root = useRef(null);
  const trigger = useRef(null);
  const itemsId = useId();

  useEffect(() => {
    if (!open) return;
    function dismissOutside(event) {
      if (!root.current?.contains(event.target)) setOpen(false);
    }
    function dismissOnEscape(event) {
      if (event.key !== 'Escape') return;
      setOpen(false);
      trigger.current?.focus();
    }
    document.addEventListener('pointerdown', dismissOutside);
    document.addEventListener('keydown', dismissOnEscape);
    return () => {
      document.removeEventListener('pointerdown', dismissOutside);
      document.removeEventListener('keydown', dismissOnEscape);
    };
  }, [open]);

  function close() {
    setOpen(false);
    if (open) trigger.current?.focus();
  }

  return (
    <div className={className ? `compact-navigation ${className}` : 'compact-navigation'} ref={root}>
      <button type="button" className="compact-navigation__trigger" ref={trigger}
        aria-label={`${label}: ${currentLabel}`} aria-expanded={open} aria-controls={itemsId}
        onClick={() => setOpen(value => !value)}>
        <NavigationIcon />
        <span>{currentLabel}</span>
      </button>
      <nav className={`compact-navigation__items ${itemsClassName}`} id={itemsId} aria-label={label}
        data-expanded={open}>
        {children(close)}
      </nav>
    </div>
  );
}
