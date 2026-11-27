import { apiBlobRequest, apiRequest } from './apiClient';

const ADMIN = '/api/admin';
const query = (params) => {
  const search = new URLSearchParams();
  Object.entries(params).forEach(([key, value]) => {
    if (value !== undefined && value !== null && value !== '') search.set(key, String(value));
  });
  const value = search.toString();
  return value ? `?${value}` : '';
};

export const getAdminOverview = () => apiRequest(`${ADMIN}/overview`);

export const getDepartments = () => apiRequest(`${ADMIN}/departments`);
export const createDepartment = (data) => apiRequest(`${ADMIN}/departments`, {
  method: 'POST', body: JSON.stringify(data),
});
export const updateDepartment = (id, data) => apiRequest(`${ADMIN}/departments/${id}`, {
  method: 'PUT', body: JSON.stringify(data),
});

export const getEmployees = (params = {}) => apiRequest(`${ADMIN}/employees${query(params)}`);
export const getEmployee = (id) => apiRequest(`${ADMIN}/employees/${id}`);
export const createEmployee = (data) => apiRequest(`${ADMIN}/employees`, {
  method: 'POST', body: JSON.stringify(data),
});
export const updateEmployee = (id, data) => apiRequest(`${ADMIN}/employees/${id}`, {
  method: 'PUT', body: JSON.stringify(data),
});
export const setEmployeeActive = (id, active) => apiRequest(`${ADMIN}/employees/${id}/active`, {
  method: 'PATCH', body: JSON.stringify({ active }),
});
export const retireEmployee = (id) => apiRequest(`${ADMIN}/employees/${id}`, { method: 'DELETE' });
export const importEmployees = (rows) => apiRequest(`${ADMIN}/employees/import`, {
  method: 'POST', body: JSON.stringify({ rows }),
});

export const getRegistrations = (date, departmentId) => apiRequest(
  `${ADMIN}/registrations${query({ date, departmentId })}`,
);
export const setRegistration = (userId, date, registered, cancellationReason, withdrawal) => apiRequest(
  `${ADMIN}/registrations/${userId}/${date}`,
  { method: 'PUT', body: JSON.stringify({ registered, cancellationReason, withdrawal }) },
);

export const getRegistrationSettings = () => apiRequest(`${ADMIN}/registration-settings`);
export const updateRegistrationSettings = (data) => apiRequest(`${ADMIN}/registration-settings`, {
  method: 'PUT', body: JSON.stringify(data),
});

export const getMealPrices = () => apiRequest(`${ADMIN}/meal-prices`);
export const createMealPrice = (data) => apiRequest(`${ADMIN}/meal-prices`, {
  method: 'POST', body: JSON.stringify(data),
});

export const getAdminMenu = (weekStart) => apiRequest(`${ADMIN}/menus${query({ weekStart })}`);
export const uploadAdminMenu = (file, weekStart) => {
  const form = new FormData();
  form.append('file', file);
  if (weekStart) form.append('weekStart', weekStart);
  return apiRequest(`${ADMIN}/menus`, { method: 'PUT', body: form });
};
export const getMenuImage = (imageUrl) => apiBlobRequest(imageUrl);

export const getMonthlyReport = (month, year) => apiRequest(
  `${ADMIN}/reports/month${query({ month, year })}`,
);
export const getReportDetail = (from, to) => apiRequest(
  `${ADMIN}/reports/detail${query({ from, to })}`,
);
export const exportReportRange = (from, to) => apiBlobRequest(
  `${ADMIN}/reports/range/export${query({ from, to })}`,
);
