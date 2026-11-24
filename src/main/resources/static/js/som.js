/* Estande Sombrio — sons e música do minigame, todos sintetizados na hora com a Web Audio API (sem arquivos de
   áudio). Cada arma tem o seu tiro e a sua recarga; a música é uma valsa de parque de diversões sinistra,
   composta para este projeto. */
window.Som = (() => {
    'use strict';
    const CHAVE_EFEITOS = 'estande-sombrio:som';
    const CHAVE_MUSICA = 'estande-sombrio:musica';
    const ler = (k, p) => { try { const v = localStorage.getItem(k); return v === null ? p : v; } catch (e) { return p; } };
    const gravar = (k, v) => { try { localStorage.setItem(k, v); } catch (e) { /* sem armazenamento: tudo bem */ } };

    let ctx = null;
    let busEfeitos, busMusica, envioEfeitos, envioMusica, ruidoBranco, ruidoMarrom;
    let efeitosLigados = ler(CHAVE_EFEITOS, '1') !== '0';
    let musicaLigada = ler(CHAVE_MUSICA, '1') !== '0';
    let recargaAgendada = [];

    // ------------------------------------------------------------ montagem (no primeiro clique do jogador)
    function iniciar() {
        if (ctx) {
            if (ctx.state === 'suspended') ctx.resume();
            return true;
        }
        const Ctx = window.AudioContext || window.webkitAudioContext;
        if (!Ctx) return false;
        try {
            ctx = new Ctx();
            const compressor = ctx.createDynamicsCompressor();
            compressor.threshold.value = -16;
            compressor.ratio.value = 4;
            compressor.attack.value = 0.003;
            compressor.release.value = 0.25;
            compressor.connect(ctx.destination);
            const saida = ctx.createGain();
            saida.gain.value = 0.9;
            saida.connect(compressor);

            busEfeitos = ctx.createGain();
            busEfeitos.gain.value = efeitosLigados ? 1 : 0;
            busEfeitos.connect(saida);
            busMusica = ctx.createGain();
            busMusica.gain.value = musicaLigada ? 0.3 : 0;
            busMusica.connect(saida);

            // eco de galpão de madeira (resposta ao impulso gerada aqui mesmo)
            const reverb = ctx.createConvolver();
            reverb.buffer = respostaDoGalpao(1.9, 2.8);
            reverb.connect(saida);
            envioEfeitos = ctx.createGain();
            envioEfeitos.gain.value = efeitosLigados ? 1 : 0;
            envioEfeitos.connect(reverb);
            envioMusica = ctx.createGain();
            envioMusica.gain.value = musicaLigada ? 0.3 : 0;
            envioMusica.connect(reverb);

            ruidoBranco = criarRuido(false);
            ruidoMarrom = criarRuido(true);
            return true;
        } catch (e) {
            ctx = null;
            return false;
        }
    }

    function criarRuido(marrom) {
        const n = ctx.sampleRate * 2, buffer = ctx.createBuffer(1, n, ctx.sampleRate), d = buffer.getChannelData(0);
        let anterior = 0;
        for (let i = 0; i < n; i++) {
            const branco = Math.random() * 2 - 1;
            if (marrom) {
                anterior = (anterior + 0.02 * branco) / 1.02;
                d[i] = anterior * 3.5;
            } else {
                d[i] = branco;
            }
        }
        return buffer;
    }

    function respostaDoGalpao(duracao, queda) {
        const n = Math.floor(ctx.sampleRate * duracao), buffer = ctx.createBuffer(2, n, ctx.sampleRate);
        for (let c = 0; c < 2; c++) {
            const d = buffer.getChannelData(c);
            for (let i = 0; i < n; i++) d[i] = (Math.random() * 2 - 1) * Math.pow(1 - i / n, queda);
        }
        return buffer;
    }

    // ------------------------------------------------------------ peças básicas
    function envelope(t0, ataque, dur, vol) {
        const g = ctx.createGain();
        g.gain.setValueAtTime(0.0001, t0);
        g.gain.exponentialRampToValueAtTime(Math.max(0.0002, vol), t0 + ataque);
        g.gain.exponentialRampToValueAtTime(0.0001, t0 + dur);
        return g;
    }

    function ligar(g, destino, envio, reverb) {
        g.connect(destino);
        if (reverb > 0) {
            const e = ctx.createGain();
            e.gain.value = reverb;
            g.connect(e);
            e.connect(envio);
        }
    }

    /** Ruído filtrado com envelope: estalos, estouros, chiados e cliques. */
    function ruido(o) {
        const t0 = o.quando != null ? o.quando : ctx.currentTime + (o.t || 0);
        const dur = o.dur || 0.1;
        const fonte = ctx.createBufferSource();
        fonte.buffer = o.marrom ? ruidoMarrom : ruidoBranco;
        const filtro = ctx.createBiquadFilter();
        filtro.type = o.filtro || 'lowpass';
        filtro.Q.value = o.q || 0.8;
        filtro.frequency.setValueAtTime(o.freq || 2000, t0);
        if (o.freqFim) filtro.frequency.exponentialRampToValueAtTime(o.freqFim, t0 + dur);
        const g = envelope(t0, o.ataque || 0.002, dur, o.vol || 0.4);
        fonte.connect(filtro);
        filtro.connect(g);
        ligar(g, o.destino || busEfeitos, o.envio || envioEfeitos, o.reverb || 0);
        fonte.start(t0, Math.random() * 1.5, dur + 0.05);
        return fonte;
    }

    /** Tom com queda de altura opcional: corpo dos tiros, sinos e notas da música. */
    function tom(o) {
        const t0 = o.quando != null ? o.quando : ctx.currentTime + (o.t || 0);
        const dur = o.dur || 0.2;
        const osc = ctx.createOscillator();
        osc.type = o.forma || 'sine';
        osc.frequency.setValueAtTime(o.freq || 440, t0);
        if (o.freqFim) osc.frequency.exponentialRampToValueAtTime(o.freqFim, t0 + dur);
        if (o.detune) osc.detune.value = o.detune;
        const g = envelope(t0, o.ataque || 0.003, dur, o.vol || 0.3);
        let ultimo = osc;
        if (o.filtro) {
            const f = ctx.createBiquadFilter();
            f.type = 'lowpass';
            f.frequency.value = o.filtro;
            osc.connect(f);
            ultimo = f;
        }
        ultimo.connect(g);
        ligar(g, o.destino || busEfeitos, o.envio || envioEfeitos, o.reverb || 0);
        osc.start(t0);
        osc.stop(t0 + dur + 0.05);
        return osc;
    }

    /** Clique metálico de mecanismo (ferrolho, pente, cão do revólver). */
    function mecanismo(t, freq, vol) {
        return [
            ruido({ t, dur: 0.035, vol, filtro: 'bandpass', freq, q: 3 }),
            tom({ t, dur: 0.03, vol: vol * 0.45, forma: 'square', freq: freq * 1.9, filtro: 4500 })
        ];
    }

    // ------------------------------------------------------------ tiros: um timbre para cada arma
    const TIROS = {
        PISTOLA() {     // estalo seco e curto
            ruido({ dur: 0.06, vol: 0.55, filtro: 'bandpass', freq: 2600, q: 0.7, reverb: 0.25 });
            ruido({ dur: 0.15, vol: 0.32, filtro: 'lowpass', freq: 1900, freqFim: 300, reverb: 0.3 });
            tom({ dur: 0.09, vol: 0.4, freq: 190, freqFim: 70 });
        },
        REVOLVER() {    // estrondo grave com anel metálico
            ruido({ dur: 0.34, vol: 0.85, filtro: 'lowpass', freq: 3200, freqFim: 200, reverb: 0.55 });
            tom({ dur: 0.26, vol: 0.55, freq: 130, freqFim: 36 });
            tom({ dur: 0.4, vol: 0.05, forma: 'triangle', freq: 2240, reverb: 0.3 });
            tom({ dur: 0.34, vol: 0.04, forma: 'triangle', freq: 2311, reverb: 0.3 });
        },
        ESCOPETA() {    // explosão larga e o bombeamento logo depois
            ruido({ dur: 0.55, vol: 0.95, marrom: true, filtro: 'lowpass', freq: 1400, freqFim: 130, reverb: 0.6 });
            ruido({ dur: 0.08, vol: 0.5, filtro: 'highpass', freq: 1500, reverb: 0.2 });
            tom({ dur: 0.34, vol: 0.6, freq: 95, freqFim: 30 });
            mecanismo(0.42, 900, 0.2);
            mecanismo(0.56, 650, 0.22);
        },
        RIFLE() {       // estalo agudo, eco rolando pelo galpão e o ferrolho
            ruido({ dur: 0.035, vol: 0.7, filtro: 'highpass', freq: 3500, reverb: 0.15 });
            ruido({ dur: 0.2, vol: 0.45, filtro: 'lowpass', freq: 2600, freqFim: 380, reverb: 0.45 });
            tom({ dur: 0.12, vol: 0.35, freq: 160, freqFim: 48 });
            [0.22, 0.46, 0.72].forEach((t, i) =>
                ruido({ t, dur: 0.26, vol: 0.18 / (i + 1), filtro: 'lowpass', freq: 900 - i * 220, reverb: 0.5 }));
            mecanismo(0.78, 1100, 0.16);
            mecanismo(0.9, 760, 0.2);
        },
        SUBMETRALHADORA() {   // estalinho rápido, com pequena variação a cada tiro
            const v = 0.9 + Math.random() * 0.2;
            ruido({ dur: 0.045, vol: 0.38, filtro: 'bandpass', freq: 1700 * v, q: 0.9, reverb: 0.12 });
            tom({ dur: 0.045, vol: 0.24, freq: 220 * v, freqFim: 90 });
        },
        LANCA_GRANADAS() {    // "tump" oco do disparo (a explosão vem depois)
            tom({ dur: 0.2, vol: 0.55, freq: 250, freqFim: 65 });
            ruido({ dur: 0.15, vol: 0.32, filtro: 'lowpass', freq: 700, freqFim: 180, reverb: 0.25 });
        }
    };

    // ------------------------------------------------------------ recargas: [momento (fração da recarga), freq, volume]
    const RECARGAS = {
        PISTOLA: [[0, 1500, 0.2], [0.45, 700, 0.26], [0.84, 1200, 0.22], [0.9, 850, 0.24]],
        REVOLVER: [[0, 1800, 0.18], [0.12, 3200, 0.08], [0.15, 3500, 0.07], [0.18, 3000, 0.07],
            [0.32, 1400, 0.1], [0.41, 1400, 0.1], [0.5, 1400, 0.1], [0.59, 1400, 0.1], [0.68, 1400, 0.1],
            [0.77, 1400, 0.1], [0.9, 950, 0.24]],
        ESCOPETA: [[0.08, 600, 0.18], [0.27, 600, 0.18], [0.46, 600, 0.18], [0.65, 600, 0.18],
            [0.84, 900, 0.22], [0.92, 650, 0.24]],
        RIFLE: [[0, 1100, 0.2], [0.12, 760, 0.2], [0.5, 650, 0.24], [0.84, 1100, 0.2], [0.92, 760, 0.24]],
        SUBMETRALHADORA: [[0, 1300, 0.2], [0.55, 650, 0.26], [0.86, 1500, 0.2], [0.92, 900, 0.22]],
        LANCA_GRANADAS: [[0, 420, 0.28], [0.3, 300, 0.22], [0.5, 300, 0.22], [0.7, 300, 0.22], [0.9, 420, 0.3]]
    };

    // ------------------------------------------------------------ outros efeitos
    const EFEITOS = {
        acerto() {        // tiro no recorte de madeira
            ruido({ dur: 0.07, vol: 0.32, filtro: 'bandpass', freq: 1100, q: 1.5 });
            tom({ dur: 0.08, vol: 0.2, forma: 'triangle', freq: 330, freqFim: 220 });
        },
        cabeca() {        // acerto + "plim"
            EFEITOS.acerto();
            tom({ t: 0.02, dur: 0.38, vol: 0.17, freq: 1760, reverb: 0.4 });
            tom({ t: 0.02, dur: 0.3, vol: 0.07, freq: 2640 });
        },
        queda() {         // o recorte tombando para trás
            ruido({ t: 0.05, dur: 0.24, vol: 0.24, filtro: 'lowpass', freq: 900, freqFim: 140 });
            tom({ t: 0.24, dur: 0.1, vol: 0.18, forma: 'triangle', freq: 140, freqFim: 80 });
        },
        escudo() {        // bala batendo no metal
            [523, 1347, 2211].forEach((f, i) =>
                tom({ dur: 0.42 - i * 0.08, vol: 0.12 - i * 0.025, forma: 'triangle', freq: f, reverb: 0.3 }));
            ruido({ dur: 0.05, vol: 0.3, filtro: 'highpass', freq: 3000 });
        },
        vazio() {
            mecanismo(0, 2400, 0.24);
        },
        troca() {
            mecanismo(0, 1300, 0.16);
            mecanismo(0.09, 900, 0.15);
        },
        explosao() {
            ruido({ dur: 1.5, vol: 1, marrom: true, filtro: 'lowpass', freq: 2200, freqFim: 110, reverb: 0.7, ataque: 0.01 });
            tom({ dur: 0.85, vol: 0.7, freq: 72, freqFim: 26 });
            for (let i = 0; i < 9; i++) {
                ruido({ t: 0.05 + Math.random() * 0.6, dur: 0.05, vol: 0.1, filtro: 'highpass', freq: 2500 });
            }
        },
        refemSalva() {    // três notas subindo
            [784, 988, 1175].forEach((f, i) =>
                tom({ t: i * 0.09, dur: 0.36, vol: 0.13, forma: 'triangle', freq: f, reverb: 0.4 }));
        },
        refemAtingida() { // acorde dissonante e pesado
            [110, 116.5, 155.6].forEach(f => tom({ dur: 1.3, vol: 0.2, forma: 'sawtooth', freq: f, filtro: 900, reverb: 0.5 }));
            ruido({ dur: 0.7, vol: 0.4, marrom: true, filtro: 'lowpass', freq: 600, reverb: 0.5 });
        },
        tique() {
            ruido({ dur: 0.03, vol: 0.22, filtro: 'bandpass', freq: 2000, q: 4 });
        },
        tiqueFinal() {
            tom({ dur: 0.13, vol: 0.16, forma: 'square', freq: 1320, filtro: 3000 });
        },
        inicio() {
            [523, 659, 784, 1047].forEach((f, i) =>
                tom({ t: i * 0.08, dur: 0.26, vol: 0.11, forma: 'triangle', freq: f, reverb: 0.3 }));
        },
        fim() {
            [659, 784, 1047, 1319].forEach((f, i) =>
                tom({ t: i * 0.11, dur: i === 3 ? 0.8 : 0.3, vol: 0.12, forma: 'triangle', freq: f, reverb: 0.45 }));
        },
        derrota() {
            [392, 370, 349, 311].forEach((f, i) =>
                tom({ t: i * 0.24, dur: 0.45, vol: 0.12, forma: 'sawtooth', freq: f, filtro: 1200, reverb: 0.4 }));
        }
    };

    // ------------------------------------------------------------ música: valsa sinistra de parque de diversões
    // Composição original em lá menor, 3/4. Forma A A B A (32 compassos) em loop.
    const BPM = 138;
    const freqDe = midi => 440 * Math.pow(2, (midi - 69) / 12);
    const ACORDES = {   // fundamental do baixo (alterna com a quinta) e as notas do "pá-pá"
        Am: { baixo: [45, 40], notas: [57, 60, 64] },
        Dm: { baixo: [38, 45], notas: [57, 62, 65] },
        E7: { baixo: [40, 47], notas: [56, 62, 64] },
        F: { baixo: [41, 48], notas: [57, 60, 65] }
    };
    const HARMONIA_A = ['Am', 'Am', 'Dm', 'Am', 'E7', 'E7', 'Am', 'E7'];
    const HARMONIA_B = ['F', 'Dm', 'E7', 'Am', 'F', 'Dm', 'E7', 'E7'];
    // melodia em colcheias (6 por compasso): número = nota MIDI, '-' = segura, null = pausa
    const MELODIA_A = [
        [76, '-', 81, '-', 84, '-'], [83, 81, 80, 81, 76, '-'], [77, '-', 81, '-', 86, '-'], [84, 83, 81, '-', 76, '-'],
        [80, '-', 83, '-', 86, '-'], [84, 83, 80, '-', 76, '-'], [81, '-', 84, '-', 88, '-'], [86, 84, 83, '-', 80, '-']
    ];
    const MELODIA_B = [
        [81, '-', 84, '-', 89, '-'], [88, 86, 81, '-', 77, '-'], [80, 81, 83, '-', 86, '-'], [84, '-', 81, '-', 76, '-'],
        [77, 81, 84, 81, 77, '-'], [74, 77, 81, 77, 74, '-'], [76, 80, 83, 86, 83, 80], [76, '-', '-', '-', null, null]
    ];
    const FORMA = [[HARMONIA_A, MELODIA_A], [HARMONIA_A, MELODIA_A], [HARMONIA_B, MELODIA_B], [HARMONIA_A, MELODIA_A]];
    const PASSOS = FORMA.length * 8 * 6;

    const musica = (() => {
        let timer = 0, passo = 0, proximo = 0, velocidade = 1, tocando = false;

        function nota(midi, quando, dur) {     // "calíope": triângulo + quadrada levemente desafinada
            const f = freqDe(midi);
            tom({ quando, dur: dur + 0.04, vol: 0.15, forma: 'triangle', freq: f, ataque: 0.01,
                destino: busMusica, envio: envioMusica, reverb: 0.3 });
            tom({ quando, dur: dur + 0.04, vol: 0.045, forma: 'square', freq: f, detune: 8, filtro: 2400, ataque: 0.01,
                destino: busMusica, envio: envioMusica });
        }

        function passoDaMusica(p, quando, colcheia) {
            const compasso = Math.floor(p / 6), sub = p % 6;
            const [harmonia, melodia] = FORMA[Math.floor(compasso / 8)];
            const acorde = ACORDES[harmonia[compasso % 8]];
            if (sub === 0) {                   // "um": baixo
                tom({ quando, dur: colcheia * 1.8, vol: 0.32, forma: 'triangle', freq: freqDe(acorde.baixo[compasso % 2]),
                    filtro: 700, destino: busMusica, envio: envioMusica });
            }
            if (sub === 2 || sub === 4) {      // "pá-pá": acorde curto
                acorde.notas.forEach(n => tom({ quando, dur: colcheia * 0.8, vol: 0.05, forma: 'square', freq: freqDe(n),
                    filtro: 1500, destino: busMusica, envio: envioMusica, reverb: 0.15 }));
            }
            if (sub % 2 === 0) {               // marcação leve
                ruido({ quando, dur: 0.025, vol: sub === 0 ? 0.07 : 0.04, filtro: 'bandpass', freq: 3200, q: 3,
                    destino: busMusica, envio: envioMusica });
            }
            const linha = melodia[compasso % 8];
            if (typeof linha[sub] === 'number') {
                let n = 1;
                while (sub + n < 6 && linha[sub + n] === '-') n++;
                nota(linha[sub], quando, colcheia * n * 0.92);
            }
        }

        function agendar() {
            const colcheia = 60 / (BPM * velocidade) / 2;
            while (proximo < ctx.currentTime + 0.15) {
                passoDaMusica(passo, proximo, colcheia);
                proximo += colcheia;
                passo = (passo + 1) % PASSOS;
            }
        }

        return {
            tocar() {
                if (!ctx || tocando || !musicaLigada) return;
                tocando = true;
                passo = 0;
                velocidade = 1;
                proximo = ctx.currentTime + 0.1;
                timer = setInterval(agendar, 25);
            },
            parar() {
                tocando = false;
                clearInterval(timer);
            },
            /** Fica mais rápida nos últimos segundos da partida. */
            acelerar(fator) {
                velocidade = fator;
            }
        };
    })();

    // ------------------------------------------------------------ faixas em arquivo (opcionais)
    // Se existirem em /audio (pasta audio-local, fora do Git), substituem a música sintetizada e
    // acrescentam abertura, derrota e gritos dos inimigos. Sem os arquivos, nada muda.
    const faixas = {}, disponivel = {};
    const GRITOS = ['grito-1', 'grito-2', 'grito-3', 'grito-4', 'grito-5'];
    ['abertura', 'menu', 'partida', 'derrota', 'grito-feminino'].concat(GRITOS).forEach(nome => {
        const a = new Audio('/audio/' + nome + '.mp3');
        a.preload = 'auto';
        a.addEventListener('canplaythrough', () => { disponivel[nome] = true; }, { once: true });
        a.addEventListener('error', () => { disponivel[nome] = false; });
        faixas[nome] = a;
    });
    faixas.menu.loop = true;
    faixas.partida.loop = true;
    let faixaAtual = null;

    function volumeDe(nome) {
        const musical = nome === 'menu' || nome === 'partida' || nome === 'derrota';
        return musical ? (musicaLigada ? 0.45 : 0) : (efeitosLigados ? 0.9 : 0);
    }
    function tocarFaixa(nome) {
        const a = faixas[nome];
        if (!a || disponivel[nome] === false) return false;
        if (nome === 'menu' || nome === 'partida' || nome === 'derrota') {
            pararFaixa();
            faixaAtual = a;
        }
        a.volume = volumeDe(nome);
        a.currentTime = 0;
        const p = a.play();
        if (p && p.catch) p.catch(() => { /* sem gesto do usuário ainda ou arquivo ausente */ });
        return true;
    }
    function pararFaixa() {
        if (faixaAtual) { faixaAtual.pause(); faixaAtual = null; }
    }
    // limite de vozes: no máximo um grito a cada 1,5 s, e nunca dois ao mesmo tempo
    let ultimoGrito = -1e9, gritoTocando = null;
    function grito(feminino) {
        if (!efeitosLigados) return;
        const agora = performance.now();
        if (agora - ultimoGrito < 1500 || (gritoTocando && !gritoTocando.ended && !gritoTocando.paused)) return;
        const nome = feminino && disponivel['grito-feminino'] ? 'grito-feminino'
            : GRITOS.filter(g => disponivel[g])[Math.floor(Math.random() * GRITOS.filter(g => disponivel[g]).length)];
        if (!nome) return;
        const a = faixas[nome].cloneNode();
        a.volume = 0.7;
        ultimoGrito = agora;
        gritoTocando = a;
        a.play().catch(() => {});
    }

    // ------------------------------------------------------------ API usada pelo jogo
    const pronto = () => ctx !== null;
    return {
        iniciar,
        tiro(tipo) {
            if (pronto() && efeitosLigados && TIROS[tipo]) TIROS[tipo]();
        },
        efeito(nome) {
            if (pronto() && efeitosLigados && EFEITOS[nome]) EFEITOS[nome]();
        },
        recarga(tipo, duracaoMs) {
            if (!pronto() || !efeitosLigados) return;
            const passos = RECARGAS[tipo] || RECARGAS.PISTOLA;
            const dur = Math.max(0.3, duracaoMs / 1000);
            recargaAgendada = passos.flatMap(([fracao, freq, vol]) => mecanismo(fracao * dur, freq, vol));
        },
        /** Trocar de arma no meio da recarga interrompe os sons que ainda não tocaram. */
        cancelarRecarga() {
            recargaAgendada.forEach(fonte => { try { fonte.stop(); } catch (e) { /* já terminou */ } });
            recargaAgendada = [];
        },
        musica: {
            /** Música da partida: a faixa em arquivo, se houver; senão a valsa sintetizada. */
            tocar() {
                if (!musicaLigada) return;
                if (disponivel.partida && tocarFaixa('partida')) return;
                musica.tocar();
            },
            parar() { musica.parar(); if (faixaAtual === faixas.partida) pararFaixa(); },
            acelerar(f) { musica.acelerar(f); if (faixas.partida) faixas.partida.playbackRate = f > 1 ? 1.08 : 1; }
        },
        /** Abertura (uma vez) e música do menu, antes de começar a partida. */
        menu() {
            if (disponivel.abertura && efeitosLigados) tocarFaixa('abertura');
            if (musicaLigada) tocarFaixa('menu');
        },
        derrota() {
            pararFaixa();
            if (musicaLigada && disponivel.derrota) tocarFaixa('derrota');
            else if (efeitosLigados && EFEITOS.derrota && ctx) EFEITOS.derrota();
        },
        grito,
        pararFaixas: pararFaixa,
        pausar() {
            if (ctx && ctx.state === 'running') ctx.suspend();
        },
        continuar() {
            if (ctx && ctx.state === 'suspended') ctx.resume();
        },
        efeitosLigados: () => efeitosLigados,
        musicaLigada: () => musicaLigada,
        alternarEfeitos() {
            efeitosLigados = !efeitosLigados;
            gravar(CHAVE_EFEITOS, efeitosLigados ? '1' : '0');
            if (ctx) {
                busEfeitos.gain.setTargetAtTime(efeitosLigados ? 1 : 0, ctx.currentTime, 0.05);
                envioEfeitos.gain.setTargetAtTime(efeitosLigados ? 1 : 0, ctx.currentTime, 0.05);
            }
            return efeitosLigados;
        },
        alternarMusica() {
            musicaLigada = !musicaLigada;
            gravar(CHAVE_MUSICA, musicaLigada ? '1' : '0');
            if (ctx) {
                busMusica.gain.setTargetAtTime(musicaLigada ? 0.3 : 0, ctx.currentTime, 0.08);
                envioMusica.gain.setTargetAtTime(musicaLigada ? 0.3 : 0, ctx.currentTime, 0.08);
            }
            if (!musicaLigada) { musica.parar(); pararFaixa(); }
            if (faixaAtual) faixaAtual.volume = musicaLigada ? 0.45 : 0;
            return musicaLigada;
        }
    };
})();
