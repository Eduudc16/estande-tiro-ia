/* Estande Sombrio — desenhos ORIGINAIS dos alvos: recortes de madeira pintados (SVG, 100 x 200).
   Cada tipo de estande tem o seu grupo de inimigos; a refém é igual em todos.
   Partes com data-hit contam como acerto; data-parte="cabeca" é tiro na cabeça; data-parte="escudo" bloqueia. */
window.Alvos = (() => {
    'use strict';
    const L = '#23160c';        // contorno escuro
    const MADEIRA = '#5b3d22';
    const METAL = '#a3a8ad';

    const linha = (x1, y1, x2, y2, cor, w) =>
        `<line x1="${x1}" y1="${y1}" x2="${x2}" y2="${y2}" stroke="${cor}" stroke-width="${w}" stroke-linecap="round"/>`;
    // braço com contorno
    const membro = (x1, y1, x2, y2, cor, w) => linha(x1, y1, x2, y2, L, w + 4) + linha(x1, y1, x2, y2, cor, w);
    const mao = (x, y, pele) => `<circle cx="${x}" cy="${y}" r="5.6" fill="${pele}" stroke="${L}" stroke-width="2"/>`;

    // ---- o que cada inimigo segura (é só desenho: atirar nisso não conta) ----
    const OBJETOS = {
        forcado: () => linha(85, 104, 85, 8, MADEIRA, 4) +
            `<path d="M76 18 V5 M85 18 V2 M94 18 V5 M76 18 H94" stroke="${METAL}" stroke-width="2.6" fill="none" stroke-linecap="round"/>`,
        foice: () => linha(85, 52, 85, 30, MADEIRA, 4) +
            `<path d="M84 31 Q104 22 97 3 Q93 19 79 25 Z" fill="${METAL}" stroke="${L}" stroke-width="1.6"/>`,
        machado: () => linha(85, 68, 85, 16, MADEIRA, 4.5) +
            `<path d="M85 18 L101 11 Q107 25 101 38 L85 30 Z" fill="${METAL}" stroke="${L}" stroke-width="1.8"/>`,
        tocha: () => linha(85, 66, 85, 26, MADEIRA, 5) +
            `<g class="chama"><path d="M85 3 Q97 15 91 24 Q85 31 79 24 Q73 15 85 3 Z" fill="#ff8a1f"/>` +
            `<path d="M85 12 Q90 19 87 24 Q85 27 82 24 Q80 19 85 12 Z" fill="#ffe08a"/></g>`,
        cajado: () => linha(85, 120, 85, 12, MADEIRA, 4) +
            `<circle class="brilho" cx="85" cy="9" r="5.6" fill="#9d6bff" stroke="${L}" stroke-width="1.5"/>`,
        cassetete: () => linha(85, 46, 92, 12, '#1d1d20', 5),
        faca: () => `<path d="M84 40 L88 16 L92 40 Z" fill="${METAL}" stroke="${L}" stroke-width="1.4"/>` +
            linha(83, 44, 93, 44, '#3a2a1a', 4),
        picareta: () => linha(85, 74, 85, 18, MADEIRA, 4) +
            `<path d="M68 24 Q85 10 102 24 L100 27 Q85 17 70 27 Z" fill="${METAL}" stroke="${L}" stroke-width="1.6"/>`,
        marreta: () => linha(85, 86, 85, 20, MADEIRA, 5) +
            `<rect x="72" y="7" width="26" height="15" rx="2" fill="#6f7377" stroke="${L}" stroke-width="2"/>`,
        sabre: () => `<path d="M84 42 Q94 20 88 0 Q87 22 81 41 Z" fill="#d6dadf" stroke="${L}" stroke-width="1.4"/>` +
            `<ellipse cx="84" cy="44" rx="6.5" ry="2.6" fill="#c9a646" stroke="${L}" stroke-width="1"/>`,
        dinamite: () => `<rect x="80" y="25" width="10" height="17" rx="2" fill="#b8322a" stroke="${L}" stroke-width="1.6"/>` +
            `<path d="M85 25 Q87 19 91 17" stroke="#c9b48f" stroke-width="1.5" fill="none"/>` +
            `<circle class="brilho" cx="91.5" cy="16" r="2.7" fill="#ffd36b"/>`
    };

    // ---- chapéus, capacetes e cabelos ----
    const CHAPEUS = {
        boina: c => `<path d="M33 27 Q35 14 52 13 Q67 14 69 24 Q61 21 51 22 Q41 22 33 27 Z" fill="${c}" stroke="${L}" stroke-width="2"/>`,
        lenco: c => `<path d="M34 41 Q32 15 50 14 Q68 15 66 41 Q63 25 50 24 Q37 25 34 41 Z" fill="${c}" stroke="${L}" stroke-width="2"/>`,
        capacete: c => `<path d="M33 31 Q33 14 50 13 Q67 14 67 31 L71 33 L29 33 Z" fill="${c}" stroke="${L}" stroke-width="2"/>`,
        mineiro: c => `<path d="M33 30 Q33 13 50 13 Q67 13 67 30 L70 31 L30 31 Z" fill="${c}" stroke="${L}" stroke-width="2"/>` +
            `<circle class="brilho" cx="50" cy="21" r="4.6" fill="#fff4b8" stroke="${L}" stroke-width="1.5"/>`,
        bandana: c => `<path d="M35 29 Q36 15 50 15 Q64 15 65 29 Z" fill="${c}" stroke="${L}" stroke-width="2"/>` +
            `<path d="M64 25 L77 29 L67 33 Z" fill="${c}" stroke="${L}" stroke-width="1.6"/>`,
        tricornio: c => `<path d="M25 27 Q50 2 75 27 Q50 19 25 27 Z" fill="${c}" stroke="${L}" stroke-width="2"/>`,
        cabeloFrente: c => `<path d="M35 36 Q33 17 50 16 Q67 17 65 36 Q60 25 50 26 Q40 25 35 36 Z" fill="${c}" stroke="${L}" stroke-width="2"/>`,
        cabeloTras: c => `<path d="M62 24 Q82 33 74 58 Q69 43 60 33 Z" fill="${c}" stroke="${L}" stroke-width="2"/>`
    };

    // ---- rostos ----
    const olhosVermelhos = y =>
        `<circle cx="44.5" cy="${y}" r="4.4" fill="rgba(255,60,40,.35)"/><circle cx="55.5" cy="${y}" r="4.4" fill="rgba(255,60,40,.35)"/>` +
        `<circle cx="44.5" cy="${y}" r="2.3" fill="#ff3b2f"/><circle cx="55.5" cy="${y}" r="2.3" fill="#ff3b2f"/>`;
    const rostoMau = () => olhosVermelhos(33) +
        `<path d="M39.5 28 L47.5 30.5 M60.5 28 L52.5 30.5" stroke="${L}" stroke-width="2.2" stroke-linecap="round"/>` +
        `<path d="M43 41 Q50 37.5 57 41 Q50 46.5 43 41 Z" fill="#3a0f0a" stroke="${L}" stroke-width="1.2"/>`;
    const rostoAssustado = () =>
        `<circle cx="44.5" cy="33.5" r="1.9" fill="#2a1a12"/><circle cx="55.5" cy="33.5" r="1.9" fill="#2a1a12"/>` +
        `<path d="M40 29.5 L47 27.5 M60 29.5 L53 27.5" stroke="#5a3a22" stroke-width="1.8" stroke-linecap="round"/>` +
        `<ellipse cx="50" cy="41.5" rx="2.4" ry="3" fill="#6a2a1e"/>`;
    const mascaraGas = () =>
        `<path d="M37 30 Q50 26 63 30 Q63 45 50 50 Q37 45 37 30 Z" fill="#2c2f2b" stroke="${L}" stroke-width="2"/>` +
        `<circle cx="50" cy="46" r="4.6" fill="#454a43" stroke="${L}" stroke-width="1.4"/>` + olhosVermelhos(35);
    const mascaraBranca = () =>
        `<path d="M39 29 Q50 25 61 29 Q61 45 50 49 Q39 45 39 29 Z" fill="#e8e2d4" stroke="${L}" stroke-width="1.8"/>` +
        `<path d="M44 42 Q50 45 56 42" stroke="#6a5f52" stroke-width="1.4" fill="none"/>`;

    function cabeca(d) {
        if (d.chapeu === 'capuz') {   // rosto escondido dentro do capuz
            return `<path d="M29 53 Q26 26 50 6 Q74 26 71 53 Q63 42 50 42 Q37 42 29 53 Z" fill="${d.corChapeu}" stroke="${L}" stroke-width="2.2"/>` +
                `<ellipse cx="50" cy="39" rx="11.5" ry="11" fill="#130c08"/>` +
                (d.mascara === 'branca' ? mascaraBranca() + olhosVermelhos(35) : olhosVermelhos(37));
        }
        let c = '';
        if (d.chapeu === 'cabelo') c += CHAPEUS.cabeloTras(d.corChapeu);
        c += `<circle cx="50" cy="34" r="14.5" fill="${d.pele}" stroke="${L}" stroke-width="2.4"/>`;
        if (d.barba) c += `<path d="M37 36 Q38 52 50 54 Q62 52 63 36 Q57 45 50 45 Q43 45 37 36 Z" fill="${d.barba}" stroke="${L}" stroke-width="1.6"/>`;
        if (d.refem) c += rostoAssustado();
        else if (d.mascara === 'gas') c += mascaraGas();
        else if (d.mascara === 'branca') c += mascaraBranca() + olhosVermelhos(34);
        else c += rostoMau();
        if (d.chapeu === 'cabelo') c += CHAPEUS.cabeloFrente(d.corChapeu);
        else if (d.chapeu && CHAPEUS[d.chapeu]) c += CHAPEUS[d.chapeu](d.corChapeu);
        return c;
    }

    /** Monta o SVG de um alvo a partir da sua definição. */
    function svg(d) {
        const forte = d.classe === 'forte';
        const p = [];
        // haste e base sobre o trilho (decoração)
        p.push(`<rect x="46" y="170" width="8" height="30" fill="#3a2716"/>`);
        p.push(`<rect x="28" y="166" width="44" height="8" rx="2" fill="#4a3220" stroke="${L}" stroke-width="2"/>`);
        if (d.objeto && OBJETOS[d.objeto]) p.push(`<g class="objeto">${OBJETOS[d.objeto]()}</g>`);

        p.push('<g data-hit>');
        p.push(`<rect x="35" y="122" width="13" height="44" rx="3" fill="${d.calca}" stroke="${L}" stroke-width="2.4"/>`);
        p.push(`<rect x="52" y="122" width="13" height="44" rx="3" fill="${d.calca}" stroke="${L}" stroke-width="2.4"/>`);
        p.push(`<rect x="32" y="158" width="17" height="10" rx="3" fill="#2a1d14" stroke="${L}" stroke-width="2"/>`);
        p.push(`<rect x="51" y="158" width="17" height="10" rx="3" fill="#2a1d14" stroke="${L}" stroke-width="2"/>`);
        if (d.saia) {
            const fim = d.longa ? 164 : 150;
            p.push(forte
                ? `<path d="M25 108 L75 108 L84 ${fim} L16 ${fim} Z" fill="${d.saia}" stroke="${L}" stroke-width="2.4"/>`
                : `<path d="M31 108 L69 108 L78 ${fim} L22 ${fim} Z" fill="${d.saia}" stroke="${L}" stroke-width="2.4"/>`);
        }
        p.push(`<rect x="45" y="44" width="10" height="13" fill="${d.pele}" stroke="${L}" stroke-width="2"/>`);
        p.push(`<path d="${forte ? 'M20 69 Q22 54 50 53 Q78 54 80 69 L75 130 L25 130 Z' : 'M27 66 Q28 56 50 55 Q72 56 73 66 L69 128 L31 128 Z'}" fill="${d.roupa}" stroke="${L}" stroke-width="2.4"/>`);
        if (d.colete) {
            p.push(forte
                ? `<path d="M24 66 L45 57 L46 128 L27 128 Z M76 66 L55 57 L54 128 L73 128 Z" fill="${d.colete}" stroke="${L}" stroke-width="1.8"/>`
                : `<path d="M31 64 L45 58 L46 126 L32 126 Z M69 64 L55 58 L54 126 L68 126 Z" fill="${d.colete}" stroke="${L}" stroke-width="1.8"/>`);
        }
        if (!d.longa) p.push(`<rect x="${forte ? 25 : 31}" y="113" width="${forte ? 50 : 38}" height="6" fill="#2a1d14"/>`);

        const w = forte ? 13 : 10.5;
        if (d.refem) {
            // mãos amarradas na frente e placa "REFÉM" (atirar na placa também acerta a refém)
            p.push(membro(31, 66, 45, 98, d.roupa, w) + membro(69, 66, 55, 98, d.roupa, w));
            p.push(mao(46, 101, d.pele) + mao(54, 101, d.pele));
            p.push(`<rect x="40" y="97" width="20" height="6" rx="2" fill="#b08a55" stroke="${L}" stroke-width="1.6"/>`);
            p.push(linha(40, 72, 46, 55, '#6b4a2b', 1.4) + linha(60, 72, 54, 55, '#6b4a2b', 1.4));
            p.push(`<rect x="29" y="70" width="42" height="18" rx="2" fill="#f4efe2" stroke="${L}" stroke-width="2"/>`);
            p.push(`<text x="50" y="83.2" text-anchor="middle" font-family="Oswald, Impact, sans-serif" font-size="10.5" font-weight="700" fill="#b3261e">REFÉM</text>`);
        } else {
            const ombro = forte ? 24 : 31;
            p.push(membro(ombro, 68, ombro - 9, 112, d.roupa, w) + mao(ombro - 10, 116, d.pele));
            p.push(membro(100 - ombro, 68, 84, 45, d.roupa, w));
            p.push(d.objeto === 'gancho'
                ? `<path d="M84 46 Q97 44 94 32 Q91 25 85 29" stroke="#b9bec4" stroke-width="3.4" fill="none" stroke-linecap="round"/>`
                : mao(85, 42, d.pele));
        }
        p.push('</g>');
        p.push(`<g data-hit data-parte="cabeca">${cabeca(d)}</g>`);
        if (d.escudo) {
            p.push(`<g data-parte="escudo"><rect x="5" y="62" width="33" height="66" rx="7" fill="${d.escudo}" stroke="${L}" stroke-width="2.6"/>` +
                `<rect x="10" y="68" width="23" height="54" rx="4" fill="none" stroke="rgba(255,255,255,.2)" stroke-width="2"/>` +
                `<circle cx="21.5" cy="95" r="4" fill="#2a2d33"/></g>`);
        }
        return `<svg viewBox="0 0 100 200" aria-hidden="true">${p.join('')}</svg>`;
    }

    // ---- grupos de inimigos por tipo de estande (comum: 1 tiro; ágil: rápido; forte: 4 de vida) ----
    const TEMAS = {
        VILA_RURAL: [
            { classe: 'comum', pele: '#b6a18a', roupa: '#6b4a2b', calca: '#3b3a33', chapeu: 'boina', corChapeu: '#3d3a30', objeto: 'forcado' },
            { classe: 'comum', pele: '#b19b84', roupa: '#7a6a4f', calca: '#3a2f25', chapeu: 'boina', corChapeu: '#5a4b3a', objeto: 'tocha' },
            { classe: 'agil', pele: '#c0a98e', roupa: '#4d5a34', calca: '#2e2a24', chapeu: 'lenco', corChapeu: '#7b5e3c', saia: '#3f4a2c', objeto: 'foice' },
            { classe: 'forte', pele: '#a68f78', roupa: '#8a7a5c', calca: '#2f3b45', colete: '#3a2a1c', barba: '#4a3424', objeto: 'machado' }
        ],
        CASTELO: [
            { classe: 'comum', pele: '#9d8f86', roupa: '#3b2350', calca: '#24182f', chapeu: 'capuz', corChapeu: '#3b2350', saia: '#3b2350', longa: true, objeto: 'cajado' },
            { classe: 'agil', pele: '#9d8f86', roupa: '#5a1a1a', calca: '#2a1010', chapeu: 'capuz', corChapeu: '#5a1a1a', saia: '#5a1a1a', longa: true, objeto: 'foice' },
            { classe: 'forte', pele: '#9d8f86', roupa: '#2a2530', calca: '#1d1a20', chapeu: 'capuz', corChapeu: '#1d1a20', saia: '#2a2530', longa: true, mascara: 'branca', escudo: '#6b6f78', objeto: 'cassetete' }
        ],
        BASE_MILITAR: [
            { classe: 'comum', pele: '#b39a84', roupa: '#4b5a3f', calca: '#3a4232', chapeu: 'capacete', corChapeu: '#3c4a36', mascara: 'gas', objeto: 'cassetete' },
            { classe: 'agil', pele: '#b39a84', roupa: '#55603f', calca: '#353d2c', chapeu: 'capacete', corChapeu: '#46523d', mascara: 'gas', objeto: 'faca' },
            { classe: 'forte', pele: '#b39a84', roupa: '#3a3f44', calca: '#2b2f33', chapeu: 'capacete', corChapeu: '#2f3439', mascara: 'gas', escudo: '#4d5866', objeto: 'marreta' }
        ],
        MINA: [
            { classe: 'comum', pele: '#a99582', roupa: '#6d5a3a', calca: '#3b3a40', chapeu: 'mineiro', corChapeu: '#c99a2e', objeto: 'picareta' },
            { classe: 'agil', pele: '#a99582', roupa: '#5a4a3a', calca: '#34333a', chapeu: 'mineiro', corChapeu: '#b88a2a', objeto: 'dinamite' },
            { classe: 'forte', pele: '#9c8875', roupa: '#4a3a2a', calca: '#2c2b30', barba: '#3a2a1c', colete: '#2a1f16', objeto: 'marreta' }
        ],
        PORTO: [
            { classe: 'comum', pele: '#b8a088', roupa: 'url(#listras-vermelhas)', calca: '#2a2f3a', chapeu: 'bandana', corChapeu: '#8a1f1f', objeto: 'sabre' },
            { classe: 'agil', pele: '#b8a088', roupa: 'url(#listras-azuis)', calca: '#2f2a24', chapeu: 'bandana', corChapeu: '#1f3a6a', objeto: 'gancho' },
            { classe: 'forte', pele: '#a8917a', roupa: '#3a1f1f', calca: '#1f1a16', chapeu: 'tricornio', corChapeu: '#1e1a16', barba: '#2a1d14', objeto: 'sabre' }
        ]
    };

    const REFEM = {
        classe: 'refem', refem: true, pele: '#e6c29f', roupa: '#eef1f3', calca: '#e6c29f',
        chapeu: 'cabelo', corChapeu: '#5a3a22', saia: '#3f5f8a'
    };

    /** Padrões de listras usados pelas roupas dos piratas (definidos uma vez na página). */
    function garantirDefs() {
        if (document.getElementById('alvos-defs')) return;
        const box = document.createElement('div');
        box.innerHTML = '<svg id="alvos-defs" width="0" height="0" style="position:absolute" aria-hidden="true"><defs>' +
            '<pattern id="listras-vermelhas" width="8" height="8" patternUnits="userSpaceOnUse"><rect width="8" height="8" fill="#e8e1cf"/><rect width="8" height="3.6" fill="#8a2323"/></pattern>' +
            '<pattern id="listras-azuis" width="8" height="8" patternUnits="userSpaceOnUse"><rect width="8" height="8" fill="#e8e1cf"/><rect width="8" height="3.6" fill="#24406e"/></pattern>' +
            '</defs></svg>';
        document.body.appendChild(box.firstElementChild);
    }

    return {
        svg,
        garantirDefs,
        inimigos: tipo => TEMAS[tipo] || TEMAS.VILA_RURAL,
        refem: REFEM
    };
})();
