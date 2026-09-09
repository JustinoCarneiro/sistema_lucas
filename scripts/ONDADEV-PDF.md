# Padrão OndaDev — geração de PDF sem cortes nem quebras bugadas

> Escopo: **estrutura de formatação** para paged media. Não trata de paleta,
> tipografia de marca ou layout criativo — isso é camada de tema, por projeto.
> Arquivo canônico: [`ondadev-pdf-base.css`](./ondadev-pdf-base.css).

## Pipeline

1. Conteúdo em **Markdown** → HTML com um renderer (ex.: `marked`), adicionando
   `id` nos títulos no padrão GitHub (para os links internos `[x](#slug)`
   resolverem no PDF).
2. Montar um HTML único: `<style>` = **`ondadev-pdf-base.css`** + CSS de tema do
   projeto (nessa ordem — o tema só adiciona cor/fonte/borda).
3. Renderizar com **WeasyPrint**, não com Chrome:
   `weasyprint --base-url <pasta-dos-assets>/ entrada.html saida.pdf`
   WeasyPrint tem suporte real a `@page`, margin boxes, `break-*`,
   `widows`/`orphans` e repetição de `<thead>`. O `--print-to-pdf` do Chrome
   erra tudo isso.
4. **Sempre** rasterizar 2–4 páginas (`pdftoppm -png`) e conferir a olho antes
   de entregar.

## As regras que impedem corte/quebra (resumo do CSS base)

| Problema recorrente | Regra que resolve |
|---|---|
| Imagem cortada na direita / estourando a folha | `img { max-width:100%; height:auto }` + **teto de altura** `figure>img { max-height:225mm }` (A4 retrato, margem 20mm). Imagem alta **escala**, não corta. |
| Imagem separada da legenda | `<figcaption>` **dentro** de `<figure>` + `figure { break-inside:avoid }` → viajam juntos. |
| Título sozinho no pé da página | `h1..h6 { break-after:avoid; break-inside:avoid }` (+ alias `page-break-after`). |
| Bloco (código, citação, card) rachado no meio | `figure, blockquote, pre, table, .callout, .card { break-inside:avoid }`. |
| Bloco **maior que a página** quebrado feio mesmo assim | `break-inside:avoid` só vale se couber em 1 página → limitar altura de imagens; para tabela longa, ver abaixo. |
| Tabela longa "some" nas páginas 2+ | **Não** usar `break-inside:avoid` na `<table>`. Usar `thead { display:table-header-group }` (cabeçalho repete) + `tr { break-inside:avoid }` (linha não racha). |
| Célula com texto longo estica a coluna pra fora | `table { table-layout:fixed }` + `th,td { overflow-wrap:anywhere }`. |
| URL / token gigante alarga a página | `body { overflow-wrap:break-word; hyphens:auto }`, `code,a { overflow-wrap:anywhere }`. |
| ASCII art / diagrama de largura fixa cortado | **Não colocar no PDF.** Converter para HTML que reflui (`display:flex; flex-wrap:wrap`) ou para SVG/imagem que escala. Último caso: `<pre class="wide">` + `.landscape` (vira a página). |
| Linha órfã/viúva de parágrafo | `body { widows:3; orphans:3 }`. |
| Padding empurra o bloco pra fora | `*{ box-sizing:border-box }`. |
| Rodapé/nº de página colidindo com texto | Página só em `@page { @bottom-* { content: counter(page) "/" counter(pages) } }` — **nunca** `position:fixed` no corpo. |
| "justify" abre buracos e vaza | `text-align:left` no corpo. |
| Fundos não imprimem (Chrome) | `print-color-adjust: exact` (WeasyPrint imprime por padrão e só avisa). |
| Quebra de seção onde não quer / página vazia | `break-before:page` **só** via classe `.pb`, aplicada de propósito. |
| Capa/divisória meia-página | `.sheet { min-height:245mm; break-after:page }`. |

## Camada de tema (por projeto) — o que pode mexer

Cor, `font-family`, `border`, `background`, `border-radius`, escala de tamanho
dos títulos, `@page { @bottom-center { content: "<nome do doc>" } }`.
**Não** redefinir as propriedades `break-*`, `overflow-wrap`, `table-layout`,
`max-width`/`max-height` de imagem, `widows`/`orphans` — essas são o padrão.

## Checklist antes de entregar

- [ ] Renderizado com WeasyPrint + `--base-url`.
- [ ] 3–4 páginas conferidas em imagem (capa, uma com figura, uma com tabela, a última).
- [ ] Nenhuma imagem toca/ultrapassa a margem; nenhuma legenda órfã.
- [ ] Nenhum título no pé da página sem conteúdo embaixo.
- [ ] Tabelas largas: coluna não vaza; tabela longa repete cabeçalho.
- [ ] Sem `<pre>` de largura fixa (ASCII) — virou HTML/imagem.
- [ ] Warnings do WeasyPrint revisados (só `print-color-adjust` é tolerado).
- [ ] Assets rasterizados a ~2× do tamanho final (nitidez em impressão).
