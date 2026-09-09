// scripts/gerar-prints-manual.mjs
//
// Captura as telas reais do Sistema Lucas (ambiente dev em localhost:8082) para
// ilustrar o docs/MANUAL_USUARIO.pdf. Usa puppeteer-core + o Google Chrome já
// instalado na máquina (nenhum Chromium é baixado).
//
// Pré-requisitos:
//   - stack dev de pé: ./deploy-dev.sh  (frontend :8082, API :8081, profile "dev")
//   - dados de demonstração semeados pelo DataInitializer
//
// Uso:
//   node scripts/gerar-prints-manual.mjs
//
// Saída: docs/manual-assets/*.png  +  docs/manual-assets/manifest.json

import puppeteer from 'puppeteer-core';
import { mkdirSync, writeFileSync, existsSync } from 'node:fs';
import { dirname, resolve } from 'node:path';
import { fileURLToPath } from 'node:url';

const __dirname = dirname(fileURLToPath(import.meta.url));
const OUT_DIR = resolve(__dirname, '../docs/manual-assets');
const BASE = process.env.LUCAS_WEB ?? 'http://localhost:8082';
const API = process.env.LUCAS_API ?? 'http://localhost:8081';
const CHROME =
  process.env.CHROME_PATH ??
  ['/usr/bin/google-chrome', '/usr/bin/google-chrome-stable', '/usr/bin/chromium', '/usr/bin/chromium-browser']
    .find((p) => existsSync(p)) ??
  '/usr/bin/google-chrome';

const VIEWPORT = { width: 1440, height: 1000, deviceScaleFactor: 2 };

const USERS = {
  paciente:     { email: 'lucas@email.com', password: '123456' },
  profissional: { email: 'ana@clinica.com', password: '123456' },
  admin:        { email: process.env.ADMIN_EMAIL ?? 'admin@clinica.com', password: process.env.ADMIN_PASSWORD ?? 'admin' },
};

const sleep = (ms) => new Promise((r) => setTimeout(r, ms));

// Clica no primeiro elemento (button/a) cujo texto contém `txt`.
async function clickByText(page, txt) {
  const clicked = await page.evaluate((t) => {
    const els = [...document.querySelectorAll('button, a, [role="tab"]')];
    const el = els.find((e) => e.textContent.trim().toLowerCase().includes(t.toLowerCase()));
    if (el) { el.click(); return true; }
    return false;
  }, txt);
  if (clicked) await sleep(1200);
  return clicked;
}

async function newLightPage(context) {
  const page = await context.newPage();
  await page.setViewport(VIEWPORT);
  await page.emulateMediaFeatures([{ name: 'prefers-color-scheme', value: 'light' }]);
  await page.evaluateOnNewDocument(() => {
    try { localStorage.setItem('theme', 'light'); } catch (e) {}
  });
  page.setDefaultTimeout(25000);
  return page;
}

async function login(context, { email, password }) {
  const page = await newLightPage(context);
  await page.goto(`${BASE}/login`, { waitUntil: 'networkidle2' });
  await page.waitForSelector('#email');
  await page.type('#email', email, { delay: 15 });
  await page.type('#password', password, { delay: 15 });
  await page.click('button[type="submit"]');
  // O router do Angular navega no cliente (sem full page load) — faz polling da URL.
  for (let i = 0; i < 40; i++) {
    await sleep(500);
    if (page.url().includes('/panel')) break;
  }
  if (!page.url().includes('/panel')) {
    throw new Error(`login falhou para ${email} — parou em ${page.url()}`);
  }
  await sleep(1500); // deixa o dashboard hidratar
  return page;
}

async function shoot(page, slug, { path, action, waitExtra = 1200, caption } = {}) {
  const file = resolve(OUT_DIR, `${slug}.png`);
  try {
    if (path) await page.goto(`${BASE}${path}`, { waitUntil: 'networkidle2' });
    await sleep(waitExtra);
    if (action) await action(page);
    await sleep(400);
    await page.screenshot({ path: file, fullPage: false });
    console.log(`  ✓ ${slug}.png`);
    return { slug, file: `manual-assets/${slug}.png`, caption: caption ?? '', ok: true };
  } catch (err) {
    console.log(`  ✗ ${slug} — ${err.message}`);
    return { slug, file: `manual-assets/${slug}.png`, caption: caption ?? '', ok: false, error: err.message };
  }
}

async function run() {
  mkdirSync(OUT_DIR, { recursive: true });
  const manifest = [];
  const browser = await puppeteer.launch({
    executablePath: CHROME,
    headless: 'new',
    args: ['--no-sandbox', '--disable-gpu', '--disable-dev-shm-usage', '--lang=pt-BR', '--font-render-hinting=none'],
  });

  try {
    // ─── PÚBLICO ─────────────────────────────────────────────────────────
    console.log('· Público');
    {
      const ctx = await browser.createBrowserContext();
      const page = await newLightPage(ctx);
      manifest.push(await shoot(page, '01-login', { path: '/login', caption: 'Tela de acesso (1.2)' }));
      manifest.push(await shoot(page, '02-cadastro', { path: '/register', caption: 'Cadastro de paciente (1.1)' }));
      manifest.push(await shoot(page, '03-esqueci-senha', { path: '/forgot-password', caption: 'Recuperação de senha (1.3)' }));
      if (process.env.NPS_TOKEN) {
        manifest.push(await shoot(page, '04-avaliar-nps', { path: `/avaliar?token=${process.env.NPS_TOKEN}`, caption: 'Avaliação pós-consulta (2.12)' }));
      }
      await ctx.close();
    }

    // ─── PACIENTE ────────────────────────────────────────────────────────
    console.log('· Paciente');
    {
      const ctx = await browser.createBrowserContext();
      const page = await login(ctx, USERS.paciente);
      manifest.push(await shoot(page, '10-paciente-inicio', { path: '/panel/dashboard', caption: 'Início do paciente (2.2)' }));
      manifest.push(await shoot(page, '11-paciente-minhas-consultas', { path: '/panel/my-appointments', caption: 'Minhas Consultas — lista e status (2.4)' }));
      manifest.push(await shoot(page, '12-paciente-agendar', {
        path: '/panel/my-appointments',
        action: (p) => clickByText(p, 'Agendar consulta'),
        caption: 'Formulário de agendamento guiado (2.3)',
      }));
      manifest.push(await shoot(page, '13-paciente-meus-documentos', { path: '/panel/my-documents', caption: 'Meus Documentos e Portabilidade (2.8 / 2.10)' }));
      manifest.push(await shoot(page, '14-paciente-lista-espera', { path: '/panel/waitlist', caption: 'Lista de Espera (2.11)' }));
      manifest.push(await shoot(page, '15-paciente-meu-perfil', { path: '/panel/my-profile', caption: 'Meu Perfil + verificação em duas etapas (1.6 / 1.8)' }));
      await ctx.close();
    }

    // ─── PROFISSIONAL ────────────────────────────────────────────────────
    console.log('· Profissional');
    {
      const ctx = await browser.createBrowserContext();
      const page = await login(ctx, USERS.profissional);

      // Descobre um id de consulta pra abrir o prontuário.
      let consultaId = process.env.PRONTUARIO_APPT_ID ?? null;
      if (!consultaId) {
        try {
          const data = await page.evaluate(async (api) => {
            const r = await fetch(`${api}/consultas/profissional/todas`, { credentials: 'include' });
            return r.ok ? r.json() : null;
          }, API);
          const arr = Array.isArray(data) ? data : data?.content ?? [];
          const alvo = arr.find((c) => ['AGENDADA', 'CONFIRMADA', 'CONFIRMADA_PROFISSIONAL'].includes(c.status)) ?? arr[0];
          consultaId = alvo?.id ?? null;
        } catch (e) {}
      }

      manifest.push(await shoot(page, '20-profissional-inicio', { path: '/panel/dashboard', caption: 'Início do profissional (3.11)' }));
      manifest.push(await shoot(page, '21-profissional-agenda-hoje', { path: '/panel/professional-appointments', caption: 'Minha Agenda — aba Hoje (3.3)' }));
      manifest.push(await shoot(page, '22-profissional-agenda-proximas', {
        path: '/panel/professional-appointments',
        action: (p) => clickByText(p, 'Próximas'),
        caption: 'Minha Agenda — aba Próximas (3.3)',
      }));
      manifest.push(await shoot(page, '23-profissional-agenda-atrasadas', {
        path: '/panel/professional-appointments',
        action: (p) => clickByText(p, 'Atrasadas'),
        caption: 'Minha Agenda — aba Atrasadas (3.8)',
      }));
      manifest.push(await shoot(page, '24-profissional-disponibilidade', { path: '/panel/my-availability', caption: 'Minha Disponibilidade (3.2)' }));
      manifest.push(await shoot(page, '25-profissional-documentos', { path: '/panel/document-management', caption: 'Documentos — upload e visibilidade (3.9)' }));
      if (consultaId) {
        manifest.push(await shoot(page, '26-profissional-prontuario', { path: `/panel/medical-record/${consultaId}`, caption: 'Prontuário Eletrônico e histórico clínico (3.6 / 3.10)' }));
      }
      await ctx.close();
    }

    // ─── ADMIN ───────────────────────────────────────────────────────────
    console.log('· Administrador');
    {
      const ctx = await browser.createBrowserContext();
      const page = await login(ctx, USERS.admin);
      manifest.push(await shoot(page, '30-admin-inicio', { path: '/panel/dashboard', caption: 'Início do administrador (4.1)' }));
      manifest.push(await shoot(page, '31-admin-profissionais', { path: '/panel/professionals', caption: 'Profissionais — cadastrar, editar, excluir (4.2)' }));
      manifest.push(await shoot(page, '32-admin-pacientes', { path: '/panel/patients', caption: 'Pacientes — visualizar, desbloquear, excluir (4.3)' }));
      manifest.push(await shoot(page, '33-admin-agenda-geral', { path: '/panel/appointments', caption: 'Agenda Geral — cancelamento pela administração (4.4)' }));
      manifest.push(await shoot(page, '34-admin-seguranca', { path: '/panel/seguranca', caption: 'Segurança — 2FA do administrador (4.7)' }));
      manifest.push(await shoot(page, '35-admin-logs', { path: '/panel/logs', caption: 'Painel de Logs (4.6)' }));
      await ctx.close();
    }
  } finally {
    await browser.close();
  }

  writeFileSync(resolve(OUT_DIR, 'manifest.json'), JSON.stringify(manifest, null, 2));
  const ok = manifest.filter((m) => m.ok).length;
  console.log(`\n${ok}/${manifest.length} telas capturadas → ${OUT_DIR}`);
  const fail = manifest.filter((m) => !m.ok);
  if (fail.length) console.log('Falhas:', fail.map((f) => `${f.slug} (${f.error})`).join('; '));
}

run().catch((e) => { console.error(e); process.exit(1); });
