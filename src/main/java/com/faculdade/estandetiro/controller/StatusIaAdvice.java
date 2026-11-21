package com.faculdade.estandetiro.controller;

import com.faculdade.estandetiro.model.service.LimiteGeracoesImagem;
import com.faculdade.estandetiro.model.service.ia.IaTextoService;
import com.faculdade.estandetiro.model.service.ia.ImageService;
import org.springframework.web.bind.annotation.ControllerAdvice;
import org.springframework.web.bind.annotation.ModelAttribute;

/** Disponibiliza em todas as telas o estado da IA (modelos, chaves configuradas e uso do limite). */
@ControllerAdvice
public class StatusIaAdvice {

    public record StatusIa(String modeloTexto, boolean textoConfigurado, String modeloImagem,
                           boolean imagemConfigurada, int geracoesUsadas, int maxGeracoes) {
        public int restantes() {
            return Math.max(0, maxGeracoes - geracoesUsadas);
        }
    }

    private final IaTextoService iaTextoService;
    private final ImageService imageService;
    private final LimiteGeracoesImagem limite;

    public StatusIaAdvice(IaTextoService iaTextoService, ImageService imageService, LimiteGeracoesImagem limite) {
        this.iaTextoService = iaTextoService;
        this.imageService = imageService;
        this.limite = limite;
    }

    @ModelAttribute("statusIa")
    public StatusIa statusIa() {
        return new StatusIa(iaTextoService.getModelo(), iaTextoService.isConfigurado(),
                imageService.getModelo(), imageService.isConfigurado(), limite.getUsadas(), limite.getMaximo());
    }
}
