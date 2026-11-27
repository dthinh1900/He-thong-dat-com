import { apiRequest } from './apiClient';
/** selectedTypes maps YYYY-MM-DD to NORMAL or SPECIAL; Backend validates Friday-only special meals. */
export function getMonthRegistrations(month,year) {
 const query = new URLSearchParams({month:String(month),year:String(year)});
 return apiRequest(`/api/meal-registrations/month?${query.toString()}`);
}
export function saveMonthRegistrations(month,year,selectedDates,selectedTypes={}){
 const payload={month,year,selectedDates,selectedTypes};
 return apiRequest('/api/meal-registrations',{method:'POST',body:JSON.stringify(payload)});
}
