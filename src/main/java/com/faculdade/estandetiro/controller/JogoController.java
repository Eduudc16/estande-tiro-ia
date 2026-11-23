package com.faculdade.estandetiro.controller;

import com.faculdade.estandetiro.model.entity.AlvoInimigo;
import com.faculdade.estandetiro.model.entity.Arma;
import com.faculdade.estandetiro.model.entity.EstandeTiro;
import com.faculdade.estandetiro.model.service.AlvoInimigoService;
import com.faculdade.estandetiro.model.service.ArmaService;
import com.faculdade.estandetiro.model.service.EstandeTiroService;
import org.springframework.stereotype.Controller;
import org.springframework.ui.Model;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;

/**
 * Minigame do estande: o fundo fixo é o cenário gerado por IA e a parte dinâmica são os alvos
 * (inimigos do tema e a refém), animados em static/js/jogo.js. Cada inimigo usa a imagem gerada pela IA
 * quando ela existe; senão, o desenho SVG de static/js/alvos.js.
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

    /** Inimigo do tipo do estande: imagem da IA (ou null) e o índice do desenho SVG de reserva. */
    public record InimigoJogo(String nome, String classe, int indice, String imagem) {
        static InimigoJogo de(AlvoInimigo a) {
            return new InimigoJogo(a.getNome(), a.getClasse().getChaveJogo(), a.getIndiceDesenho(),
                    a.temImagem() ? a.getCaminhoImagem() : null);
        }
    }

    private final EstandeTiroService estandeService;
    private final ArmaService armaService;
    private final AlvoInimigoService alvoService;

    public JogoController(EstandeTiroService estandeService, ArmaService armaService,
                          AlvoInimigoService alvoService) {
        this.estandeService = estandeService;
        this.armaService = armaService;
        this.alvoService = alvoService;
    }

    @GetMapping("/estandes/{id}/jogar")
    public String jogar(@PathVariable Long id, Model model) {
        EstandeTiro estande = estandeService.buscarPorId(id);
        AlvoInimigoService.AlvosDoJogo alvos = alvoService.alvosParaJogo(estande.getTipo());
        model.addAttribute("estande", estande);
        model.addAttribute("alvos", alvos);
        model.addAttribute("armasJogo", armaService.listarTodas().stream().map(ArmaJogo::de).toList());
        model.addAttribute("inimigosJogo", alvos.inimigos().stream().map(InimigoJogo::de).toList());
        // imagem da refém (null = usa o desenho da refém)
        model.addAttribute("refemJogo", alvos.refem() != null && alvos.refem().temImagem()
                ? alvos.refem().getCaminhoImagem() : null);
        return "estandes/jogo";
    }
}
