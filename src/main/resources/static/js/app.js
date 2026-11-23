// Estande Sombrio — comportamento das telas (o minigame fica em jogo.js e alvos.js)
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

    // Ao voltar pelo histórico (cache do navegador), recarrega para os botões voltarem ao normal.
    window.addEventListener('pageshow', function (ev) {
        if (ev.persisted) window.location.reload();
    });

    // 2) Cadastro de arma: mostra o diferencial do tipo escolhido.
    var tipo = document.querySelector('[data-tipo-arma]');
    var texto = document.querySelector('[data-comportamento-tipo]');
    if (tipo && texto) {
        var mostrar = function () {
            var opcao = tipo.options[tipo.selectedIndex];
            texto.textContent = opcao && opcao.dataset.comportamento ? 'Diferencial do tipo: ' + opcao.dataset.comportamento : '';
        };
        tipo.addEventListener('change', mostrar);
        mostrar();
    }
})();
