import { useEffect, useMemo, useRef, useState } from 'react';
import { getMonthRegistrations, saveMonthRegistrations } from '../services/mealService';
import { parseMonthKey } from '../utils/date';
import { dateShift, mealTimeLock, todayKey } from '../utils/mealDates';

const signature = (days,types) => [...days].sort().map(d=>`${d}:${types[d]||'NORMAL'}`).join('|');
const toDayMap = response => Object.fromEntries((response?.days??[]).map(day=>[day.mealDate,day]));
function getTypes(response){return Object.fromEntries((response?.days??[]).filter(d=>d.selected).map(d=>[d.mealDate,d.ticketType||'NORMAL']))}
function currentDates(response){return (response?.days??[]).filter(d=>d.selected).map(d=>d.mealDate)}
const fare = (day,type) => {
 if(day?.selected && (day.ticketType||'NORMAL')===type) return Number(day.price??0);
 return Number(type==='SPECIAL' ? (day?.specialPrice??0) : (day?.normalPrice??0));
};

export function useRegistration(monthKey, now){
 const [selectedDays,setSelectedDays]=useState([]);
 const [selectedTypes,setSelectedTypes]=useState({});
 const [savedDayMap,setDayMap]=useState({});
 const [savedSignature,setSavedSignature]=useState('');
 const [extraAmount,setExtraAmount]=useState(0);
 const [extraTickets,setExtraTickets]=useState(0);
 const [deadlines,setDeadlines]=useState(null);
 const [loading,setLoading]=useState(true),[saving,setSaving]=useState(false),[loadError,setLoadError]=useState('');
 const currentMonth=useRef(monthKey);
 currentMonth.current=monthKey;
 function accept(result){const days=currentDates(result),types=getTypes(result),map=toDayMap(result);
  setSelectedDays(days);setSelectedTypes(types);setDayMap(map);setSavedSignature(signature(days,types));setDeadlines({ cutoffTime: result.cutoffTime, cancelDeadline: result.cancelDeadline });
  // Server owns the allocation; the difference is unchanged by calendar edits.
  const primaryAmount=days.reduce((a,d)=>a+Number(map[d]?.price??0),0);
  setExtraAmount(Math.max(0,Number(result.totalAmount??primaryAmount)-primaryAmount));
  setExtraTickets(Math.max(0,Number(result.totalTickets??result.selectedCount??days.length)-days.length));
 }
 useEffect(()=>{let active=true;const {year,monthIndex}=parseMonthKey(monthKey);setLoading(true);setLoadError('');setDeadlines(null);
  setSelectedDays([]);setSelectedTypes({});setDayMap({});setSavedSignature('');setExtraAmount(0);setExtraTickets(0);
  getMonthRegistrations(monthIndex+1,year).then(res=>{if(active)accept(res)}).catch(err=>{if(active)setLoadError(err.message||'Không thể tải lịch đăng ký.')}).finally(()=>{if(active)setLoading(false)});
  return()=>{active=false};
 },[monthKey]);
 const dayMap = useMemo(() => {
  const today = todayKey(now);
  const tomorrow = dateShift(today, 1);
  return Object.fromEntries(Object.entries(savedDayMap).map(([date, day]) => {
   const typeLock = mealTimeLock(date, deadlines?.cutoffTime, now, today, tomorrow);
   const lock = day.selected ? mealTimeLock(date, deadlines?.cancelDeadline, now, today, tomorrow) : typeLock;
   return [date, { ...day, editable: day.editable && !lock,
    typeEditable: (day.typeEditable ?? day.editable) && !typeLock,
    lockReason: day.lockReason || (lock && day.selected ? lock + ' Liên hệ quản trị viên để hủy vé.' : lock) }];
  }));
 }, [savedDayMap, deadlines, now]);
 const selectedSet=useMemo(()=>new Set(selectedDays),[selectedDays]);
 const editableDays=useMemo(()=>Object.values(dayMap).filter(d=>d.editable).map(d=>d.mealDate),[dayMap]);
 const allWorkdaysSelected=editableDays.length>0&&editableDays.every(d=>selectedSet.has(d));
 const totalAmount=useMemo(()=>selectedDays.reduce((a,d)=>a+fare(dayMap[d],selectedTypes[d]||dayMap[d]?.ticketType||'NORMAL'),0)+extraAmount,[selectedDays,dayMap,selectedTypes,extraAmount]);
 const priceLabel='Theo đơn giá từng ngày / loại suất';
 function toggleDate(date){const info=dayMap[date];if(!info)return {ok:false,message:'Dữ liệu ngày chưa được tải.'};if(!info.editable)return {ok:false,message:info.lockReason||'Ngày này đã khóa.'};
  const was=selectedSet.has(date);setSelectedDays(previous=>was?previous.filter(d=>d!==date):[...previous,date].sort());
  if(!was)setSelectedTypes(previous=>({...previous,[date]:'NORMAL'}));
  return {ok:true,message:was?'Đã bỏ chọn trong bản nháp. Nhấn Lưu đăng ký để xác nhận.':'Đã chọn trong bản nháp. Nhấn Lưu đăng ký để xác nhận.'};
 }
 function chooseType(date,type){const info=dayMap[date];if(!(type==='NONE'?info?.editable:(info?.typeEditable??info?.editable)))return {ok:false,message:info?.lockReason||'Ngày đã khóa.'};
  const weekday=new Date(`${date}T12:00:00Z`).getUTCDay();
  if(type==='SPECIAL'&&weekday!==5)return {ok:false,message:'Suất đặc biệt chỉ có vào thứ Sáu.'};
  if(!['NORMAL','SPECIAL','NONE'].includes(type))return {ok:false,message:'Loại suất không hợp lệ.'};
  if(type==='SPECIAL'&&info.specialPrice==null)return {ok:false,message:'Chưa có giá áp dụng cho suất đặc biệt.'};
  if(type==='NORMAL'&&info.normalPrice==null)return {ok:false,message:'Chưa có giá áp dụng cho suất thường.'};
  if(type==='NONE'){setSelectedDays(prev=>prev.filter(d=>d!==date))}
  else {setSelectedDays(prev=>[...new Set([...prev,date])].sort());setSelectedTypes(prev=>({...prev,[date]:type}))}
  return {ok:true,message:'Đã cập nhật lựa chọn. Nhấn Lưu đăng ký để xác nhận.'};
 }
 function toggleAllWorkdays(){if(!editableDays.length)return {ok:false,message:'Tháng này không còn ngày được phép chọn hoặc bỏ chọn.'};
  const all=editableDays.every(d=>selectedSet.has(d));const editableSet=new Set(editableDays);
  setSelectedDays(previous=>all?previous.filter(d=>!editableSet.has(d)):[...new Set([...previous,...editableDays])].sort());
  if(!all)setSelectedTypes(previous=>({...previous,...Object.fromEntries(editableDays.map(d=>[d,'NORMAL']))}));
  return {ok:true,message:all?'Đã bỏ chọn các ngày hợp lệ. Nhấn Lưu đăng ký.':'Đã chọn các ngày hợp lệ. Nhấn Lưu đăng ký.'};
 }
 function discardChanges() {
  const savedDays = { days: Object.values(savedDayMap) };
  setSelectedDays(currentDates(savedDays));
  setSelectedTypes(getTypes(savedDays));
 }
 async function save(){const {year,monthIndex}=parseMonthKey(monthKey);
  if(signature(selectedDays,selectedTypes)===savedSignature)return {ok:false,type:'info',message:'Không có thay đổi nào cần lưu.'};
  setSaving(true);try{const result=await saveMonthRegistrations(monthIndex+1,year,selectedDays,selectedTypes);if(currentMonth.current===monthKey)accept(result);return {ok:true,message:'Đã lưu đăng ký suất ăn.'}}catch(e){return {ok:false,message:e.message||'Lưu thất bại.'}}finally{setSaving(false)}
 }
 return {selectedDays,selectedTypes,selectedSet,dayMap,toggleDate,chooseType,toggleAllWorkdays,allWorkdaysSelected,editableCount:editableDays.length,
  discardChanges,totalAmount,totalTickets:selectedDays.length+extraTickets,priceLabel,cutoffTime: deadlines?.cutoffTime,cancelDeadline: deadlines?.cancelDeadline,hasChanges:signature(selectedDays,selectedTypes)!==savedSignature,loading,saving,loadError,save};
}
