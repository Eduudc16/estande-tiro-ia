package com.faculdade.estandetiro.controller;

import com.faculdade.estandetiro.model.entity.AlvoInimigo;
import com.faculdade.estandetiro.model.entity.TipoEstande;
import com.faculdade.estandetiro.model.service.AlvoInimigoService;
import com.faculdade.estandetiro.model.service.LimiteGeracoesImagem;
import com.faculdade.estandetiro.model.service.LogGeracaoImagemService;
import com.faculdade.estandetiro.model.service.RegraNegocioException;
import com.faculdade.estandetiro.model.service.ia.IaException;
import org.springframework.stereotype.Controller;
import org.springframework.ui.Model;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.servlet.mvc.support.RedirectAttributes;

import java.util.EnumMap;
import java.util.List;
import java.util.Map;
import java.util.stream.Collectors;

/** Inimigos de cada tipo de estande: veja os desenhos e gere imagens originais com a IA (chave faturada). */
@Controller
@RequestMapping("/alvos")
public class AlvoController {

    private final AlvoInimigoService alvoService;
    private final LogGeracaoImagemService logService;
    private final LimiteGeracoesImagem limite;

    public AlvoController(AlvoInimigoService alvoService, LogGeracaoImagemService logService,
                          LimiteGeracoesImagem limite) {
        this.alvoService = alvoService;
        this.logService = logService;
        this.limite = limite;
    }

    @GetMapping
    public String listar(Model model) {
        // EnumMap mantém a ordem dos tipos (vila, castelo, base militar, mina, porto)
        Map<TipoEstande, List<AlvoInimigo>> grupos = alvoService.listarTodos().stream()
                .collect(Collectors.groupingBy(AlvoInimigo::getTipoEstande, () -> new EnumMap<>(TipoEstande.class),
                        Collectors.toList()));
        model.addAttribute("grupos", grupos);
        model.addAttribute("precoImagem", logService.getPrecoPorImagemUsd());
        return "alvos/lista";
    }

    @PostMapping("/{id}/gerar")
    public String gerar(@PathVariable Long id, RedirectAttributes ra) {
        return executar(id, false, ra);
    }

    @GetMapping("/{id}/regenerar")
    public String confirmarRegeneracao(@PathVariable Long id, Model model, RedirectAttributes ra) {
        AlvoInimigo alvo = alvoService.buscarPorId(id);
        if (!alvo.temImagem()) {
            ra.addFlashAttribute("erro", "Este alvo ainda não tem imagem. Use \"Gerar imagem com IA\".");
            return voltar(alvo);
        }
        model.addAttribute("alvo", alvo);
        model.addAttribute("precoImagem", logService.getPrecoPorImagemUsd());
        return "alvos/regenerar";
    }

    @PostMapping("/{id}/regenerar")
    public String regenerar(@PathVariable Long id,
                            @RequestParam(name = "confirmacao", required = false) String confirmacao,
                            RedirectAttributes ra) {
        if (!"sim".equals(confirmacao)) {
            return "redirect:/alvos/" + id + "/regenerar";
        }
        return executar(id, true, ra);
    }

    /** Refaz o recorte a partir da imagem original já paga (não chama a API). */
    @PostMapping("/{id}/recortar")
    public String recortar(@PathVariable Long id, RedirectAttributes ra) {
        AlvoInimigo alvo = alvoService.buscarPorId(id);
        try {
            alvoService.refazerRecorte(id);
            ra.addFlashAttribute("sucesso", "Recorte de \"" + alvo.getNome() + "\" refeito, sem gastar nada.");
        } catch (RegraNegocioException e) {
            ra.addFlashAttribute("erro", e.getMessage());
        }
        return voltar(alvo);
    }

    private String executar(Long id, boolean regenerar, RedirectAttributes ra) {
        AlvoInimigo alvo = alvoService.buscarPorId(id);
        try {
            alvoService.gerarImagem(id, regenerar);
            ra.addFlashAttribute("sucesso", "Imagem de \"" + alvo.getNome() + "\" " + (regenerar ? "regenerada" : "gerada")
                    + " e recortada (gerações nesta execução: " + limite.getUsadas() + "/" + limite.getMaximo() + ").");
        } catch (IaException | RegraNegocioException e) {
            ra.addFlashAttribute("erro", e.getMessage());
            if (e instanceof IaException ia && ia.getDetalhe() != null && !ia.getDetalhe().isBlank()) {
                ra.addFlashAttribute("erroDetalhe", ia.getDetalhe());
            }
        }
        return voltar(alvo);
    }

    private static String voltar(AlvoInimigo alvo) {
        return "redirect:/alvos#tipo-" + alvo.getTipoEstande().getCssTema();
    }
}
