/*
 * Modelos desenhados (SVG original) das armas do jogo, um por tipo. Usados nos cartões da tela Armas,
 * na barra de armas e no HUD do minigame. Uso: ModelosArmas.svg('ESCOPETA').
 */
(function () {
    'use strict';
    // peças comuns: aço (cano/corpo), aço escuro, madeira e polímero
    const DEFS = `<defs>
        <linearGradient id="ma-aco" x1="0" y1="0" x2="0" y2="1"><stop offset="0" stop-color="#d7dde4"/><stop offset=".45" stop-color="#8b949e"/><stop offset="1" stop-color="#4b525a"/></linearGradient>
        <linearGradient id="ma-escuro" x1="0" y1="0" x2="0" y2="1"><stop offset="0" stop-color="#6b737c"/><stop offset="1" stop-color="#262a2f"/></linearGradient>
        <linearGradient id="ma-madeira" x1="0" y1="0" x2="0" y2="1"><stop offset="0" stop-color="#9c6b43"/><stop offset="1" stop-color="#4e3220"/></linearGradient>
        <linearGradient id="ma-polimero" x1="0" y1="0" x2="0" y2="1"><stop offset="0" stop-color="#4a5058"/><stop offset="1" stop-color="#1c1f23"/></linearGradient>
    </defs>`;
    const L = 'stroke="#15171a" stroke-width="1.2" stroke-linejoin="round"';
    const rep = (n, f) => Array.from({ length: n }, (_, i) => f(i)).join('');

    const MODELOS = {
        PISTOLA: `
            <path d="M38 14 h80 l4 4 v10 h-84 z" fill="url(#ma-aco)" ${L}/>
            <path d="M44 16 h70" stroke="#eef2f5" stroke-width="1" opacity=".6"/>
            <g stroke="#3a3f45" stroke-width="1.4">${rep(6, i => `<line x1="${50 + i * 4}" y1="16" x2="${50 + i * 4}" y2="26"/>`)}</g>
            <rect x="118" y="20" width="10" height="5" fill="url(#ma-escuro)" ${L}/>
            <path d="M40 28 h56 l-2 6 h-30 l-4 4 h-6 z" fill="url(#ma-polimero)" ${L}/>
            <path d="M66 34 q4 9 14 6" fill="none" ${L} stroke-width="2"/>
            <path d="M40 28 h22 l-6 28 h-20 l2 -22 z" fill="url(#ma-polimero)" ${L}/>
            <g stroke="#0d0f11" stroke-width=".8" opacity=".7">${rep(5, i => `<line x1="${41 - i * .4}" y1="${36 + i * 4}" x2="${56 - i * .8}" y2="${36 + i * 4}"/>`)}</g>
            <rect x="113" y="10" width="3" height="4" fill="#2a2e33"/><rect x="40" y="10" width="5" height="4" fill="#2a2e33"/>`,
        REVOLVER: `
            <rect x="70" y="16" width="70" height="8" rx="1" fill="url(#ma-aco)" ${L}/>
            <rect x="72" y="24" width="64" height="5" fill="url(#ma-escuro)" ${L}/>
            <rect x="136" y="11" width="3" height="5" fill="#2a2e33"/>
            <path d="M44 14 h28 v22 h-28 z" fill="url(#ma-aco)" ${L}/>
            <rect x="48" y="17" width="22" height="16" rx="3" fill="url(#ma-escuro)" ${L}/>
            <g stroke="#15171a" stroke-width="1">${rep(3, i => `<line x1="${52 + i * 7}" y1="18" x2="${52 + i * 7}" y2="32"/>`)}</g>
            <path d="M44 14 l-8 -4 l-2 3 l8 5 z" fill="url(#ma-escuro)" ${L}/>
            <path d="M58 36 q2 9 12 6" fill="none" ${L} stroke-width="2"/>
            <path d="M44 34 h12 q-2 6 -6 22 h-20 q2 -14 8 -22 z" fill="url(#ma-madeira)" ${L}/>
            <circle cx="40" cy="44" r="1.8" fill="#c9cfd6"/>`,
        ESCOPETA: `
            <rect x="60" y="15" width="96" height="6" fill="url(#ma-aco)" ${L}/>
            <rect x="60" y="22" width="88" height="5" fill="url(#ma-escuro)" ${L}/>
            <rect x="92" y="25" width="38" height="8" rx="2" fill="url(#ma-madeira)" ${L}/>
            <g stroke="#3a2416" stroke-width="1">${rep(6, i => `<line x1="${96 + i * 6}" y1="26" x2="${96 + i * 6}" y2="32"/>`)}</g>
            <path d="M40 14 h22 v16 h-22 z" fill="url(#ma-escuro)" ${L}/>
            <path d="M48 30 q2 8 12 5" fill="none" ${L} stroke-width="2"/>
            <path d="M40 16 l-34 6 l-2 16 l10 2 l26 -14 z" fill="url(#ma-madeira)" ${L}/>
            <rect x="3" y="21" width="5" height="18" fill="#1c1f23" ${L}/>
            <circle cx="154" cy="13" r="1.5" fill="#e8ecef"/>`,
        RIFLE: `
            <rect x="74" y="17" width="82" height="5" fill="url(#ma-aco)" ${L}/>
            <rect x="152" y="15" width="6" height="9" fill="url(#ma-escuro)" ${L}/>
            <rect x="60" y="5" width="44" height="8" rx="4" fill="url(#ma-polimero)" ${L}/>
            <circle cx="60" cy="9" r="5" fill="url(#ma-escuro)" ${L}/><circle cx="104" cy="9" r="5" fill="url(#ma-escuro)" ${L}/>
            <circle cx="104" cy="9" r="2.5" fill="#9fc3e0" opacity=".8"/>
            <rect x="72" y="13" width="4" height="4" fill="#2a2e33"/><rect x="90" y="13" width="4" height="4" fill="#2a2e33"/>
            <path d="M50 16 h28 v10 h-28 z" fill="url(#ma-escuro)" ${L}/>
            <path d="M66 16 l4 -4 h3" fill="none" stroke="#c9cfd6" stroke-width="2"/>
            <path d="M74 22 h46 l-4 7 h-42 z" fill="url(#ma-madeira)" ${L}/>
            <path d="M56 26 q2 8 10 5" fill="none" ${L} stroke-width="2"/>
            <path d="M50 16 l-14 4 l-30 2 l-2 14 l10 1 l14 -6 h10 l12 -5 z" fill="url(#ma-madeira)" ${L}/>
            <path d="M28 30 l-6 12 l8 1 l6 -10" fill="url(#ma-madeira)" ${L}/>`,
        SUBMETRALHADORA: `
            <rect x="52" y="14" width="64" height="14" rx="2" fill="url(#ma-escuro)" ${L}/>
            <rect x="116" y="18" width="26" height="5" fill="url(#ma-aco)" ${L}/>
            <rect x="96" y="16" width="20" height="10" fill="url(#ma-polimero)" ${L}/>
            <g fill="#0d0f11">${rep(3, i => `<circle cx="${100 + i * 6}" cy="21" r="1.4"/>`)}</g>
            <rect x="56" y="10" width="40" height="4" fill="#2a2e33" ${L}/>
            <path d="M80 28 h10 l2 24 h-10 z" fill="url(#ma-polimero)" ${L}/>
            <path d="M66 28 q2 7 10 5" fill="none" ${L} stroke-width="2"/>
            <path d="M54 28 h12 l-4 20 h-12 z" fill="url(#ma-polimero)" ${L}/>
            <path d="M52 16 h-30 v4 h26 v6 h-26 v4 h30" fill="none" stroke="url(#ma-aco)" stroke-width="3"/>
            <rect x="18" y="14" width="5" height="18" rx="1" fill="url(#ma-escuro)" ${L}/>`,
        LANCA_GRANADAS: `
            <rect x="70" y="10" width="70" height="20" rx="3" fill="url(#ma-escuro)" ${L}/>
            <circle cx="140" cy="20" r="8" fill="#1c1f23" ${L}/><circle cx="140" cy="20" r="5" fill="#050607"/>
            <line x1="76" y1="13" x2="134" y2="13" stroke="#9aa3ad" stroke-width="1.2" opacity=".7"/>
            <rect x="44" y="12" width="28" height="22" rx="3" fill="url(#ma-aco)" ${L}/>
            <circle cx="58" cy="23" r="8" fill="url(#ma-escuro)" ${L}/>
            <g fill="#4b525a">${rep(6, i => { const a = i * Math.PI / 3; return `<circle cx="${(58 + Math.cos(a) * 5).toFixed(1)}" cy="${(23 + Math.sin(a) * 5).toFixed(1)}" r="1.5"/>`; })}</g>
            <path d="M96 30 h14 l-2 14 h-12 z" fill="url(#ma-polimero)" ${L}/>
            <path d="M58 34 q2 8 12 5" fill="none" ${L} stroke-width="2"/>
            <path d="M46 34 h12 l-4 20 h-12 z" fill="url(#ma-madeira)" ${L}/>
            <path d="M44 16 h-34 l-4 14 h34 l4 -6 z" fill="url(#ma-madeira)" ${L}/>`
    };

    function svg(tipo) {
        const corpo = MODELOS[tipo] || MODELOS.PISTOLA;
        return `<svg class="modelo-arma" viewBox="0 0 160 60" role="img" aria-label="Modelo da arma">${DEFS}${corpo}</svg>`;
    }

    // preenche qualquer elemento data-modelo-arma="TIPO" da página (tela Armas, por exemplo)
    function preencher(raiz) {
        (raiz || document).querySelectorAll('[data-modelo-arma]').forEach(el => { el.innerHTML = svg(el.dataset.modeloArma); });
    }
    if (document.readyState === 'loading') document.addEventListener('DOMContentLoaded', () => preencher());
    else preencher();

    window.ModelosArmas = { svg, preencher };
})();
