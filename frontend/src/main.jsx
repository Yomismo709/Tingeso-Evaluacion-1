import React from 'react';
import ReactDOM from 'react-dom/client';
import App from './App.jsx';
import './index.css';
import keycloak from './keycloak';

keycloak.init({ 
  onLoad: 'login-required', 
  pkceMethod: 'S256' 
}).then((authenticated) => {
  if (!authenticated) {
    window.location.reload();
  } else {
    // Renovar el token cada minuto si está a punto de expirar
    setInterval(() => {
      keycloak.updateToken(70).catch(() => {
        console.error('Error al renovar el token');
        keycloak.login();
      });
    }, 60000);

    ReactDOM.createRoot(document.getElementById('root')).render(
      <React.StrictMode>
        <App />
      </React.StrictMode>
    );
  }
}).catch(console.error);