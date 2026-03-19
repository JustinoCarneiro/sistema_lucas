/// <reference types="cypress" />

// Importa os custom commands
import './commands';

// Ignora exceções não-capturadas do Angular/Nginx para não travar os testes
Cypress.on('uncaught:exception', (err, runnable) => {
  return false;
});
