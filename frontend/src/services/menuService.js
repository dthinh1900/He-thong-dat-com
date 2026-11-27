
import { apiBlobRequest, apiRequest } from './apiClient';

const query = (params) => {
    const search = new URLSearchParams();

    Object.entries(params).forEach(([key, value]) => {
        if (value !== undefined && value !== null && value !== '') {
            search.set(key, String(value));
        }
    });

    const value = search.toString();
    return value ? `?${value}` : '';
};

// Lấy thông tin thực đơn của tuần
export const getMenu = (weekStart) =>
    apiRequest(`/api/menus${query({ weekStart })}`);

// Lấy nội dung ảnh thực đơn
export const getMenuImage = (imageUrl) =>
    apiBlobRequest(imageUrl);
