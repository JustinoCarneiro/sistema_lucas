// scripts/gerar-manual-pdf.mjs
//
// Monta docs/MANUAL_USUARIO.html e docs/MANUAL_USUARIO.pdf a partir de
// docs/MANUAL_USUARIO.md, no visual do Sistema Lucas (azul/cinza, Inter),
// intercalando as capturas de tela de docs/manual-assets/ (geradas por
// scripts/gerar-prints-manual.mjs).
//
// Uso:  node scripts/gerar-manual-pdf.mjs
// Requer: marked (devDependency do frontend) e weasyprint no PATH.

import { readFileSync, writeFileSync, existsSync } from 'node:fs';
import { dirname, resolve } from 'node:path';
import { fileURLToPath } from 'node:url';
import { execFileSync } from 'node:child_process';
import { marked } from 'marked';

const __dirname = dirname(fileURLToPath(import.meta.url));
const DOCS = resolve(__dirname, '../docs');
const MD = resolve(DOCS, 'MANUAL_USUARIO.md');
const ASSETS = resolve(DOCS, 'manual-assets');
const OUT_HTML = resolve(DOCS, 'MANUAL_USUARIO.html');
const OUT_PDF = resolve(DOCS, 'MANUAL_USUARIO.pdf');

const manifest = existsSync(resolve(ASSETS, 'manifest.json'))
  ? JSON.parse(readFileSync(resolve(ASSETS, 'manifest.json'), 'utf8')).filter((m) => m.ok)
  : [];

// ── markdown → html ──────────────────────────────────────────────────────
// Slug no padrão GitHub: minúsculas, remove pontuação (mantém letras
// acentuadas), cada espaço vira um "-" (sem colapsar). Casa com os links
// internos [x.y](#xy-...) escritos no .md.
const ghSlug = (s) =>
  s.trim().toLowerCase()
    .replace(/<[^>]+>/g, '')
    .replace(/[^\p{L}\p{N}\s-]/gu, '')
    .replace(/\s/g, '-')
    .replace(/^-+|-+$/g, '');

marked.setOptions({ gfm: true });
marked.use({
  renderer: {
    heading(token) {
      const depth = token.depth;
      const inner = this.parser.parseInline(token.tokens);
      return `<h${depth} id="${ghSlug(token.text)}">${inner}</h${depth}>\n`;
    },
  },
});
let raw = readFileSync(MD, 'utf8');
// A 1ª linha "# Manual de Uso…" vira a capa; remove do corpo.
raw = raw.replace(/^#\s+Manual de Uso.*\n/, '');
let html = marked.parse(raw);

// ── o diagrama ASCII da máquina de estados não cabe em A4: troca por um
//    fluxo em "chips" que quebra linha ────────────────────────────────────
const fluxo = [
  'Aguardando Confirmação', 'profissional aprova', 'Agendada', 'profissional confirma',
  'Aguardando paciente', 'paciente confirma', 'Confirmada', 'prontuário salvo', 'Concluída',
];
const chips = fluxo
  .map((t, i) =>
    i % 2 === 0
      ? `<span class="st">${t}</span>`
      : `<span class="ar">→ <em>${t}</em> →</span>`)
  .join(' ');
const diagramaHtml =
  `<div class="fluxo">${chips}</div>` +
  `<p class="fluxo-nota"><strong>Recusa:</strong> de <span class="st sm">Aguardando Confirmação</span>, ` +
  `<em>profissional recusa</em> → <span class="st sm">Cancelada</span>.<br>` +
  `<strong>De qualquer estado:</strong> <em>cancelar</em> → <span class="st sm">Cancelada</span> · ` +
  `<em>marcar falta</em> → <span class="st sm">Faltou</span> · ` +
  `<em>reagendar</em> → volta para <span class="st sm">Agendada</span>.</p>`;
html = html.replace(/<pre>[\s\S]*?Aguardando Confirmação[\s\S]*?<\/pre>/, diagramaHtml);

// ── injeta as figuras logo após o título da subseção correspondente ──────
const esc = (s) => s.replace(/[.*+?^${}()|[\]\\]/g, '\\$&');
let inlined = 0;
for (const fig of manifest) {
  const num = (fig.caption.match(/\(([\d]+(?:\.[\d]+)*)/) || [])[1];
  if (!num) continue;
  const figHtml =
    `\n<figure class="shot">` +
    `<img src="manual-assets/${fig.slug}.png" alt="${fig.caption}">` +
    `<figcaption>${fig.caption}</figcaption></figure>\n`;
  const re = new RegExp(`(<h([2-4])[^>]*>\\s*${esc(num)}(?:[^0-9.].*?)?</h\\2>)`, 'i');
  if (re.test(html)) { html = html.replace(re, `$1${figHtml}`); inlined++; }
}

// As figuras entram inline, na seção correspondente — sem galeria/anexo
// (evita repetir cada captura duas vezes).
const naoInline = manifest.filter((m) => {
  const num = (m.caption.match(/\(([\d]+(?:\.[\d]+)*)/) || [])[1];
  return !num || !new RegExp(`<h[2-4][^>]*>\\s*${esc(num)}(?:[^0-9.]|<)`).test(html);
});
const galeria = naoInline.length
  ? `<h2 class="pb">Anexo — Telas sem seção fixa</h2><div class="galeria">` +
    naoInline.map((m) =>
      `<figure class="shot"><img src="manual-assets/${m.slug}.png" alt="${m.caption}">` +
      `<figcaption>${m.caption}</figcaption></figure>`).join('\n') +
    `</div>`
  : '';

// ── documento final ─────────────────────────────────────────────────────
// Estrutura de formatação (quebras, overflow, imagens, tabelas): base
// compartilhada OndaDev. O bloco TEMA abaixo só adiciona cor/fonte/borda.
const BASE_CSS = readFileSync(resolve(__dirname, 'ondadev-pdf-base.css'), 'utf8');

const TEMA = `
  /* ---- Tema Sistema Lucas (só estética; estrutura vem do base) ---- */
  @page { @bottom-center { content: "Sistema Lucas · Manual de Uso"; font-size: 8pt; color: #94a3b8; }
          @bottom-right  { color: #94a3b8; } }
  body { font-family: "Inter", "Helvetica Neue", Arial, sans-serif; color: #1f2937; }
  h1, h2, h3, h4 { color: #1e3a8a; }
  h2 { font-size: 15pt; padding-bottom: 5px; border-bottom: 2px solid #1e3a8a; }
  h3 { font-size: 12pt; color: #2563eb; }
  h4 { font-size: 10.7pt; color: #1d4ed8; }
  ul, ol { padding-left: 22px; }
  strong { color: #0f172a; }
  code { background: #f1f5f9; padding: 1px 4px; border-radius: 3px;
         font-family: "SFMono-Regular", Consolas, monospace; }
  table { font-size: 9pt; }
  th { background: #1e3a8a; color: #fff; padding: 6px 8px; }
  td { border: 1px solid #d1d5db; padding: 6px 8px; }
  tr:nth-child(even) td { background: #f8fafc; }
  blockquote { background: #eff6ff; border-left: 4px solid #2563eb; padding: 8px 14px;
               border-radius: 4px; color: #1e293b; }
  a { color: #2563eb; text-decoration: none; }
  hr { border: none; border-top: 1px solid #e2e8f0; margin: 20px 0; }

  .fluxo { display: flex; flex-wrap: wrap; align-items: center; gap: 6px 4px; margin: 12px 0; }
  .fluxo .st { background: #1e3a8a; color: #fff; font-weight: 600; font-size: 8.5pt;
               padding: 4px 9px; border-radius: 6px; white-space: nowrap; }
  .fluxo .ar { color: #2563eb; font-size: 8pt; white-space: nowrap; }
  .fluxo .ar em { font-style: normal; color: #475569; }
  .fluxo-nota { font-size: 9pt; color: #334155; background: #f8fafc; border: 1px solid #e2e8f0;
                border-radius: 6px; padding: 8px 12px; }
  .fluxo-nota .st.sm { background: #e0e7ff; color: #1e3a8a; font-weight: 600;
                       padding: 1px 6px; border-radius: 4px; }
  .fluxo-nota em { font-style: italic; color: #1d4ed8; }

  figure.shot img { border: 1px solid #cbd5e1; border-radius: 8px; }
  figure.shot figcaption { color: #64748b; }

  .cover { text-align: center; justify-content: center; }
  .cover .badge { display: inline-block; background: #2563eb; color: #fff; padding: 6px 20px;
                  border-radius: 20px; font-size: 9pt; letter-spacing: 2px; margin-bottom: 22px; }
  .cover h1 { font-size: 34pt; color: #1e3a8a; margin: 0; }
  .cover .sub { font-size: 14pt; color: #475569; margin-top: 10px; }
  .cover .meta { margin-top: 60px; font-size: 9.5pt; color: #64748b; display: inline-block;
                 text-align: left; border-top: 1px solid #cbd5e1; padding-top: 18px; }
  .cover .meta div { margin: 3px 0; }
`;

const hoje = new Date().toLocaleDateString('pt-BR', { day: '2-digit', month: 'long', year: 'numeric' });
const doc = `<!DOCTYPE html>
<html lang="pt-BR"><head><meta charset="UTF-8"><title>Sistema Lucas — Manual de Uso</title>
<style>
${BASE_CSS}
${TEMA}
</style></head><body>

<div class="cover sheet">
  <div><span class="badge">MANUAL DE USO</span></div>
  <h1>Sistema Lucas</h1>
  <div class="sub">Prontuário eletrônico e agendamento de consultas</div>
  <div class="sub" style="font-size:11pt;margin-top:18px;">Passo a passo por perfil: Paciente · Profissional · Administrador · Técnico</div>
  <div class="meta">
    <div><strong>Documento</strong>&nbsp;&nbsp;Manual operacional das telas</div>
    <div><strong>Gerado em</strong>&nbsp;&nbsp;${hoje}</div>
    <div><strong>Fonte</strong>&nbsp;&nbsp;docs/MANUAL_USUARIO.md + capturas do ambiente de demonstração</div>
    <div><strong>Conformidade</strong>&nbsp;&nbsp;LGPD — Lei nº 13.709/2018</div>
  </div>
</div>

${html}
${galeria}

</body></html>`;

writeFileSync(OUT_HTML, doc);
console.log(`HTML  → ${OUT_HTML}  (${manifest.length} capturas: ${inlined} inline, ${naoInline.length} no anexo)`);

try {
  execFileSync('weasyprint', ['--base-url', DOCS + '/', OUT_HTML, OUT_PDF], { stdio: 'inherit' });
  console.log(`PDF   → ${OUT_PDF}`);
} catch (e) {
  console.error('weasyprint falhou:', e.message);
  process.exit(1);
}
