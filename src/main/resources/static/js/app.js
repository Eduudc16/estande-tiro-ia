// Estande Sombrio — comportamento das telas (o minigame fica em jogo.js, alvos.js e som.js)
(function () {
    'use strict';

    // 1) Confirmação desenhada pela própria página. O navegador embutido do app bloqueia window.confirm
    //    (o "Excluir" não fazia nada); um <dialog> funciona em qualquer navegador.
    var modal = null;
    var formPendente = null;

    function abrirConfirmacao(form) {
        if (!modal) {
            modal = document.createElement('dialog');
            modal.className = 'modal-confirmacao';
            modal.innerHTML = '<div class="modal-corpo"><h2>Tem certeza?</h2><p data-texto></p><div class="acoes">' +
                '<button type="button" class="btn fantasma" data-cancelar>Cancelar</button>' +
                '<button type="button" class="btn perigo cheio" data-confirmar-acao>Excluir</button></div></div>';
            document.body.appendChild(modal);
            modal.querySelector('[data-cancelar]').addEventListener('click', function () { modal.close(); });
            modal.querySelector('[data-confirmar-acao]').addEventListener('click', function () {
                var form = formPendente;
                modal.close();
                if (!form) return;
                form.dataset.confirmado = '1';
                if (form.requestSubmit) form.requestSubmit(); else form.submit();
            });
            modal.addEventListener('close', function () { formPendente = null; });
            modal.addEventListener('click', function (ev) {      // clique fora da caixa fecha
                if (ev.target === modal) modal.close();
            });
        }
        formPendente = form;
        modal.querySelector('[data-texto]').textContent = form.dataset.confirmar;
        modal.querySelector('[data-confirmar-acao]').textContent = form.dataset.confirmarRotulo || 'Excluir';
        if (typeof modal.showModal === 'function') {
            modal.showModal();
            modal.querySelector('[data-cancelar]').focus();
        } else if (window.confirm(form.dataset.confirmar)) {       // navegador muito antigo
            form.dataset.confirmado = '1';
            form.submit();
        }
    }

    // 2) Formulários: confirmação (data-confirmar) e "aguarde" nas chamadas de IA (data-aguarde).
    //    Bloquear o duplo clique é importante: cada clique a mais na geração de imagem custa dinheiro.
    document.addEventListener('submit', function (ev) {
        var form = ev.target;
        if (form.dataset.confirmar && form.dataset.confirmado !== '1') {
            ev.preventDefault();
            abrirConfirmacao(form);
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

    // 3) Cadastro de arma: mostra o diferencial do tipo escolhido.
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
