import Keycloak from 'keycloak-js';

const keycloakConfig = {
  // Detecta automáticamente http/https y el dominio (ej. localhost o misistema.duckdns.org)
  url: '/auth', // Completamente relativo, a prueba de cambios de dominio
  realm: 'siga-ipt',
  clientId: 'siga-ipt-frontend'
};

const keycloak = new Keycloak(keycloakConfig);

export default keycloak;



