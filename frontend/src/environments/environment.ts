export const environment = {
    production: true,
    apiUrl: (window as any).__env?.apiUrl || 'http://localhost:8081' // No Docker, o browser ainda acessa via localhost:8081, a não ser que window.__env exija diferente
  };