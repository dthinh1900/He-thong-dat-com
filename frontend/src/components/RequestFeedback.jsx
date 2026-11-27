export default function RequestFeedback({ request, loadingText = 'Đang tải dữ liệu...' }) {
  if (request.loading) return <p className="preview-note" role="status">{loadingText}</p>;
  if (!request.error) return null;
  return <div className="form-message form-message--error" role="alert">
    <p>{request.error}</p>
    <button type="button" className="preview-outlined" onClick={request.reload}>Thử lại</button>
  </div>;
}
