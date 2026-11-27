import { useCallback, useEffect, useMemo, useState } from 'react';
import { useToast } from '../../hooks/useToast';
import { createDepartment, createEmployee, getDepartments, getEmployee, getEmployees,
  retireEmployee, setEmployeeActive, updateDepartment, updateEmployee } from '../../services/adminService';

export function useAdminPeople({ active, navigationRevision, departments, setDepartments, setLoading, setSaving }) {
  const { notify } = useToast();
  const [employees, setEmployees] = useState([]);
  const [selectedEmployee, setSelectedEmployee] = useState(null);
  const [employeeDraft, setEmployeeDraft] = useState(null);
  const [departmentDraft, setDepartmentDraft] = useState(null);
  const [employeeSearch, setEmployeeSearch] = useState('');
  const [appliedEmployeeSearch, setAppliedEmployeeSearch] = useState('');
  const [employeeDepartmentFilter, setEmployeeDepartmentFilter] = useState('all');

  const activeDepartments = useMemo(() => departments.filter((item) => item.active), [departments]);
  const filteredEmployees = useMemo(() => {
    const keyword = appliedEmployeeSearch.trim().toLocaleLowerCase('vi');
    return employees.filter((employee) => !keyword || [employee.name, employee.code, employee.username]
      .some((value) => String(value ?? '').toLocaleLowerCase('vi').includes(keyword)));
  }, [employees, appliedEmployeeSearch]);

  const loadPeople = useCallback(async () => {
    setLoading(true);
    try {
      const [departmentRows, employeeRows] = await Promise.all([
        getDepartments(),
        getEmployees(),
      ]);
      setDepartments(departmentRows);
      setEmployees(employeeRows);
    } catch (error) {
      notify(error.message || 'Không tải được danh sách nhân viên và phòng ban.', 'error');
    } finally {
      setLoading(false);
    }
  }, [notify, setDepartments, setLoading]);

  useEffect(() => {
    if (active) loadPeople();
  }, [active, loadPeople]);

  useEffect(() => {
    setEmployeeDraft(null);
    setSelectedEmployee(null);
  }, [navigationRevision]);

  function startAddEmployee() {
    const firstDepartment = activeDepartments[0];
    if (!firstDepartment) {
      notify('Bạn cần tạo hoặc kích hoạt ít nhất một phòng ban trước.', 'error');
      return;
    }
    const draft = {
      id: null,
      code: '',
      name: '',
      username: '',
      email: '',
      phone: '',
      departmentId: firstDepartment.id,
      role: 'USER',
      active: true,
      password: '',
    };
    setSelectedEmployee(draft);
    setEmployeeDraft(draft);
  }

  function cancelEmployeeEdit() {
    if (selectedEmployee?.id) setEmployeeDraft(null);
    else {
      setEmployeeDraft(null);
      setSelectedEmployee(null);
    }
  }

  async function saveEmployee() {
    if (!employeeDraft?.name?.trim() || !employeeDraft?.code?.trim() || !employeeDraft?.username?.trim()) {
      notify('Vui lòng nhập mã nhân viên, họ tên và tên đăng nhập.', 'error');
      return;
    }
    if (!employeeDraft.departmentId) {
      notify('Vui lòng chọn phòng ban.', 'error');
      return;
    }
    if (!employeeDraft.id && (employeeDraft.password ?? '').length < 8) {
      notify('Mật khẩu ban đầu phải có ít nhất 8 ký tự.', 'error');
      return;
    }
    if (employeeDraft.password && employeeDraft.password.length < 8) {
      notify('Mật khẩu mới phải có ít nhất 8 ký tự.', 'error');
      return;
    }

    const payload = {
      code: employeeDraft.code.trim(),
      name: employeeDraft.name.trim(),
      username: employeeDraft.username.trim(),
      email: employeeDraft.email?.trim() || null,
      phone: employeeDraft.phone?.trim() || null,
      departmentId: Number(employeeDraft.departmentId),
      role: employeeDraft.role,
      active: Boolean(employeeDraft.active),
    };
    if (employeeDraft.password) payload.password = employeeDraft.password;

    setSaving(true);
    try {
      const saved = employeeDraft.id
        ? await updateEmployee(employeeDraft.id, payload)
        : await createEmployee(payload);
      setEmployees((items) => {
        const exists = items.some((item) => item.id === saved.id);
        return exists ? items.map((item) => item.id === saved.id ? saved : item) : [...items, saved];
      });
      setSelectedEmployee(saved);
      setEmployeeDraft(null);
      notify(employeeDraft.id ? 'Đã cập nhật nhân viên.' : 'Đã tạo tài khoản nhân viên.', 'success');
      // Refresh department counts after a new employee is added or moved.
      try {
        const departmentRows = await getDepartments();
        setDepartments(departmentRows);
      } catch (refreshError) {
        notify(`Đã lưu nhân viên nhưng chưa làm mới được số lượng phòng ban: ${refreshError.message}`, 'error');
      }
    } catch (error) {
      notify(error.message || 'Không lưu được thông tin nhân viên.', 'error');
    } finally {
      setSaving(false);
    }
  }

  async function changeEmployeeActive() {
    if (!selectedEmployee) return;
    const nextActive = !selectedEmployee.active;
    const message = nextActive
      ? `Mở khóa tài khoản ${selectedEmployee.name}?`
      : `Khóa tài khoản ${selectedEmployee.name}?`;
    if (!window.confirm(message)) return;
    setSaving(true);
    try {
      const updated = await setEmployeeActive(selectedEmployee.id, nextActive);
      setSelectedEmployee(updated);
      setEmployees((items) => items.map((item) => item.id === updated.id ? updated : item));
      notify(nextActive ? 'Đã mở khóa tài khoản.' : 'Đã khóa tài khoản.', 'success');
    } catch (error) {
      notify(error.message || 'Không thay đổi được trạng thái tài khoản.', 'error');
    } finally {
      setSaving(false);
    }
  }

  async function retireSelectedEmployee() {
    if (!selectedEmployee) return;
    if (!window.confirm(`Ngừng hoạt động tài khoản ${selectedEmployee.name}? Lịch sử đăng ký và vé ăn vẫn được giữ.`)) return;
    setSaving(true);
    try {
      await retireEmployee(selectedEmployee.id);
      const updated = { ...selectedEmployee, active: false };
      setSelectedEmployee(updated);
      setEmployees((items) => items.map((item) => item.id === updated.id ? updated : item));
      notify('Đã ngừng hoạt động tài khoản và giữ nguyên lịch sử.', 'success');
    } catch (error) {
      notify(error.message || 'Không ngừng hoạt động được tài khoản.', 'error');
    } finally {
      setSaving(false);
    }
  }

  async function saveDepartment() {
    if (!departmentDraft?.name?.trim()) {
      notify('Vui lòng nhập tên phòng ban.', 'error');
      return;
    }
    setSaving(true);
    try {
      const payload = {
        name: departmentDraft.name.trim(),
        description: departmentDraft.description?.trim() || null,
        active: departmentDraft.active ?? true,
      };
      const saved = departmentDraft.id
        ? await updateDepartment(departmentDraft.id, payload)
        : await createDepartment(payload);
      setDepartments((items) => {
        const exists = items.some((item) => item.id === saved.id);
        return exists ? items.map((item) => item.id === saved.id ? saved : item) : [...items, saved];
      });
      setDepartmentDraft(null);
      notify(departmentDraft.id ? 'Đã cập nhật phòng ban.' : 'Đã tạo phòng ban.', 'success');
    } catch (error) {
      notify(error.message || 'Không lưu được phòng ban.', 'error');
    } finally {
      setSaving(false);
    }
  }

  async function openEmployee(employee) {
    setSaving(true);
    try {
      const full = await getEmployee(employee.id);
      setSelectedEmployee(full);
      setEmployeeDraft(null);
    } catch (error) {
      notify(error.message || 'Không tải được thông tin nhân viên.', 'error');
    } finally {
      setSaving(false);
    }
  }

  return {
    employees, selectedEmployee, setSelectedEmployee, employeeDraft, setEmployeeDraft, departmentDraft, setDepartmentDraft,
    employeeSearch, setEmployeeSearch, appliedEmployeeSearch, setAppliedEmployeeSearch, employeeDepartmentFilter, setEmployeeDepartmentFilter, filteredEmployees,
    loadPeople, startAddEmployee, cancelEmployeeEdit, saveEmployee, changeEmployeeActive, retireSelectedEmployee, saveDepartment, openEmployee,
  };
}
