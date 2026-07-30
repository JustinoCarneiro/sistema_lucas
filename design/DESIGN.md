# Sistema Lucas — Identidade Visual

> Reconstruído em 29/07/2026 a partir do que já existe em `frontend/src/styles.css`. A Fase 2 não
> gerou esses artefatos no momento certo do processo — este documento é a formalização retroativa
> do que já estava implementado, não uma nova decisão de design.

## Origem

Os tokens visuais foram garimpados manualmente do template comercial **Mosaic React** (React 19 +
Vite + Tailwind v4), copiado como referência estática em `frontend/template-ref/` — **sem**
dependência de build ou import de código entre os dois projetos. A equipe extraiu tipografia,
sombra, paleta de cinza e classes utilitárias de botão/título, adaptando pro Angular.

## Decisão de cor

O Mosaic usa **violeta** como cor de destaque. Aqui a cor de destaque foi trocada para o
**azul padrão do Tailwind** (`blue-*`, sem redefinição customizada) — só a paleta neutra
(cinza) e a tipografia vieram do template.

## Tipografia

**Inter** (400/500/600/700), via Google Fonts. Escala de `text-xs` a `text-5xl` com
`line-height`/`letter-spacing` próprios por tamanho (não é a escala default do Tailwind — foi
ajustada pelo Mosaic e mantida como está).

## Modo escuro

Baseado em classe (`.dark` no `<html>`, alternado por um `ThemeService` no Angular) — não é
`prefers-color-scheme`. Cor de borda ajustada nos dois temas (`gray-200` no claro, `gray-700` no
escuro) porque o Tailwind v4 mudou o default de borda pra `currentColor`; o projeto restaura o
comportamento v3 (borda cinza discreta) explicitamente.

## O que foi conscientemente deixado de fora

A seção de **formulários** do Mosaic não foi portada — depende do plugin `@tailwindcss/forms`,
que não está instalado neste projeto. Não usar classes de formulário do template como referência
sem antes instalar o plugin ou adaptar manualmente.

## Fonte única da verdade

`frontend/src/styles.css` (bloco `@theme`) é o que o Tailwind realmente lê — Tailwind v4 é
CSS-first, não existe `tailwind.config.js` aqui. `./tokens.css` neste diretório é só a cópia de
referência no padrão Onda; se os dois divergirem, o arquivo do frontend vale.
