import { useEffect, useRef, useState } from 'react';
import { Link, NavLink, useNavigate } from 'react-router-dom';
import brandLogo from '../assets/vietnam-post-logo.png';
import { useAuth } from '../hooks/useAuth';
import { ROUTES } from '../constants/app';
import { useToast } from '../hooks/useToast';
import { NavigationIcon } from './CompactNavigation';
export default function AppHeader(){
 const {user,signOut}=useAuth(),[isOpen,setIsOpen]=useState(false),menuRef=useRef(null),navigate=useNavigate(),{notify}=useToast();
 useEffect(()=>{function outside(event){if(menuRef.current&&!menuRef.current.contains(event.target))setIsOpen(false)}function esc(event){if(event.key==='Escape')setIsOpen(false)}document.addEventListener('pointerdown',outside);document.addEventListener('keydown',esc);return()=>{document.removeEventListener('pointerdown',outside);document.removeEventListener('keydown',esc)}},[]);
 function logout(){setIsOpen(false);signOut();navigate(ROUTES.LOGIN,{replace:true});notify('Đã đăng xuất thành công.','success')}
 const link=(to,label)=><NavLink key={to} to={to} onClick={()=>setIsOpen(false)}>{label}</NavLink>;
 return <header className="site-header"><div className="site-header__inner"><Link to={user?ROUTES.REGISTRATION:ROUTES.LOGIN} className="site-header__brand" aria-label="Hệ thống đặt cơm - Trang chủ"><img src={brandLogo} alt="Tổng công ty Bưu điện Việt Nam"/></Link>
 {user?<div className="account-menu" ref={menuRef}><button className="account-menu__trigger" type="button" aria-label={`Menu tài khoản của ${user.fullName}`} aria-expanded={isOpen} aria-controls={isOpen ? 'account-navigation' : undefined} onClick={()=>setIsOpen(open=>!open)}><span className="account-menu__name">{user.fullName}</span><span className={`account-menu__chevron${isOpen?' account-menu__chevron--open':''}`} aria-hidden="true">▾</span><span className="account-menu__icon"><NavigationIcon /></span></button>
 {isOpen&&<nav id="account-navigation" className="account-menu__dropdown account-menu__dropdown--wide" aria-label="Tài khoản">
 {user.role==='ADMIN'&&link(ROUTES.ADMIN,'Quản trị hệ thống')}
 {link(ROUTES.REGISTRATION,'Đăng ký ăn trưa')}
 {link(ROUTES.PURCHASE,'Mua thêm vé')}{link(ROUTES.EXCHANGE,'Chợ vé / Chuyển nhượng')}
 {link(ROUTES.PROFILE,'Thông tin cá nhân')}
 <button type="button" onClick={logout}>Đăng xuất</button></nav>}</div>:<Link className="site-header__signin" to={ROUTES.LOGIN}>Đăng nhập</Link>}
 </div></header>;
}
