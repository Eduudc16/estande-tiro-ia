/* Estande Sombrio — minigame do estande de tiro.
   Fundo fixo = cenário gerado por IA. Parte dinâmica = alvos (inimigos do tema e a refém) que surgem e somem. */
(() => {
    'use strict';
    const cfg = window.JOGO;
    const palco = document.querySelector('[data-jogo-palco]');
    if (!cfg || !palco || !window.Alvos) return;
    Alvos.garantirDefs();

    // ------------------------------------------------------------ regras
    const DURACAO_MS = 60000;
    const CLASSES = {
        comum: { vida: 1, pontos: 100, velocidade: 1 },
        agil: { vida: 1, pontos: 150, velocidade: 1.7 },
        forte: { vida: 4, pontos: 300, velocidade: 0.65 },
        refem: { vida: 1, pontos: 0, velocidade: 0.9 }
    };
    // trilhos: base (% a partir de baixo), escala do alvo e camada (o mais perto fica na frente)
    const TRILHOS = [
        { base: 45, escala: 0.6, z: 1 },
        { base: 35, escala: 0.8, z: 2 },
        { base: 24, escala: 1, z: 3 }
    ];
    const ALTURA_ALVO = 34;                       // % da altura do palco com escala 1 (igual ao CSS)
    const NOTAS = [[6000, 'S'], [4500, 'A'], [3000, 'B'], [1500, 'C'], [0, 'D']];
    const ESPALHAMENTO = { ESCOPETA: 0.045, SUBMETRALHADORA: 0.014 };   // raio, em fração da largura
    const RAIO_EXPLOSAO = 0.11;
    const BONUS_REFEM = 200;
    const ARMAS = (cfg.armas || []).slice(0, 9);
    // desenhos de reserva do tipo cujos inimigos o jogo usa (pode ser outro tipo, se este ainda não tem imagens)
    const DESENHOS = Alvos.inimigos(cfg.tipoAlvos || cfg.estande.tipo);
    // inimigos do banco: a imagem gerada pela IA quando existe; senão, o desenho SVG de reserva
    const INIMIGOS = cfg.inimigos && cfg.inimigos.length
        ? cfg.inimigos.map(i => i.imagem
            ? { classe: i.classe, imagem: i.imagem, nome: i.nome }
            : Object.assign({}, DESENHOS[i.indice] || DESENHOS[0], { classe: i.classe }))
        : DESENHOS;
    // a refém: imagem gerada pela IA para este tipo, ou o desenho padrão
    const REFEM = cfg.refem ? { classe: 'refem', refem: true, imagem: cfg.refem } : Alvos.refem;
    const MASCARAS = new Map();                   // imagem -> transparência e posição da cabeça
    const CHAVE_RECORDE = 'estande-sombrio:recorde:' + cfg.estande.id;

    // ------------------------------------------------------------ elementos
    const q = s => palco.querySelector(s);
    const mundo = q('.jogo-mundo'), camada = q('.jogo-alvos'), efeitos = q('.jogo-efeitos');
    const mira = q('.jogo-mira');
    const hud = {
        tempo: q('[data-hud-tempo]'), pontos: q('[data-hud-pontos]'), arma: q('[data-hud-arma]'),
        municao: q('[data-hud-municao]'), recarga: q('[data-hud-recarga]'), aviso: q('[data-hud-aviso]'),
        zoom: q('[data-zoom]')
    };
    const telaInicio = q('[data-tela-inicio]'), telaFim = q('[data-tela-fim]');
    const barra = document.querySelector('[data-armas]');
    const botaoSom = document.querySelector('[data-som]');
    const botaoMusica = document.querySelector('[data-musica]');

    // ------------------------------------------------------------ estado
    let rodando = false, inicio = 0, ultimoQuadro = 0, proximoSurgimento = 0, pausadoEm = 0;
    let alvos = [];
    let armaIdx = 0, recarga = null;                    // recarga = { idx, inicio, fim }
    const SAQUE_MS = 200;                               // tempo para sacar a arma depois de trocar
    let trocouEm = -1e9;
    const ultimosTiros = ARMAS.map(() => -1e9);         // a cadência vale para cada arma
    const pente = ARMAS.map(a => a.capacidade);
    let zoom = false, shift = false, gatilho = false, rajada = 0, ultimaRoda = 0, avisoTimer = 0;
    const ponteiro = { x: 0, y: 0, dentro: false };
    let st = novaPartida();
    let ultimoSegundo = 0, acelerou = false;           // contagem final e música mais rápida

    function novaPartida() {
        return { pontos: 0, tiros: 0, acertos: 0, abatidos: 0, cabeca: 0, refens: 0 };
    }
    function ler(chave, padrao) {
        try { const v = localStorage.getItem(chave); return v === null ? padrao : v; } catch (e) { return padrao; }
    }
    function gravar(chave, valor) {
        try { localStorage.setItem(chave, valor); } catch (e) { /* sem armazenamento: tudo bem */ }
    }
    const lerp = (a, b, t) => a + (b - a) * Math.min(1, Math.max(0, t));
    const sortear = lista => lista[Math.floor(Math.random() * lista.length)];
    const esc = s => String(s == null ? '' : s).replace(/[&<>"']/g,
        c => ({ '&': '&amp;', '<': '&lt;', '>': '&gt;', '"': '&quot;', "'": '&#39;' }[c]));
    const armaAtual = () => ARMAS[armaIdx];
    const vivo = a => a.estado === 'subindo' || a.estado === 'ativo';

    // ------------------------------------------------------------ barra de armas
    const ICONES = {
        PISTOLA: '<path d="M14 6h36v6H31l-3 10h-8l2-10h-8z"/>',
        REVOLVER: '<path d="M8 8h46v4H33l-2 3h-5l-4 8h-8l4-9H8z"/><circle cx="28" cy="11" r="4.5"/>',
        ESCOPETA: '<path d="M2 11l12-3h48v4H34v3h-8v-2H16l-4 7H4l4-7H2z"/>',
        RIFLE: '<path d="M2 12l12-3h48v3H36l-2 4h-6v-2H16l-4 6H4l4-6H2z"/><rect x="24" y="2" width="16" height="4" rx="2"/>',
        SUBMETRALHADORA: '<path d="M12 7h42v5H39v3h-6v-3h-7l-3 9h-7l2-9h-4z"/><rect x="30" y="14" width="5" height="9" rx="1"/>',
        LANCA_GRANADAS: '<path d="M4 7h54v9H4z"/><circle cx="24" cy="17" r="5"/><path d="M16 15h-6l-3 8h6z"/>'
    };
    const icone = tipo => `<svg viewBox="0 0 64 24" aria-hidden="true">${ICONES[tipo] || ICONES.PISTOLA}</svg>`;
    const stat = (nome, valor) =>
        `<span class="stat"><small>${nome}</small><i style="--v:${Math.round(Math.min(1, Math.max(0.06, valor)) * 100)}%"></i></span>`;

    function montarBarra() {
        if (!barra) return;
        barra.innerHTML = ARMAS.map((a, i) => `
            <button type="button" class="arma-card" data-arma="${i}">
                <span class="arma-tecla">${i + 1}</span>
                <span class="arma-icone">${icone(a.tipo)}</span>
                <span class="arma-nome">${esc(a.nome)}<small>${esc(a.tipoRotulo)}</small></span>
                <span class="arma-diferencial">${esc(a.diferencial)}</span>
                <span class="arma-stats">${stat('Dano', a.dano / 10)}${stat('Cadência', 1 - (a.cadenciaMs - 50) / 2950)}${stat('Pente', a.capacidade / 40)}${stat('Recarga', 1 - (a.recargaMs - 300) / 5700)}</span>
            </button>`).join('');
        barra.addEventListener('click', ev => {
            const b = ev.target.closest('[data-arma]');
            if (b) trocarArma(Number(b.dataset.arma));
        });
    }

    function atualizarHudArma() {
        const a = armaAtual();
        if (!a) return;
        hud.arma.textContent = a.nome;
        hud.municao.textContent = `${pente[armaIdx]} / ${a.capacidade}`;
        hud.municao.classList.toggle('vazio', pente[armaIdx] === 0);
        hud.zoom.hidden = a.tipo !== 'RIFLE';
        if (barra) {
            barra.querySelectorAll('[data-arma]').forEach(b =>
                b.classList.toggle('ativa', Number(b.dataset.arma) === armaIdx));
        }
        atualizarMira();
    }

    // ------------------------------------------------------------ mira e zoom
    function raioMira() {
        const larg = palco.clientWidth, a = armaAtual();
        if (!a) return 12;
        if (a.tipo === 'LANCA_GRANADAS') return RAIO_EXPLOSAO * larg;
        if (ESPALHAMENTO[a.tipo]) return ESPALHAMENTO[a.tipo] * larg + 4;
        return a.tipo === 'RIFLE' ? 9 : 13;
    }
    function atualizarMira() {
        palco.style.setProperty('--raio-mira', raioMira().toFixed(1) + 'px');
        palco.style.setProperty('--raio-luneta', (palco.clientHeight * 0.36).toFixed(0) + 'px');
        const a = armaAtual();
        mira.dataset.tipo = a ? a.tipo : '';
    }
    function atualizarPonteiro(ev) {
        ponteiro.x = ev.clientX;
        ponteiro.y = ev.clientY;
        ponteiro.dentro = true;
        const r = palco.getBoundingClientRect();
        const x = ev.clientX - r.left, y = ev.clientY - r.top;
        palco.style.setProperty('--mx', x + 'px');
        palco.style.setProperty('--my', y + 'px');
        mira.hidden = !rodando;
        if (zoom) mundo.style.transformOrigin = `${x}px ${y}px`;
    }
    function definirZoom(ativo) {
        const a = armaAtual();
        zoom = !!ativo && rodando && !!a && a.tipo === 'RIFLE';
        if (zoom) {
            const r = palco.getBoundingClientRect();
            mundo.style.transformOrigin = `${ponteiro.x - r.left}px ${ponteiro.y - r.top}px`;
        }
        palco.classList.toggle('mirando', zoom);
        hud.zoom.classList.toggle('ativo', zoom);
    }

    // ------------------------------------------------------------ alvos
    function lugarLivre(trilho, x) {
        return alvos.every(a => a.trilho !== trilho || !vivo(a) || Math.abs(a.x - x) > 10);
    }

    function surgir(progresso, agora) {
        if (alvos.filter(vivo).length >= 7) return;
        const sorte = Math.random();
        if (sorte < 0.16) {
            // a refém, muitas vezes com um inimigo colado nela
            const refem = criarAlvo(REFEM, { trilho: sortear([0, 1, 2]) }, progresso, agora);
            if (refem && Math.random() < 0.65) {
                criarAlvo(sortear(INIMIGOS), {
                    trilho: Math.max(0, refem.trilho - 1), x: refem.x + sortear([-7, 7]), forcar: true
                }, progresso, agora);
            }
        } else if (sorte < 0.32) {
            // fila de inimigos deslizando no mesmo trilho
            const trilho = sortear([0, 1, 2]), dir = sortear([-1, 1]);
            const n = Math.random() < 0.4 ? 3 : 2;
            const naoFortes = INIMIGOS.filter(d => d.classe !== 'forte');
            const leves = naoFortes.length ? naoFortes : INIMIGOS;
            for (let i = 0; i < n; i++) {
                criarAlvo(sortear(leves), { trilho, dir, x: dir > 0 ? 8 - i * 11 : 92 + i * 11, forcar: true },
                    progresso, agora);
            }
        } else {
            const dir = Math.random() < 0.35 ? sortear([-1, 1]) : 0;
            criarAlvo(sortear(INIMIGOS), { trilho: sortear([0, 1, 2]), dir }, progresso, agora);
        }
    }

    function criarAlvo(def, o, progresso, agora) {
        const classe = CLASSES[def.classe] || CLASSES.comum;
        const trilho = TRILHOS[o.trilho];
        let x = o.x;
        if (x == null) {
            x = o.dir ? (o.dir > 0 ? 8 : 92) : 20 + Math.random() * 60;
            let tentativas = 0;
            while (!lugarLivre(o.trilho, x) && !o.dir && tentativas++ < 6) x = 20 + Math.random() * 60;
            if (!lugarLivre(o.trilho, x)) return null;
        }
        const el = document.createElement('div');
        el.className = 'alvo' + (def.refem ? ' refem' : '') + (def.classe === 'forte' ? ' forte' : '')
            + (def.imagem ? ' imagem' : '');
        el.style.setProperty('--x', x.toFixed(2));
        el.style.setProperty('--base', trilho.base);
        el.style.setProperty('--escala', trilho.escala);
        el.style.zIndex = String(trilho.z);
        const figura = def.imagem ? `<img src="${esc(def.imagem)}" alt="" draggable="false">` : Alvos.svg(def);
        const placa = def.refem && def.imagem ? '<span class="placa-refem">REFÉM</span>' : '';   // o desenho já tem a placa
        el.innerHTML = figura + placa + (classe.vida > 1 ? '<span class="vida"><i></i></span>' : '');
        camada.appendChild(el);
        void el.offsetWidth;                      // aplica o estado "deitado" antes de animar a subida
        el.classList.add('de-pe');
        const vida = def.refem ? 2600 : lerp(2400, 1400, progresso) / Math.sqrt(classe.velocidade);
        const alvo = {
            el, def, refem: !!def.refem, x, trilho: o.trilho, vida: classe.vida, vidaMax: classe.vida,
            pontos: classe.pontos, estado: 'subindo', nasceu: agora,
            vx: o.dir ? o.dir * lerp(10, 17, progresso) * classe.velocidade : 0,
            expira: agora + (o.dir ? 25000 : vida)
        };
        el._alvo = alvo;
        alvos.push(alvo);
        return alvo;
    }

    function atualizarAlvos(dt, agora) {
        for (const a of alvos) {
            if (!vivo(a)) continue;
            if (a.estado === 'subindo' && agora - a.nasceu > 180) a.estado = 'ativo';
            if (a.vx) {
                a.x += a.vx * dt;
                a.el.style.setProperty('--x', a.x.toFixed(2));
                if ((a.vx > 0 && a.x > 97) || (a.vx < 0 && a.x < 3)) { descer(a, 0); continue; }
            }
            if (agora >= a.expira) descer(a, 220);
        }
        alvos = alvos.filter(a => a.estado !== 'removido');
    }

    function descer(a, atraso) {
        a.estado = 'descendo';
        a.el.classList.remove('de-pe');
        if (a.refem && rodando) {
            st.refens++;
            somarPontos(BONUS_REFEM, a, 'Refém salva!');
            Som.efeito('refemSalva');
        }
        remover(a, atraso);
    }
    function remover(a, atraso) {
        setTimeout(() => { a.el.remove(); a.estado = 'removido'; }, atraso);
    }

    // ------------------------------------------------------------ tiros
    function disparar() {
        const arma = armaAtual();
        const agora = performance.now();
        if (!rodando || !arma) return;
        atualizarRecarga(agora);
        if (recarga) { avisar('Recarregando…'); return; }
        if (agora - trocouEm < SAQUE_MS || agora - ultimosTiros[armaIdx] < arma.cadenciaMs) return;
        if (pente[armaIdx] <= 0) { Som.efeito('vazio'); recarregar(); return; }
        ultimosTiros[armaIdx] = agora;
        pente[armaIdx]--;
        st.tiros++;
        Som.tiro(arma.tipo);

        const { x, y } = ponteiro;
        const larg = palco.clientWidth;
        let acertou = false;
        if (arma.tipo === 'ESCOPETA') {
            const raio = ESPALHAMENTO.ESCOPETA * larg;
            for (let i = 0; i < 6 && rodando; i++) {
                const ang = Math.random() * Math.PI * 2, r = raio * Math.sqrt(Math.random());
                if (tiroNoPonto(x + Math.cos(ang) * r, y + Math.sin(ang) * r, arma, false)) acertou = true;
            }
        } else if (arma.tipo === 'SUBMETRALHADORA') {
            const raio = ESPALHAMENTO.SUBMETRALHADORA * larg;
            acertou = tiroNoPonto(x + (Math.random() * 2 - 1) * raio, y + (Math.random() * 2 - 1) * raio, arma, false);
        } else if (arma.tipo === 'LANCA_GRANADAS') {
            acertou = explodir(x, y, arma);
        } else {
            acertou = tiroNoPonto(x, y, arma, arma.tipo === 'REVOLVER');
        }
        if (acertou) st.acertos++;
        atualizarHudArma();
        if (rodando && pente[armaIdx] === 0) recarregar();
    }

    /** Acha o que foi atingido no ponto (só as partes pintadas do recorte contam). */
    function tiroNoPonto(cx, cy, arma, perfura) {
        marcar('impacto', cx, cy);
        const partes = new Map();                 // alvo -> parte atingida (corpo, cabeca ou escudo)
        for (const el of document.elementsFromPoint(cx, cy)) {
            const caixa = el.closest ? el.closest('.alvo') : null;
            const a = caixa && caixa._alvo;
            if (!a || !vivo(a)) continue;
            let parte;
            if (el.tagName === 'IMG') {
                parte = parteDaImagem(el, cx, cy);
                if (!parte) continue;                 // parte transparente da imagem: o tiro passa direto
            } else {
                const marca = el.closest('[data-parte]');
                parte = marca ? marca.dataset.parte : 'corpo';
            }
            if (!partes.has(a)) {
                if (!perfura && partes.size) break;   // tiro comum para no primeiro alvo
                partes.set(a, parte);
            } else if (perfura && partes.get(a) === 'escudo' && parte !== 'escudo') {
                partes.set(a, parte);                 // o revólver atravessa o escudo
            }
        }
        let acertou = false;
        for (const [a, parte] of partes) {
            if (parte === 'escudo') {
                if (!perfura) { marcar('faisca', cx, cy); Som.efeito('escudo'); avisar('Bloqueado pelo escudo'); return false; }
                continue;
            }
            const cabeca = parte === 'cabeca';
            const dano = arma.dano * (cabeca ? (arma.tipo === 'RIFLE' ? 3 : 2) : 1);
            if (atingir(a, dano, cabeca, arma)) acertou = true;
            if (!rodando) break;
        }
        return acertou;
    }

    // ------------------------------------------------------------ alvos com imagem da IA
    /** Carrega as imagens antes da partida e guarda a transparência de cada uma (para o acerto no pixel). */
    function prepararImagens() {
        const urls = [...new Set(INIMIGOS.concat([REFEM]).filter(d => d.imagem).map(d => d.imagem))];
        return Promise.all(urls.map(url => new Promise(pronto => {
            const img = new Image();
            img.onload = () => {
                try { MASCARAS.set(url, mascara(img)); } catch (e) { /* sem máscara: vale a caixa inteira */ }
                pronto();
            };
            img.onerror = () => pronto();
            img.src = url;
        })));
    }

    /** Transparência da imagem + a cabeça (centro da faixa mais alta do recorte). */
    function mascara(img) {
        const w = img.naturalWidth, h = img.naturalHeight;
        const canvas = document.createElement('canvas');
        canvas.width = w;
        canvas.height = h;
        const ctx = canvas.getContext('2d', { willReadFrequently: true });
        ctx.drawImage(img, 0, 0);
        const dados = ctx.getImageData(0, 0, w, h).data;
        const alfa = new Uint8Array(w * h);
        for (let i = 0; i < alfa.length; i++) alfa[i] = dados[i * 4 + 3];
        const opaca = y => { for (let x = 0; x < w; x++) if (alfa[y * w + x] > 128) return true; return false; };
        let topo = 0;
        while (topo < h && !opaca(topo)) topo++;
        let soma = 0, n = 0;
        for (let y = topo; y < Math.min(h, topo + Math.round(h * 0.12)); y++) {
            for (let x = 0; x < w; x++) if (alfa[y * w + x] > 128) { soma += x; n++; }
        }
        const centro = n ? soma / n / w : 0.5;
        const meia = (h * 0.07) / w;
        return { w, h, alfa, cabeca: { x0: centro - meia, x1: centro + meia, y1: (topo + h * 0.17) / h } };
    }

    /** 'cabeca', 'corpo' ou null (parte transparente) no ponto da tela. */
    function parteDaImagem(img, cx, cy) {
        const r = img.getBoundingClientRect();
        const u = (cx - r.left) / r.width, v = (cy - r.top) / r.height;
        if (u < 0 || u > 1 || v < 0 || v > 1) return null;
        const m = MASCARAS.get(img.getAttribute('src'));
        if (!m) return 'corpo';
        const x = Math.min(m.w - 1, Math.floor(u * m.w)), y = Math.min(m.h - 1, Math.floor(v * m.h));
        if (m.alfa[y * m.w + x] < 60) return null;
        return v <= m.cabeca.y1 && u >= m.cabeca.x0 && u <= m.cabeca.x1 ? 'cabeca' : 'corpo';
    }

    function explodir(cx, cy, arma) {
        const p = noMundo(cx, cy);
        const ex = document.createElement('span');
        ex.className = 'explosao';
        ex.style.left = p.x + '%';
        ex.style.top = p.y + '%';
        ex.style.setProperty('--r', (RAIO_EXPLOSAO * 100) + '%');
        efeitos.appendChild(ex);
        setTimeout(() => ex.remove(), 650);
        tremer();
        Som.efeito('explosao');
        const raio = RAIO_EXPLOSAO * palco.clientWidth;
        let acertou = false;
        for (const a of alvos.slice()) {
            if (!vivo(a)) continue;
            const r = a.el.getBoundingClientRect();
            const dx = Math.max(r.left - cx, 0, cx - r.right), dy = Math.max(r.top - cy, 0, cy - r.bottom);
            if (Math.hypot(dx, dy) <= raio) {
                if (atingir(a, arma.dano, false, arma)) acertou = true;
                if (!rodando) break;
            }
        }
        return acertou;
    }

    function atingir(a, dano, cabeca, arma) {
        if (a.refem) { derrota(a); return false; }
        a.vida -= dano;
        if (cabeca) st.cabeca++;
        a.el.classList.remove('atingido');
        void a.el.offsetWidth;
        a.el.classList.add('atingido');
        Som.efeito(cabeca ? 'cabeca' : 'acerto');
        if (a.vida > 0) {
            const barraVida = a.el.querySelector('.vida i');
            if (barraVida) barraVida.style.width = Math.max(0, a.vida / a.vidaMax * 100) + '%';
            if (cabeca) textoVoando(a, 'Na cabeça!', false);
            return true;
        }
        a.estado = 'caido';
        a.el.classList.add('caido');
        Som.efeito('queda');
        st.abatidos++;
        const mult = cabeca ? (arma.tipo === 'RIFLE' ? 2 : 1.5) : 1;
        somarPontos(Math.round(a.pontos * mult), a, cabeca ? 'Na cabeça!' : '');
        remover(a, 420);
        return true;
    }

    function derrota(a) {
        a.estado = 'caido';
        a.el.classList.add('caido', 'refem-atingida');
        Som.efeito('refemAtingida');
        palco.classList.add('alerta-refem');
        terminar(true);
    }

    // ------------------------------------------------------------ recarga e troca de arma
    function recarregar() {
        const a = armaAtual();
        if (!rodando || !a || recarga || pente[armaIdx] === a.capacidade) return;
        const agora = performance.now();
        recarga = { idx: armaIdx, inicio: agora, fim: agora + a.recargaMs };
        palco.classList.add('recarregando');
        Som.recarga(a.tipo, a.recargaMs);
        avisar('Recarregando…', a.recargaMs);
    }
    function atualizarRecarga(agora) {
        if (!recarga) { hud.recarga.style.width = '0%'; return; }
        hud.recarga.style.width = Math.min(100, (agora - recarga.inicio) / (recarga.fim - recarga.inicio) * 100) + '%';
        if (agora >= recarga.fim) {
            pente[recarga.idx] = ARMAS[recarga.idx].capacidade;
            recarga = null;
            palco.classList.remove('recarregando');
            atualizarHudArma();
        }
    }
    function trocarArma(i) {
        if (i < 0 || i >= ARMAS.length || i === armaIdx) return;
        armaIdx = i;
        trocouEm = performance.now();
        if (recarga) { recarga = null; palco.classList.remove('recarregando'); Som.cancelarRecarga(); }
        pararRajada();
        definirZoom(shift);
        atualizarHudArma();
        Som.efeito('troca');
        if (rodando && pente[armaIdx] === 0) recarregar();
    }
    function iniciarRajada() {
        pararRajada();
        rajada = setInterval(() => {
            if (gatilho && rodando) disparar(); else pararRajada();
        }, Math.max(40, armaAtual().cadenciaMs));
    }
    function pararRajada() {
        if (rajada) { clearInterval(rajada); rajada = 0; }
    }

    // ------------------------------------------------------------ efeitos visuais
    function noMundo(cx, cy) {
        const r = mundo.getBoundingClientRect();
        return { x: (cx - r.left) / r.width * 100, y: (cy - r.top) / r.height * 100 };
    }
    function marcar(classe, cx, cy) {
        const p = noMundo(cx, cy);
        const s = document.createElement('span');
        s.className = classe;
        s.style.left = p.x + '%';
        s.style.top = p.y + '%';
        efeitos.appendChild(s);
        setTimeout(() => s.remove(), 1200);
    }
    function somarPontos(valor, a, rotulo) {
        st.pontos += valor;
        hud.pontos.textContent = st.pontos;
        textoVoando(a, `+${valor}${rotulo ? ' ' + rotulo : ''}`, valor >= 200);
    }
    function textoVoando(a, texto, destaque) {
        const t = TRILHOS[a.trilho];
        const s = document.createElement('span');
        s.className = 'pontos-voando' + (destaque ? ' destaque' : '');
        s.textContent = texto;
        s.style.left = a.x + '%';
        s.style.bottom = (t.base + ALTURA_ALVO * t.escala) + '%';
        efeitos.appendChild(s);
        setTimeout(() => s.remove(), 900);
    }
    function avisar(texto, duracao = 900) {
        hud.aviso.textContent = texto;
        hud.aviso.classList.add('visivel');
        clearTimeout(avisoTimer);
        avisoTimer = setTimeout(() => hud.aviso.classList.remove('visivel'), duracao);
    }
    function tremer() {
        palco.classList.remove('tremor');
        void palco.offsetWidth;
        palco.classList.add('tremor');
    }

    // ------------------------------------------------------------ partida
    function quadro(agora) {
        if (!rodando) return;
        if (pausadoEm) { requestAnimationFrame(quadro); return; }
        const dt = Math.min(0.05, Math.max(0, (agora - ultimoQuadro) / 1000));
        ultimoQuadro = agora;
        const passou = agora - inicio;
        const resta = Math.max(0, DURACAO_MS - passou);
        hud.tempo.textContent = (resta / 1000).toFixed(2);
        hud.tempo.classList.toggle('acabando', resta < 10000);
        if (resta <= 0) { terminar(false); return; }
        if (resta < 10000) {                      // últimos 10 s: relógio tiquetaqueando e música mais rápida
            const segundo = Math.ceil(resta / 1000);
            if (segundo !== ultimoSegundo) { ultimoSegundo = segundo; Som.efeito(segundo <= 3 ? 'tiqueFinal' : 'tique'); }
            if (!acelerou) { acelerou = true; Som.musica.acelerar(1.18); }
        }
        const progresso = passou / DURACAO_MS;
        if (agora >= proximoSurgimento) {
            surgir(progresso, agora);
            proximoSurgimento = agora + lerp(1150, 560, progresso) * (0.8 + Math.random() * 0.4);
        }
        atualizarAlvos(dt, agora);
        atualizarRecarga(agora);
        requestAnimationFrame(quadro);
    }

    function comecar() {
        if (!ARMAS.length || rodando) return;     // nunca dois laços de jogo ao mesmo tempo
        limpar();
        st = novaPartida();
        ARMAS.forEach((a, i) => { pente[i] = a.capacidade; ultimosTiros[i] = -1e9; });
        recarga = null;
        trocouEm = -1e9;
        telaInicio.hidden = true;
        telaFim.hidden = true;
        palco.classList.remove('alerta-refem', 'recarregando');
        palco.classList.add('jogando');
        hud.pontos.textContent = '0';
        rodando = true;
        inicio = ultimoQuadro = performance.now();
        proximoSurgimento = inicio + 500;
        ultimoSegundo = 0;
        acelerou = false;
        Som.iniciar();
        Som.efeito('inicio');
        Som.musica.tocar();
        atualizarHudArma();
        mira.hidden = !ponteiro.dentro;
        requestAnimationFrame(quadro);
    }

    function terminar(perdeu) {
        rodando = false;
        Som.musica.parar();
        Som.cancelarRecarga();
        gatilho = false;
        pararRajada();
        definirZoom(false);
        palco.classList.remove('jogando', 'recarregando');
        mira.hidden = true;
        recarga = null;
        const recordeAntigo = Number(ler(CHAVE_RECORDE, '0')) || 0;
        const novoRecorde = !perdeu && st.pontos > recordeAntigo;
        if (novoRecorde) gravar(CHAVE_RECORDE, String(st.pontos));
        const nota = perdeu ? '✗' : NOTAS.find(([minimo]) => st.pontos >= minimo)[1];
        setTimeout(() => {
            limpar();
            q('[data-fim-titulo]').textContent = perdeu ? 'Você acertou a refém!' : 'Tempo esgotado!';
            const notaEl = q('[data-fim-nota]');
            notaEl.textContent = nota;
            notaEl.className = 'nota nota-' + (perdeu ? 'derrota' : nota.toLowerCase());
            q('[data-fim-pontos]').textContent = st.pontos;
            q('[data-fim-abatidos]').textContent = st.abatidos;
            q('[data-fim-precisao]').textContent = (st.tiros ? Math.round(st.acertos / st.tiros * 100) : 0) + '%';
            q('[data-fim-cabeca]').textContent = st.cabeca;
            q('[data-fim-refens]').textContent = st.refens;
            q('[data-fim-recorde]').textContent = novoRecorde ? `${st.pontos} (novo recorde!)` : String(recordeAntigo);
            telaFim.classList.toggle('derrota', perdeu);
            Som.efeito(perdeu ? 'derrota' : 'fim');
            telaFim.hidden = false;
        }, perdeu ? 900 : 300);
    }

    function limpar() {
        alvos.forEach(a => a.el.remove());
        alvos = [];
        efeitos.innerHTML = '';
    }

    // ------------------------------------------------------------ som e música (static/js/som.js)
    function atualizarBotoesSom() {
        if (botaoSom) botaoSom.textContent = Som.efeitosLigados() ? '🔊 Efeitos ligados' : '🔇 Efeitos desligados';
        if (botaoMusica) botaoMusica.textContent = Som.musicaLigada() ? '🎵 Música ligada' : '🎵 Música desligada';
    }

    // ------------------------------------------------------------ controles
    palco.addEventListener('pointerdown', ev => {
        if (!rodando || ev.target.closest('[data-zoom]')) return;
        atualizarPonteiro(ev);
        if (ev.button === 2) { definirZoom(true); return; }
        if (ev.button !== 0) return;
        ev.preventDefault();
        gatilho = true;
        disparar();
        if (armaAtual().tipo === 'SUBMETRALHADORA') iniciarRajada();
    });
    palco.addEventListener('pointermove', atualizarPonteiro);
    palco.addEventListener('pointerleave', () => { ponteiro.dentro = false; mira.hidden = true; });
    window.addEventListener('pointerup', ev => {
        if (ev.button === 2) { if (!shift) definirZoom(false); return; }
        gatilho = false;
        pararRajada();
    });
    palco.addEventListener('contextmenu', ev => ev.preventDefault());
    palco.addEventListener('wheel', ev => {
        if (!rodando || ARMAS.length < 2) return;
        ev.preventDefault();
        const agora = performance.now();
        if (agora - ultimaRoda < 160) return;
        ultimaRoda = agora;
        trocarArma((armaIdx + (ev.deltaY > 0 ? 1 : -1) + ARMAS.length) % ARMAS.length);
    }, { passive: false });
    hud.zoom.addEventListener('click', () => definirZoom(!zoom));

    document.addEventListener('keydown', ev => {
        if (ev.target.closest && ev.target.closest('input, textarea, select')) return;
        if (ev.key === 'Shift') { shift = true; definirZoom(true); return; }
        const n = Number(ev.key);
        if (Number.isInteger(n) && n >= 1 && n <= ARMAS.length) { trocarArma(n - 1); ev.preventDefault(); }
        else if (ev.key === 'r' || ev.key === 'R') recarregar();
    });
    document.addEventListener('keyup', ev => {
        if (ev.key === 'Shift') { shift = false; definirZoom(false); }
    });

    // pausa sozinho se a aba ficar escondida (o relógio não corre enquanto você não vê)
    document.addEventListener('visibilitychange', () => {
        if (!rodando) return;
        if (document.hidden) {
            pausadoEm = performance.now();
            Som.pausar();
            gatilho = false;
            pararRajada();
            return;
        }
        if (!pausadoEm) return;
        Som.continuar();
        const d = performance.now() - pausadoEm;
        pausadoEm = 0;
        inicio += d; proximoSurgimento += d; ultimoQuadro += d; trocouEm += d;
        ultimosTiros.forEach((t, i) => { ultimosTiros[i] = t + d; });
        alvos.forEach(a => { a.nasceu += d; a.expira += d; });
        if (recarga) { recarga.inicio += d; recarga.fim += d; }
    });

    // só libera o "Começar" depois que as imagens dos alvos carregarem
    const botoesComecar = document.querySelectorAll('[data-comecar]');
    botoesComecar.forEach(b => { b.disabled = true; b.addEventListener('click', comecar); });
    prepararImagens().then(() => botoesComecar.forEach(b => { b.disabled = !ARMAS.length; }));
    if (botaoSom) {
        botaoSom.addEventListener('click', () => { Som.alternarEfeitos(); atualizarBotoesSom(); });
    }
    if (botaoMusica) {
        botaoMusica.addEventListener('click', () => {
            if (Som.alternarMusica() && rodando) { Som.iniciar(); Som.musica.tocar(); }
            atualizarBotoesSom();
        });
    }
    window.addEventListener('resize', atualizarMira);

    montarBarra();
    atualizarBotoesSom();
    atualizarHudArma();
    hud.tempo.textContent = (DURACAO_MS / 1000).toFixed(2);
})();
