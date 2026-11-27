import { Navigate, Route, Routes, useLocation } from 'react-router-dom';
import { useAuth } from '../hooks/useAuth';
import { ROUTES } from '../constants/app';
import LoginPage from '../pages/LoginPage';
import RegistrationPage from '../pages/RegistrationPage';
import ProfilePage from '../pages/ProfilePage';
import AdminPage from '../pages/AdminPage';
import PurchasePage from '../features/tickets/PurchasePage';
import ExchangePage from '../features/tickets/ExchangePage';
function PrivatePage({children}){const {user,loading}=useAuth();const location=useLocation();if(loading)return <div className="app-loading">Đang kiểm tra phiên đăng nhập...</div>;if(!user)return <Navigate to={ROUTES.LOGIN} replace state={{from:location.pathname}}/>;return children}
function AdminPageGuard({children}){const {user,loading}=useAuth();if(loading)return <div className="app-loading">Đang kiểm tra quyền quản trị...</div>;if(!user)return <Navigate to={ROUTES.LOGIN} replace/>;if(user.role!=='ADMIN')return <Navigate to={ROUTES.REGISTRATION} replace/>;return children}
export default function AppRoutes(){const {user,loading}=useAuth();if(loading)return <div className="app-loading">Đang tải...</div>;
 return <Routes><Route path="/" element={<Navigate to={user?ROUTES.REGISTRATION:ROUTES.LOGIN} replace/>}/><Route path={ROUTES.LOGIN} element={user?<Navigate to={ROUTES.REGISTRATION} replace/>:<LoginPage/>}/>
 <Route path={ROUTES.REGISTRATION} element={<PrivatePage><RegistrationPage/></PrivatePage>}/><Route path={ROUTES.PROFILE} element={<PrivatePage><ProfilePage/></PrivatePage>}/>
 <Route path={ROUTES.MENU} element={<PrivatePage><Navigate to={`${ROUTES.REGISTRATION}?menu=1`} replace/></PrivatePage>}/><Route path={ROUTES.PURCHASE} element={<PrivatePage><PurchasePage/></PrivatePage>}/><Route path={ROUTES.EXCHANGE} element={<PrivatePage><ExchangePage/></PrivatePage>}/><Route path={ROUTES.DEPARTMENT} element={<PrivatePage><Navigate to={`${ROUTES.REGISTRATION}?tab=department`} replace/></PrivatePage>}/>
 <Route path={ROUTES.ADMIN} element={<AdminPageGuard><AdminPage/></AdminPageGuard>}/><Route path="*" element={<Navigate to="/" replace/>}/></Routes>;
}
