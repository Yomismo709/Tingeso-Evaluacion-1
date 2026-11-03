import Keycloak from 'keycloak-js';

const keycloakConfig = {
  url: 'http://localhost:8081',
  realm: 'siga-ipt',
  clientId: 'siga-ipt-frontend'
};

const keycloak = new Keycloak(keycloakConfig);

export default keycloak;