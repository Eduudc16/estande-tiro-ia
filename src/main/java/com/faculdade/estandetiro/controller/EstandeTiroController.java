package com.faculdade.estandetiro.controller;

import com.faculdade.estandetiro.model.entity.EstandeTiro;
import com.faculdade.estandetiro.model.entity.EstiloVisual;
import com.faculdade.estandetiro.model.entity.TipoEstande;
import com.faculdade.estandetiro.model.service.AlvoInimigoService;
import com.faculdade.estandetiro.model.service.CenarioService;
import com.faculdade.estandetiro.model.service.EstandeTiroService;
import com.faculdade.estandetiro.model.service.LimiteGeracoesImagem;
import com.faculdade.estandetiro.model.service.LogGeracaoImagemService;
import com.faculdade.estandetiro.model.service.RegraNegocioException;
import com.faculdade.estandetiro.model.service.ia.IaException;
import com.faculdade.estandetiro.model.service.ia.IaTextoService;
import jakarta.validation.Valid;
import org.springframework.stereotype.Controller;
import org.springframework.ui.Model;
import org.springframework.validation.BindingResult;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.ModelAttribute;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.servlet.mvc.support.RedirectAttributes;

@Controller
@RequestMapping("/estandes")
public class EstandeTiroController {

    private final EstandeTiroService estandeService;
    private final IaTextoService iaTextoService;
    private final CenarioService cenarioService;
    private final LogGeracaoImagemService logService;
    private final LimiteGeracoesImagem limite;
    private final AlvoInimigoService alvoService;

    public EstandeTiroController(EstandeTiroService estandeService, IaTextoService iaTextoService,
                                 CenarioService cenarioService, LogGeracaoImagemService logService,
                                 LimiteGeracoesImagem limite, AlvoInimigoService alvoService) {
        this.estandeService = estandeService;
        this.iaTextoService = iaTextoService;
        this.cenarioService = cenarioService;
        this.logService = logService;
        this.limite = limite;
        this.alvoService = alvoService;
    }

    // ------------------------------------------------------------------ CRUD

    @GetMapping
    public String listar(Model model) {
        model.addAttribute("estandes", estandeService.listarTodos());
        return "estandes/lista";
    }

    @GetMapping("/novo")
    public String novo(Model model) {
        model.addAttribute("form", new EstandeForm());
        return formulario(model, null);
    }

    @PostMapping
    public String criar(@Valid @ModelAttribute("form") EstandeForm form, BindingResult validacao, Model model,
                        RedirectAttributes ra) {
        if (validacao.hasErrors()) {
            return formulario(model, null);
        }
        EstandeTiro criado = estandeService.criar(form.paraEntidade());
        ra.addFlashAttribute("sucesso", "Estande \"" + criado.getNome() + "\" cadastrado. Agora gere a descrição com IA.");
        return "redirect:/estandes/" + criado.getId();
    }

    @GetMapping("/{id}")
    public String detalhe(@PathVariable Long id, Model model) {
        model.addAttribute("estande", estandeService.buscarPorId(id));
        model.addAttribute("logs", logService.ultimosDoEstande(id));
        model.addAttribute("precoImagem", logService.getPrecoPorImagemUsd());
        model.addAttribute("gerandoAgora", cenarioService.emAndamento(id));
        model.addAttribute("alvos", alvoService.alvosParaJogo(estandeService.buscarPorId(id).getTipo()));
        return "estandes/detalhe";
    }

    @GetMapping("/{id}/editar")
    public String editar(@PathVariable Long id, Model model) {
        EstandeTiro estande = estandeService.buscarPorId(id);
        model.addAttribute("form", EstandeForm.de(estande));
        return formulario(model, estande);
    }

    @PostMapping("/{id}")
    public String atualizar(@PathVariable Long id, @Valid @ModelAttribute("form") EstandeForm form,
                            BindingResult validacao, Model model, RedirectAttributes ra) {
        if (validacao.hasErrors()) {
            return formulario(model, estandeService.buscarPorId(id));
        }
        estandeService.atualizar(id, form.paraEntidade());
        ra.addFlashAttribute("sucesso", "Estande atualizado.");
        return "redirect:/estandes/" + id;
    }

    @PostMapping("/{id}/excluir")
    public String excluir(@PathVariable Long id, RedirectAttributes ra) {
        String nome = estandeService.buscarPorId(id).getNome();
        estandeService.excluir(id);
        ra.addFlashAttribute("sucesso", "Estande \"" + nome + "\" excluído. O log de gerações foi mantido.");
        return "redirect:/estandes";
    }

    private String formulario(Model model, EstandeTiro estande) {
        model.addAttribute("estande", estande);
        model.addAttribute("tipos", TipoEstande.values());
        model.addAttribute("estilos", EstiloVisual.values());
        return "estandes/form";
    }

    // ------------------------------------------------------------------ GenAI

    /** 1) Texto (chave gratuita): descrição curta com clima de terror e suspense. */
    @PostMapping("/{id}/descricao")
    public String gerarDescricao(@PathVariable Long id, RedirectAttributes ra) {
        EstandeTiro estande = estandeService.buscarPorId(id);
        try {
            estandeService.salvarDescricao(id, iaTextoService.gerarDescricao(estande));
            ra.addFlashAttribute("sucesso", "Descrição gerada pela IA (" + iaTextoService.getModelo() + ").");
        } catch (IaException | RegraNegocioException e) {
            mostrarErro(ra, e);
        }
        return "redirect:/estandes/" + id;
    }

    /** 2) Texto (chave gratuita): transforma a descrição em prompt de imagem seguindo a base. */
    @PostMapping("/{id}/prompt")
    public String gerarPrompt(@PathVariable Long id, RedirectAttributes ra) {
        EstandeTiro estande = estandeService.buscarPorId(id);
        try {
            estandeService.salvarPromptImagem(id, iaTextoService.gerarPromptImagem(estande));
            ra.addFlashAttribute("sucesso", "Prompt de imagem gerado pela IA (" + iaTextoService.getModelo() + ").");
        } catch (IaException | RegraNegocioException e) {
            mostrarErro(ra, e);
        }
        return "redirect:/estandes/" + id;
    }

    /** 3) Imagem (chave faturada): só gera se o estande ainda não tem cenário (cache). */
    @PostMapping("/{id}/cenario")
    public String gerarCenario(@PathVariable Long id, RedirectAttributes ra) {
        return executarGeracao(id, false, ra);
    }

    /** Tela de confirmação do "Regenerar" (gera de novo, mesmo já tendo imagem). */
    @GetMapping("/{id}/regenerar")
    public String confirmarRegeneracao(@PathVariable Long id, Model model, RedirectAttributes ra) {
        EstandeTiro estande = estandeService.buscarPorId(id);
        if (!estande.temImagem()) {
            ra.addFlashAttribute("erro", "Este estande ainda não tem cenário. Use \"Gerar cenário com IA\".");
            return "redirect:/estandes/" + id;
        }
        model.addAttribute("estande", estande);
        model.addAttribute("precoImagem", logService.getPrecoPorImagemUsd());
        return "estandes/regenerar";
    }

    @PostMapping("/{id}/regenerar")
    public String regenerar(@PathVariable Long id,
                            @RequestParam(name = "confirmacao", required = false) String confirmacao,
                            RedirectAttributes ra) {
        if (!"sim".equals(confirmacao)) {
            return "redirect:/estandes/" + id + "/regenerar";
        }
        return executarGeracao(id, true, ra);
    }

    private String executarGeracao(Long id, boolean regenerar, RedirectAttributes ra) {
        try {
            CenarioService.Resultado resultado = cenarioService.gerar(id, regenerar);
            ra.addFlashAttribute("sucesso", (regenerar ? "Cenário regenerado" : "Cenário gerado") + " e salvo em "
                    + resultado.caminhoImagem() + " (gerações nesta execução: " + limite.getUsadas() + "/"
                    + limite.getMaximo() + ").");
            if (!resultado.termosRemovidos().isEmpty()) {
                ra.addFlashAttribute("aviso", "Estes nomes foram tirados do prompt antes do envio: "
                        + String.join(", ", resultado.termosRemovidos()) + ".");
            }
        } catch (IaException | RegraNegocioException e) {
            mostrarErro(ra, e);
        }
        return "redirect:/estandes/" + id;
    }

    private static void mostrarErro(RedirectAttributes ra, RuntimeException e) {
        ra.addFlashAttribute("erro", e.getMessage());
        if (e instanceof IaException ia && ia.getDetalhe() != null && !ia.getDetalhe().isBlank()) {
            ra.addFlashAttribute("erroDetalhe", ia.getDetalhe());
        }
    }
}
