// Estande Sombrio — comportamento das telas
(function () {
    'use strict';

    // 1) Formulários: confirmação (data-confirmar) e "aguarde" nas chamadas de IA (data-aguarde).
    //    Bloquear o duplo clique é importante: cada clique a mais na geração de imagem custa dinheiro.
    document.addEventListener('submit', function (ev) {
        var form = ev.target;
        if (form.dataset.confirmar && !window.confirm(form.dataset.confirmar)) {
            ev.preventDefault();
            return;
        }
        if (form.dataset.enviando) {
            ev.preventDefault();
            return;
        }
        form.dataset.enviando = '1';
        if (form.dataset.aguarde) {
            document.body.classList.add('ocupado');
            document.querySelectorAll('form[data-aguarde] button').forEach(function (b) { b.disabled = true; });
            var botao = form.querySelector('button');
            if (botao) {
                botao.classList.add('carregando');
                botao.textContent = form.dataset.aguarde;
            }
        }
    });

    // Ao voltar pelo histórico (cache do navegador), reabilita os botões.
    window.addEventListener('pageshow', function (ev) {
        if (!ev.persisted) return;
        document.body.classList.remove('ocupado');
        document.querySelectorAll('form[data-enviando]').forEach(function (f) { delete f.dataset.enviando; });
        document.querySelectorAll('button.carregando').forEach(function (b) { b.classList.remove('carregando'); });
        window.location.reload();
    });

    // 2) Prévia da parte dinâmica do minigame: alvos de madeira surgem na área central do cenário e somem.
    var SVG_ALVO =
        '<svg viewBox="0 0 64 120" aria-hidden="true">' +
        '<rect x="29" y="84" width="6" height="36" fill="#3a2716"/>' +
        '<circle cx="32" cy="20" r="14" fill="var(--cor)" stroke="#4a3220" stroke-width="3"/>' +
        '<path d="M7 90 Q7 40 32 37 Q57 40 57 90 Z" fill="var(--cor)" stroke="#4a3220" stroke-width="3"/>' +
        '<circle cx="32" cy="63" r="15" fill="#efe3c8"/><circle cx="32" cy="63" r="10" fill="#a8322b"/>' +
        '<circle cx="32" cy="63" r="4.5" fill="#efe3c8"/></svg>';
    var DURACAO_MS = 30000;

    document.querySelectorAll('[data-testar-alvos]').forEach(function (botao) {
        var palco = document.querySelector('[data-palco]');
        if (!palco) return;
        var camada = palco.querySelector('[data-alvos]');
        var hud = palco.querySelector('[data-hud]');
        var tempoEl = palco.querySelector('[data-tempo]');
        var pontosEl = palco.querySelector('[data-pontos]');
        var rotuloOriginal = botao.textContent;
        var timers = [];
        var pontos = 0;
        var fim = 0;
        var rodando = false;

        function criarAlvo() {
            if (camada.querySelectorAll('.alvo:not(.saindo):not(.acertado)').length >= 4) return;
            var bonus = Math.random() < 0.15;
            var alvo = document.createElement('button');
            alvo.type = 'button';
            alvo.className = 'alvo';
            alvo.setAttribute('aria-label', bonus ? 'Alvo bônus' : 'Alvo');
            alvo.style.setProperty('--cor', bonus ? '#d9a836' : '#b98d5c');
            alvo.style.left = (24 + Math.random() * 52) + '%';   // área central
            alvo.style.bottom = (22 + Math.random() * 12) + '%'; // acima do balcão
            alvo.innerHTML = SVG_ALVO;
            alvo.addEventListener('click', function (ev) {
                ev.stopPropagation();
                var valor = bonus ? 200 : 100;
                pontos += valor;
                pontosEl.textContent = pontos;
                alvo.classList.add('acertado');
                var voando = document.createElement('span');
                voando.className = 'pontos-voando';
                voando.textContent = '+' + valor;
                voando.style.left = alvo.style.left;
                voando.style.bottom = 'calc(' + alvo.style.bottom + ' + 18%)';
                camada.appendChild(voando);
                setTimeout(function () { voando.remove(); }, 800);
                setTimeout(function () { alvo.remove(); }, 360);
            });
            camada.appendChild(alvo);
            var vida = 1000 + Math.random() * 700;
            timers.push(setTimeout(function () { alvo.classList.add('saindo'); }, vida));
            timers.push(setTimeout(function () { alvo.remove(); }, vida + 220));
        }

        function parar(mostrarResultado) {
            rodando = false;
            timers.forEach(clearTimeout);
            timers.forEach(clearInterval);
            timers = [];
            palco.classList.remove('jogando');
            camada.querySelectorAll('.alvo').forEach(function (a) { a.remove(); });
            botao.textContent = rotuloOriginal;
            if (mostrarResultado) {
                tempoEl.textContent = '0.00';
                var placar = document.createElement('div');
                placar.className = 'hud-fim';
                placar.innerHTML = 'Fim! <span>' + pontos + ' pontos</span><small>Clique em “Testar alvos” para jogar de novo</small>';
                camada.appendChild(placar);
            } else {
                hud.hidden = true;
            }
        }

        function iniciar() {
            camada.innerHTML = '';
            pontos = 0;
            pontosEl.textContent = '0';
            hud.hidden = false;
            palco.classList.add('jogando');
            rodando = true;
            fim = performance.now() + DURACAO_MS;
            botao.textContent = '■ Parar';
            criarAlvo();
            timers.push(setInterval(criarAlvo, 600));
            timers.push(setInterval(function () {
                var resta = Math.max(0, fim - performance.now());
                tempoEl.textContent = (resta / 1000).toFixed(2);
                if (resta <= 0) parar(true);
            }, 50));
        }

        botao.addEventListener('click', function () {
            if (rodando) parar(false); else iniciar();
        });
    });
})();
