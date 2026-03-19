import { defineConfig } from "cypress";

export default defineConfig({
  e2e: {
    baseUrl: 'http://localhost',
    supportFile: 'cypress/support/e2e.ts',
    video: true,
    viewportWidth: 1280,
    viewportHeight: 720,
    setupNodeEvents(on, config) {
      // Configurações e Listeners de ambiente Node
    },
  },
});
