package com.faculdade.estandetiro.controller;

import com.faculdade.estandetiro.model.entity.Arma;
import com.faculdade.estandetiro.model.service.ArmaService;
import com.faculdade.estandetiro.model.service.EstandeTiroService;
import org.springframework.stereotype.Controller;
import org.springframework.ui.Model;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;

/**
 * Minigame do estande: o fundo fixo é o cenário gerado por IA e a parte dinâmica são os alvos
 * (inimigos do tema e a refém), desenhados e animados em static/js/alvos.js e static/js/jogo.js.
 */
@Controller
public class JogoController {

    /** Dados de cada arma enviados para o JavaScript do jogo. */
    public record ArmaJogo(String nome, String tipo, String tipoRotulo, int dano, int capacidade, int cadenciaMs,
                           int recargaMs, String diferencial) {
        static ArmaJogo de(Arma a) {
            String dif = a.getDiferencial() == null ? a.getTipo().getComportamento() : a.getDiferencial();
            return new ArmaJogo(a.getNome(), a.getTipo().name(), a.getTipo().getRotulo(), a.getDano(),
                    a.getCapacidade(), a.getCadenciaMs(), a.getRecargaMs(), dif);
        }
    }

    private final EstandeTiroService estandeService;
    private final ArmaService armaService;

    public JogoController(EstandeTiroService estandeService, ArmaService armaService) {
        this.estandeService = estandeService;
        this.armaService = armaService;
    }

    @GetMapping("/estandes/{id}/jogar")
    public String jogar(@PathVariable Long id, Model model) {
        model.addAttribute("estande", estandeService.buscarPorId(id));
        model.addAttribute("armasJogo", armaService.listarTodas().stream().map(ArmaJogo::de).toList());
        return "estandes/jogo";
    }
}
