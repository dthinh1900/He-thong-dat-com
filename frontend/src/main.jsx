import React from 'react';
import ReactDOM from 'react-dom/client';
import App from './App';
import './styles/global.css';
import './styles/header.css';
import './styles/login.css';
import './styles/registration.css';
import './styles/profile.css';
import './styles/toast.css';
import './styles/navigation.css';

ReactDOM.createRoot(document.getElementById('root')).render(
  <React.StrictMode><App /></React.StrictMode>,
);
