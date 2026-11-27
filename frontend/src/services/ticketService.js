import { apiRequest } from './apiClient';

const PURCHASES = '/api/ticket-purchases';
const EXCHANGES = '/api/ticket-exchanges';
const ADMIN = '/api/admin';
const json = (method, data) => ({ method, body: JSON.stringify(data) });

export const listPurchases = () => apiRequest(PURCHASES);
export const getPurchaseQuote = (mealDate, ticketType) =>
  apiRequest(`${PURCHASES}/quote?${new URLSearchParams({ mealDate, ticketType })}`, { cache: 'no-store' });
export const requestPurchase = (input) => apiRequest(PURCHASES, json('POST', input));
export const getExchangeBoard = () => apiRequest(EXCHANGES);
export const createSellOffer = (input) => apiRequest(EXCHANGES, json('POST', { ...input, exchangeType: 'SELL' }));
export const matchOffer = (id, quantity) => apiRequest(`${EXCHANGES}/${encodeURIComponent(id)}/match`, json('POST', { quantity }));
export const cancelOffer = (id) => apiRequest(`${EXCHANGES}/${encodeURIComponent(id)}`, { method: 'DELETE' });
export const listDepartmentOptions = () => apiRequest('/api/departments');

export function getDepartmentRoster(date, departmentId) {
  const query = new URLSearchParams({ date });
  if (departmentId !== 'all') query.set('departmentId', departmentId);
  return apiRequest(`/api/departments/roster?${query}`);
}

export const listAdminPurchases = () => apiRequest(`${ADMIN}/purchases`);
export const decidePurchase = (id, decision) => apiRequest(`${ADMIN}/purchases/${encodeURIComponent(id)}`, json('PATCH', { decision }));
export const listTransfers = () => apiRequest(`${ADMIN}/exchange-transactions`);
export const listExchangeAudit = () => apiRequest(`${ADMIN}/exchange-audit`);
export function getEmployeeTicketHistory(id, month, year) {
  return apiRequest(`${ADMIN}/employees/${id}/history?${new URLSearchParams({ month, year })}`);
}
export const cancelTicketAsAdmin = (id, reason, withdrawal) => apiRequest(`${ADMIN}/meal-tickets/${encodeURIComponent(id)}/cancel`, { method: 'PATCH', body: JSON.stringify({ reason, withdrawal }) });
